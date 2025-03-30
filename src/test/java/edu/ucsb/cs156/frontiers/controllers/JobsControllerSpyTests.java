package edu.ucsb.cs156.frontiers.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.entities.Job;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.jobs.UpdateAllJob;
import edu.ucsb.cs156.frontiers.repositories.JobsRepository;
import edu.ucsb.cs156.frontiers.repositories.UserRepository;
import edu.ucsb.cs156.frontiers.services.UpdateUserService;
import edu.ucsb.cs156.frontiers.services.jobs.JobService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@WebMvcTest(controllers = JobsController.class)
@Import(JobService.class)
@AutoConfigureDataJpa
public class JobsControllerSpyTests extends ControllerTestCase {

  @MockitoBean
  JobsRepository jobsRepository;

  @MockitoBean
  UserRepository userRepository;

  @MockitoBean
  UpdateUserService updateUserService; // This will be used in the UpdateAllJob to call the GithubSignInService

  @MockitoBean
  JobService jobService;

  @Autowired
  ObjectMapper objectMapper;


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

    when(jobService.runAsJob (any(UpdateAllJob.class))).thenReturn(jobStarted);


    // act
    mockMvc
        .perform(post("/api/jobs/launch/updateAll").with(csrf()))
        .andExpect(status().isOk())
        .andReturn();

    // assert

    verify(jobService, times(1)).runAsJob(any(UpdateAllJob.class));
    
  }

}