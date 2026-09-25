package edu.ucsb.cs156.frontiers.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.annotations.WithInstructorCoursePermissions;
import edu.ucsb.cs156.frontiers.annotations.WithStaffCoursePermissions;
import edu.ucsb.cs156.frontiers.entities.Assignment;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.enums.AssignmentType;
import edu.ucsb.cs156.frontiers.enums.Permission;
import edu.ucsb.cs156.frontiers.enums.RepositoryCreationOption;
import edu.ucsb.cs156.frontiers.enums.RepositoryPermissions;
import edu.ucsb.cs156.frontiers.enums.Visibility;
import edu.ucsb.cs156.frontiers.jobs.CreateStudentOrStaffRepositoriesJob;
import edu.ucsb.cs156.frontiers.jobs.CreateTeamRepositoriesJob;
import edu.ucsb.cs156.frontiers.models.AssignmentWithJob;
import edu.ucsb.cs156.frontiers.repositories.AssignmentRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.services.GithubTeamService;
import edu.ucsb.cs156.frontiers.services.RepositoryService;
import edu.ucsb.cs156.frontiers.testconfig.TestConfig;
import edu.ucsb.cs156.jobs.entities.Job;
import edu.ucsb.cs156.jobs.services.JobContextConsumer;
import edu.ucsb.cs156.jobs.services.JobService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = AssignmentsController.class)
@Import(TestConfig.class)
public class AssignmentsControllerTests extends ControllerTestCase {
  @MockitoBean private CourseRepository courseRepository;

  @MockitoBean private AssignmentRepository assignmentRepository;

  @MockitoBean private JobService jobService;

  @MockitoBean private RepositoryService repositoryService;

  @MockitoBean private GithubTeamService githubTeamService;

  private Course course;
  private Job job;

  @BeforeEach
  public void setUp() {
    course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .orgName("ucsb-cs156")
            .installationId("1234")
            .build();
    job = Job.builder().id(99L).status("processing").build();
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(jobService.runAsJob(any(JobContextConsumer.class))).thenReturn(job);
    when(assignmentRepository.save(any(Assignment.class))).thenAnswer(i -> i.getArgument(0));
  }

  private Assignment individual(long id, String prefix) {
    return Assignment.builder()
        .id(id)
        .course(course)
        .repoPrefix(prefix)
        .asnType(AssignmentType.INDIVIDUAL)
        .visibility(Visibility.PUBLIC)
        .permission(Permission.READ)
        .createReposFor(RepositoryCreationOption.STUDENTS_ONLY)
        .build();
  }

  private Assignment team(long id, String prefix, String regex) {
    return Assignment.builder()
        .id(id)
        .course(course)
        .repoPrefix(prefix)
        .asnType(AssignmentType.TEAM)
        .visibility(Visibility.PRIVATE)
        .permission(Permission.WRITE)
        .teamRegex(regex)
        .build();
  }

  private JobContextConsumer launchedJob() {
    ArgumentCaptor<JobContextConsumer> captor = ArgumentCaptor.forClass(JobContextConsumer.class);
    verify(jobService, times(1)).runAsJob(captor.capture());
    return captor.getValue();
  }

  private Map<String, String> errorBody(MvcResult response) throws Exception {
    return mapper.readValue(response.getResponse().getContentAsString(), Map.class);
  }

  // ---- security ----

