package edu.ucsb.cs156.frontiers.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.annotations.WithInstructorCoursePermissions;
import edu.ucsb.cs156.frontiers.config.AllowApiKeyAccess;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.Team;
import edu.ucsb.cs156.frontiers.entities.TeamMember;
import edu.ucsb.cs156.frontiers.enums.RosterStatus;
import edu.ucsb.cs156.frontiers.models.StudentInfo;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.services.TeamCsvService;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = StudentInfoController.class)
@Import(TeamCsvService.class)
public class StudentInfoControllerTests extends ControllerTestCase {

  @MockitoBean private RosterStudentRepository rosterStudentRepository;

  private static RosterStudent student(
      String email,
      String firstName,
      String lastName,
      String githubLogin,
      RosterStatus status,
      String... teams) {
    List<TeamMember> teamMembers =
        Arrays.stream(teams)
            .map(name -> TeamMember.builder().team(Team.builder().name(name).build()).build())
            .toList();
    return RosterStudent.builder()
        .email(email)
        .firstName(firstName)
        .lastName(lastName)
        .githubLogin(githubLogin)
        .rosterStatus(status)
        .teamMembers(teamMembers)
        .build();
  }

  private String getStudentInfo(String email) throws Exception {
    MvcResult response =
        mockMvc
            .perform(get("/api/courses/studentInfo").param("courseId", "1").param("email", email))
            .andExpect(status().isOk())
            .andReturn();
    return response.getResponse().getContentAsString();
  }

  @Test
  public void normalizeEmail_lowercases_strips_and_maps_umail() {
    assertEquals("cgaucho@ucsb.edu", StudentInfoController.normalizeEmail("cgaucho@ucsb.edu"));
    assertEquals("cgaucho@ucsb.edu", StudentInfoController.normalizeEmail(" CGaucho@UCSB.edu\n"));
    assertEquals(
        "cgaucho@ucsb.edu", StudentInfoController.normalizeEmail("CGaucho@umail.ucsb.edu"));
    assertEquals(
        "cgaucho@umail.ucsb.edu.example.com",
        StudentInfoController.normalizeEmail("cgaucho@umail.ucsb.edu.example.com"));
    assertEquals("someone@gmail.com", StudentInfoController.normalizeEmail("someone@gmail.com"));
  }

  @Test
  public void endpoint_allows_api_key_access() throws Exception {
    assertNotNull(
        StudentInfoController.class
            .getMethod("getStudentInfo", Long.class, String.class)
            .getAnnotation(AllowApiKeyAccess.class));
  }

  @Test
  @WithInstructorCoursePermissions
  public void returns_student_with_unique_short_names_and_sorted_teammates() throws Exception {
    // Three students called Chris in the course, two of them on the team, so the short names
    // need a last initial. A dropped student on the team must not be listed. The roster comes
    // back from the repository in no particular order; the teammates are sorted by name, with
    // the email as a final tie-breaker.
    RosterStudent lauren =
        student(
            "ldelplaya@ucsb.edu", "LAUREN", "DELPLAYA", "ldelplaya", RosterStatus.ROSTER, "f26-04");
    RosterStudent chrisLee =
        student("clee@ucsb.edu", "CHRIS", "LEE", "clee", RosterStatus.MANUAL, "f26-04");
    RosterStudent chrisGaucho =
        student(
            "cgaucho@ucsb.edu", "CHRIS EDWARD", "GAUCHO", "cgaucho", RosterStatus.ROSTER, "f26-04");
    RosterStudent chrisZeta =
        student("czeta@ucsb.edu", "CHRIS", "ZETA", "czeta", RosterStatus.ROSTER, "f26-05");
    RosterStudent dropped =
        student("zed@ucsb.edu", "ZED", "DROPPED", "zed", RosterStatus.DROPPED, "f26-04");
    RosterStudent samTwo =
        student("sam2@ucsb.edu", "SAM", "SMITH", "sam2", RosterStatus.ROSTER, "f26-04");
    RosterStudent samOne =
        student("sam1@ucsb.edu", "SAM", "SMITH", "sam1", RosterStatus.ROSTER, "f26-04");

    when(rosterStudentRepository.findByCourseId(eq(1L)))
        .thenReturn(List.of(lauren, chrisLee, dropped, samTwo, chrisGaucho, samOne, chrisZeta));

    StudentInfo expected =
        new StudentInfo(
            "cgaucho@ucsb.edu",
            "Chris G",
            "CHRIS EDWARD",
            "GAUCHO",
            "cgaucho",
            "f26-04",
            List.of("f26-04"),
            List.of(
                new StudentInfo.Member("cgaucho@ucsb.edu", "Chris G", "CHRIS EDWARD", "cgaucho"),
                new StudentInfo.Member("clee@ucsb.edu", "Chris L", "CHRIS", "clee"),
                new StudentInfo.Member("ldelplaya@ucsb.edu", "Lauren", "LAUREN", "ldelplaya"),
                new StudentInfo.Member("sam1@ucsb.edu", "Sam Smith*", "SAM", "sam1"),
                new StudentInfo.Member("sam2@ucsb.edu", "Sam Smith*", "SAM", "sam2")));

    assertEquals(mapper.writeValueAsString(expected), getStudentInfo(" CGaucho@umail.ucsb.edu "));
  }

