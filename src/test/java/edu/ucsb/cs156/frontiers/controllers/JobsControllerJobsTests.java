package edu.ucsb.cs156.frontiers.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.annotations.WithInstructorCoursePermissions;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.Team;
import edu.ucsb.cs156.frontiers.entities.TeamMember;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.jobs.AddTeamMemberToGithubJob;
import edu.ucsb.cs156.frontiers.jobs.AddTeamToGithubJob;
import edu.ucsb.cs156.frontiers.jobs.DeleteTeamFromGithubJob;
import edu.ucsb.cs156.frontiers.jobs.DeleteTeamMemberFromGithubJob;
import edu.ucsb.cs156.frontiers.jobs.MembershipAuditJob;
import edu.ucsb.cs156.frontiers.jobs.PullTeamsFromGithubJob;
import edu.ucsb.cs156.frontiers.jobs.PushTeamsToGithubJob;
import edu.ucsb.cs156.frontiers.jobs.UpdateAllJob;
import edu.ucsb.cs156.frontiers.models.JobLogTail;
import edu.ucsb.cs156.frontiers.repositories.*;
import edu.ucsb.cs156.frontiers.services.GithubTeamService;
import edu.ucsb.cs156.frontiers.services.OrganizationMemberService;
import edu.ucsb.cs156.frontiers.services.UpdateUserService;
import edu.ucsb.cs156.jobs.entities.Job;
import edu.ucsb.cs156.jobs.entities.JobLog;
import edu.ucsb.cs156.jobs.repositories.JobsRepository;
import edu.ucsb.cs156.jobs.services.JobService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

/**
 * This class tests the ability of the JobsController to launch jobs, and to list jobs scoped to a
 * course. The generic admin endpoints (list all / paginated / logs / delete) are provided and
 * tested by the lib-jobs library itself.
 *
 * @see JobsController
 */
@Slf4j
@WebMvcTest(controllers = JobsController.class)
public class JobsControllerJobsTests extends ControllerTestCase {

  @MockitoBean JobsRepository jobsRepository;

  @MockitoBean UserRepository userRepository;

  @MockitoBean
  UpdateUserService
      updateUserService; // This will be used in the UpdateAllJob to call the GithubSignInService

  @MockitoBean JobService jobService;

  @MockitoBean RosterStudentRepository rosterStudentRepository;

  @MockitoBean CourseRepository courseRepository;

  @MockitoBean OrganizationMemberService organizationMemberService;

  @MockitoBean CourseStaffRepository courseStaffRepository;

  @MockitoBean TeamRepository teamRepository;

  @MockitoBean TeamMemberRepository teamMemberRepository;

  @MockitoBean GithubTeamService githubTeamService;

