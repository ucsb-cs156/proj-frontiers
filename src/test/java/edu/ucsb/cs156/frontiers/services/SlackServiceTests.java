package edu.ucsb.cs156.frontiers.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import edu.ucsb.cs156.frontiers.errors.SlackApiException;
import edu.ucsb.cs156.frontiers.models.SlackAuthTestResponse;
import edu.ucsb.cs156.frontiers.models.SlackUser;
import edu.ucsb.cs156.frontiers.services.wiremock.WiremockService;
import edu.ucsb.cs156.frontiers.testconfig.TestConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.client.ExpectedCount;
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

  private static final String USERS_URL = "https://slack.com/api/users.list?limit=200";

  @Test
  void listUsers_singlePage_parsesUsers() {
    String json =
        """
        {
          "ok": true,
          "members": [
            {
              "id": "U01",
              "name": "cgaucho",
              "deleted": false,
              "real_name": "Chris Gaucho",
              "is_bot": false,
              "profile": { "email": "cgaucho@ucsb.edu", "display_name": "chris", "phone": "" }
            },
            { "id": "U02", "name": "frontiers", "is_bot": true, "profile": {} },
            { "id": "U03", "name": "old", "deleted": true },
            { "id": "U04", "name": "new", "is_invited_user": true }
          ],
          "response_metadata": { "next_cursor": "" }
        }
        """;

    mockServer
        .expect(requestTo(USERS_URL))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("Authorization", "Bearer " + TEST_TOKEN))
        .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

    List<SlackUser> result = slackService.listUsers(TEST_TOKEN);

    mockServer.verify();
    List<SlackUser> expected =
        List.of(
            SlackUser.builder()
                .id("U01")
                .name("cgaucho")
                .realName("Chris Gaucho")
                .profile(
                    SlackUser.Profile.builder()
                        .email("cgaucho@ucsb.edu")
                        .displayName("chris")
                        .build())
                .build(),
            SlackUser.builder()
                .id("U02")
                .name("frontiers")
                .bot(true)
                .profile(new SlackUser.Profile())
                .build(),
            SlackUser.builder().id("U03").name("old").deleted(true).build(),
            SlackUser.builder().id("U04").name("new").invitedUser(true).build());
    assertEquals(expected, result);
  }

  @Test
  void listUsers_followsCursor_urlEncoded() {
    mockServer
        .expect(requestTo(USERS_URL))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withSuccess(
                """
                { "ok": true, "members": [ { "id": "U01" } ],
                  "response_metadata": { "next_cursor": "dXNlcjpVMDE=" } }
                """,
                MediaType.APPLICATION_JSON));
    mockServer
        .expect(requestTo(USERS_URL + "&cursor=dXNlcjpVMDE%3D"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("Authorization", "Bearer " + TEST_TOKEN))
        .andRespond(
            withSuccess(
                """
                { "ok": true, "members": [ { "id": "U02" } ] }
                """,
                MediaType.APPLICATION_JSON));

    List<SlackUser> result = slackService.listUsers(TEST_TOKEN);

    mockServer.verify();
    assertEquals(
        List.of(SlackUser.builder().id("U01").build(), SlackUser.builder().id("U02").build()),
        result);
  }

  @Test
  void listUsers_noMembersField_returnsEmptyList() {
    mockServer
        .expect(requestTo(USERS_URL))
        .andRespond(
            withSuccess(
                """
                { "ok": true, "response_metadata": {} }
                """,
                MediaType.APPLICATION_JSON));

    assertEquals(List.of(), slackService.listUsers(TEST_TOKEN));
    mockServer.verify();
  }

  @Test
  void listUsers_stopsAfterMaxPages() {
    mockServer
        .expect(ExpectedCount.once(), requestTo(USERS_URL))
        .andRespond(
            withSuccess(
                """
                { "ok": true, "members": [ { "id": "U01" } ],
                  "response_metadata": { "next_cursor": "again" } }
                """,
                MediaType.APPLICATION_JSON));
    mockServer
        .expect(
            ExpectedCount.times(SlackService.MAX_USER_PAGES - 1),
            requestTo(USERS_URL + "&cursor=again"))
        .andRespond(
            withSuccess(
                """
                { "ok": true, "members": [ { "id": "U01" } ],
                  "response_metadata": { "next_cursor": "again" } }
                """,
                MediaType.APPLICATION_JSON));

    List<SlackUser> result = slackService.listUsers(TEST_TOKEN);

    mockServer.verify();
    assertEquals(50, SlackService.MAX_USER_PAGES);
    assertEquals(50, result.size());
  }

  @Test
  void listUsers_slackReportsError_throwsWithErrorCode() {
    mockServer
        .expect(requestTo(USERS_URL))
        .andRespond(
            withSuccess(
                """
                { "ok": false, "error": "missing_scope" }
                """,
                MediaType.APPLICATION_JSON));

    SlackApiException e =
        assertThrows(SlackApiException.class, () -> slackService.listUsers(TEST_TOKEN));
    assertEquals("missing_scope", e.getMessage());
  }

  @Test
  void listUsers_slackReportsErrorWithoutCode_throws() {
    mockServer
        .expect(requestTo(USERS_URL))
        .andRespond(
            withSuccess(
                """
                { "ok": false }
                """,
                MediaType.APPLICATION_JSON));

    SlackApiException e =
        assertThrows(SlackApiException.class, () -> slackService.listUsers(TEST_TOKEN));
    assertEquals("null", e.getMessage());
  }

  @Test
  void listUsers_serverError_throwsSlackUnreachable() {
    mockServer.expect(requestTo(USERS_URL)).andRespond(withServerError());

    SlackApiException e =
        assertThrows(SlackApiException.class, () -> slackService.listUsers(TEST_TOKEN));
    assertEquals("slack_unreachable", e.getMessage());
  }

  @Test
  void listUsers_errorOnLaterPage_throwsSlackUnreachable() {
    mockServer
        .expect(requestTo(USERS_URL))
        .andRespond(
            withSuccess(
                """
                { "ok": true, "members": [],
                  "response_metadata": { "next_cursor": "next" } }
                """,
                MediaType.APPLICATION_JSON));
    mockServer.expect(requestTo(USERS_URL + "&cursor=next")).andRespond(withServerError());

    SlackApiException e =
        assertThrows(SlackApiException.class, () -> slackService.listUsers(TEST_TOKEN));
    assertEquals("slack_unreachable", e.getMessage());
  }

  @Test
  void listUsers_emptyBody_throwsSlackUnreachable() {
    mockServer.expect(requestTo(USERS_URL)).andRespond(withStatus(HttpStatus.OK));

    SlackApiException e =
        assertThrows(SlackApiException.class, () -> slackService.listUsers(TEST_TOKEN));
    assertEquals("slack_unreachable", e.getMessage());
  }
}
