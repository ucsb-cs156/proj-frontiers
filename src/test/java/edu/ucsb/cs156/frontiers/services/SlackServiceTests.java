package edu.ucsb.cs156.frontiers.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import edu.ucsb.cs156.frontiers.models.SlackAuthTestResponse;
import edu.ucsb.cs156.frontiers.services.wiremock.WiremockService;
import edu.ucsb.cs156.frontiers.testconfig.TestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.client.MockRestServiceServer;

@RestClientTest(SlackService.class)
@Import(TestConfig.class)
public class SlackServiceTests {

  @Autowired private SlackService slackService;

  @Autowired private MockRestServiceServer mockServer;

  @MockitoBean private WiremockService wiremockService;

  private static final String TEST_TOKEN = "xoxb-test-token";

  @Test
  void authTest_validToken_returnsTeamInfo() {
    String json =
        """
        {
          "ok": true,
          "url": "https://ucsb-cs156-f26.slack.com/",
          "team": "ucsb-cs156-f26",
          "user": "frontiers",
          "team_id": "T12345678",
          "user_id": "U12345678",
          "bot_id": "B12345678"
        }
        """;

    mockServer
        .expect(requestTo("https://slack.com/api/auth.test"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer " + TEST_TOKEN))
        .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

    SlackAuthTestResponse result = slackService.authTest(TEST_TOKEN);

    mockServer.verify();
    SlackAuthTestResponse expected =
        SlackAuthTestResponse.builder()
            .ok(true)
            .url("https://ucsb-cs156-f26.slack.com/")
            .team("ucsb-cs156-f26")
            .teamId("T12345678")
            .build();
    assertEquals(expected, result);
  }

  @Test
  void authTest_invalidToken_returnsError() {
    String json = """
        { "ok": false, "error": "invalid_auth" }
        """;

    mockServer
        .expect(requestTo("https://slack.com/api/auth.test"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer " + TEST_TOKEN))
        .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

    SlackAuthTestResponse result = slackService.authTest(TEST_TOKEN);

    mockServer.verify();
    assertEquals(SlackAuthTestResponse.builder().ok(false).error("invalid_auth").build(), result);
  }

  @Test
  void authTest_slackError_returnsSlackUnreachable() {
    mockServer
        .expect(requestTo("https://slack.com/api/auth.test"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withServerError());

    SlackAuthTestResponse result = slackService.authTest(TEST_TOKEN);

    mockServer.verify();
    assertEquals(
        SlackAuthTestResponse.builder().ok(false).error("slack_unreachable").build(), result);
  }

  @Test
  void authTest_emptyBody_returnsSlackUnreachable() {
    mockServer
        .expect(requestTo("https://slack.com/api/auth.test"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withStatus(HttpStatus.OK));

    SlackAuthTestResponse result = slackService.authTest(TEST_TOKEN);

    mockServer.verify();
    assertEquals(
        SlackAuthTestResponse.builder().ok(false).error("slack_unreachable").build(), result);
  }
}
