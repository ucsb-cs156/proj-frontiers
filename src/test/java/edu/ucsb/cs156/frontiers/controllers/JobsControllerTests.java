package edu.ucsb.cs156.frontiers.controllers;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import edu.ucsb.cs156.frontiers.controllers.JobsController;
import edu.ucsb.cs156.frontiers.entities.Job;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.repositories.JobsRepository;
import edu.ucsb.cs156.frontiers.repositories.UserRepository;
import edu.ucsb.cs156.frontiers.services.UpdateUserService;
import edu.ucsb.cs156.frontiers.services.jobs.JobContext;
import edu.ucsb.cs156.frontiers.services.jobs.JobService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@Slf4j
@WebMvcTest(controllers = JobsController.class)
@Import(JobService.class)
@AutoConfigureDataJpa
public class JobsControllerTests extends ControllerTestCase {

  @MockitoBean
  JobsRepository jobsRepository;

  @MockitoBean
  UserRepository userRepository;

  @MockitoBean
  UpdateUserService updateUserService; // This will be used in the UpdateAllJob to call the GithubSignInService

  @Autowired
  JobService jobService;

  @Autowired
  ObjectMapper objectMapper;


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
    Job job = Job.builder().build();
    job.setLog(jobLog);
    when(jobsRepository.findById(jobId)).thenReturn(Optional.of(job));

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
    Job job = Job.builder().build();
    job.setLog("");
    when(jobsRepository.findById(jobId)).thenReturn(Optional.of(job));

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
  public void admin_can_launch_test_job() throws Exception {

    // arrange

    User user = currentUserService.getUser();

    Job jobStarted = Job.builder()
        .id(0L)
        .createdBy(user)
        .createdAt(null)
        .updatedAt(null)
        .status("running")
        .log("Hello World! from test job!")
        .build();

    Job jobCompleted = Job.builder()
        .id(0L)
        .createdBy(user)
        .createdAt(null)
        .updatedAt(null)
        .status("complete")
        .log("Hello World! from test job!\nGoodbye from test job!")
        .build();

    when(jobsRepository.save(any(Job.class))).thenReturn(jobStarted).thenReturn(jobCompleted);

    // act
    MvcResult response = mockMvc
        .perform(post("/api/jobs/launch/testjob?fail=false&sleepMs=2000").with(csrf()))
        .andExpect(status().isOk())
        .andReturn();

    // assert
    String responseString = response.getResponse().getContentAsString();
    Job jobReturned = objectMapper.readValue(responseString, Job.class);

    assertEquals("running", jobReturned.getStatus());

    await()
        .atMost(1, SECONDS)
        .untilAsserted(() -> verify(jobsRepository, times(2)).save(eq(jobStarted)));
    await()
        .atMost(10, SECONDS)
        .untilAsserted(() -> verify(jobsRepository, times(4)).save(eq(jobCompleted)));
  }

  @WithMockUser(roles = { "ADMIN" })
  @Test
  public void admin_can_launch_test_job_that_fails() throws Exception {

    // arrange

    User user = currentUserService.getUser();

    Job jobStarted = Job.builder()
        .id(0L)
        .createdBy(user)
        .createdAt(null)
        .updatedAt(null)
        .status("running")
        .log("Hello World! from test job!")
        .build();

    Job jobFailed = Job.builder()
        .id(0L)
        .createdBy(user)
        .createdAt(null)
        .updatedAt(null)
        .status("error")
        .log("Hello World! from test job!\nFail!")
        .build();

    when(jobsRepository.save(any(Job.class))).thenReturn(jobStarted).thenReturn(jobFailed);

    // act
    MvcResult response = mockMvc
        .perform(post("/api/jobs/launch/testjob?fail=true&sleepMs=4000").with(csrf()))
        .andExpect(status().isOk())
        .andReturn();

    String responseString = response.getResponse().getContentAsString();
    Job jobReturned = objectMapper.readValue(responseString, Job.class);

    assertEquals("running", jobReturned.getStatus());

    await()
        .atMost(1, SECONDS)
        .untilAsserted(() -> verify(jobsRepository, times(2)).save(eq(jobStarted)));

    await()
        .atMost(10, SECONDS)
        .untilAsserted(() -> verify(jobsRepository, times(3)).save(eq(jobFailed)));
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
        .status("complete")
        .build();

    when(jobsRepository.save(any(Job.class))).thenReturn(jobStarted).thenReturn(jobStarted);

    doNothing().when(updateUserService).attachRosterStudentsAllUsers();

    // act
    MvcResult response = mockMvc
        .perform(post("/api/jobs/launch/updateAll").with(csrf()))
        .andExpect(status().isOk())
        .andReturn();

    // assert
    verify(jobsRepository, atLeast(1)).save(any(Job.class));
    verify(updateUserService, times(1)).attachRosterStudentsAllUsers();
    }  

}