  @Test
  @WithInstructorCoursePermissions
  public void student_with_no_team_has_null_team_and_no_teammates() throws Exception {
    RosterStudent solo = student("solo@ucsb.edu", "SOLO", null, null, RosterStatus.ROSTER);
    solo.setTeamMembers(null);
    RosterStudent other =
        student("other@ucsb.edu", "OTHER", "PERSON", "other", RosterStatus.ROSTER, "f26-04");

    when(rosterStudentRepository.findByCourseId(eq(1L))).thenReturn(List.of(solo, other));

    StudentInfo expected =
        new StudentInfo("solo@ucsb.edu", "Solo", "SOLO", null, null, null, List.of(), List.of());

    assertEquals(mapper.writeValueAsString(expected), getStudentInfo("solo@ucsb.edu"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void student_on_several_teams_reports_first_team_and_its_members() throws Exception {
    RosterStudent busy =
        student("busy@ucsb.edu", "BUSY", "BEE", "busy", RosterStatus.ROSTER, "f26-04", "f26-09");
    RosterStudent onFirst =
        student("first@ucsb.edu", "ALICE", "FIRST", "alice", RosterStatus.ROSTER, "f26-04");
    RosterStudent onSecond =
        student("second@ucsb.edu", "BOB", "SECOND", "bob", RosterStatus.ROSTER, "f26-09");

    when(rosterStudentRepository.findByCourseId(eq(1L)))
        .thenReturn(List.of(onSecond, busy, onFirst));

    StudentInfo expected =
        new StudentInfo(
            "busy@ucsb.edu",
            "Busy",
            "BUSY",
            "BEE",
            "busy",
            "f26-04",
            List.of("f26-04", "f26-09"),
            List.of(
                new StudentInfo.Member("first@ucsb.edu", "Alice", "ALICE", "alice"),
                new StudentInfo.Member("busy@ucsb.edu", "Busy", "BUSY", "busy")));

    assertEquals(mapper.writeValueAsString(expected), getStudentInfo("busy@ucsb.edu"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void roster_rows_without_email_are_skipped_and_missing_github_login_is_null()
      throws Exception {
    RosterStudent noEmail = student(null, "NO", "EMAIL", "noemail", RosterStatus.ROSTER, "f26-04");
    RosterStudent target =
        student("target@ucsb.edu", "TARGET", "PERSON", null, RosterStatus.ROSTER, "f26-04");

    when(rosterStudentRepository.findByCourseId(eq(1L))).thenReturn(List.of(noEmail, target));

    StudentInfo expected =
        new StudentInfo(
            "target@ucsb.edu",
            "Target",
            "TARGET",
            "PERSON",
            null,
            "f26-04",
            List.of("f26-04"),
            List.of(
                new StudentInfo.Member(null, "No", "NO", "noemail"),
                new StudentInfo.Member("target@ucsb.edu", "Target", "TARGET", null)));

    assertEquals(mapper.writeValueAsString(expected), getStudentInfo("target@ucsb.edu"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void unknown_email_is_not_found() throws Exception {
    RosterStudent other =
        student("other@ucsb.edu", "OTHER", "PERSON", "other", RosterStatus.ROSTER, "f26-04");
    when(rosterStudentRepository.findByCourseId(eq(1L))).thenReturn(List.of(other));

    mockMvc
        .perform(
            get("/api/courses/studentInfo")
                .param("courseId", "1")
                .param("email", "Nobody@umail.ucsb.edu"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.type").value("StudentNotOnRosterException"))
        .andExpect(
            jsonPath("$.message")
                .value("No roster student with email nobody@ucsb.edu in course 1"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void dropped_student_is_not_found() throws Exception {
    RosterStudent dropped =
        student("zed@ucsb.edu", "ZED", "DROPPED", "zed", RosterStatus.DROPPED, "f26-04");
    when(rosterStudentRepository.findByCourseId(eq(1L))).thenReturn(List.of(dropped));

    mockMvc
        .perform(
            get("/api/courses/studentInfo").param("courseId", "1").param("email", "zed@ucsb.edu"))
        .andExpect(status().isNotFound())
        .andExpect(
            jsonPath("$.message").value("No roster student with email zed@ucsb.edu in course 1"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void empty_roster_is_not_found() throws Exception {
    when(rosterStudentRepository.findByCourseId(eq(1L))).thenReturn(List.of());

    mockMvc
        .perform(
            get("/api/courses/studentInfo").param("courseId", "1").param("email", "x@ucsb.edu"))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(authorities = {"ROLE_API_KEY", "ROLE_USER", "COURSE_PERMISSIONS"})
  public void api_key_with_course_permissions_can_look_up_students() throws Exception {
    RosterStudent target =
        student("target@ucsb.edu", "TARGET", "PERSON", "target", RosterStatus.ROSTER, "f26-04");
    when(rosterStudentRepository.findByCourseId(eq(1L))).thenReturn(List.of(target));

    mockMvc
        .perform(
            get("/api/courses/studentInfo")
                .param("courseId", "1")
                .param("email", "target@ucsb.edu"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.githubLogin").value("target"));
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void forbidden_without_course_manage_permissions() throws Exception {
    mockMvc
        .perform(
            get("/api/courses/studentInfo").param("courseId", "1").param("email", "x@ucsb.edu"))
        .andExpect(status().isForbidden());
  }

  @Test
  public void forbidden_when_logged_out() throws Exception {
    mockMvc
        .perform(
            get("/api/courses/studentInfo").param("courseId", "1").param("email", "x@ucsb.edu"))
        .andExpect(status().isForbidden());
  }
}