  @Autowired ObjectMapper objectMapper;

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void admin_can_launch_updateAll_job() throws Exception {

    // arrange

    User user = currentUserService.getUser();

    Job jobStarted =
        Job.builder()
            .id(0L)
            .createdById(user.getId())
            .createdByEmail(user.getEmail())
            .createdAt(null)
            .updatedAt(null)
            .status("started")
            .build();

    when(jobService.runAsJob(any(UpdateAllJob.class))).thenReturn(jobStarted);

    // act
    MvcResult result =
        mockMvc
            .perform(post("/api/jobs/launch/updateAll").with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // assert

    String response = result.getResponse().getContentAsString();
    String expectedResponse = objectMapper.writeValueAsString(jobStarted);
    verify(jobService, times(1)).runAsJob(any(UpdateAllJob.class));
    assertEquals(expectedResponse, response);
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void admin_can_launch_auditAllCourses_job() throws Exception {

    // arrange

    User user = currentUserService.getUser();

    Job jobStarted =
        Job.builder()
            .id(0L)
            .createdById(user.getId())
            .createdByEmail(user.getEmail())
            .createdAt(null)
            .updatedAt(null)
            .status("started")
            .build();

    String expectedResponse = objectMapper.writeValueAsString(jobStarted);

    when(jobService.runAsJob(any(MembershipAuditJob.class))).thenReturn(jobStarted);

    // act
    MvcResult result =
        mockMvc
            .perform(post("/api/jobs/launch/auditAllCourses").with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // assert

    String response = result.getResponse().getContentAsString();
    verify(jobService, times(1)).runAsJob(any(MembershipAuditJob.class));
    assertEquals(expectedResponse, response);
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void admin_can_launch_pushTeamsToGithub_job() throws Exception {

    // arrange

    User user = currentUserService.getUser();

    Job jobStarted =
        Job.builder()
            .id(0L)
            .createdById(user.getId())
            .createdByEmail(user.getEmail())
            .createdAt(null)
            .updatedAt(null)
            .status("started")
            .build();

    String expectedResponse = objectMapper.writeValueAsString(jobStarted);

    when(jobService.runAsJob(any(PushTeamsToGithubJob.class))).thenReturn(jobStarted);

    // act
    MvcResult result =
        mockMvc
            .perform(post("/api/jobs/launch/pushTeamsToGithub").param("courseId", "1").with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // assert

    String response = result.getResponse().getContentAsString();
    verify(jobService, times(1)).runAsJob(any(PushTeamsToGithubJob.class));
    assertEquals(expectedResponse, response);
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void admin_can_launch_pullTeamsFromGithub_job() throws Exception {

    User user = currentUserService.getUser();

    Job jobStarted =
        Job.builder()
            .id(0L)
            .createdById(user.getId())
            .createdByEmail(user.getEmail())
            .createdAt(null)
            .updatedAt(null)
            .status("started")
            .build();

    String expectedResponse = objectMapper.writeValueAsString(jobStarted);

    when(jobService.runAsJob(any(PullTeamsFromGithubJob.class))).thenReturn(jobStarted);

    MvcResult result =
        mockMvc
            .perform(
                post("/api/jobs/launch/pullTeamsFromGithub").param("courseId", "1").with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    String response = result.getResponse().getContentAsString();
    verify(jobService, times(1)).runAsJob(any(PullTeamsFromGithubJob.class));
    assertEquals(expectedResponse, response);
  }

  @WithInstructorCoursePermissions
  @Test
  public void instructor_can_launch_deleteTeamMemberFromGithub_job() throws Exception {

    // arrange

    User user = currentUserService.getUser();

    Course course = Course.builder().id(1L).orgName("test-org").installationId("123").build();
    Team team = Team.builder().id(1L).githubTeamId(456).build();
    RosterStudent rosterStudent = RosterStudent.builder().id(1L).githubLogin("testuser").build();
    TeamMember teamMember =
        TeamMember.builder().id(123L).team(team).rosterStudent(rosterStudent).build();

    Job jobStarted =
        Job.builder()
            .id(0L)
            .createdById(user.getId())
            .createdByEmail(user.getEmail())
            .createdAt(null)
            .updatedAt(null)
            .status("started")
            .build();

    String expectedResponse = objectMapper.writeValueAsString(jobStarted);

    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
    when(jobService.runAsJob(any(DeleteTeamMemberFromGithubJob.class))).thenReturn(jobStarted);

    // act
    MvcResult result =
        mockMvc
            .perform(
                post("/api/jobs/launch/deleteTeamMemberFromGithub")
                    .param("memberGithubLogin", "testuser")
                    .param("githubTeamId", "456")
                    .param("courseId", "1")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // assert

    String response = result.getResponse().getContentAsString();
    verify(jobService, times(1)).runAsJob(any(DeleteTeamMemberFromGithubJob.class));
    assertEquals(expectedResponse, response);
  }

  @WithInstructorCoursePermissions
  @Test
  public void instructor_can_launch_addTeamMemberToGithub_job() throws Exception {

    // arrange
    User user = currentUserService.getUser();

    Course course = Course.builder().id(1L).orgName("test-org").installationId("123").build();
    Team team = Team.builder().id(1L).githubTeamId(456).build();
    RosterStudent rosterStudent = RosterStudent.builder().id(1L).githubLogin("testuser").build();
    TeamMember teamMember =
        TeamMember.builder().id(123L).team(team).rosterStudent(rosterStudent).build();

    Job jobStarted =
        Job.builder()
            .id(0L)
            .createdById(user.getId())
            .createdByEmail(user.getEmail())
            .createdAt(null)
            .updatedAt(null)
            .status("started")
            .build();

    String expectedResponse = objectMapper.writeValueAsString(jobStarted);

    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
    when(jobService.runAsJob(any(AddTeamMemberToGithubJob.class))).thenReturn(jobStarted);

    // act
    MvcResult result =
        mockMvc
            .perform(
                post("/api/jobs/launch/addTeamMemberToGithub")
                    .param("memberGithubLogin", "testuser")
                    .param("githubTeamId", "456")
                    .param("teamMemberId", "123")
                    .param("courseId", "1")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // assert

    String response = result.getResponse().getContentAsString();
    verify(jobService, times(1)).runAsJob(any(AddTeamMemberToGithubJob.class));
    assertEquals(expectedResponse, response);
  }

  @WithInstructorCoursePermissions
  @Test
  public void instructor_can_launch_deleteTeamFromGithub_job() throws Exception {

    // arrange

    User user = currentUserService.getUser();

    Course course = Course.builder().id(1L).orgName("test-org").installationId("123").build();
    Team team = Team.builder().id(1L).githubTeamId(456).build();

    Job jobStarted =
        Job.builder()
            .id(0L)
            .createdById(user.getId())
            .createdByEmail(user.getEmail())
            .createdAt(null)
            .updatedAt(null)
            .status("started")
            .build();

    String expectedResponse = objectMapper.writeValueAsString(jobStarted);

    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
    when(jobService.runAsJob(any(DeleteTeamFromGithubJob.class))).thenReturn(jobStarted);

    // act
    MvcResult result =
        mockMvc
            .perform(
                post("/api/jobs/launch/deleteTeamFromGithub")
                    .param("githubTeamId", "456")
                    .param("courseId", "1")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // assert

    String response = result.getResponse().getContentAsString();
    verify(jobService, times(1)).runAsJob(any(DeleteTeamFromGithubJob.class));
    assertEquals(expectedResponse, response);
  }

  @WithInstructorCoursePermissions
  @Test
  public void instructor_can_launch_addTeamToGithub_job() throws Exception {
    User user = currentUserService.getUser();
    Course course = Course.builder().id(1L).orgName("test-org").installationId("123").build();
    Job jobStarted =
        Job.builder()
            .id(0L)
            .createdById(user.getId())
            .createdByEmail(user.getEmail())
            .createdAt(null)
            .updatedAt(null)
            .status("started")
            .build();

    String expectedResponse = objectMapper.writeValueAsString(jobStarted);

    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
    when(jobService.runAsJob(any(AddTeamToGithubJob.class))).thenReturn(jobStarted);

    MvcResult result =
        mockMvc
            .perform(
                post("/api/jobs/launch/addTeamToGithub")
                    .param("teamName", "test-team")
                    .param("courseId", "1")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    String response = result.getResponse().getContentAsString();
    verify(jobService, times(1)).runAsJob(any(AddTeamToGithubJob.class));
    assertEquals(expectedResponse, response);
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void admin_can_get_jobs_by_course() throws Exception {

    // arrange
    Long courseId = 5L;

    Job job1 = Job.builder().id(1L).scopeType("course").scopeId(courseId).build();
    Job job2 = Job.builder().id(2L).scopeType("course").scopeId(courseId).build();

    when(jobsRepository.findByScopeTypeAndScopeIdOrderByIdDesc("course", courseId))
        .thenReturn(List.of(job1, job2));
    when(jobService.getJobLogPreview(1L)).thenReturn("job for course 5 - 1");
    when(jobService.getJobLogPreview(2L)).thenReturn("job for course 5 - 2");

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/jobs/course").param("courseId", courseId.toString()))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(jobsRepository).findByScopeTypeAndScopeIdOrderByIdDesc(eq("course"), eq(courseId));
    verify(jobService).getJobLogPreview(1L);
    verify(jobService).getJobLogPreview(2L);

    Job expectedJob1 =
        Job.builder()
            .id(1L)
            .scopeType("course")
            .scopeId(courseId)
            .log("job for course 5 - 1")
            .build();
    Job expectedJob2 =
        Job.builder()
            .id(2L)
            .scopeType("course")
            .scopeId(courseId)
            .log("job for course 5 - 2")
            .build();

    String expectedJson = objectMapper.writeValueAsString(List.of(expectedJob1, expectedJob2));
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  // Tests for GET /api/jobs/course/logs

  @Test
  public void logged_out_users_cannot_get_job_logs_by_course() throws Exception {
    mockMvc
        .perform(get("/api/jobs/course/logs").param("courseId", "5").param("jobId", "1"))
        .andExpect(status().isForbidden());
    verify(jobService, never()).getJobLogs(any());
  }

  @WithMockUser(roles = {"INSTRUCTOR"})
  @Test
  public void instructor_without_course_permissions_cannot_get_job_logs_by_course()
      throws Exception {
    mockMvc
        .perform(get("/api/jobs/course/logs").param("courseId", "5").param("jobId", "1"))
        .andExpect(status().isForbidden());
    verify(jobService, never()).getJobLogs(any());
  }

  @WithInstructorCoursePermissions
  @Test
  public void job_logs_by_course_returns_404_when_job_does_not_exist() throws Exception {
    // arrange
    when(jobsRepository.findById(eq(7L))).thenReturn(Optional.empty());

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/jobs/course/logs").param("courseId", "5").param("jobId", "7"))
            .andExpect(status().isNotFound())
            .andReturn();

    // assert
    verify(jobsRepository).findById(eq(7L));
    verify(jobService, never()).getJobLogs(any());
    Map<String, Object> json = responseToJson(response);
    assertEquals("EntityNotFoundException", json.get("type"));
    assertEquals("Job with id 7 not found", json.get("message"));
  }

  @WithInstructorCoursePermissions
  @Test
  public void job_logs_by_course_returns_404_when_job_belongs_to_a_different_course()
      throws Exception {
    // arrange
    Job job = Job.builder().id(7L).scopeType("course").scopeId(6L).build();
    when(jobsRepository.findById(eq(7L))).thenReturn(Optional.of(job));

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/jobs/course/logs").param("courseId", "5").param("jobId", "7"))
            .andExpect(status().isNotFound())
            .andReturn();

    // assert
    verify(jobsRepository).findById(eq(7L));
    verify(jobService, never()).getJobLogs(any());
    Map<String, Object> json = responseToJson(response);
    assertEquals("EntityNotFoundException", json.get("type"));
    assertEquals("Job with id 7 not found", json.get("message"));
  }

  @WithInstructorCoursePermissions
  @Test
  public void job_logs_by_course_returns_404_when_job_has_a_different_scope_type()
      throws Exception {
    // arrange: same scopeId as the requested course, but not a course-scoped job
    Job job = Job.builder().id(7L).scopeType("other").scopeId(5L).build();
    when(jobsRepository.findById(eq(7L))).thenReturn(Optional.of(job));

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/jobs/course/logs").param("courseId", "5").param("jobId", "7"))
            .andExpect(status().isNotFound())
            .andReturn();

    // assert
    verify(jobsRepository).findById(eq(7L));
    verify(jobService, never()).getJobLogs(any());
    Map<String, Object> json = responseToJson(response);
    assertEquals("EntityNotFoundException", json.get("type"));
    assertEquals("Job with id 7 not found", json.get("message"));
  }

  @WithInstructorCoursePermissions
  @Test
  public void job_logs_by_course_returns_404_when_job_is_unscoped() throws Exception {
    // arrange: scopeType and scopeId both null (an admin-global job)
    Job job = Job.builder().id(7L).scopeType(null).scopeId(null).build();
    when(jobsRepository.findById(eq(7L))).thenReturn(Optional.of(job));

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/jobs/course/logs").param("courseId", "5").param("jobId", "7"))
            .andExpect(status().isNotFound())
            .andReturn();

    // assert
    verify(jobService, never()).getJobLogs(any());
    Map<String, Object> json = responseToJson(response);
    assertEquals("EntityNotFoundException", json.get("type"));
    assertEquals("Job with id 7 not found", json.get("message"));
  }

  @WithInstructorCoursePermissions
  @Test
  public void instructor_with_course_permissions_can_get_full_job_log() throws Exception {
    // arrange
    Job job = Job.builder().id(7L).scopeType("course").scopeId(5L).build();
    String fullLog =
        "line 1\nline 2\nline 3\nline 4\nline 5\nline 6\nline 7\nline 8\nline 9\nline 10\nline 11";
    when(jobsRepository.findById(eq(7L))).thenReturn(Optional.of(job));
    when(jobService.getJobLogs(eq(7L))).thenReturn(fullLog);

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/jobs/course/logs").param("courseId", "5").param("jobId", "7"))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(jobsRepository).findById(eq(7L));
    verify(jobService, times(1)).getJobLogs(eq(7L));
    assertEquals(fullLog, response.getResponse().getContentAsString());
  }

  // Tests for GET /api/jobs/course/logs/tail

  private static JobLog logLine(long id, long jobId, String message) {
    return JobLog.builder().id(id).jobId(jobId).message(message).build();
  }

  @Test
  public void logged_out_users_cannot_get_the_job_log_tail() throws Exception {
    mockMvc
        .perform(get("/api/jobs/course/logs/tail").param("courseId", "5").param("jobId", "1"))
        .andExpect(status().isForbidden());
    verify(jobService, never()).getJobLogTail(any(), any());
  }

  @WithMockUser(roles = {"INSTRUCTOR"})
  @Test
  public void instructor_without_course_permissions_cannot_get_the_job_log_tail() throws Exception {
    mockMvc
        .perform(get("/api/jobs/course/logs/tail").param("courseId", "5").param("jobId", "1"))
        .andExpect(status().isForbidden());
    verify(jobService, never()).getJobLogTail(any(), any());
  }

  @WithInstructorCoursePermissions
  @Test
  public void job_log_tail_returns_404_when_job_does_not_exist() throws Exception {
    when(jobsRepository.findById(eq(7L))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(get("/api/jobs/course/logs/tail").param("courseId", "5").param("jobId", "7"))
            .andExpect(status().isNotFound())
            .andReturn();

    verify(jobService, never()).getJobLogTail(any(), any());
    Map<String, Object> json = responseToJson(response);
    assertEquals("EntityNotFoundException", json.get("type"));
    assertEquals("Job with id 7 not found", json.get("message"));
  }

  @WithInstructorCoursePermissions
  @Test
  public void job_log_tail_returns_404_for_a_job_of_a_different_course_or_scope() throws Exception {
    Job otherCourse = Job.builder().id(7L).scopeType("course").scopeId(6L).build();
    Job otherScope = Job.builder().id(8L).scopeType("other").scopeId(5L).build();
    Job unscoped = Job.builder().id(9L).build();
    when(jobsRepository.findById(eq(7L))).thenReturn(Optional.of(otherCourse));
    when(jobsRepository.findById(eq(8L))).thenReturn(Optional.of(otherScope));
    when(jobsRepository.findById(eq(9L))).thenReturn(Optional.of(unscoped));

    for (String jobId : new String[] {"7", "8", "9"}) {
      mockMvc
          .perform(get("/api/jobs/course/logs/tail").param("courseId", "5").param("jobId", jobId))
          .andExpect(status().isNotFound());
    }

    verify(jobService, never()).getJobLogTail(any(), any());
  }

  @WithInstructorCoursePermissions
  @Test
  public void job_log_tail_returns_the_status_and_the_lines_after_the_given_id() throws Exception {
    Job job = Job.builder().id(7L).status("running").scopeType("course").scopeId(5L).build();
    List<JobLog> lines = List.of(logLine(41L, 7L, "first new"), logLine(42L, 7L, "second new"));
    when(jobsRepository.findById(eq(7L))).thenReturn(Optional.of(job));
    when(jobService.getJobLogTail(eq(7L), eq(40L))).thenReturn(lines);

    MvcResult response =
        mockMvc
            .perform(
                get("/api/jobs/course/logs/tail")
                    .param("courseId", "5")
                    .param("jobId", "7")
                    .param("afterId", "40"))
            .andExpect(status().isOk())
            .andReturn();

    assertEquals(
        objectMapper.writeValueAsString(new JobLogTail("running", lines)),
        response.getResponse().getContentAsString());
    verify(jobService, times(1)).getJobLogTail(eq(7L), eq(40L));
  }

  @WithInstructorCoursePermissions
  @Test
  public void job_log_tail_starts_from_the_beginning_when_after_id_is_not_given() throws Exception {
    Job job = Job.builder().id(7L).status("complete").scopeType("course").scopeId(5L).build();
    when(jobsRepository.findById(eq(7L))).thenReturn(Optional.of(job));
    when(jobService.getJobLogTail(eq(7L), eq(0L))).thenReturn(List.of());

    MvcResult response =
        mockMvc
            .perform(get("/api/jobs/course/logs/tail").param("courseId", "5").param("jobId", "7"))
            .andExpect(status().isOk())
            .andReturn();

    assertEquals(
        objectMapper.writeValueAsString(new JobLogTail("complete", List.of())),
        response.getResponse().getContentAsString());
    verify(jobService, times(1)).getJobLogTail(eq(7L), eq(0L));
  }

  @WithInstructorCoursePermissions
  @Test
  public void job_log_tail_reads_the_status_before_the_lines() throws Exception {
    Job job = Job.builder().id(7L).status("running").scopeType("course").scopeId(5L).build();
    when(jobsRepository.findById(eq(7L))).thenReturn(Optional.of(job));
    when(jobService.getJobLogTail(eq(7L), eq(0L))).thenReturn(List.of());

    mockMvc
        .perform(get("/api/jobs/course/logs/tail").param("courseId", "5").param("jobId", "7"))
        .andExpect(status().isOk());

    // a job that has finished when its status is read has written all of its lines by the time
    // they are read, so a client that stops polling on a finished status misses none
    InOrder inOrder = inOrder(jobsRepository, jobService);
    inOrder.verify(jobsRepository).findById(eq(7L));
    inOrder.verify(jobService).getJobLogTail(eq(7L), eq(0L));
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void admin_can_get_the_job_log_tail_by_course() throws Exception {
    Job job = Job.builder().id(8L).status("error").scopeType("course").scopeId(5L).build();
    when(jobsRepository.findById(eq(8L))).thenReturn(Optional.of(job));
    when(jobService.getJobLogTail(eq(8L), eq(0L))).thenReturn(List.of(logLine(1L, 8L, "boom")));

    mockMvc
        .perform(get("/api/jobs/course/logs/tail").param("courseId", "5").param("jobId", "8"))
        .andExpect(status().isOk());

    verify(jobService, times(1)).getJobLogTail(eq(8L), eq(0L));
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void admin_can_get_full_job_log_by_course() throws Exception {
    // arrange
    Job job = Job.builder().id(8L).scopeType("course").scopeId(5L).build();
    when(jobsRepository.findById(eq(8L))).thenReturn(Optional.of(job));
    when(jobService.getJobLogs(eq(8L))).thenReturn("");

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/jobs/course/logs").param("courseId", "5").param("jobId", "8"))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(jobService, times(1)).getJobLogs(eq(8L));
    assertEquals("", response.getResponse().getContentAsString());
  }
}
