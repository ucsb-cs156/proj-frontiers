package edu.ucsb.cs156.frontiers.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.annotations.WithInstructorCoursePermissions;
import edu.ucsb.cs156.frontiers.annotations.WithStaffCoursePermissions;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.CourseStaff;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.Team;
import edu.ucsb.cs156.frontiers.entities.TeamMember;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseStaffRepository;
import edu.ucsb.cs156.frontiers.repositories.TeamRepository;
import edu.ucsb.cs156.frontiers.testconfig.TestConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = {DokkuController.class})
@Import({TestConfig.class})
public class DokkuControllerTests extends ControllerTestCase {

  @MockitoBean CourseRepository courseRepository;

  @MockitoBean CourseStaffRepository courseStaffRepository;

  @MockitoBean TeamRepository teamRepository;

  private static CourseStaff staff(String email) {
    return CourseStaff.builder().email(email).build();
  }

  private static TeamMember member(String email) {
    return TeamMember.builder().rosterStudent(RosterStudent.builder().email(email).build()).build();
  }

  private static Team team(String name, TeamMember... members) {
    return Team.builder().name(name).teamMembers(List.of(members)).build();
  }

  /** Every line a staff member is expected to get: dokku-00 through dokku-18 in order. */
  private static String expectedStaffLines(String username) {
    StringBuilder sb = new StringBuilder();
    for (int n = DokkuController.LOWEST_DOKKU_NUMBER;
        n <= DokkuController.HIGHEST_DOKKU_NUMBER;
        n++) {
      sb.append(String.format("%s,dokku-%02d\n", username, n));
    }
    return sb.toString();
  }

  private MvcResult performDownload() throws Exception {
    return mockMvc
        .perform(get("/api/dokku/dokku_users_list?courseId=1"))
        .andExpect(request().asyncStarted())
        .andDo(MvcResult::getAsyncResult)
        .andExpect(status().isOk())
        .andReturn();
  }

  // ---- static helpers ----

  @Test
  public void allDokkuNames_lists_every_instance_in_order_with_two_digit_padding() {
    List<String> names = new ArrayList<>(DokkuController.allDokkuNames());
    assertEquals(19, names.size());
    assertEquals("dokku-00", names.get(0));
    assertEquals("dokku-07", names.get(7));
    assertEquals("dokku-18", names.get(18));
    assertEquals(
        DokkuController.HIGHEST_DOKKU_NUMBER - DokkuController.LOWEST_DOKKU_NUMBER + 1,
        names.size());
  }

  @Test
  public void usernameFromEmail_takes_everything_before_the_at_sign() {
    assertEquals("cgaucho", DokkuController.usernameFromEmail("cgaucho@ucsb.edu"));
    assertEquals("a.b", DokkuController.usernameFromEmail("a.b@example.com"));
    assertEquals("first", DokkuController.usernameFromEmail("first@second@third"));
    assertEquals("", DokkuController.usernameFromEmail("@ucsb.edu"));
  }

  @Test
  public void usernameFromEmail_returns_input_unchanged_when_there_is_no_at_sign() {
    assertEquals("noatsign", DokkuController.usernameFromEmail("noatsign"));
    assertEquals("", DokkuController.usernameFromEmail(""));
  }

  @Test
  public void dokkuNameForTeam_accepts_the_lowest_and_highest_dokku_numbers() {
    assertEquals(Optional.of("dokku-00"), DokkuController.dokkuNameForTeam("s26-00"));
    assertEquals(Optional.of("dokku-18"), DokkuController.dokkuNameForTeam("s26-18"));
    assertEquals(Optional.of("dokku-07"), DokkuController.dokkuNameForTeam("f25-4pm-07"));
    assertEquals(Optional.of("dokku-07"), DokkuController.dokkuNameForTeam("-07"));
    assertEquals(
        Optional.of(String.format("dokku-%02d", DokkuController.LOWEST_DOKKU_NUMBER)),
        DokkuController.dokkuNameForTeam(
            String.format("x-%02d", DokkuController.LOWEST_DOKKU_NUMBER)));
    assertEquals(
        Optional.of(String.format("dokku-%02d", DokkuController.HIGHEST_DOKKU_NUMBER)),
        DokkuController.dokkuNameForTeam(
            String.format("x-%02d", DokkuController.HIGHEST_DOKKU_NUMBER)));
  }

