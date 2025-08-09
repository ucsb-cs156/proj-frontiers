package edu.ucsb.cs156.frontiers.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.fixtures.GithubGraphQLFixtures;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.services.CurrentUserService;
import edu.ucsb.cs156.frontiers.services.GithubGraphQLService;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@Slf4j
@WebMvcTest(controllers = GithubGraphQLController.class)
public class GithubGraphQLControllerTests extends ControllerTestCase {

  @MockitoBean private CourseRepository courseRepository;

  @MockitoBean private GithubGraphQLService githubGraphQLService;

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
            .creator(user)
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
  @WithMockUser(roles = {"INSTRUCTOR"})
  public void test_getDefaultMainBranch_happyPath_Instructor() throws Exception {

    User user = currentUserService.getCurrentUser().getUser();

    // arrange
    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .term("S25")
            .school("UCSB")
            .creator(user)
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
  @WithMockUser(roles = {"INSTRUCTOR"})
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
  @WithMockUser(roles = {"INSTRUCTOR"})
  public void test_getDefaultMainBranch_unauthorized() throws Exception {
    // arrange
    User user = currentUserService.getCurrentUser().getUser();
    User otherUser = User.builder().id(user.getId() + 1L).build();
    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .term("S25")
            .school("UCSB")
            .creator(otherUser)
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));

    // act & assert
    MvcResult response =
        mockMvc
            .perform(
                get("/api/github/graphql/defaultBranchName")
                    .param("courseId", "1")
                    .param("owner", "ucsb-cs156-f24")
                    .param("repo", "STARTER-jpa00")
                    .with(csrf()))
            .andExpect(status().isForbidden())
            .andReturn();

    verify(courseRepository, times(1)).findById(eq(1L));
    Map<String, String> expectedResponse =
        Map.of(
            "type", "CourseNotAuthorized",
            "message", "User not authorized to access course with id 1");
    String expectedJson = objectMapper.writeValueAsString(expectedResponse);

    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
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
            .creator(user)
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
  @WithMockUser(roles = {"INSTRUCTOR"})
  public void test_getCommits_happyPath_instructor() throws Exception {

    User user = currentUserService.getCurrentUser().getUser();

    // arrange
    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .term("S25")
            .school("UCSB")
            .creator(user)
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
  @WithMockUser(roles = {"INSTRUCTOR"})
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

  @Test
  @WithMockUser(roles = {"INSTRUCTOR"})
  public void test_getCommits_unauthorized() throws Exception {
    // arrange
    User user = currentUserService.getCurrentUser().getUser();
    User otherUser = User.builder().id(user.getId() + 1L).build();
    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .term("S25")
            .school("UCSB")
            .creator(otherUser)
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));

    // act & assert
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
            .andExpect(status().isForbidden())
            .andReturn();

    verify(courseRepository, times(1)).findById(eq(1L));
    Map<String, String> expectedResponse =
        Map.of(
            "type", "CourseNotAuthorized",
            "message", "User not authorized to access course with id 1");
    String expectedJson = objectMapper.writeValueAsString(expectedResponse);

    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }
}
