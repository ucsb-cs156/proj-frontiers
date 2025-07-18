package edu.ucsb.cs156.frontiers.controllers;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.entities.Job;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.jobs.UpdateAllJob;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.JobsRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.repositories.UserRepository;
import edu.ucsb.cs156.frontiers.services.OrganizationMemberService;
import edu.ucsb.cs156.frontiers.services.UpdateUserService;
import edu.ucsb.cs156.frontiers.services.jobs.JobService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

/**
 * This class tests the ability of the JobsController to manipulate jobs, and
 * the funcitonality of the jobs system in general.
 * 
 * By contrast, JobsControllerJobsTests tests the ability of the JobsController
 * to launch specific jobs.
 *
 * @see JobsController
 * @see JobsControllerJobsTests
 */

@Slf4j
@WebMvcTest(controllers = JobsController.class)
public class JobsControllerDetailedTests extends ControllerTestCase {

  @MockitoBean
  JobsRepository jobsRepository;

  @MockitoBean
  UserRepository userRepository;

  @MockitoBean
  RosterStudentRepository rosterStudentRepository;

  @MockitoBean
  CourseRepository courseRepository;

  @MockitoBean
  UpdateUserService updateUserService; // This will be used in the UpdateAllJob to call the GithubSignInService

  @MockitoBean
  JobService jobService;

  @Autowired
  ObjectMapper objectMapper;

  @MockitoBean
  OrganizationMemberService organizationMemberService;

  @WithMockUser(roles = { "ADMIN" })
  @Test
  public void admin_can_get_all_jobs() throws Exception {

    // arrange

    Job job1 = Job.builder().log("this is job 1").build();
    Job job2 = Job.builder().log("this is job 2").build();

    ArrayList<Job> expectedJobs = new ArrayList<>();
    expectedJobs.addAll(Arrays.asList(job1, job2));

    when(jobsRepository.findAll()).thenReturn(expectedJobs);

    // act
    MvcResult response = mockMvc.perform(get("/api/jobs/all")).andExpect(status().isOk()).andReturn();

    // assert

    verify(jobsRepository, atLeastOnce()).findAll();
    String expectedJson = mapper.writeValueAsString(expectedJobs);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = { "ADMIN" })
  @Test
  public void api_getJobLogById__admin_logged_in__returns_job_by_id() throws Exception {

    // arrange

    Job job = Job.builder().id(1L).status("completed").log("This is a test job log.").build();

    when(jobsRepository.findById(eq(1L))).thenReturn(Optional.of(job));

    // act

    MvcResult response = mockMvc.perform(get("/api/jobs?id=1")).andExpect(status().isOk()).andReturn();

    // assert

    verify(jobsRepository, times(1)).findById(1L);
    String expectedJson = mapper.writeValueAsString(job);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = { "ADMIN" })
  @Test
  public void api_getJobLogById__admin_logged_in__returns_not_found_for_missing_job()
      throws Exception {

    // arrange

    when(jobsRepository.findById(eq(2L))).thenReturn(Optional.empty());

    // act

    MvcResult response = mockMvc.perform(get("/api/jobs?id=2")).andExpect(status().isNotFound()).andReturn();

    // assert

    verify(jobsRepository, times(1)).findById(2L);
    Map<String, Object> json = responseToJson(response);
    assertEquals("EntityNotFoundException", json.get("type"));
    assertEquals("Job with id 2 not found", json.get("message"));
  }

  @WithMockUser(roles = { "ADMIN" })
  @Test
  public void admin_can_delete_all_jobs() throws Exception {

    doNothing().when(jobsRepository).deleteAll();

    // act
    MvcResult response = mockMvc
        .perform(delete("/api/jobs/all").with(csrf()))
        .andExpect(status().isOk())
        .andReturn();

    // assert

    verify(jobsRepository, times(1)).deleteAll();
    String expectedJson = mapper.writeValueAsString(Map.of("message", "All jobs deleted"));
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = { "ADMIN" })
  @Test
  public void test_getJobLogs_admin_can_get_job_log() throws Exception {
    // Arrange
    Long jobId = 1L;
    String jobLog = "This is a job log";
    when(jobService.getJobLogs(eq(jobId))).thenReturn(jobLog);

    // Act & Assert
    mockMvc
        .perform(get("/api/jobs/logs/{id}", jobId))
        .andExpect(status().isOk())
        .andExpect(content().string(jobLog));
  }

  @WithMockUser(roles = { "ADMIN" })
  @Test
  public void test_getJobLogs_admin_can_get_empty_log() throws Exception {
    // Arrange
    Long jobId = 2L;
    when(jobService.getJobLogs(eq(jobId))).thenReturn("");

    // Act & Assert
    mockMvc
        .perform(get("/api/jobs/logs/{id}", jobId))
        .andExpect(status().isOk())
        .andExpect(content().string(""));
  }

  @WithMockUser(roles = { "ADMIN" })
  @Test
  public void admin_can_delete_specific_job() throws Exception {

    // arrange

    when(jobsRepository.existsById(eq(1L))).thenReturn(true);
    doNothing().when(jobsRepository).deleteById(eq(1L));

    // act
    MvcResult response = mockMvc
        .perform(delete("/api/jobs?id=1").with(csrf()))
        .andExpect(status().isOk())
        .andReturn();

    // assert

    verify(jobsRepository, times(1)).deleteById(eq(1L));
    String expectedJson = mapper.writeValueAsString(Map.of("message", "Job with id 1 deleted"));
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = { "ADMIN" })
  @Test
  public void admin_gets_reasonable_error_when_deleting_non_existing_job() throws Exception {

    // arrange

    when(jobsRepository.existsById(eq(2L))).thenReturn(false);

    // act
    MvcResult response = mockMvc
        .perform(delete("/api/jobs?id=2").with(csrf()))
        .andExpect(status().isOk())
        .andReturn();

    // assert

    verify(jobsRepository, times(1)).existsById(eq(2L));
    String expectedJson = mapper.writeValueAsString(Map.of("message", "Job with id 2 not found"));
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }



  @WithMockUser(roles = { "ADMIN" })
  @Test
  public void admin_can_launch_updateAll_job() throws Exception {

    // arrange

    User user = currentUserService.getUser();

    Job jobStarted = Job.builder()
        .id(0L)
        .createdBy(user)
        .createdAt(null)
        .updatedAt(null)
        .status("started")
        .build();

    when(jobService.runAsJob(any(UpdateAllJob.class))).thenReturn(jobStarted);

    // act
    MvcResult response = mockMvc
        .perform(post("/api/jobs/launch/updateAll").with(csrf()))
        .andExpect(status().isOk())
        .andReturn();

    verify(jobService).runAsJob(any(UpdateAllJob.class));
    // assert
    String responseString = response.getResponse().getContentAsString();
    String expectedResponse = objectMapper.writeValueAsString(jobStarted);
    assertEquals(expectedResponse, responseString);
  }
}