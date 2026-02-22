package edu.ucsb.cs156.frontiers.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.annotations.WithInstructorCoursePermissions;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.fixtures.GithubGraphQLFixtures;
import edu.ucsb.cs156.frontiers.repositories.BranchRepository;
import edu.ucsb.cs156.frontiers.repositories.CommitRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.services.CurrentUserService;
import edu.ucsb.cs156.frontiers.services.GithubGraphQLService;
import edu.ucsb.cs156.frontiers.services.jobs.JobService;
import edu.ucsb.cs156.frontiers.utilities.StatefulBeanToCsvBuilderFactory;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@Slf4j
@WebMvcTest(controllers = GithubGraphQLController.class)
public class GithubGraphQLControllerTests extends ControllerTestCase {

  @MockitoBean private CourseRepository courseRepository;

  @MockitoBean private GithubGraphQLService githubGraphQLService;

  @MockitoBean private JobService jobService;

  @MockitoBean private DateTimeProvider time;

  @MockitoBean private CommitRepository commitRepository;

  @MockitoBean private BranchRepository branchRepository;

  @MockitoBean private StatefulBeanToCsvBuilderFactory csvBuilderFactory;

  @Autowired private CurrentUserService currentUserService;

  @Autowired private ObjectMapper objectMapper;

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void test_getDefaultMainBranch_happyPath_Admin() throws Exception {

    User user = currentUserService.getCurrentUser().getUser();

    // arrange
    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .term("S25")
            .school("UCSB")
            .instructorEmail(user.getEmail())
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(githubGraphQLService.getDefaultBranchName(
            eq(course), eq("ucsb-cs156-f24"), eq("STARTER-jpa00")))
        .thenReturn("main");

    // act

    MvcResult response =
        mockMvc
            .perform(
                get("/api/github/graphql/defaultBranchName")
                    .param("courseId", "1")
                    .param("owner", "ucsb-cs156-f24")
                    .param("repo", "STARTER-jpa00"))
            .andExpect(status().isOk())
            .andReturn();

    // assert

    verify(courseRepository, times(1)).findById(eq(1L));

    String responseString = response.getResponse().getContentAsString();
    assertEquals("main", responseString);
  }

  @Test
  @WithInstructorCoursePermissions
  public void test_getDefaultMainBranch_happyPath_Instructor() throws Exception {

    User user = currentUserService.getCurrentUser().getUser();

    // arrange
    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .term("S25")
            .school("UCSB")
            .instructorEmail(user.getEmail())
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(githubGraphQLService.getDefaultBranchName(
            eq(course), eq("ucsb-cs156-f24"), eq("STARTER-jpa00")))
        .thenReturn("main");

    // act

    MvcResult response =
        mockMvc
            .perform(
                get("/api/github/graphql/defaultBranchName")
                    .param("courseId", "1")
                    .param("owner", "ucsb-cs156-f24")
                    .param("repo", "STARTER-jpa00"))
            .andExpect(status().isOk())
            .andReturn();

    // assert

    verify(courseRepository, times(1)).findById(eq(1L));

    String responseString = response.getResponse().getContentAsString();
    assertEquals("main", responseString);
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void test_getDefaultMainBranch_courseNotFound() throws Exception {

    // arrange
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.empty());

    // act & assert
    mockMvc
        .perform(
            get("/api/github/graphql/defaultBranchName")
                .param("courseId", "1")
                .param("owner", "ucsb-cs156-f24")
                .param("repo", "STARTER-jpa00"))
        .andExpect(status().isNotFound());

    verify(courseRepository, times(1)).findById(eq(1L));
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void test_getCommits_happyPath_Admin() throws Exception {

    User user = currentUserService.getCurrentUser().getUser();

    // arrange
    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .term("S25")
            .school("UCSB")
            .instructorEmail(user.getEmail())
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(githubGraphQLService.getCommits(
            eq(course), eq("ucsb-cs156-f24"), eq("STARTER-jpa00"), eq("main"), eq(10), eq("")))
        .thenReturn(GithubGraphQLFixtures.COMMITS_RESPONSE);

    // act

    MvcResult response =
        mockMvc
            .perform(
                get("/api/github/graphql/commits")
                    .param("courseId", "1")
                    .param("owner", "ucsb-cs156-f24")
                    .param("repo", "STARTER-jpa00")
                    .param("branch", "main")
                    .param("first", "10")
                    .param("after", ""))
            .andExpect(status().isOk())
            .andReturn();

    // assert

    verify(courseRepository, times(1)).findById(eq(1L));

    String responseString = response.getResponse().getContentAsString();
    assertEquals(GithubGraphQLFixtures.COMMITS_RESPONSE, responseString);
  }

  @Test
  @WithInstructorCoursePermissions
  public void test_getCommits_happyPath_instructor() throws Exception {

    User user = currentUserService.getCurrentUser().getUser();

    // arrange
    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .term("S25")
            .school("UCSB")
            .instructorEmail(user.getEmail())
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(githubGraphQLService.getCommits(
            eq(course), eq("ucsb-cs156-f24"), eq("STARTER-jpa00"), eq("main"), eq(10), eq("")))
        .thenReturn(GithubGraphQLFixtures.COMMITS_RESPONSE);

    // act

    MvcResult response =
        mockMvc
            .perform(
                get("/api/github/graphql/commits")
                    .param("courseId", "1")
                    .param("owner", "ucsb-cs156-f24")
                    .param("repo", "STARTER-jpa00")
                    .param("branch", "main")
                    .param("first", "10")
                    .param("after", ""))
            .andExpect(status().isOk())
            .andReturn();

    // assert

    verify(courseRepository, times(1)).findById(eq(1L));

    String responseString = response.getResponse().getContentAsString();
    assertEquals(GithubGraphQLFixtures.COMMITS_RESPONSE, responseString);
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void test_getCommits_courseNotFound() throws Exception {

    // arrange
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.empty());

    // act & assert
    mockMvc
        .perform(
            get("/api/github/graphql/commits")
                .param("courseId", "1")
                .param("owner", "ucsb-cs156-f24")
                .param("repo", "STARTER-jpa00")
                .param("branch", "main")
                .param("first", "10")
                .param("after", ""))
        .andExpect(status().isNotFound());

    verify(courseRepository, times(1)).findById(eq(1L));
  }
}
