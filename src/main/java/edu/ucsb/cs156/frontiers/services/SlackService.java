package edu.ucsb.cs156.frontiers.services;

import edu.ucsb.cs156.frontiers.errors.SlackApiException;
import edu.ucsb.cs156.frontiers.models.SlackAuthTestResponse;
import edu.ucsb.cs156.frontiers.models.SlackUser;
import edu.ucsb.cs156.frontiers.models.SlackUsersListResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
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

  private final RestTemplate restTemplate;

  public SlackService(RestTemplateBuilder builder) {
    this.restTemplate = builder.connectTimeout(CONNECT_TIMEOUT).readTimeout(READ_TIMEOUT).build();
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
}