  @Test
  public void logged_out_users_cannot_do_anything() throws Exception {
    mockMvc.perform(get("/api/assignments").param("courseId", "1")).andExpect(status().is(403));
    mockMvc
        .perform(
            post("/api/assignments/post")
                .with(csrf())
                .param("courseId", "1")
                .param("repoPrefix", "hw1")
                .param("asnType", "INDIVIDUAL")
                .param("visibility", "PUBLIC")
                .param("permission", "READ"))
        .andExpect(status().is(403));
    mockMvc
        .perform(
            put("/api/assignments/put")
                .with(csrf())
                .param("courseId", "1")
                .param("assignmentId", "5")
                .param("repoPrefix", "hw1")
                .param("visibility", "PUBLIC")
                .param("permission", "READ"))
        .andExpect(status().is(403));
    mockMvc
        .perform(delete("/api/assignments/5").with(csrf()).param("courseId", "1"))
        .andExpect(status().is(403));
    verify(assignmentRepository, never()).save(any());
    verify(jobService, never()).runAsJob(any(JobContextConsumer.class));
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void users_without_course_permissions_cannot_do_anything() throws Exception {
    mockMvc.perform(get("/api/assignments").param("courseId", "1")).andExpect(status().is(403));
    mockMvc
        .perform(
            post("/api/assignments/post")
                .with(csrf())
                .param("courseId", "1")
                .param("repoPrefix", "hw1")
                .param("asnType", "INDIVIDUAL")
                .param("visibility", "PUBLIC")
                .param("permission", "READ"))
        .andExpect(status().is(403));
    mockMvc
        .perform(
            put("/api/assignments/put")
                .with(csrf())
                .param("courseId", "1")
                .param("assignmentId", "5")
                .param("repoPrefix", "hw1")
                .param("visibility", "PUBLIC")
                .param("permission", "READ"))
        .andExpect(status().is(403));
    mockMvc
        .perform(delete("/api/assignments/5").with(csrf()).param("courseId", "1"))
        .andExpect(status().is(403));
    verify(assignmentRepository, never()).save(any());
    verify(assignmentRepository, never()).delete(any());
    verify(jobService, never()).runAsJob(any(JobContextConsumer.class));
  }

  // ---- GET ----

  @Test
  @WithStaffCoursePermissions
  public void get_lists_the_assignments_of_the_course() throws Exception {
    List<Assignment> assignments = List.of(individual(1L, "hw1"), team(2L, "proj", "team-.*"));
    when(assignmentRepository.findByCourseIdOrderByRepoPrefixAsc(eq(1L))).thenReturn(assignments);

    MvcResult response =
        mockMvc
            .perform(get("/api/assignments").param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    assertEquals(
        mapper.writeValueAsString(assignments), response.getResponse().getContentAsString());
    verify(assignmentRepository, times(1)).findByCourseIdOrderByRepoPrefixAsc(eq(1L));
  }

  @Test
  @WithInstructorCoursePermissions
  public void get_does_not_include_the_course_in_the_json() throws Exception {
    when(assignmentRepository.findByCourseIdOrderByRepoPrefixAsc(eq(1L)))
        .thenReturn(List.of(individual(1L, "hw1")));

    MvcResult response =
        mockMvc
            .perform(get("/api/assignments").param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    List<Map<String, Object>> body =
        mapper.readValue(response.getResponse().getContentAsString(), List.class);
    assertEquals(
        Map.of(
            "id", 1,
            "repoPrefix", "hw1",
            "asnType", "INDIVIDUAL",
            "visibility", "PUBLIC",
            "permission", "READ",
            "createReposFor", "STUDENTS_ONLY",
            "teamRegex", ""),
        withNullsAsEmpty(body.get(0)));
  }

  private static Map<String, Object> withNullsAsEmpty(Map<String, Object> m) {
    Map<String, Object> copy = new java.util.HashMap<>(m);
    copy.replaceAll((k, v) -> v == null ? "" : v);
    return copy;
  }

  @Test
  @WithInstructorCoursePermissions
  public void get_returns_404_when_course_does_not_exist() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(get("/api/assignments").param("courseId", "1"))
            .andExpect(status().isNotFound())
            .andReturn();

    assertEquals(
        Map.of("message", "Course with id 1 not found", "type", "EntityNotFoundException"),
        errorBody(response));
    verify(assignmentRepository, never()).findByCourseIdOrderByRepoPrefixAsc(any());
  }

  // ---- POST ----

  @Test
  @WithInstructorCoursePermissions
  public void post_individual_assignment_saves_it_and_starts_the_student_repo_job()
      throws Exception {
    MvcResult response =
        mockMvc
            .perform(
                post("/api/assignments/post")
                    .with(csrf())
                    .param("courseId", "1")
                    .param("repoPrefix", "  hw1  ")
                    .param("asnType", "INDIVIDUAL")
                    .param("visibility", "PUBLIC")
                    .param("permission", "READ"))
            .andExpect(status().isOk())
            .andReturn();

    ArgumentCaptor<Assignment> saved = ArgumentCaptor.forClass(Assignment.class);
    verify(assignmentRepository, times(1)).save(saved.capture());
    Assignment a = saved.getValue();
    assertEquals(course, a.getCourse());
    assertEquals("hw1", a.getRepoPrefix());
    assertEquals(AssignmentType.INDIVIDUAL, a.getAsnType());
    assertEquals(Visibility.PUBLIC, a.getVisibility());
    assertEquals(Permission.READ, a.getPermission());
    assertEquals(RepositoryCreationOption.STUDENTS_ONLY, a.getCreateReposFor());
    assertNull(a.getTeamRegex());

    CreateStudentOrStaffRepositoriesJob started =
        assertInstanceOf(CreateStudentOrStaffRepositoriesJob.class, launchedJob());
    assertEquals("hw1", ReflectionTestUtils.getField(started, "repositoryPrefix"));
    assertEquals(false, ReflectionTestUtils.getField(started, "isPrivate"));
    assertEquals(RepositoryPermissions.READ, ReflectionTestUtils.getField(started, "permissions"));
    assertEquals(
        RepositoryCreationOption.STUDENTS_ONLY,
        ReflectionTestUtils.getField(started, "creationOption"));
    assertEquals(course, ReflectionTestUtils.getField(started, "course"));
    assertEquals(repositoryService, ReflectionTestUtils.getField(started, "repositoryService"));

    assertEquals(
        mapper.writeValueAsString(new AssignmentWithJob(a, job)),
        response.getResponse().getContentAsString());
  }

  @Test
  @WithInstructorCoursePermissions
  public void post_individual_assignment_uses_the_given_options() throws Exception {
    mockMvc
        .perform(
            post("/api/assignments/post")
                .with(csrf())
                .param("courseId", "1")
                .param("repoPrefix", "hw2")
                .param("asnType", "INDIVIDUAL")
                .param("visibility", "PRIVATE")
                .param("permission", "ADMIN")
                .param("createReposFor", "STAFF_ONLY")
                .param("teamRegex", "  "))
        .andExpect(status().isOk());

    ArgumentCaptor<Assignment> saved = ArgumentCaptor.forClass(Assignment.class);
    verify(assignmentRepository, times(1)).save(saved.capture());
    assertEquals(RepositoryCreationOption.STAFF_ONLY, saved.getValue().getCreateReposFor());
    assertNull(saved.getValue().getTeamRegex());

    CreateStudentOrStaffRepositoriesJob started =
        assertInstanceOf(CreateStudentOrStaffRepositoriesJob.class, launchedJob());
    assertEquals(true, ReflectionTestUtils.getField(started, "isPrivate"));
    assertEquals(RepositoryPermissions.ADMIN, ReflectionTestUtils.getField(started, "permissions"));
    assertEquals(
        RepositoryCreationOption.STAFF_ONLY,
        ReflectionTestUtils.getField(started, "creationOption"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void post_team_assignment_saves_it_and_starts_the_team_repo_job() throws Exception {
    MvcResult response =
        mockMvc
            .perform(
                post("/api/assignments/post")
                    .with(csrf())
                    .param("courseId", "1")
                    .param("repoPrefix", "proj")
                    .param("asnType", "TEAM")
                    .param("visibility", "PRIVATE")
                    .param("permission", "MAINTAIN")
                    .param("teamRegex", "team-\\d+"))
            .andExpect(status().isOk())
            .andReturn();

    ArgumentCaptor<Assignment> saved = ArgumentCaptor.forClass(Assignment.class);
    verify(assignmentRepository, times(1)).save(saved.capture());
    Assignment a = saved.getValue();
    assertEquals(AssignmentType.TEAM, a.getAsnType());
    assertEquals("team-\\d+", a.getTeamRegex());
    assertNull(a.getCreateReposFor());

    CreateTeamRepositoriesJob started =
        assertInstanceOf(CreateTeamRepositoriesJob.class, launchedJob());
    assertEquals("proj", ReflectionTestUtils.getField(started, "repositoryPrefix"));
    assertEquals(true, ReflectionTestUtils.getField(started, "isPrivate"));
    assertEquals(
        RepositoryPermissions.MAINTAIN, ReflectionTestUtils.getField(started, "permissions"));
    assertEquals("team-\\d+", ReflectionTestUtils.getField(started, "teamRegex"));
    assertEquals(course, ReflectionTestUtils.getField(started, "course"));
    assertEquals(repositoryService, ReflectionTestUtils.getField(started, "repositoryService"));
    assertEquals(githubTeamService, ReflectionTestUtils.getField(started, "githubTeamService"));

    assertEquals(
        mapper.writeValueAsString(new AssignmentWithJob(a, job)),
        response.getResponse().getContentAsString());
  }

  @Test
  @WithInstructorCoursePermissions
  public void post_team_assignment_without_a_team_regex_matches_every_team() throws Exception {
    for (String regexParam : new String[] {null, "", "   "}) {
      org.mockito.Mockito.clearInvocations(assignmentRepository, jobService);
      var request =
          post("/api/assignments/post")
              .with(csrf())
              .param("courseId", "1")
              .param("repoPrefix", "proj")
              .param("asnType", "TEAM")
              .param("visibility", "PUBLIC")
              .param("permission", "READ");
      if (regexParam != null) {
        request.param("teamRegex", regexParam);
      }
      mockMvc.perform(request).andExpect(status().isOk());

      ArgumentCaptor<Assignment> saved = ArgumentCaptor.forClass(Assignment.class);
      verify(assignmentRepository, times(1)).save(saved.capture());
      assertEquals(".*", saved.getValue().getTeamRegex());
      assertEquals(".*", ReflectionTestUtils.getField(launchedJob(), "teamRegex"));
    }
  }

  @Test
  @WithInstructorCoursePermissions
  public void post_returns_404_when_course_does_not_exist() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(
                post("/api/assignments/post")
                    .with(csrf())
                    .param("courseId", "1")
                    .param("repoPrefix", "hw1")
                    .param("asnType", "INDIVIDUAL")
                    .param("visibility", "PUBLIC")
                    .param("permission", "READ"))
            .andExpect(status().isNotFound())
            .andReturn();

    assertEquals(
        Map.of("message", "Course with id 1 not found", "type", "EntityNotFoundException"),
        errorBody(response));
    verify(assignmentRepository, never()).save(any());
    verify(jobService, never()).runAsJob(any(JobContextConsumer.class));
  }

  @Test
  @WithInstructorCoursePermissions
  public void post_returns_400_when_the_course_has_no_linked_organization() throws Exception {
    Course unlinked = Course.builder().id(2L).courseName("Unlinked").build();
    when(courseRepository.findById(eq(2L))).thenReturn(Optional.of(unlinked));

    MvcResult response =
        mockMvc
            .perform(
                post("/api/assignments/post")
                    .with(csrf())
                    .param("courseId", "2")
                    .param("repoPrefix", "hw1")
                    .param("asnType", "INDIVIDUAL")
                    .param("visibility", "PUBLIC")
                    .param("permission", "READ"))
            .andExpect(status().isBadRequest())
            .andReturn();

    assertEquals("NoLinkedOrganizationException", errorBody(response).get("type"));
    verify(assignmentRepository, never()).save(any());
    verify(jobService, never()).runAsJob(any(JobContextConsumer.class));
  }

  @Test
  @WithInstructorCoursePermissions
  public void post_returns_400_when_only_the_installation_id_or_only_the_org_name_is_missing()
      throws Exception {
    Course noInstallation = Course.builder().id(2L).courseName("A").orgName("org").build();
    Course noOrg = Course.builder().id(3L).courseName("B").installationId("1234").build();
    when(courseRepository.findById(eq(2L))).thenReturn(Optional.of(noInstallation));
    when(courseRepository.findById(eq(3L))).thenReturn(Optional.of(noOrg));

    for (String courseId : new String[] {"2", "3"}) {
      mockMvc
          .perform(
              post("/api/assignments/post")
                  .with(csrf())
                  .param("courseId", courseId)
                  .param("repoPrefix", "hw1")
                  .param("asnType", "INDIVIDUAL")
                  .param("visibility", "PUBLIC")
                  .param("permission", "READ"))
          .andExpect(status().isBadRequest());
    }
    verify(assignmentRepository, never()).save(any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void post_returns_400_for_a_blank_repo_prefix() throws Exception {
    MvcResult response =
        mockMvc
            .perform(
                post("/api/assignments/post")
                    .with(csrf())
                    .param("courseId", "1")
                    .param("repoPrefix", "   ")
                    .param("asnType", "INDIVIDUAL")
                    .param("visibility", "PUBLIC")
                    .param("permission", "READ"))
            .andExpect(status().isBadRequest())
            .andReturn();

    assertEquals(
        Map.of("message", "repoPrefix must not be blank", "type", "IllegalArgumentException"),
        errorBody(response));
    verify(assignmentRepository, never()).save(any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void post_returns_400_for_a_team_regex_on_an_individual_assignment() throws Exception {
    MvcResult response =
        mockMvc
            .perform(
                post("/api/assignments/post")
                    .with(csrf())
                    .param("courseId", "1")
                    .param("repoPrefix", "hw1")
                    .param("asnType", "INDIVIDUAL")
                    .param("visibility", "PUBLIC")
                    .param("permission", "READ")
                    .param("teamRegex", "team-.*"))
            .andExpect(status().isBadRequest())
            .andReturn();

    assertEquals(
        Map.of(
            "message",
            "teamRegex is only allowed for TEAM assignments",
            "type",
            "IllegalArgumentException"),
        errorBody(response));
    verify(assignmentRepository, never()).save(any());
    verify(jobService, never()).runAsJob(any(JobContextConsumer.class));
  }

  @Test
  @WithInstructorCoursePermissions
  public void post_returns_400_for_create_repos_for_on_a_team_assignment() throws Exception {
    MvcResult response =
        mockMvc
            .perform(
                post("/api/assignments/post")
                    .with(csrf())
                    .param("courseId", "1")
                    .param("repoPrefix", "proj")
                    .param("asnType", "TEAM")
                    .param("visibility", "PUBLIC")
                    .param("permission", "READ")
                    .param("createReposFor", "STAFF_ONLY"))
            .andExpect(status().isBadRequest())
            .andReturn();

    assertEquals(
        Map.of(
            "message",
            "createReposFor is only allowed for INDIVIDUAL assignments",
            "type",
            "IllegalArgumentException"),
        errorBody(response));
    verify(assignmentRepository, never()).save(any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void post_returns_400_for_an_invalid_team_regex() throws Exception {
    MvcResult response =
        mockMvc
            .perform(
                post("/api/assignments/post")
                    .with(csrf())
                    .param("courseId", "1")
                    .param("repoPrefix", "proj")
                    .param("asnType", "TEAM")
                    .param("visibility", "PUBLIC")
                    .param("permission", "READ")
                    .param("teamRegex", "team-("))
            .andExpect(status().isBadRequest())
            .andReturn();

    assertEquals(
        Map.of(
            "message",
            "teamRegex is not a valid regular expression",
            "type",
            "IllegalArgumentException"),
        errorBody(response));
    verify(assignmentRepository, never()).save(any());
  }

  // ---- PUT ----

  private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder putRequest(
      String assignmentId) {
    return put("/api/assignments/put")
        .with(csrf())
        .param("courseId", "1")
        .param("assignmentId", assignmentId);
  }

  @Test
  @WithInstructorCoursePermissions
  public void put_updates_an_individual_assignment_and_starts_the_student_repo_job()
      throws Exception {
    Assignment existing = individual(5L, "hw1");
    when(assignmentRepository.findById(eq(5L))).thenReturn(Optional.of(existing));

    MvcResult response =
        mockMvc
            .perform(
                putRequest("5")
                    .param("repoPrefix", " hw1-v2 ")
                    .param("visibility", "PRIVATE")
                    .param("permission", "WRITE")
                    .param("createReposFor", "STUDENTS_AND_STAFF"))
            .andExpect(status().isOk())
            .andReturn();

    assertEquals("hw1-v2", existing.getRepoPrefix());
    assertEquals(Visibility.PRIVATE, existing.getVisibility());
    assertEquals(Permission.WRITE, existing.getPermission());
    assertEquals(RepositoryCreationOption.STUDENTS_AND_STAFF, existing.getCreateReposFor());
    assertNull(existing.getTeamRegex());
    verify(assignmentRepository, times(1)).save(eq(existing));

    CreateStudentOrStaffRepositoriesJob started =
        assertInstanceOf(CreateStudentOrStaffRepositoriesJob.class, launchedJob());
    assertEquals("hw1-v2", ReflectionTestUtils.getField(started, "repositoryPrefix"));
    assertEquals(true, ReflectionTestUtils.getField(started, "isPrivate"));
    assertEquals(RepositoryPermissions.WRITE, ReflectionTestUtils.getField(started, "permissions"));
    assertEquals(
        RepositoryCreationOption.STUDENTS_AND_STAFF,
        ReflectionTestUtils.getField(started, "creationOption"));

    assertEquals(
        mapper.writeValueAsString(new AssignmentWithJob(existing, job)),
        response.getResponse().getContentAsString());
  }

  @Test
  @WithInstructorCoursePermissions
  public void put_updates_a_team_assignment_and_starts_the_team_repo_job() throws Exception {
    Assignment existing = team(6L, "proj", "team-.*");
    when(assignmentRepository.findById(eq(6L))).thenReturn(Optional.of(existing));

    mockMvc
        .perform(
            putRequest("6")
                .param("repoPrefix", "proj2")
                .param("visibility", "PUBLIC")
                .param("permission", "ADMIN")
                .param("teamRegex", "s26-.*"))
        .andExpect(status().isOk());

    assertEquals("proj2", existing.getRepoPrefix());
    assertEquals(Visibility.PUBLIC, existing.getVisibility());
    assertEquals(Permission.ADMIN, existing.getPermission());
    assertEquals("s26-.*", existing.getTeamRegex());
    assertNull(existing.getCreateReposFor());
    verify(assignmentRepository, times(1)).save(eq(existing));

    CreateTeamRepositoriesJob started =
        assertInstanceOf(CreateTeamRepositoriesJob.class, launchedJob());
    assertEquals("proj2", ReflectionTestUtils.getField(started, "repositoryPrefix"));
    assertEquals(false, ReflectionTestUtils.getField(started, "isPrivate"));
    assertEquals(RepositoryPermissions.ADMIN, ReflectionTestUtils.getField(started, "permissions"));
    assertEquals("s26-.*", ReflectionTestUtils.getField(started, "teamRegex"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void put_without_a_team_regex_resets_a_team_assignment_to_every_team() throws Exception {
    Assignment existing = team(6L, "proj", "team-.*");
    when(assignmentRepository.findById(eq(6L))).thenReturn(Optional.of(existing));

    mockMvc
        .perform(
            putRequest("6")
                .param("repoPrefix", "proj")
                .param("visibility", "PUBLIC")
                .param("permission", "READ"))
        .andExpect(status().isOk());

    assertEquals(".*", existing.getTeamRegex());
  }

  @Test
  @WithInstructorCoursePermissions
  public void put_clears_the_field_that_does_not_apply_to_the_type_of_the_assignment()
      throws Exception {
    // rows that somehow have both fields set are normalized when they are edited
    Assignment individual = individual(5L, "hw1");
    individual.setTeamRegex("stale");
    Assignment team = team(6L, "proj", "team-.*");
    team.setCreateReposFor(RepositoryCreationOption.STAFF_ONLY);
    when(assignmentRepository.findById(eq(5L))).thenReturn(Optional.of(individual));
    when(assignmentRepository.findById(eq(6L))).thenReturn(Optional.of(team));

    mockMvc
        .perform(
            putRequest("5")
                .param("repoPrefix", "hw1")
                .param("visibility", "PUBLIC")
                .param("permission", "READ"))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            putRequest("6")
                .param("repoPrefix", "proj")
                .param("visibility", "PUBLIC")
                .param("permission", "READ")
                .param("teamRegex", "team-.*"))
        .andExpect(status().isOk());

    assertNull(individual.getTeamRegex());
    assertEquals(RepositoryCreationOption.STUDENTS_ONLY, individual.getCreateReposFor());
    assertNull(team.getCreateReposFor());
    assertEquals("team-.*", team.getTeamRegex());
  }

  @Test
  @WithInstructorCoursePermissions
  public void put_cannot_change_the_assignment_type() throws Exception {
    Assignment existing = individual(5L, "hw1");
    when(assignmentRepository.findById(eq(5L))).thenReturn(Optional.of(existing));

    mockMvc
        .perform(
            putRequest("5")
                .param("asnType", "TEAM")
                .param("repoPrefix", "hw1")
                .param("visibility", "PUBLIC")
                .param("permission", "READ"))
        .andExpect(status().isOk());

    assertEquals(AssignmentType.INDIVIDUAL, existing.getAsnType());
    assertNull(existing.getTeamRegex());
    assertInstanceOf(CreateStudentOrStaffRepositoriesJob.class, launchedJob());
  }

  @Test
  @WithInstructorCoursePermissions
  public void put_returns_404_when_course_does_not_exist() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(
                putRequest("5")
                    .param("repoPrefix", "hw1")
                    .param("visibility", "PUBLIC")
                    .param("permission", "READ"))
            .andExpect(status().isNotFound())
            .andReturn();

    assertEquals(
        Map.of("message", "Course with id 1 not found", "type", "EntityNotFoundException"),
        errorBody(response));
    verify(assignmentRepository, never()).findById(any());
    verify(assignmentRepository, never()).save(any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void put_returns_404_when_assignment_does_not_exist() throws Exception {
    when(assignmentRepository.findById(eq(5L))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(
                putRequest("5")
                    .param("repoPrefix", "hw1")
                    .param("visibility", "PUBLIC")
                    .param("permission", "READ"))
            .andExpect(status().isNotFound())
            .andReturn();

    assertEquals(
        Map.of("message", "Assignment with id 5 not found", "type", "EntityNotFoundException"),
        errorBody(response));
    verify(assignmentRepository, never()).save(any());
    verify(jobService, never()).runAsJob(any(JobContextConsumer.class));
  }

  @Test
  @WithInstructorCoursePermissions
  public void put_returns_404_when_assignment_belongs_to_another_course() throws Exception {
    Course other = Course.builder().id(2L).build();
    Assignment existing = individual(5L, "hw1");
    existing.setCourse(other);
    when(assignmentRepository.findById(eq(5L))).thenReturn(Optional.of(existing));

    mockMvc
        .perform(
            putRequest("5")
                .param("repoPrefix", "changed")
                .param("visibility", "PRIVATE")
                .param("permission", "ADMIN"))
        .andExpect(status().isNotFound());

    assertEquals("hw1", existing.getRepoPrefix());
    verify(assignmentRepository, never()).save(any());
    verify(jobService, never()).runAsJob(any(JobContextConsumer.class));
  }

  @Test
  @WithInstructorCoursePermissions
  public void put_returns_400_when_the_course_has_no_linked_organization() throws Exception {
    Course unlinked = Course.builder().id(1L).courseName("Unlinked").build();
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(unlinked));
    Assignment existing = individual(5L, "hw1");
    existing.setCourse(unlinked);
    when(assignmentRepository.findById(eq(5L))).thenReturn(Optional.of(existing));

    MvcResult response =
        mockMvc
            .perform(
                putRequest("5")
                    .param("repoPrefix", "hw1")
                    .param("visibility", "PUBLIC")
                    .param("permission", "READ"))
            .andExpect(status().isBadRequest())
            .andReturn();

    assertEquals("NoLinkedOrganizationException", errorBody(response).get("type"));
    verify(assignmentRepository, never()).save(any());
    verify(jobService, never()).runAsJob(any(JobContextConsumer.class));
  }

  @Test
  @WithInstructorCoursePermissions
  public void put_returns_400_for_fields_that_do_not_apply_to_the_assignment_type()
      throws Exception {
    Assignment individual = individual(5L, "hw1");
    Assignment team = team(6L, "proj", "team-.*");
    when(assignmentRepository.findById(eq(5L))).thenReturn(Optional.of(individual));
    when(assignmentRepository.findById(eq(6L))).thenReturn(Optional.of(team));

    mockMvc
        .perform(
            putRequest("5")
                .param("repoPrefix", "hw1")
                .param("visibility", "PUBLIC")
                .param("permission", "READ")
                .param("teamRegex", "x"))
        .andExpect(status().isBadRequest());
    mockMvc
        .perform(
            putRequest("6")
                .param("repoPrefix", "proj")
                .param("visibility", "PUBLIC")
                .param("permission", "READ")
                .param("createReposFor", "STAFF_ONLY"))
        .andExpect(status().isBadRequest());
    mockMvc
        .perform(
            putRequest("6")
                .param("repoPrefix", "proj")
                .param("visibility", "PUBLIC")
                .param("permission", "READ")
                .param("teamRegex", "("))
        .andExpect(status().isBadRequest());
    mockMvc
        .perform(
            putRequest("6")
                .param("repoPrefix", " ")
                .param("visibility", "PUBLIC")
                .param("permission", "READ"))
        .andExpect(status().isBadRequest());

    assertEquals("team-.*", team.getTeamRegex());
    verify(assignmentRepository, never()).save(any());
    verify(jobService, never()).runAsJob(any(JobContextConsumer.class));
  }

  // ---- DELETE ----

  @Test
  @WithInstructorCoursePermissions
  public void delete_removes_the_assignment() throws Exception {
    Assignment existing = individual(5L, "hw1");
    when(assignmentRepository.findById(eq(5L))).thenReturn(Optional.of(existing));

    MvcResult response =
        mockMvc
            .perform(delete("/api/assignments/5").with(csrf()).param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    verify(assignmentRepository, times(1)).delete(eq(existing));
    assertEquals(Map.of("message", "Assignment with id 5 deleted"), errorBody(response));
    verify(jobService, never()).runAsJob(any(JobContextConsumer.class));
  }

  @Test
  @WithInstructorCoursePermissions
  public void delete_returns_404_when_course_does_not_exist() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(delete("/api/assignments/5").with(csrf()).param("courseId", "1"))
            .andExpect(status().isNotFound())
            .andReturn();

    assertEquals(
        Map.of("message", "Course with id 1 not found", "type", "EntityNotFoundException"),
        errorBody(response));
    verify(assignmentRepository, never()).delete(any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void delete_returns_404_when_assignment_does_not_exist() throws Exception {
    when(assignmentRepository.findById(eq(5L))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(delete("/api/assignments/5").with(csrf()).param("courseId", "1"))
            .andExpect(status().isNotFound())
            .andReturn();

    assertEquals(
        Map.of("message", "Assignment with id 5 not found", "type", "EntityNotFoundException"),
        errorBody(response));
    verify(assignmentRepository, never()).delete(any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void delete_returns_404_when_assignment_belongs_to_another_course() throws Exception {
    Assignment existing = individual(5L, "hw1");
    existing.setCourse(Course.builder().id(2L).build());
    when(assignmentRepository.findById(eq(5L))).thenReturn(Optional.of(existing));

    mockMvc
        .perform(delete("/api/assignments/5").with(csrf()).param("courseId", "1"))
        .andExpect(status().isNotFound());

    verify(assignmentRepository, never()).delete(any());
  }
}
