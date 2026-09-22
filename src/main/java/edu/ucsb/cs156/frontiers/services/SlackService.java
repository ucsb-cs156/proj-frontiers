package edu.ucsb.cs156.frontiers.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs156.frontiers.errors.SlackApiException;
import edu.ucsb.cs156.frontiers.models.SlackAuthTestResponse;
import edu.ucsb.cs156.frontiers.models.SlackChannel;
import edu.ucsb.cs156.frontiers.models.SlackUser;
import edu.ucsb.cs156.frontiers.models.SlackUsersListResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Service for interacting with the Slack Web API.
 *
 * <p>A Slack bot token (<code>xoxb-...</code>) is already associated with a specific workspace, so
 * the token alone is sufficient to identify where API calls go.
 *
 * <p>Note that the Slack Web API typically returns HTTP 200 even for failures; the <code>ok</code>
 * field of the response indicates success, and <code>error</code> contains an error code (e.g.
 * <code>invalid_auth</code>, <code>missing_scope</code>).
 *
 * <p>Tokens are secrets; they must never be logged.
 */
@Slf4j
@Service
public class SlackService {

  public static final String AUTH_TEST_ENDPOINT = "https://slack.com/api/auth.test";

  public static final String USERS_LIST_ENDPOINT = "https://slack.com/api/users.list?limit=200";

  /**
   * Upper bound on the number of pages of users fetched (200 users per page), so that a misbehaving
   * cursor can never cause an endless loop.
   */
  public static final int MAX_USER_PAGES = 50;

  /** Error code used when Slack could not be reached or returned an unusable response. */
  public static final String SLACK_UNREACHABLE = "slack_unreachable";

  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
  private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

  public static final String SLACK_API = "https://slack.com/api/";

  public static final String CONVERSATIONS_LIST_ENDPOINT =
      SLACK_API + "conversations.list?types=public_channel&exclude_archived=false&limit=200";

  /** Upper bound on the number of pages fetched for any paginated method; see MAX_USER_PAGES. */
  public static final int MAX_PAGES = 50;

  /** Error code used when Slack kept rate limiting a call, even after waiting as it asked. */
  public static final String RATE_LIMITED = "ratelimited";

  /**
   * Slack's error code when a workspace preference forbids the action: for <code>
   * conversations.kick</code>, when the workspace does not let this bot remove members from public
   * channels (by default only Workspace Owners and Admins may).
   */
  public static final String RESTRICTED_ACTION = "restricted_action";

  /** What to tell the instructor when removing a member fails with {@link #RESTRICTED_ACTION}. */
  public static final String REMOVAL_RESTRICTED_ADVICE =
      "Slack does not allow this bot to remove members from public channels, so no more members will be removed in this run. By default only Workspace Owners and Admins may remove members. A Workspace Owner can change this in Slack under Workspace settings, Roles & permissions (on older workspaces: Permissions, Channel Management): set \"People who can remove members from public channels\" to \"Everyone, except guests\". Then run this job again.";

  /** How many times a rate limited (HTTP 429) call is tried again, after waiting. */
  public static final int MAX_RATE_LIMIT_RETRIES = 3;

  /** Longest wait, in seconds, before trying a rate limited call again. */
  public static final long MAX_RETRY_AFTER_SECONDS = 60;

  /** Waits for the given number of milliseconds; replaceable so that tests do not really wait. */
  @FunctionalInterface
  public interface Sleeper {
    void sleep(long milliseconds) throws InterruptedException;
  }

  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private Sleeper sleeper = Thread::sleep;

  public SlackService(RestTemplateBuilder builder) {
    this.restTemplate = builder.connectTimeout(CONNECT_TIMEOUT).readTimeout(READ_TIMEOUT).build();
  }

  public void setSleeper(Sleeper sleeper) {
    this.sleeper = sleeper;
  }

  /**
   * Calls the Slack <code>auth.test</code> method to check whether a bot token is valid.
   *
   * @param token the (plaintext) Slack bot token
   * @return the response from Slack; if <code>ok</code> is true, <code>team</code> and <code>teamId
   *     </code> identify the workspace the token belongs to. If Slack cannot be reached, a response
   *     with <code>ok</code> false and error {@link #SLACK_UNREACHABLE} is returned.
   */
  public SlackAuthTestResponse authTest(String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    HttpEntity<String> entity = new HttpEntity<>(headers);
    try {
      ResponseEntity<SlackAuthTestResponse> response =
          restTemplate.exchange(
              AUTH_TEST_ENDPOINT, HttpMethod.POST, entity, SlackAuthTestResponse.class);
      SlackAuthTestResponse body = response.getBody();
      if (body == null) {
        return SlackAuthTestResponse.builder().ok(false).error(SLACK_UNREACHABLE).build();
      }
      return body;
    } catch (RestClientException e) {
      // Deliberately log only the exception class: the message could echo request details.
      log.warn("Error calling Slack auth.test: {}", e.getClass().getSimpleName());
      return SlackAuthTestResponse.builder().ok(false).error(SLACK_UNREACHABLE).build();
    }
  }