  @Test
  public void dokkuNameForTeam_rejects_numbers_above_the_highest_dokku_number() {
    assertEquals(Optional.empty(), DokkuController.dokkuNameForTeam("s26-19"));
    assertEquals(Optional.empty(), DokkuController.dokkuNameForTeam("s26-99"));
    assertEquals(
        Optional.empty(),
        DokkuController.dokkuNameForTeam(
            String.format("x-%02d", DokkuController.HIGHEST_DOKKU_NUMBER + 1)));
  }

  @Test
  public void dokkuNameForTeam_rejects_names_that_do_not_match_the_pattern() {
    assertEquals(Optional.empty(), DokkuController.dokkuNameForTeam(null));
    assertEquals(Optional.empty(), DokkuController.dokkuNameForTeam(""));
    assertEquals(Optional.empty(), DokkuController.dokkuNameForTeam("07"));
    assertEquals(Optional.empty(), DokkuController.dokkuNameForTeam("s26-7"));
    assertEquals(Optional.empty(), DokkuController.dokkuNameForTeam("s26-007"));
    assertEquals(Optional.empty(), DokkuController.dokkuNameForTeam("s26-07-extra"));
    assertEquals(Optional.empty(), DokkuController.dokkuNameForTeam("s26-0a"));
    assertEquals(Optional.empty(), DokkuController.dokkuNameForTeam("s26_07"));
    assertEquals(Optional.empty(), DokkuController.dokkuNameForTeam("s26-07 "));
  }

  @Test
  public void constants_match_the_ucsb_dokku_range() {
    assertEquals(0, DokkuController.LOWEST_DOKKU_NUMBER);
    assertEquals(18, DokkuController.HIGHEST_DOKKU_NUMBER);
  }

  // ---- security ----

