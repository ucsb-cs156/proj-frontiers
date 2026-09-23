package edu.ucsb.cs156.frontiers.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import edu.ucsb.cs156.frontiers.errors.SlackApiException;
import edu.ucsb.cs156.frontiers.models.SlackAuthTestResponse;
import edu.ucsb.cs156.frontiers.models.SlackChannel;
import edu.ucsb.cs156.frontiers.models.SlackUser;
import edu.ucsb.cs156.frontiers.services.wiremock.WiremockService;
import edu.ucsb.cs156.frontiers.testconfig.TestConfig;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

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
    String json =
        """
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

  private static final String CHANNELS_URL =
      "https://slack.com/api/conversations.list?types=public_channel&exclude_archived=false&limit=200";

  private final List<Long> sleeps = new ArrayList<>();

  @BeforeEach
  void doNotReallySleep() {
    sleeps.clear();
    slackService.setSleeper(sleeps::add);
  }

  @Test
  void listPublicChannels_followsCursor_andParsesChannels() {
    mockServer
        .expect(requestTo(CHANNELS_URL))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("Authorization", "Bearer " + TEST_TOKEN))
        .andRespond(
            withSuccess(
                """
                { "ok": true,
                  "channels": [
                    { "id": "C01", "name": "general", "is_archived": false, "is_channel": true },
                    { "id": "C02", "name": "old", "is_archived": true } ],
                  "response_metadata": { "next_cursor": "bmV4dA==" } }
                """,
                MediaType.APPLICATION_JSON));
    mockServer
        .expect(requestTo(CHANNELS_URL + "&cursor=bmV4dA%3D%3D"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("Authorization", "Bearer " + TEST_TOKEN))
        .andRespond(
            withSuccess(
                """
                { "ok": true, "channels": [ { "id": "C03", "name": "sec-0100" } ],
                  "response_metadata": { "next_cursor": "" } }
                """,
                MediaType.APPLICATION_JSON));

    List<SlackChannel> result = slackService.listPublicChannels(TEST_TOKEN);

    mockServer.verify();
    assertEquals(
        List.of(
            SlackChannel.builder().id("C01").name("general").build(),
            SlackChannel.builder().id("C02").name("old").archived(true).build(),
            SlackChannel.builder().id("C03").name("sec-0100").build()),
        result);
  }

  @Test
  void listPublicChannels_stopsAfterMaxPages() {
    String page =
        """
        { "ok": true, "channels": [ { "id": "C01", "name": "general" } ],
          "response_metadata": { "next_cursor": "again" } }
        """;
    mockServer
        .expect(ExpectedCount.once(), requestTo(CHANNELS_URL))
        .andRespond(withSuccess(page, MediaType.APPLICATION_JSON));
    mockServer
        .expect(
            ExpectedCount.times(SlackService.MAX_PAGES - 1),
            requestTo(CHANNELS_URL + "&cursor=again"))
        .andRespond(withSuccess(page, MediaType.APPLICATION_JSON));

    List<SlackChannel> result = slackService.listPublicChannels(TEST_TOKEN);

    mockServer.verify();
    assertEquals(50, SlackService.MAX_PAGES);
    assertEquals(50, result.size());
  }

  @Test
  void createPublicChannel_postsName_andReturnsChannel() {
    mockServer
        .expect(requestTo("https://slack.com/api/conversations.create"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer " + TEST_TOKEN))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
        .andExpect(content().formData(form("name", "sec-0100")))
        .andRespond(
            withSuccess(
                """
                { "ok": true, "channel": { "id": "C100", "name": "sec-0100", "is_archived": false } }
                """,
                MediaType.APPLICATION_JSON));

    SlackChannel result = slackService.createPublicChannel(TEST_TOKEN, "sec-0100");

    mockServer.verify();
    assertEquals(SlackChannel.builder().id("C100").name("sec-0100").build(), result);
  }

  @Test
  void createPublicChannel_nameTaken_throwsWithErrorCode() {
    mockServer
        .expect(requestTo("https://slack.com/api/conversations.create"))
        .andRespond(
            withSuccess(
                """
                { "ok": false, "error": "name_taken" }
                """,
                MediaType.APPLICATION_JSON));

    SlackApiException e =
        assertThrows(
            SlackApiException.class,
            () -> slackService.createPublicChannel(TEST_TOKEN, "sec-0100"));
    assertEquals("name_taken", e.getMessage());
  }

  @Test
  void joinChannel_postsChannel() {
    mockServer
        .expect(requestTo("https://slack.com/api/conversations.join"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer " + TEST_TOKEN))
        .andExpect(content().formData(form("channel", "C100")))
        .andRespond(
            withSuccess(
                """
                { "ok": true, "warning": "already_in_channel" }
                """,
                MediaType.APPLICATION_JSON));

    slackService.joinChannel(TEST_TOKEN, "C100");

    mockServer.verify();
  }

  @Test
  void listChannelMembers_followsCursor() {
    String url = "https://slack.com/api/conversations.members?limit=200&channel=C100";
    mockServer
        .expect(requestTo(url))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("Authorization", "Bearer " + TEST_TOKEN))
        .andRespond(
            withSuccess(
                """
                { "ok": true, "members": [ "U01", "U02" ],
                  "response_metadata": { "next_cursor": "next" } }
                """,
                MediaType.APPLICATION_JSON));
    mockServer
        .expect(requestTo(url + "&cursor=next"))
        .andRespond(
            withSuccess(
                """
                { "ok": true, "members": [ "U03" ] }
                """,
                MediaType.APPLICATION_JSON));

    assertEquals(List.of("U01", "U02", "U03"), slackService.listChannelMembers(TEST_TOKEN, "C100"));
    mockServer.verify();
  }

  @Test
  void inviteToChannel_postsCommaSeparatedUsers() {
    MultiValueMap<String, String> expectedForm = form("channel", "C100");
    expectedForm.add("users", "U01,U02");
    mockServer
        .expect(requestTo("https://slack.com/api/conversations.invite"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer " + TEST_TOKEN))
        .andExpect(content().formData(expectedForm))
        .andRespond(
            withSuccess(
                """
                { "ok": true }
                """,
                MediaType.APPLICATION_JSON));

    slackService.inviteToChannel(TEST_TOKEN, "C100", List.of("U01", "U02"));

    mockServer.verify();
  }

  @Test
  void removeFromChannel_postsChannelAndUser() {
    MultiValueMap<String, String> expectedForm = form("channel", "C100");
    expectedForm.add("user", "U01");
    mockServer
        .expect(requestTo("https://slack.com/api/conversations.kick"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer " + TEST_TOKEN))
        .andExpect(content().formData(expectedForm))
        .andRespond(
            withSuccess(
                """
                { "ok": true }
                """,
                MediaType.APPLICATION_JSON));

    slackService.removeFromChannel(TEST_TOKEN, "C100", "U01");

    mockServer.verify();
  }

  @Test
  void removeFromChannel_errorWithoutCode_throws() {
    mockServer
        .expect(requestTo("https://slack.com/api/conversations.kick"))
        .andRespond(
            withSuccess(
                """
                { "ok": false }
                """,
                MediaType.APPLICATION_JSON));

    SlackApiException e =
        assertThrows(
            SlackApiException.class, () -> slackService.removeFromChannel(TEST_TOKEN, "C", "U"));
    assertEquals("null", e.getMessage());
  }

  @Test
  void call_emptyBody_throwsSlackUnreachable() {
    mockServer
        .expect(requestTo("https://slack.com/api/conversations.join"))
        .andRespond(withStatus(HttpStatus.OK));

    SlackApiException e =
        assertThrows(SlackApiException.class, () -> slackService.joinChannel(TEST_TOKEN, "C100"));
    assertEquals("slack_unreachable", e.getMessage());
  }

  @Test
  void call_serverError_throwsSlackUnreachable_withoutRetrying() {
    mockServer
        .expect(ExpectedCount.once(), requestTo("https://slack.com/api/conversations.join"))
        .andRespond(withServerError());

    SlackApiException e =
        assertThrows(SlackApiException.class, () -> slackService.joinChannel(TEST_TOKEN, "C100"));
    assertEquals("slack_unreachable", e.getMessage());
    mockServer.verify();
    assertEquals(List.of(), sleeps);
  }

  private static HttpHeaders retryAfter(String value) {
    HttpHeaders headers = new HttpHeaders();
    headers.add("Retry-After", value);
    return headers;
  }

  @Test
  void call_rateLimited_waitsAsLongAsSlackAsks_thenTriesAgain() {
    String url = "https://slack.com/api/conversations.join";
    mockServer
        .expect(requestTo(url))
        .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).headers(retryAfter(" 7 ")));
    // capped at 60 seconds
    mockServer
        .expect(requestTo(url))
        .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).headers(retryAfter("3600")));
    // at least 1 second
    mockServer
        .expect(requestTo(url))
        .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).headers(retryAfter("0")));
    mockServer
        .expect(requestTo(url))
        .andRespond(
            withSuccess(
                """
                { "ok": true }
                """,
                MediaType.APPLICATION_JSON));

    slackService.joinChannel(TEST_TOKEN, "C100");

    mockServer.verify();
    assertEquals(List.of(7000L, 60000L, 1000L), sleeps);
  }

  @Test
  void call_rateLimited_withoutUsableRetryAfter_waitsOneSecond() {
    String url = "https://slack.com/api/conversations.join";
    mockServer.expect(requestTo(url)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
    mockServer
        .expect(requestTo(url))
        .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).headers(retryAfter("soon")));
    mockServer
        .expect(requestTo(url))
        .andRespond(
            withSuccess(
                """
                { "ok": true }
                """,
                MediaType.APPLICATION_JSON));

    slackService.joinChannel(TEST_TOKEN, "C100");

    mockServer.verify();
    assertEquals(List.of(1000L, 1000L), sleeps);
  }

  @Test
  void call_stillRateLimitedAfterRetries_throwsRatelimited() {
    mockServer
        .expect(
            ExpectedCount.times(SlackService.MAX_RATE_LIMIT_RETRIES + 1),
            requestTo("https://slack.com/api/conversations.join"))
        .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).headers(retryAfter("2")));

    SlackApiException e =
        assertThrows(SlackApiException.class, () -> slackService.joinChannel(TEST_TOKEN, "C100"));

    assertEquals("ratelimited", e.getMessage());
    mockServer.verify();
    assertEquals(3, SlackService.MAX_RATE_LIMIT_RETRIES);
    assertEquals(List.of(2000L, 2000L, 2000L), sleeps);
  }

  @Test
  void call_interruptedWhileWaiting_throwsSlackUnreachable_andKeepsInterruptFlag() {
    slackService.setSleeper(
        ms -> {
          throw new InterruptedException();
        });
    mockServer
        .expect(ExpectedCount.once(), requestTo("https://slack.com/api/conversations.join"))
        .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

    SlackApiException e =
        assertThrows(SlackApiException.class, () -> slackService.joinChannel(TEST_TOKEN, "C100"));

    assertEquals("slack_unreachable", e.getMessage());
    assertTrue(Thread.interrupted(), "interrupt flag should have been set again");
    mockServer.verify();
  }

  private static MultiValueMap<String, String> form(String key, String value) {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add(key, value);
    return form;
  }
}
