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
import edu.ucsb.cs156.frontiers.entities.CourseOption;
import edu.ucsb.cs156.frontiers.entities.CourseStaff;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.enums.RosterStatus;
import edu.ucsb.cs156.frontiers.enums.School;
import edu.ucsb.cs156.frontiers.errors.SlackApiException;
import edu.ucsb.cs156.frontiers.jobs.SetupSectionSlackChannelsJob;
import edu.ucsb.cs156.frontiers.models.SlackAuthTestResponse;
import edu.ucsb.cs156.frontiers.models.SlackUser;
import edu.ucsb.cs156.frontiers.repositories.CourseOptionRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseStaffRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.repositories.SectionRepository;
import edu.ucsb.cs156.frontiers.services.CanvasApiTokenSecurityService;
import edu.ucsb.cs156.frontiers.services.SlackService;
import edu.ucsb.cs156.jobs.entities.Job;
import edu.ucsb.cs156.jobs.services.JobService;
import java.util.LinkedHashMap;
import java.util.List;
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
  @MockitoBean private RosterStudentRepository rosterStudentRepository;
  @MockitoBean private CourseStaffRepository courseStaffRepository;
  @MockitoBean private SectionRepository sectionRepository;
  @MockitoBean private CourseOptionRepository courseOptionRepository;
  @MockitoBean private JobService jobService;
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
            .slackTeamUrl("https://ucsb-cs156-f26.slack.com/")
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
    expected.put("slackTeamUrl", "https://ucsb-cs156-f26.slack.com/");
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
    expected.put("slackTeamUrl", "");
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
                .url("https://ucsb-cs156-f26.slack.com/")
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
    assertEquals("https://ucsb-cs156-f26.slack.com/", captor.getValue().getSlackTeamUrl());

    LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
    expected.put("ok", true);
    expected.put("courseId", "1");
    expected.put("slackBotToken", MASKED_TOKEN);
    expected.put("slackTeamId", "T12345678");
    expected.put("slackTeamName", "ucsb-cs156-f26");
    expected.put("slackTeamUrl", "https://ucsb-cs156-f26.slack.com/");
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

  private static SlackUser slackUser(String id, String email) {
    return SlackUser.builder()
        .id(id)
        .name("name-" + id)
        .realName("Real " + id)
        .profile(SlackUser.Profile.builder().email(email).displayName("display-" + id).build())
        .build();
  }

  /** Course with a stored token, for which decrypt is mocked. */
  private Course courseWithToken() {
    Course course = courseBuilder().slackBotToken("enc:v1:ciphertext").build();
    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
    when(tokenSecurityService.decrypt("enc:v1:ciphertext")).thenReturn(TOKEN);
    return course;
  }

  @Test
  @WithInstructorCoursePermissions
  public void getSlackUsers_listsActivePeopleWithCourseRole() throws Exception {
    courseWithToken();

    SlackUser noProfile = SlackUser.builder().id("U08").name("noprofile").build();
    SlackUser bot = slackUser("U09", "student@ucsb.edu");
    bot.setBot(true);
    SlackUser slackbot = slackUser("USLACKBOT", null);
    SlackUser deactivated = slackUser("U10", "student@ucsb.edu");
    deactivated.setDeleted(true);
    SlackUser invited = slackUser("U11", "student@ucsb.edu");
    invited.setInvitedUser(true);

    when(slackService.listUsers(TOKEN))
        .thenReturn(
            List.of(
                slackUser("U01", "Instructor@UCSB.edu"),
                slackUser("U02", "staff@ucsb.edu"),
                slackUser("U03", " student@umail.ucsb.edu "),
                slackUser("U04", "dropped@ucsb.edu"),
                slackUser("U05", "stranger@example.org"),
                slackUser("U06", "studentandstaff@ucsb.edu"),
                slackUser("U07", null),
                noProfile,
                bot,
                slackbot,
                deactivated,
                invited));

    when(rosterStudentRepository
            .findByCourseIdAndRosterStatusInOrderByFirstNameAscLastNameAscIgnoreCase(
                1L, List.of(RosterStatus.ROSTER, RosterStatus.MANUAL)))
        .thenReturn(
            List.of(
                RosterStudent.builder()
                    .email("student@ucsb.edu")
                    .rosterStatus(RosterStatus.ROSTER)
                    .build(),
                RosterStudent.builder()
                    .email("studentandstaff@ucsb.edu")
                    .rosterStatus(RosterStatus.MANUAL)
                    .build(),
                RosterStudent.builder()
                    .email("instructor@ucsb.edu")
                    .rosterStatus(RosterStatus.MANUAL)
                    .build(),
                RosterStudent.builder().email(null).rosterStatus(RosterStatus.ROSTER).build()));
    when(courseStaffRepository.findByCourseId(1L))
        .thenReturn(
            List.of(
                CourseStaff.builder().email("STAFF@ucsb.edu").build(),
                CourseStaff.builder().email("studentandstaff@ucsb.edu").build(),
                CourseStaff.builder().email("instructor@ucsb.edu").build(),
                CourseStaff.builder().email(null).build()));

    MvcResult response =
        mockMvc
            .perform(get("/api/courses/slack/users").param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    // only students with status ROSTER or MANUAL are asked for; dropped@ucsb.edu is in Slack, but
    // as a dropped student is not among them, and so has no course role
    verify(rosterStudentRepository, never()).findByCourseId(any());

    List<SlackController.SlackUserView> expected =
        List.of(
            new SlackController.SlackUserView(
                "U01", "name-U01", "Real U01", "display-U01", "Instructor@UCSB.edu", "INSTRUCTOR"),
            new SlackController.SlackUserView(
                "U02", "name-U02", "Real U02", "display-U02", "staff@ucsb.edu", "STAFF"),
            new SlackController.SlackUserView(
                "U03",
                "name-U03",
                "Real U03",
                "display-U03",
                " student@umail.ucsb.edu ",
                "STUDENT"),
            new SlackController.SlackUserView(
                "U04", "name-U04", "Real U04", "display-U04", "dropped@ucsb.edu", "NONE"),
            new SlackController.SlackUserView(
                "U05", "name-U05", "Real U05", "display-U05", "stranger@example.org", "NONE"),
            new SlackController.SlackUserView(
                "U06", "name-U06", "Real U06", "display-U06", "studentandstaff@ucsb.edu", "STAFF"),
            new SlackController.SlackUserView(
                "U07", "name-U07", "Real U07", "display-U07", null, "NONE"),
            new SlackController.SlackUserView("U08", "noprofile", null, null, null, "NONE"));
    assertEquals(mapper.writeValueAsString(expected), response.getResponse().getContentAsString());
  }

  @Test
  @WithInstructorCoursePermissions
  public void getSlackUsers_courseWithoutInstructorEmail() throws Exception {
    Course course = courseBuilder().instructorEmail(null).slackBotToken("enc:v1:x").build();
    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
    when(tokenSecurityService.decrypt("enc:v1:x")).thenReturn(TOKEN);
    when(slackService.listUsers(TOKEN)).thenReturn(List.of(slackUser("U01", "a@ucsb.edu")));
    when(rosterStudentRepository
            .findByCourseIdAndRosterStatusInOrderByFirstNameAscLastNameAscIgnoreCase(
                1L, List.of(RosterStatus.ROSTER, RosterStatus.MANUAL)))
        .thenReturn(List.of());
    when(courseStaffRepository.findByCourseId(1L)).thenReturn(List.of());

    MvcResult response =
        mockMvc
            .perform(get("/api/courses/slack/users").param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    List<SlackController.SlackUserView> expected =
        List.of(
            new SlackController.SlackUserView(
                "U01", "name-U01", "Real U01", "display-U01", "a@ucsb.edu", "NONE"));
    assertEquals(mapper.writeValueAsString(expected), response.getResponse().getContentAsString());
  }

  @Test
  @WithInstructorCoursePermissions
  public void getSlackUsers_noTokenStored_isBadRequest() throws Exception {
    when(courseRepository.findById(1L)).thenReturn(Optional.of(courseBuilder().build()));
    when(tokenSecurityService.decrypt(null)).thenReturn(null);

    MvcResult response =
        mockMvc
            .perform(get("/api/courses/slack/users").param("courseId", "1"))
            .andExpect(status().isBadRequest())
            .andReturn();

    verify(slackService, never()).listUsers(any());
    Map<String, Object> json = responseToJson(response);
    assertEquals("IllegalArgumentException", json.get("type"));
    assertEquals(
        "No Slack token has been set for this course; enter one on the Settings tab.",
        json.get("message"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void getMissingMembers_emptyTokenStored_isBadRequest() throws Exception {
    when(courseRepository.findById(1L))
        .thenReturn(Optional.of(courseBuilder().slackBotToken("").build()));
    when(tokenSecurityService.decrypt("")).thenReturn("");

    mockMvc
        .perform(get("/api/courses/slack/missing").param("courseId", "1"))
        .andExpect(status().isBadRequest());

    verify(slackService, never()).listUsers(any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void getSlackUsers_courseDoesNotExist() throws Exception {
    when(courseRepository.findById(1L)).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(get("/api/courses/slack/users").param("courseId", "1"))
            .andExpect(status().isNotFound())
            .andReturn();

    Map<String, Object> json = responseToJson(response);
    assertEquals("EntityNotFoundException", json.get("type"));
    assertEquals("Course with id 1 not found", json.get("message"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void getMissingMembers_courseDoesNotExist() throws Exception {
    when(courseRepository.findById(1L)).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(get("/api/courses/slack/missing").param("courseId", "1"))
            .andExpect(status().isNotFound())
            .andReturn();

    Map<String, Object> json = responseToJson(response);
    assertEquals("EntityNotFoundException", json.get("type"));
    assertEquals("Course with id 1 not found", json.get("message"));
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void slackUsersAndMissing_forbiddenForRegularUser() throws Exception {
    when(courseRepository.findById(1L)).thenReturn(Optional.of(courseBuilder().build()));

    mockMvc
        .perform(get("/api/courses/slack/users").param("courseId", "1"))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(get("/api/courses/slack/missing").param("courseId", "1"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithInstructorCoursePermissions
  public void getSlackUsers_slackError_isBadGateway() throws Exception {
    courseWithToken();
    when(slackService.listUsers(TOKEN)).thenThrow(new SlackApiException("missing_scope"));

    MvcResult response =
        mockMvc
            .perform(get("/api/courses/slack/users").param("courseId", "1"))
            .andExpect(status().isBadGateway())
            .andReturn();

    LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
    expected.put("ok", false);
    expected.put("error", "missing_scope");
    expected.put(
        "message",
        "This Slack token is missing a required scope. Add the scope under OAuth & Permissions, reinstall the Slack app to the workspace, and enter the new token.");
    assertEquals(mapper.writeValueAsString(expected), response.getResponse().getContentAsString());
  }

  @Test
  @WithInstructorCoursePermissions
  public void getMissingMembers_listsStaffThenStudentsNotActiveInSlack() throws Exception {
    courseWithToken();

    SlackUser bot = slackUser("U05", "botonly@ucsb.edu");
    bot.setBot(true);
    SlackUser invited = slackUser("U06", "Invited@ucsb.edu");
    invited.setInvitedUser(true);
    SlackUser deactivated = slackUser("U07", "deactivated@ucsb.edu");
    deactivated.setDeleted(true);
    // same person: an active account listed before a deactivated one
    SlackUser oldAccount = slackUser("U08", "activestudent@ucsb.edu");
    oldAccount.setDeleted(true);
    // same person: an invitation listed before a deactivated account
    SlackUser reinvited = slackUser("U09", "reinvited@ucsb.edu");
    reinvited.setInvitedUser(true);
    SlackUser reinvitedOldAccount = slackUser("U10", "reinvited@ucsb.edu");
    reinvitedOldAccount.setDeleted(true);
    // a deactivated account that was also never accepted counts as deactivated
    SlackUser deletedInvitation = slackUser("U11", "deletedinvitation@ucsb.edu");
    deletedInvitation.setDeleted(true);
    deletedInvitation.setInvitedUser(true);

    when(slackService.listUsers(TOKEN))
        .thenReturn(
            List.of(
                slackUser("U01", "activestaff@ucsb.edu"),
                slackUser("U02", "ActiveStudent@umail.ucsb.edu"),
                slackUser("U03", null),
                SlackUser.builder().id("U04").build(),
                bot,
                invited,
                deactivated,
                oldAccount,
                reinvited,
                reinvitedOldAccount,
                deletedInvitation));

    when(courseStaffRepository.findByCourseId(1L))
        .thenReturn(
            List.of(
                CourseStaff.builder()
                    .firstName("Active")
                    .lastName("Staff")
                    .email("activestaff@ucsb.edu")
                    .build(),
                CourseStaff.builder()
                    .firstName("Missing")
                    .lastName("Staff")
                    .email("missingstaff@ucsb.edu")
                    .build(),
                CourseStaff.builder().firstName("NoEmail").lastName("Staff").email(null).build()));
    when(rosterStudentRepository
            .findByCourseIdAndRosterStatusInOrderByFirstNameAscLastNameAscIgnoreCase(
                1L, List.of(RosterStatus.ROSTER, RosterStatus.MANUAL)))
        .thenReturn(
            List.of(
                student("Active", "activestudent@ucsb.edu", RosterStatus.ROSTER),
                student("Missing", "missingstudent@ucsb.edu", RosterStatus.ROSTER),
                student("BotOnly", "botonly@ucsb.edu", RosterStatus.MANUAL),
                student("Invited", "invited@ucsb.edu", RosterStatus.ROSTER),
                student("Deactivated", "deactivated@ucsb.edu", RosterStatus.ROSTER),
                student("Reinvited", "reinvited@ucsb.edu", RosterStatus.ROSTER),
                student("DeletedInvitation", "deletedinvitation@ucsb.edu", RosterStatus.ROSTER),
                student("NoEmail", null, RosterStatus.ROSTER)));

    MvcResult response =
        mockMvc
            .perform(get("/api/courses/slack/missing").param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    // only students with status ROSTER or MANUAL are asked for, so dropped students are not listed
    verify(rosterStudentRepository, never()).findByCourseId(any());

    List<SlackController.SlackMissingMemberView> expected =
        List.of(
            missing("STAFF", "Missing", "Staff", "missingstaff@ucsb.edu", "NOT_IN_SLACK"),
            missing("STAFF", "NoEmail", "Staff", null, "NOT_IN_SLACK"),
            missing("STUDENT", "Missing", "Student", "missingstudent@ucsb.edu", "NOT_IN_SLACK"),
            missing("STUDENT", "BotOnly", "Student", "botonly@ucsb.edu", "NOT_IN_SLACK"),
            missing("STUDENT", "Invited", "Student", "invited@ucsb.edu", "INVITED"),
            missing("STUDENT", "Deactivated", "Student", "deactivated@ucsb.edu", "DEACTIVATED"),
            missing("STUDENT", "Reinvited", "Student", "reinvited@ucsb.edu", "INVITED"),
            missing(
                "STUDENT",
                "DeletedInvitation",
                "Student",
                "deletedinvitation@ucsb.edu",
                "DEACTIVATED"),
            missing("STUDENT", "NoEmail", "Student", null, "NOT_IN_SLACK"));
    assertEquals(mapper.writeValueAsString(expected), response.getResponse().getContentAsString());
  }

  private static RosterStudent student(String firstName, String email, RosterStatus status) {
    return RosterStudent.builder()
        .firstName(firstName)
        .lastName("Student")
        .email(email)
        .rosterStatus(status)
        .build();
  }

  private static SlackController.SlackMissingMemberView missing(
      String role, String firstName, String lastName, String email, String status) {
    return new SlackController.SlackMissingMemberView(role, firstName, lastName, email, status);
  }

  @Test
  public void slackTeamUrl_prefersStoredUrl_thenFallsBackToTeamId() {
    assertEquals(
        "https://ucsb-cs156-f26.slack.com/",
        SlackController.slackTeamUrl(
            Course.builder()
                .slackTeamUrl("https://ucsb-cs156-f26.slack.com/")
                .slackTeamId("T12345678")
                .build()));
    assertEquals(
        "https://app.slack.com/client/T12345678",
        SlackController.slackTeamUrl(Course.builder().slackTeamId("T12345678").build()));
    assertEquals(
        "https://app.slack.com/client/T12345678",
        SlackController.slackTeamUrl(
            Course.builder().slackTeamUrl("").slackTeamId("T12345678").build()));
    assertEquals("", SlackController.slackTeamUrl(Course.builder().build()));
    assertEquals("", SlackController.slackTeamUrl(Course.builder().slackTeamId("").build()));
  }

  @Test
  public void describeError_hasNoTokenNotSavedSuffix() {
    assertEquals(
        "Could not reach Slack. Please try again later.",
        SlackController.describeError("slack_unreachable"));
    assertEquals(
        "Slack is limiting requests from this app. Please try again in a minute.",
        SlackController.describeError("ratelimited"));
  }

  private void courseOption(String option, Boolean enabled) {
    when(courseOptionRepository.findByCourseIdAndOption(1L, option))
        .thenReturn(
            enabled == null
                ? Optional.empty()
                : Optional.of(
                    CourseOption.builder().courseId(1L).option(option).enabled(enabled).build()));
  }

  @Test
  @WithInstructorCoursePermissions
  public void setupSectionChannels_launchesJobScopedToTheCourse() throws Exception {
    Course course = courseWithToken();
    courseOption("SLACK_INTEGRATION", true);
    courseOption("TRANSLATE_SECTIONS", true);
    Job launched = Job.builder().id(17L).status("running").build();
    when(jobService.runAsJob(any(SetupSectionSlackChannelsJob.class))).thenReturn(launched);

    MvcResult response =
        mockMvc
            .perform(post("/api/courses/slack/sectionChannels").with(csrf()).param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    assertEquals(mapper.writeValueAsString(launched), response.getResponse().getContentAsString());

    ArgumentCaptor<SetupSectionSlackChannelsJob> captor =
        ArgumentCaptor.forClass(SetupSectionSlackChannelsJob.class);
    verify(jobService).runAsJob(captor.capture());
    SetupSectionSlackChannelsJob job = captor.getValue();
    assertEquals("course", job.getScopeType());
    assertEquals(1L, job.getScopeId());

    // the job was given everything it needs: running it reaches Slack with the decrypted token
    when(slackService.listUsers(TOKEN))
        .thenReturn(List.of(slackUser("U01", "instructor@ucsb.edu")));
    when(slackService.listPublicChannels(TOKEN)).thenReturn(List.of());
    when(sectionRepository.findByCourseIdOrderBySectionAsc(1L)).thenReturn(List.of());
    when(rosterStudentRepository.findByCourseIdOrderByFirstNameAscLastNameAscIgnoreCase(1L))
        .thenReturn(List.of());
    when(courseStaffRepository.findByCourseId(1L)).thenReturn(List.of());
    Job record = Job.builder().build();
    job.accept(new edu.ucsb.cs156.jobs.services.JobContext(null, record));
    verify(slackService).listUsers(TOKEN);
    verify(slackService).listPublicChannels(TOKEN);
    verify(sectionRepository).findByCourseIdOrderBySectionAsc(1L);
    verify(courseStaffRepository).findByCourseId(1L);
    // in particular, the job was given the roster student repository, and used it
    verify(rosterStudentRepository).findByCourseIdOrderByFirstNameAscLastNameAscIgnoreCase(1L);
    assertEquals(true, record.getLog().endsWith("Done"));
    assertEquals(course.getId(), job.getScopeId());
  }

  @Test
  @WithInstructorCoursePermissions
  public void setupSectionChannels_requiresSlackIntegrationOption() throws Exception {
    courseWithToken();
    courseOption("SLACK_INTEGRATION", false);
    courseOption("TRANSLATE_SECTIONS", true);

    MvcResult response =
        mockMvc
            .perform(post("/api/courses/slack/sectionChannels").with(csrf()).param("courseId", "1"))
            .andExpect(status().isBadRequest())
            .andReturn();

    verify(jobService, never()).runAsJob(any());
    assertEquals(
        "The course option SLACK_INTEGRATION must be enabled to set up section Slack channels.",
        responseToJson(response).get("message"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void setupSectionChannels_requiresTranslateSectionsOption() throws Exception {
    courseWithToken();
    courseOption("SLACK_INTEGRATION", true);
    courseOption("TRANSLATE_SECTIONS", null);

    MvcResult response =
        mockMvc
            .perform(post("/api/courses/slack/sectionChannels").with(csrf()).param("courseId", "1"))
            .andExpect(status().isBadRequest())
            .andReturn();

    verify(jobService, never()).runAsJob(any());
    assertEquals(
        "The course option TRANSLATE_SECTIONS must be enabled to set up section Slack channels.",
        responseToJson(response).get("message"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void setupSectionChannels_requiresToken() throws Exception {
    when(courseRepository.findById(1L))
        .thenReturn(Optional.of(courseBuilder().slackBotToken("").build()));
    when(tokenSecurityService.decrypt("")).thenReturn("");
    courseOption("SLACK_INTEGRATION", true);
    courseOption("TRANSLATE_SECTIONS", true);

    MvcResult response =
        mockMvc
            .perform(post("/api/courses/slack/sectionChannels").with(csrf()).param("courseId", "1"))
            .andExpect(status().isBadRequest())
            .andReturn();

    verify(jobService, never()).runAsJob(any());
    assertEquals(
        "No Slack token has been set for this course; enter one on the Settings tab.",
        responseToJson(response).get("message"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void setupSectionChannels_requiresToken_nullToken() throws Exception {
    when(courseRepository.findById(1L)).thenReturn(Optional.of(courseBuilder().build()));
    when(tokenSecurityService.decrypt(null)).thenReturn(null);
    courseOption("SLACK_INTEGRATION", true);
    courseOption("TRANSLATE_SECTIONS", true);

    mockMvc
        .perform(post("/api/courses/slack/sectionChannels").with(csrf()).param("courseId", "1"))
        .andExpect(status().isBadRequest());

    verify(jobService, never()).runAsJob(any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void setupSectionChannels_courseDoesNotExist() throws Exception {
    when(courseRepository.findById(1L)).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(post("/api/courses/slack/sectionChannels").with(csrf()).param("courseId", "1"))
            .andExpect(status().isNotFound())
            .andReturn();

    verify(jobService, never()).runAsJob(any());
    assertEquals("Course with id 1 not found", responseToJson(response).get("message"));
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void setupSectionChannels_forbiddenForRegularUser() throws Exception {
    when(courseRepository.findById(1L)).thenReturn(Optional.of(courseBuilder().build()));

    mockMvc
        .perform(post("/api/courses/slack/sectionChannels").with(csrf()).param("courseId", "1"))
        .andExpect(status().isForbidden());

    verify(jobService, never()).runAsJob(any());
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
        "Could not reach Slack. Please try again later. The token was not saved.",
        SlackController.errorMessage("slack_unreachable"));
    assertEquals(
        "Slack is limiting requests from this app. Please try again in a minute. The token was not saved.",
        SlackController.errorMessage("ratelimited"));
    assertEquals(
        "Slack reported an error (fatal_error). The token was not saved.",
        SlackController.errorMessage("fatal_error"));
    assertEquals(
        "Slack reported an error (null). The token was not saved.",
        SlackController.errorMessage(null));
  }
}
