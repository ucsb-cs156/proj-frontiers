package edu.ucsb.cs156.frontiers.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.annotations.WithInstructorCoursePermissions;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.enums.School;
import edu.ucsb.cs156.frontiers.models.SlackAuthTestResponse;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.services.CanvasApiTokenSecurityService;
import edu.ucsb.cs156.frontiers.services.SlackService;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = SlackController.class)
public class SlackControllerTests extends ControllerTestCase {

  @MockitoBean private CourseRepository courseRepository;
  @MockitoBean private SlackService slackService;
  @MockitoBean private CanvasApiTokenSecurityService tokenSecurityService;

  private static final String TOKEN = "xoxb-1234567890-abcdefghij";
  private static final String MASKED_TOKEN = "xoxb******************ghij";

  private Course.CourseBuilder courseBuilder() {
    return Course.builder()
        .id(1L)
        .courseName("CS156")
        .term("S25")
        .school(School.UCSB)
        .instructorEmail("instructor@ucsb.edu");
  }

  @Test
  @WithInstructorCoursePermissions
  public void getSlackInfo_returnsMaskedTokenAndTeam() throws Exception {
    Course course =
        courseBuilder()
            .slackBotToken("enc:v1:ciphertext")
            .slackTeamId("T12345678")
            .slackTeamName("ucsb-cs156-f26")
            .build();
    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
    when(tokenSecurityService.decrypt("enc:v1:ciphertext")).thenReturn(TOKEN);

    MvcResult response =
        mockMvc
            .perform(get("/api/courses/slack/info").param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    LinkedHashMap<String, String> expected = new LinkedHashMap<>();
    expected.put("courseId", "1");
    expected.put("slackBotToken", MASKED_TOKEN);
    expected.put("slackTeamId", "T12345678");
    expected.put("slackTeamName", "ucsb-cs156-f26");
    assertEquals(mapper.writeValueAsString(expected), response.getResponse().getContentAsString());
  }

  @Test
  @WithInstructorCoursePermissions
  public void getSlackInfo_noTokenStored_returnsEmptyStrings() throws Exception {
    when(courseRepository.findById(1L)).thenReturn(Optional.of(courseBuilder().build()));
    when(tokenSecurityService.decrypt(null)).thenReturn(null);

    MvcResult response =
        mockMvc
            .perform(get("/api/courses/slack/info").param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    LinkedHashMap<String, String> expected = new LinkedHashMap<>();
    expected.put("courseId", "1");
    expected.put("slackBotToken", "");
    expected.put("slackTeamId", "");
    expected.put("slackTeamName", "");
    assertEquals(mapper.writeValueAsString(expected), response.getResponse().getContentAsString());
  }

  @Test
  @WithInstructorCoursePermissions
  public void getSlackInfo_courseDoesNotExist() throws Exception {
    when(courseRepository.findById(1L)).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(get("/api/courses/slack/info").param("courseId", "1"))
            .andExpect(status().isNotFound())
            .andReturn();

    Map<String, Object> json = responseToJson(response);
    assertEquals("EntityNotFoundException", json.get("type"));
    assertEquals("Course with id 1 not found", json.get("message"));
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void getSlackInfo_forbiddenForRegularUser() throws Exception {
    when(courseRepository.findById(1L)).thenReturn(Optional.of(courseBuilder().build()));

    mockMvc
        .perform(get("/api/courses/slack/info").param("courseId", "1"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithInstructorCoursePermissions
  public void updateSlackToken_validToken_isEncryptedAndStored() throws Exception {
    Course course = courseBuilder().build();
    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
    when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));
    when(slackService.authTest(TOKEN))
        .thenReturn(
            SlackAuthTestResponse.builder()
                .ok(true)
                .team("ucsb-cs156-f26")
                .teamId("T12345678")
                .build());
    when(tokenSecurityService.encrypt(TOKEN)).thenReturn("enc:v1:ciphertext");
    when(tokenSecurityService.decrypt("enc:v1:ciphertext")).thenReturn(TOKEN);

    MvcResult response =
        mockMvc
            .perform(
                post("/api/courses/slack/token")
                    .with(csrf())
                    .param("courseId", "1")
                    .param("slackBotToken", "  " + TOKEN + "\n"))
            .andExpect(status().isOk())
            .andReturn();

    ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
    verify(courseRepository).save(captor.capture());
    assertEquals("enc:v1:ciphertext", captor.getValue().getSlackBotToken());
    assertEquals("T12345678", captor.getValue().getSlackTeamId());
    assertEquals("ucsb-cs156-f26", captor.getValue().getSlackTeamName());

    LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
    expected.put("ok", true);
    expected.put("courseId", "1");
    expected.put("slackBotToken", MASKED_TOKEN);
    expected.put("slackTeamId", "T12345678");
    expected.put("slackTeamName", "ucsb-cs156-f26");
    assertEquals(mapper.writeValueAsString(expected), response.getResponse().getContentAsString());
  }

  @Test
  @WithInstructorCoursePermissions
  public void updateSlackToken_invalidToken_isNotStored() throws Exception {
    Course course =
        courseBuilder()
            .slackBotToken("enc:v1:oldCiphertext")
            .slackTeamId("TOLD")
            .slackTeamName("old-team")
            .build();
    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
    when(slackService.authTest(TOKEN))
        .thenReturn(SlackAuthTestResponse.builder().ok(false).error("invalid_auth").build());

    MvcResult response =
        mockMvc
            .perform(
                post("/api/courses/slack/token")
                    .with(csrf())
                    .param("courseId", "1")
                    .param("slackBotToken", TOKEN))
            .andExpect(status().isBadRequest())
            .andReturn();

    verify(courseRepository, never()).save(any());
    verify(tokenSecurityService, never()).encrypt(any());
    assertEquals("enc:v1:oldCiphertext", course.getSlackBotToken());
    assertEquals("TOLD", course.getSlackTeamId());
    assertEquals("old-team", course.getSlackTeamName());

    LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
    expected.put("ok", false);
    expected.put("error", "invalid_auth");
    expected.put("message", SlackController.errorMessage("invalid_auth"));
    assertEquals(mapper.writeValueAsString(expected), response.getResponse().getContentAsString());
  }

  @Test
  @WithInstructorCoursePermissions
  public void updateSlackToken_encryptionKeyNotConfigured_isNotStored() throws Exception {
    Course course = courseBuilder().build();
    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
    when(slackService.authTest(TOKEN))
        .thenReturn(
            SlackAuthTestResponse.builder()
                .ok(true)
                .team("ucsb-cs156-f26")
                .teamId("T12345678")
                .build());
    when(tokenSecurityService.encrypt(TOKEN))
        .thenThrow(new IllegalStateException("Canvas API token encryption key is not configured"));

    MvcResult response =
        mockMvc
            .perform(
                post("/api/courses/slack/token")
                    .with(csrf())
                    .param("courseId", "1")
                    .param("slackBotToken", TOKEN))
            .andExpect(status().isInternalServerError())
            .andReturn();

    verify(courseRepository, never()).save(any());
    assertEquals(null, course.getSlackBotToken());
    assertEquals(null, course.getSlackTeamId());
    assertEquals(null, course.getSlackTeamName());

    LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
    expected.put("ok", false);
    expected.put("error", "encryption_failed");
    expected.put(
        "message",
        "Slack accepted this token, but the server could not encrypt it, so the token was not saved. Ask an administrator to check the TOKEN_ENCRYPTION_KEY environment variable (see docs/slack.md). Details: Canvas API token encryption key is not configured");
    assertEquals(mapper.writeValueAsString(expected), response.getResponse().getContentAsString());
  }

  @Test
  @WithInstructorCoursePermissions
  public void updateSlackToken_courseDoesNotExist() throws Exception {
    when(courseRepository.findById(1L)).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(
                post("/api/courses/slack/token")
                    .with(csrf())
                    .param("courseId", "1")
                    .param("slackBotToken", TOKEN))
            .andExpect(status().isNotFound())
            .andReturn();

    verify(slackService, never()).authTest(any());
    Map<String, Object> json = responseToJson(response);
    assertEquals("EntityNotFoundException", json.get("type"));
    assertEquals("Course with id 1 not found", json.get("message"));
  }

  @Test
  public void maskToken_showsOnlyFirstFourAndLastFour() {
    assertEquals("", SlackController.maskToken(null));
    assertEquals("", SlackController.maskToken(""));
    assertEquals("***", SlackController.maskToken("abc"));
    assertEquals("********", SlackController.maskToken("abcdefgh"));
    assertEquals("abcd*fghi", SlackController.maskToken("abcdefghi"));
    assertEquals(MASKED_TOKEN, SlackController.maskToken(TOKEN));
  }

  @Test
  public void errorMessage_coversKnownSlackErrors() {
    String invalid =
        "Slack rejected this token as invalid. Copy the Bot User OAuth Token (xoxb-...) from your Slack app's OAuth & Permissions page and try again. The token was not saved.";
    String revoked =
        "This Slack token has been revoked or is no longer active. Reinstall the Slack app to the workspace to get a new token. The token was not saved.";
    assertEquals(invalid, SlackController.errorMessage("invalid_auth"));
    assertEquals(invalid, SlackController.errorMessage("not_authed"));
    assertEquals(revoked, SlackController.errorMessage("token_revoked"));
    assertEquals(revoked, SlackController.errorMessage("token_expired"));
    assertEquals(revoked, SlackController.errorMessage("account_inactive"));
    assertEquals(
        "This Slack token is missing a required scope. Add the scope under OAuth & Permissions, reinstall the Slack app to the workspace, and enter the new token. The token was not saved.",
        SlackController.errorMessage("missing_scope"));
    assertEquals(
        "Could not reach Slack to verify the token. Please try again later. The token was not saved.",
        SlackController.errorMessage("slack_unreachable"));
    assertEquals(
        "Slack could not verify this token (ratelimited). The token was not saved.",
        SlackController.errorMessage("ratelimited"));
    assertEquals(
        "Slack could not verify this token (null). The token was not saved.",
        SlackController.errorMessage(null));
  }
}
