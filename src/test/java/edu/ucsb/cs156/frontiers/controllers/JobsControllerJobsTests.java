package edu.ucsb.cs156.frontiers.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import edu.ucsb.cs156.frontiers.jobs.MembershipAuditJob;
import edu.ucsb.cs156.frontiers.jobs.UpdateAllJob;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.JobsRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.repositories.UserRepository;
import edu.ucsb.cs156.frontiers.services.OrganizationMemberService;
import edu.ucsb.cs156.frontiers.services.UpdateUserService;
import edu.ucsb.cs156.frontiers.services.jobs.JobService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.test.web.servlet.MvcResult;


/**
 * This class tests the ability of the JobsController to launch jobs.
 * By contrast, JobsControllerDetailedTests tests the ability of the JobsController
 * to get the status of jobs that have already been launched.
 *
 * @see JobsController
 * @see JobsControllerDetailedTests
 */
@Slf4j
@WebMvcTest(controllers = JobsController.class)
public class JobsControllerJobsTests extends ControllerTestCase {

  @MockitoBean
  JobsRepository jobsRepository;

  @MockitoBean
  UserRepository userRepository;

  @MockitoBean
  UpdateUserService updateUserService; // This will be used in the UpdateAllJob to call the GithubSignInService

  @MockitoBean
  JobService jobService;

  @MockitoBean
  RosterStudentRepository rosterStudentRepository;

  @MockitoBean
  CourseRepository courseRepository;

  @MockitoBean
  OrganizationMemberService organizationMemberService;

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

  @WithMockUser(roles = { "ADMIN" })
  @Test
  public void admin_can_launch_auditAllCourses_job() throws Exception {

    // arrange

    User user = currentUserService.getUser();

    Job jobStarted = Job.builder()
        .id(0L)
        .createdBy(user)
        .createdAt(null)
        .updatedAt(null)
        .status("started")
        .build();

    String expectedResponse = objectMapper.writeValueAsString(jobStarted);

    when(jobService.runAsJob (any(MembershipAuditJob.class))).thenReturn(jobStarted);


    // act
    MvcResult result = mockMvc
        .perform(post("/api/jobs/launch/auditAllCourses").with(csrf()))
        .andExpect(status().isOk())
        .andReturn();

    // assert

    String response = result.getResponse().getContentAsString();
    verify(jobService, times(1)).runAsJob(any(MembershipAuditJob.class));
      assertEquals(expectedResponse, response);
  }

}