  /**
   * Calls the Slack <code>users.list</code> method (following pagination) to get all members of the
   * workspace the token belongs to. This includes bots, deactivated accounts, and users who have
   * been invited but have not yet signed in; see {@link SlackUser#isActivePerson()}.
   *
   * <p>Requires the <code>users:read</code> scope; emails are only included if the token also has
   * the <code>users:read.email</code> scope.
   *
   * @param token the (plaintext) Slack bot token
   * @return all members of the workspace
   * @throws SlackApiException with the Slack error code (e.g. <code>missing_scope</code>) if Slack
   *     reports an error, or {@link #SLACK_UNREACHABLE} if Slack cannot be reached
   */
  public List<SlackUser> listUsers(String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    HttpEntity<String> entity = new HttpEntity<>(headers);

    List<SlackUser> users = new ArrayList<>();
    String cursor = "";
    for (int page = 0; page < MAX_USER_PAGES; page++) {
      SlackUsersListResponse body = getUsersPage(entity, cursor);
      if (body == null) {
        throw new SlackApiException(SLACK_UNREACHABLE);
      }
      if (!body.getOk()) {
        throw new SlackApiException(String.valueOf(body.getError()));
      }
      if (body.getMembers() != null) {
        users.addAll(body.getMembers());
      }
      cursor = body.nextCursor();
      if (cursor.isEmpty()) {
        return users;
      }
    }
    log.warn("Slack users.list: stopped after {} pages", MAX_USER_PAGES);
    return users;
  }

  private SlackUsersListResponse getUsersPage(HttpEntity<String> entity, String cursor) {
    try {
      if (cursor.isEmpty()) {
        return restTemplate
            .exchange(USERS_LIST_ENDPOINT, HttpMethod.GET, entity, SlackUsersListResponse.class)
            .getBody();
      }
      // cursor is passed as a URI template variable so that it gets URL encoded
      return restTemplate
          .exchange(
              USERS_LIST_ENDPOINT + "&cursor={cursor}",
              HttpMethod.GET,
              entity,
              SlackUsersListResponse.class,
              cursor)
          .getBody();
    } catch (RestClientException e) {
      // Deliberately log only the exception class: the message could echo request details.
      log.warn("Error calling Slack users.list: {}", e.getClass().getSimpleName());
      throw new SlackApiException(SLACK_UNREACHABLE);
    }
  }

  /**
   * Calls the Slack <code>conversations.list</code> method (following pagination) to get all public
   * channels of the workspace, including archived ones (the name of an archived channel is still
   * taken). Requires the <code>channels:read</code> scope.
   *
   * @param token the (plaintext) Slack bot token
   * @return the public channels of the workspace
   * @throws SlackApiException with the Slack error code if the call fails
   */
  public List<SlackChannel> listPublicChannels(String token) {
    List<SlackChannel> channels = new ArrayList<>();
    for (JsonNode page : getAllPages(token, CONVERSATIONS_LIST_ENDPOINT)) {
      for (JsonNode channel : page.path("channels")) {
        channels.add(objectMapper.convertValue(channel, SlackChannel.class));
      }
    }
    return channels;
  }

  /**
   * Calls the Slack <code>conversations.create</code> method to create a public channel; the bot
   * becomes a member of the channel it creates. Requires the <code>channels:manage</code> scope.
   *
   * @param token the (plaintext) Slack bot token
   * @param name name of the channel (lowercase letters, digits, hyphens and underscores)
   * @return the new channel
   * @throws SlackApiException with the Slack error code (e.g. <code>name_taken</code>, <code>
   *     invalid_name_specials</code>) if the call fails
   */
  public SlackChannel createPublicChannel(String token, String name) {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("name", name);
    JsonNode body = post(token, "conversations.create", form);
    return objectMapper.convertValue(body.path("channel"), SlackChannel.class);
  }

  /**
   * Calls the Slack <code>conversations.join</code> method so that the bot is a member of a public
   * channel, which it has to be to invite or remove members. Joining a channel the bot is already
   * in is not an error. Requires the <code>channels:join</code> scope.
   *
   * @param token the (plaintext) Slack bot token
   * @param channelId id of the channel
   * @throws SlackApiException with the Slack error code if the call fails
   */
  public void joinChannel(String token, String channelId) {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("channel", channelId);
    post(token, "conversations.join", form);
  }

  /**
   * Calls the Slack <code>conversations.members</code> method (following pagination). Requires the
   * <code>channels:read</code> scope.
   *
   * @param token the (plaintext) Slack bot token
   * @param channelId id of the channel
   * @return the Slack user ids of the members of the channel
   * @throws SlackApiException with the Slack error code if the call fails
   */
  public List<String> listChannelMembers(String token, String channelId) {
    List<String> members = new ArrayList<>();
    String url = SLACK_API + "conversations.members?limit=200&channel=" + channelId;
    for (JsonNode page : getAllPages(token, url)) {
      for (JsonNode member : page.path("members")) {
        members.add(member.asText());
      }
    }
    return members;
  }

