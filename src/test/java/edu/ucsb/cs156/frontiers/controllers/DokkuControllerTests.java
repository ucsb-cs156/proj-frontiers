package edu.ucsb.cs156.frontiers.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.annotations.WithInstructorCoursePermissions;
import edu.ucsb.cs156.frontiers.annotations.WithStaffCoursePermissions;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.CourseStaff;
import edu.ucsb.cs156.frontiers.entities.DokkuAccountTranslation;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.Team;
import edu.ucsb.cs156.frontiers.entities.TeamMember;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseStaffRepository;
import edu.ucsb.cs156.frontiers.repositories.DokkuAccountTranslationRepository;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = {DokkuController.class})
@Import({TestConfig.class})
public class DokkuControllerTests extends ControllerTestCase {

  @MockitoBean CourseRepository courseRepository;

  @MockitoBean CourseStaffRepository courseStaffRepository;

  @MockitoBean TeamRepository teamRepository;

  @MockitoBean DokkuAccountTranslationRepository dokkuAccountTranslationRepository;

  private static DokkuAccountTranslation translation(String email, String username) {
    return DokkuAccountTranslation.builder().email(email).username(username).build();
  }

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
  public void usernameFor_uses_translation_when_present_comparing_canonical_emails() {
    Map<String, String> translations = Map.of("cgaucho@ucsb.edu", "chris");
    assertEquals("chris", DokkuController.usernameFor("cgaucho@ucsb.edu", translations));
    assertEquals("chris", DokkuController.usernameFor("CGaucho@umail.ucsb.edu", translations));
  }

