package edu.ucsb.cs156.frontiers.services;

import edu.ucsb.cs156.frontiers.models.SlackAuthTestResponse;
import java.time.Duration;
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
}