  /**
   * Calls the Slack <code>conversations.invite</code> method to add users to a channel that the bot
   * is a member of. Slack adds either all of the users or none of them. Requires the <code>
   * channels:manage</code> scope.
   *
   * @param token the (plaintext) Slack bot token
   * @param channelId id of the channel
   * @param userIds Slack user ids; at most 1000
   * @throws SlackApiException with the Slack error code (e.g. <code>already_in_channel</code>) if
   *     the call fails
   */
  public void inviteToChannel(String token, String channelId, List<String> userIds) {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("channel", channelId);
    form.add("users", String.join(",", userIds));
    post(token, "conversations.invite", form);
  }

  /**
   * Calls the Slack <code>conversations.kick</code> method to remove a user from a channel that the
   * bot is a member of. Requires the <code>channels:manage</code> scope. Whether the bot is allowed
   * to remove members from public channels is also a workspace setting; if it is not, Slack answers
   * {@link #RESTRICTED_ACTION}, and {@link #REMOVAL_RESTRICTED_ADVICE} says how to fix that.
   *
   * @param token the (plaintext) Slack bot token
   * @param channelId id of the channel
   * @param userId Slack user id
   * @throws SlackApiException with the Slack error code (e.g. <code>restricted_action</code>) if
   *     the call fails
   */
  public void removeFromChannel(String token, String channelId, String userId) {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("channel", channelId);
    form.add("user", userId);
    post(token, "conversations.kick", form);
  }

  private JsonNode post(String token, String method, MultiValueMap<String, String> form) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    // RestTemplate sends a MultiValueMap as application/x-www-form-urlencoded
    return call(SLACK_API + method, HttpMethod.POST, new HttpEntity<>(form, headers), null);
  }

  /** Gets every page of a cursor paginated method; url must already have a query string. */
  private List<JsonNode> getAllPages(String token, String url) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    List<JsonNode> pages = new ArrayList<>();
    String cursor = "";
    for (int page = 0; page < MAX_PAGES; page++) {
      JsonNode body =
          cursor.isEmpty()
              ? call(url, HttpMethod.GET, entity, null)
              : call(url + "&cursor={cursor}", HttpMethod.GET, entity, cursor);
      pages.add(body);
      cursor = body.path("response_metadata").path("next_cursor").asText("");
      if (cursor.isEmpty()) {
        return pages;
      }
    }
    log.warn("Slack: stopped after {} pages", MAX_PAGES);
    return pages;
  }

  /**
   * Makes one call to the Slack Web API. If Slack answers 429 (rate limited), waits for as long as
   * the Retry-After header asks and tries again, a limited number of times.
   *
   * @return the response, which has ok=true
   * @throws SlackApiException with the Slack error code if ok=false, {@link #RATE_LIMITED} if still
   *     rate limited after retrying, or {@link #SLACK_UNREACHABLE} for other failures
   */
  private JsonNode call(String url, HttpMethod httpMethod, HttpEntity<?> entity, String cursor) {
    for (int attempt = 0; ; attempt++) {
      try {
        ResponseEntity<JsonNode> response =
            cursor == null
                ? restTemplate.exchange(url, httpMethod, entity, JsonNode.class)
                : restTemplate.exchange(url, httpMethod, entity, JsonNode.class, cursor);
        JsonNode body = response.getBody();
        if (body == null) {
          throw new SlackApiException(SLACK_UNREACHABLE);
        }
        if (!body.path("ok").asBoolean()) {
          throw new SlackApiException(body.path("error").asText("null"));
        }
        return body;
      } catch (HttpClientErrorException.TooManyRequests e) {
        if (attempt >= MAX_RATE_LIMIT_RETRIES) {
          throw new SlackApiException(RATE_LIMITED);
        }
        waitBeforeRetry(e.getResponseHeaders());
      } catch (RestClientException e) {
        // Deliberately log only the exception class: the message could echo request details.
        log.warn("Error calling Slack: {}", e.getClass().getSimpleName());
        throw new SlackApiException(SLACK_UNREACHABLE);
      }
    }
  }

  private void waitBeforeRetry(HttpHeaders responseHeaders) {
    long seconds = 1;
    // (the headers of an exception are declared nullable, though a 429 response always has some)
    String retryAfter =
        Optional.ofNullable(responseHeaders)
            .map(headers -> headers.getFirst("Retry-After"))
            .orElse(null);
    if (retryAfter != null) {
      try {
        seconds = Long.parseLong(retryAfter.strip());
      } catch (NumberFormatException e) {
        seconds = 1;
      }
    }
    seconds = Math.max(1, Math.min(seconds, MAX_RETRY_AFTER_SECONDS));
    try {
      sleeper.sleep(seconds * 1000);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new SlackApiException(SLACK_UNREACHABLE);
    }
  }
}