  @Test
  public void usernameFor_falls_back_to_email_prefix_when_there_is_no_translation() {
    Map<String, String> translations = Map.of("cgaucho@ucsb.edu", "chris");
    assertEquals("other", DokkuController.usernameFor("other@ucsb.edu", translations));
    assertEquals("other", DokkuController.usernameFor("other@ucsb.edu", Map.of()));
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
  public void translations_replace_email_prefix_for_staff_and_students() throws Exception {
    when(courseRepository.findById(eq(1L)))
        .thenReturn(Optional.of(Course.builder().id(1L).build()));
    when(courseStaffRepository.findByCourseId(eq(1L)))
        .thenReturn(List.of(staff("pconrad@ucsb.edu"), staff("ta@ucsb.edu")));
    when(teamRepository.findByCourseIdOrderByNameAsc(eq(1L)))
        .thenReturn(
            List.of(team("s26-02", member("Alice@umail.ucsb.edu"), member("bob@ucsb.edu"))));
    when(dokkuAccountTranslationRepository.findAll())
        .thenReturn(
            List.of(
                translation("pconrad@ucsb.edu", "phill"),
                translation("ALICE@umail.ucsb.edu", "alice_a")));

    MvcResult response = performDownload();

    String expected =
        expectedStaffLines("phill") + expectedStaffLines("ta") + "alice_a,dokku-02\nbob,dokku-02\n";
    assertEquals(expected, response.getResponse().getContentAsString().replace("\r\n", "\n"));
    verify(dokkuAccountTranslationRepository, times(1)).findAll();
  }

  @Test
  @WithInstructorCoursePermissions
  public void
      header_lines_come_first_verbatim_with_blank_lines_dropped_and_line_endings_normalized()
          throws Exception {
    Course course =
        Course.builder()
            .id(1L)
            .dokkuUsersListHeader("eci,dokku-00\r\n\n  eci,dokku-01  \r\n\r\nguest,dokku-05\n")
            .build();
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(courseStaffRepository.findByCourseId(eq(1L))).thenReturn(List.of(staff("staff@ucsb.edu")));
    when(teamRepository.findByCourseIdOrderByNameAsc(eq(1L)))
        .thenReturn(List.of(team("s26-01", member("student@ucsb.edu"))));

    MvcResult response = performDownload();

    String expected =
        "eci,dokku-00\neci,dokku-01\nguest,dokku-05\n"
            + expectedStaffLines("staff")
            + "student,dokku-01\n";
    String actual = response.getResponse().getContentAsString();
    assertTrue(actual.startsWith("eci,dokku-00\r\neci,dokku-01\r\n"));
    assertEquals(expected, actual.replace("\r\n", "\n"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void header_that_is_only_whitespace_adds_no_lines() throws Exception {
    Course course = Course.builder().id(1L).dokkuUsersListHeader("  \n\r\n \n").build();
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(courseStaffRepository.findByCourseId(eq(1L))).thenReturn(List.of());
    when(teamRepository.findByCourseIdOrderByNameAsc(eq(1L)))
        .thenReturn(List.of(team("s26-01", member("student@ucsb.edu"))));

    MvcResult response = performDownload();

    assertEquals(
        "student,dokku-01\n", response.getResponse().getContentAsString().replace("\r\n", "\n"));
  }

  // ---- users_list_header ----

  @Test
  public void logged_out_users_cannot_get_or_put_the_header() throws Exception {
    mockMvc.perform(get("/api/dokku/users_list_header?courseId=1")).andExpect(status().is(403));
    mockMvc
        .perform(
            put("/api/dokku/users_list_header?courseId=1")
                .with(csrf())
                .contentType(MediaType.TEXT_PLAIN)
                .content("x,dokku-00"))
        .andExpect(status().is(403));
    verify(courseRepository, never()).findById(any());
    verify(courseRepository, never()).save(any());
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void users_without_course_permissions_cannot_get_or_put_the_header() throws Exception {
    mockMvc.perform(get("/api/dokku/users_list_header?courseId=1")).andExpect(status().is(403));
    mockMvc
        .perform(
            put("/api/dokku/users_list_header?courseId=1")
                .with(csrf())
                .contentType(MediaType.TEXT_PLAIN)
                .content("x,dokku-00"))
        .andExpect(status().is(403));
    verify(courseRepository, never()).save(any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void get_header_returns_404_when_course_does_not_exist() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(get("/api/dokku/users_list_header?courseId=1"))
            .andExpect(status().isNotFound())
            .andReturn();

    assertEquals(
        Map.of("message", "Course with id 1 not found", "type", "EntityNotFoundException"),
        mapper.readValue(
            response.getResponse().getContentAsString(),
            new TypeReference<Map<String, String>>() {}));
  }

  @Test
  @WithStaffCoursePermissions
  public void get_header_returns_empty_text_when_none_is_set() throws Exception {
    when(courseRepository.findById(eq(1L)))
        .thenReturn(Optional.of(Course.builder().id(1L).build()));

    MvcResult response =
        mockMvc
            .perform(get("/api/dokku/users_list_header?courseId=1"))
            .andExpect(status().isOk())
            .andReturn();

    assertEquals("", response.getResponse().getContentAsString());
    assertTrue(response.getResponse().getContentType().startsWith("text/plain"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void get_header_returns_the_header_text_with_line_breaks() throws Exception {
    when(courseRepository.findById(eq(1L)))
        .thenReturn(
            Optional.of(
                Course.builder().id(1L).dokkuUsersListHeader("a,dokku-00\nb,dokku-01").build()));

    MvcResult response =
        mockMvc
            .perform(get("/api/dokku/users_list_header?courseId=1"))
            .andExpect(status().isOk())
            .andReturn();

    assertEquals("a,dokku-00\nb,dokku-01", response.getResponse().getContentAsString());
  }

  @Test
  @WithInstructorCoursePermissions
  public void put_header_returns_404_when_course_does_not_exist() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.empty());

    mockMvc
        .perform(
            put("/api/dokku/users_list_header?courseId=1")
                .with(csrf())
                .contentType(MediaType.TEXT_PLAIN)
                .content("x,dokku-00"))
        .andExpect(status().isNotFound());

    verify(courseRepository, never()).save(any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void put_header_saves_the_text_verbatim_and_returns_it() throws Exception {
    Course course = Course.builder().id(1L).dokkuUsersListHeader("old").build();
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));

    MvcResult response =
        mockMvc
            .perform(
                put("/api/dokku/users_list_header?courseId=1")
                    .with(csrf())
                    .contentType(MediaType.TEXT_PLAIN)
                    .content("eci,dokku-00\n  eci,dokku-01\n"))
            .andExpect(status().isOk())
            .andReturn();

    assertEquals("eci,dokku-00\n  eci,dokku-01\n", course.getDokkuUsersListHeader());
    verify(courseRepository, times(1)).save(eq(course));
    assertEquals("eci,dokku-00\n  eci,dokku-01\n", response.getResponse().getContentAsString());
    assertTrue(response.getResponse().getContentType().startsWith("text/plain"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void put_header_with_blank_body_clears_the_header() throws Exception {
    Course course = Course.builder().id(1L).dokkuUsersListHeader("old").build();
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));

    MvcResult response =
        mockMvc
            .perform(
                put("/api/dokku/users_list_header?courseId=1")
                    .with(csrf())
                    .contentType(MediaType.TEXT_PLAIN)
                    .content("  \n "))
            .andExpect(status().isOk())
            .andReturn();

    assertEquals(null, course.getDokkuUsersListHeader());
    verify(courseRepository, times(1)).save(eq(course));
    assertEquals("", response.getResponse().getContentAsString());
  }

  @Test
  @WithInstructorCoursePermissions
  public void put_header_with_no_body_clears_the_header() throws Exception {
    Course course = Course.builder().id(1L).dokkuUsersListHeader("old").build();
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));

    MvcResult response =
        mockMvc
            .perform(
                put("/api/dokku/users_list_header?courseId=1")
                    .with(csrf())
                    .contentType(MediaType.TEXT_PLAIN))
            .andExpect(status().isOk())
            .andReturn();

    assertEquals(null, course.getDokkuUsersListHeader());
    verify(courseRepository, times(1)).save(eq(course));
    assertEquals("", response.getResponse().getContentAsString());
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
