package edu.ucsb.cs156.frontiers.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import edu.ucsb.cs156.frontiers.repositories.*;
import edu.ucsb.cs156.frontiers.services.GithubTeamService;
import edu.ucsb.cs156.frontiers.services.OrganizationMemberService;
import edu.ucsb.cs156.frontiers.services.UpdateUserService;
import edu.ucsb.cs156.jobs.entities.Job;
import edu.ucsb.cs156.jobs.repositories.JobsRepository;
import edu.ucsb.cs156.jobs.services.JobService;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
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
}