  @Test
  public void logged_out_users_cannot_download() throws Exception {
    mockMvc.perform(get("/api/dokku/dokku_users_list?courseId=1")).andExpect(status().is(403));
    verify(courseRepository, never()).findById(any());
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void users_without_course_permissions_cannot_download() throws Exception {
    mockMvc.perform(get("/api/dokku/dokku_users_list?courseId=1")).andExpect(status().is(403));
    verify(courseRepository, never()).findById(any());
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void returns_404_when_course_does_not_exist() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(get("/api/dokku/dokku_users_list?courseId=1"))
            .andExpect(status().isNotFound())
            .andReturn();

    Map<String, String> errorResponse =
        mapper.readValue(
            response.getResponse().getContentAsString(),
            new TypeReference<Map<String, String>>() {});
    Map<String, String> expectedResponse =
        Map.of("message", "Course with id 1 not found", "type", "EntityNotFoundException");
    assertEquals(expectedResponse, errorResponse);
    assertEquals(HttpStatus.NOT_FOUND.value(), response.getResponse().getStatus());
    verify(courseStaffRepository, never()).findByCourseId(any());
    verify(teamRepository, never()).findByCourseIdOrderByNameAsc(any());
  }

  // ---- content ----

  @Test
  @WithInstructorCoursePermissions
  public void empty_course_produces_empty_file_with_download_headers() throws Exception {
    when(courseRepository.findById(eq(1L)))
        .thenReturn(Optional.of(Course.builder().id(1L).build()));
    when(courseStaffRepository.findByCourseId(eq(1L))).thenReturn(List.of());
    when(teamRepository.findByCourseIdOrderByNameAsc(eq(1L))).thenReturn(List.of());

    MvcResult response = performDownload();

    assertEquals("", response.getResponse().getContentAsString());
    assertEquals(
        "attachment;filename=dokku_users_list.csv",
        response.getResponse().getHeader("Content-Disposition"));
    assertEquals("text/csv; charset=UTF-8", response.getResponse().getContentType());
    assertEquals(
        "Content-Disposition", response.getResponse().getHeader("Access-Control-Expose-Headers"));
    verify(courseStaffRepository, times(1)).findByCourseId(eq(1L));
    verify(teamRepository, times(1)).findByCourseIdOrderByNameAsc(eq(1L));
  }

  @Test
  @WithStaffCoursePermissions
  public void each_staff_member_gets_every_dokku_instance_in_order() throws Exception {
    when(courseRepository.findById(eq(1L)))
        .thenReturn(Optional.of(Course.builder().id(1L).build()));
    when(courseStaffRepository.findByCourseId(eq(1L)))
        .thenReturn(List.of(staff("pconrad@ucsb.edu"), staff("ta.person@ucsb.edu")));
    when(teamRepository.findByCourseIdOrderByNameAsc(eq(1L))).thenReturn(List.of());

    MvcResult response = performDownload();

    String expected = expectedStaffLines("pconrad") + expectedStaffLines("ta.person");
    String actual = response.getResponse().getContentAsString().replace("\r\n", "\n");
    assertEquals(expected, actual);
    assertEquals(2 * 19, actual.lines().count());
    assertTrue(actual.startsWith("pconrad,dokku-00\n"));
    assertTrue(actual.contains("pconrad,dokku-18\nta.person,dokku-00\n"));
    assertTrue(actual.endsWith("ta.person,dokku-18\n"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void team_members_get_the_dokku_instance_named_by_their_team() throws Exception {
    when(courseRepository.findById(eq(1L)))
        .thenReturn(Optional.of(Course.builder().id(1L).build()));
    when(courseStaffRepository.findByCourseId(eq(1L))).thenReturn(List.of());
    when(teamRepository.findByCourseIdOrderByNameAsc(eq(1L)))
        .thenReturn(
            List.of(
                team("s26-00", member("alice@ucsb.edu"), member("bob@ucsb.edu")),
                team("s26-18", member("cara@ucsb.edu")),
                team("f25-4pm-07", member("dan@umail.ucsb.edu"))));

    MvcResult response = performDownload();

    String expected =
        """
        alice,dokku-00
        bob,dokku-00
        cara,dokku-18
        dan,dokku-07
        """;
    assertEquals(expected, response.getResponse().getContentAsString().replace("\r\n", "\n"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void teams_that_do_not_name_a_dokku_instance_are_ignored() throws Exception {
    when(courseRepository.findById(eq(1L)))
        .thenReturn(Optional.of(Course.builder().id(1L).build()));
    when(courseStaffRepository.findByCourseId(eq(1L))).thenReturn(List.of());
    when(teamRepository.findByCourseIdOrderByNameAsc(eq(1L)))
        .thenReturn(
            List.of(
                team("s26-19", member("toohigh@ucsb.edu")),
                team("s26-7", member("onedigit@ucsb.edu")),
                team("s26-07-extra", member("suffix@ucsb.edu")),
                team("nodash", member("nodash@ucsb.edu")),
                team("s26-0a", member("letter@ucsb.edu")),
                team("s26-05", member("ok@ucsb.edu"))));

    MvcResult response = performDownload();

    assertEquals(
        "ok,dokku-05\n", response.getResponse().getContentAsString().replace("\r\n", "\n"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void team_with_null_or_empty_member_list_produces_no_lines() throws Exception {
    when(courseRepository.findById(eq(1L)))
        .thenReturn(Optional.of(Course.builder().id(1L).build()));
    when(courseStaffRepository.findByCourseId(eq(1L))).thenReturn(List.of());
    Team nullMembers = Team.builder().name("s26-03").teamMembers(null).build();
    Team emptyMembers = Team.builder().name("s26-04").teamMembers(new ArrayList<>()).build();
    when(teamRepository.findByCourseIdOrderByNameAsc(eq(1L)))
        .thenReturn(List.of(nullMembers, emptyMembers, team("s26-05", member("ok@ucsb.edu"))));

    MvcResult response = performDownload();

    assertEquals(
        "ok,dokku-05\n", response.getResponse().getContentAsString().replace("\r\n", "\n"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void staff_lines_come_before_team_lines() throws Exception {
    when(courseRepository.findById(eq(1L)))
        .thenReturn(Optional.of(Course.builder().id(1L).build()));
    when(courseStaffRepository.findByCourseId(eq(1L))).thenReturn(List.of(staff("staff@ucsb.edu")));
    when(teamRepository.findByCourseIdOrderByNameAsc(eq(1L)))
        .thenReturn(List.of(team("s26-01", member("student@ucsb.edu"))));

    MvcResult response = performDownload();

    String expected = expectedStaffLines("staff") + "student,dokku-01\n";
    assertEquals(expected, response.getResponse().getContentAsString().replace("\r\n", "\n"));
  }
}
