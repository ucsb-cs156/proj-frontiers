package edu.ucsb.cs156.frontiers.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.annotations.WithInstructorCoursePermissions;
import edu.ucsb.cs156.frontiers.entities.Branch;
import edu.ucsb.cs156.frontiers.entities.BranchId;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.Job;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.fixtures.GithubGraphQLFixtures;
import edu.ucsb.cs156.frontiers.jobs.LoadCommitHistoryJob;
import edu.ucsb.cs156.frontiers.models.CommitDto;
import edu.ucsb.cs156.frontiers.repositories.BranchRepository;
import edu.ucsb.cs156.frontiers.repositories.CommitRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.services.CurrentUserService;
import edu.ucsb.cs156.frontiers.services.GithubGraphQLService;
import edu.ucsb.cs156.frontiers.services.jobs.JobService;
import edu.ucsb.cs156.frontiers.utilities.StatefulBeanToCsvBuilderFactory;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.http.MediaType;
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

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void load_commits_no_branches() throws Exception {
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

    MvcResult response =
        mockMvc
            .perform(
                post("/api/github/graphql/history")
                    .param("courseId", "1")
                    .content("""
                [
                ]
                """)
                    .contentType(MediaType.APPLICATION_JSON)
                    .with(csrf()))
            .andExpect(status().isBadRequest())
            .andReturn();

    String responseString = response.getResponse().getErrorMessage();
    assertEquals("No branches specified", responseString);
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void course_not_found() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(
                post("/api/github/graphql/history")
                    .param("courseId", "1")
                    .content("""
                [
                ]
                """)
                    .contentType(MediaType.APPLICATION_JSON)
                    .with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    Map<String, Object> json = responseToJson(response);
    assertEquals("EntityNotFoundException", json.get("type"));
    assertEquals("Course with id 1 not found", json.get("message"));
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void successful_launch_load_history_job() throws Exception {
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

    Job job = Job.builder().id(1L).status("running").build();

    when(jobService.runAsJob(any(LoadCommitHistoryJob.class))).thenReturn(job);

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));

    BranchId branch1 = new BranchId("ucsb-cs156-f24", "STARTER-jpa00", "main");
    BranchId branch2 = new BranchId("ucsb-cs156-f24", "STARTER-jpa00", "branch2");

    MvcResult response =
        mockMvc
            .perform(
                post("/api/github/graphql/history")
                    .param("courseId", "1")
                    .content(objectMapper.writeValueAsString(List.of(branch1, branch2)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    String responseString = response.getResponse().getContentAsString();
    assertEquals(objectMapper.writeValueAsString(job), responseString);
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void load_commits_no_branches_get_commits_csv() throws Exception {
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

    MvcResult response =
        mockMvc
            .perform(
                post("/api/github/graphql/csv")
                    .param("courseId", "1")
                    .param("start", "2023-01-01T00:00:00Z")
                    .param("end", "2023-01-02T00:00:00Z")
                    .param("skipMergeCommits", "true")
                    .content("""
                [
                ]
                """)
                    .contentType(MediaType.APPLICATION_JSON)
                    .with(csrf()))
            .andExpect(status().isBadRequest())
            .andReturn();

    String responseString = response.getResponse().getErrorMessage();
    assertEquals("No branches specified", responseString);
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void swapped_end_and_begin() throws Exception {
    User user = currentUserService.getCurrentUser().getUser();

    BranchId branchId = new BranchId("ucsb-cs156-f24", "STARTER-jpa00", "branch2");

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

    MvcResult response =
        mockMvc
            .perform(
                post("/api/github/graphql/csv")
                    .param("courseId", "1")
                    .param("start", "2023-01-02T00:00:00Z")
                    .param("end", "2023-01-01T00:00:00Z")
                    .param("skipMergeCommits", "true")
                    .content(objectMapper.writeValueAsString(List.of(branchId)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .with(csrf()))
            .andExpect(status().isBadRequest())
            .andReturn();

    String responseString = response.getResponse().getErrorMessage();
    assertEquals("Start time must be before end time.", responseString);
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void branch_not_found() throws Exception {
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

    BranchId branchId = new BranchId("ucsb-cs156-f24", "STARTER-jpa00", "branch2");

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(branchRepository.findByIdIn(List.of(branchId))).thenReturn(List.of());

    MvcResult response =
        mockMvc
            .perform(
                post("/api/github/graphql/csv")
                    .param("courseId", "1")
                    .param("start", "2023-01-01T00:00:00Z")
                    .param("end", "2023-01-02T00:00:00Z")
                    .param("skipMergeCommits", "true")
                    .content(objectMapper.writeValueAsString(List.of(branchId)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .with(csrf()))
            .andExpect(status().isBadRequest())
            .andReturn();

    String responseString = response.getResponse().getErrorMessage();
    assertEquals(
        "One or more branches not found; Please load commit history for those branches first.",
        responseString);
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void branch_not_updated() throws Exception {
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

    BranchId branchId = new BranchId("ucsb-cs156-f24", "STARTER-jpa00", "branch2");

    Branch branch =
        Branch.builder().id(branchId).retrievedTime(Instant.parse("2022-05-01T00:00:00Z")).build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(branchRepository.findByIdIn(List.of(branchId))).thenReturn(List.of(branch));

    MvcResult response =
        mockMvc
            .perform(
                post("/api/github/graphql/csv")
                    .param("courseId", "1")
                    .param("start", "2023-01-01T00:00:00Z")
                    .param("end", "2023-01-02T00:00:00Z")
                    .param("skipMergeCommits", "true")
                    .content(objectMapper.writeValueAsString(List.of(branchId)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .with(csrf()))
            .andExpect(status().isBadRequest())
            .andReturn();

    String responseString = response.getResponse().getErrorMessage();
    assertEquals(
        "One or more branches have not been updated since the requested end time; Please load commit history for those branches first.",
        responseString);
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void streaming_throws_error() throws Exception {

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

    BranchId branchId = new BranchId("ucsb-cs156-f24", "STARTER-jpa00", "branch2");

    Branch branch =
        Branch.builder().id(branchId).retrievedTime(Instant.parse("2024-05-01T00:00:00Z")).build();

    CommitDto secondCommit =
        new CommitDto(
            1L,
            branchId.org(),
            branchId.repo(),
            branchId.branchName(),
            "f0497a983b1533f4b7f9f2779030d3fa62fd6031",
            "https://github.com/ucsb-cs156/proj-frontiers/commit/f0497a983b1533f4b7f9f2779030d3fa62fd6031",
            "dj - removed files outside of our normal unit testing area",
            Instant.parse("2026-01-26T00:58:12Z"),
            "Daniel Jensen",
            "djensen2@outlook.com",
            "Division7",
            "Daniel Jensen",
            "djensen2@outlook.com",
            "Division7");

    List<CommitDto> commits = List.of(secondCommit);

    StatefulBeanToCsv<CommitDto> mock = mock(StatefulBeanToCsv.class);

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(branchRepository.findByIdIn(List.of(branchId))).thenReturn(List.of(branch));
    when(commitRepository.findByBranchIdInAndCommitTimeBetweenAndIsMergeCommitEquals(
            List.of(branchId),
            Instant.parse("2023-01-01T00:00:00Z"),
            Instant.parse("2023-01-02T00:00:00Z"),
            false))
        .thenReturn(commits);

    when(csvBuilderFactory.build(any())).thenAnswer(invocation -> mock);

    doThrow(new CsvDataTypeMismatchException()).when(mock).write(anyList());

    MvcResult response =
        mockMvc
            .perform(
                post("/api/github/graphql/csv")
                    .param("courseId", "1")
                    .param("start", "2023-01-01T00:00:00Z")
                    .param("end", "2023-01-02T00:00:00Z")
                    .param("skipMergeCommits", "true")
                    .content(objectMapper.writeValueAsString(List.of(branchId)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .with(csrf()))
            .andExpect(request().asyncStarted())
            .andExpect(status().isOk())
            .andReturn();

    MvcResult asyncResponse =
        mockMvc.perform(asyncDispatch(response)).andExpect(status().isOk()).andReturn();

    verify(csvBuilderFactory, times(1)).build(any());

    verify(mock, times(1)).write(anyList());
    String responseString = asyncResponse.getResponse().getContentAsString();
    assertEquals("Error writing CSV file", responseString);
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void writes_commit_correctly() throws Exception {
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

    BranchId branchId = new BranchId("ucsb-cs156-f24", "STARTER-jpa00", "branch2");

    Branch branch =
        Branch.builder().id(branchId).retrievedTime(Instant.parse("2024-05-01T00:00:00Z")).build();

    CommitDto secondCommit =
        new CommitDto(
            1L,
            branchId.org(),
            branchId.repo(),
            branchId.branchName(),
            "f0497a983b1533f4b7f9f2779030d3fa62fd6031",
            "https://github.com/ucsb-cs156/proj-frontiers/commit/f0497a983b1533f4b7f9f2779030d3fa62fd6031",
            "dj - removed files outside of our normal unit testing area",
            Instant.parse("2026-01-26T00:58:12Z"),
            "Daniel Jensen",
            "djensen2@outlook.com",
            "Division7",
            "Daniel Jensen",
            "djensen2@outlook.com",
            "Division7");

    List<CommitDto> commits = List.of(secondCommit);

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(branchRepository.findByIdIn(List.of(branchId))).thenReturn(List.of(branch));
    when(commitRepository.findByBranchIdInAndCommitTimeBetweenAndIsMergeCommitEquals(
            List.of(branchId),
            Instant.parse("2023-01-01T00:00:00Z"),
            Instant.parse("2023-01-02T00:00:00Z"),
            false))
        .thenReturn(commits);

    when(csvBuilderFactory.build(any())).thenCallRealMethod();

    MvcResult response =
        mockMvc
            .perform(
                post("/api/github/graphql/csv")
                    .param("courseId", "1")
                    .param("start", "2023-01-01T00:00:00Z")
                    .param("end", "2023-01-02T00:00:00Z")
                    .param("skipMergeCommits", "true")
                    .content(objectMapper.writeValueAsString(List.of(branchId)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .with(csrf()))
            .andExpect(request().asyncStarted())
            .andExpect(status().isOk())
            .andReturn();

    MvcResult asyncResponse =
        mockMvc.perform(asyncDispatch(response)).andExpect(status().isOk()).andReturn();

    verify(commitRepository, times(1))
        .findByBranchIdInAndCommitTimeBetweenAndIsMergeCommitEquals(
            List.of(branchId),
            Instant.parse("2023-01-01T00:00:00Z"),
            Instant.parse("2023-01-02T00:00:00Z"),
            false);

    String responseString = asyncResponse.getResponse().getContentAsString();
    assertFalse(responseString.isBlank());
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void writes_commit_correctly_yes_merge_commits() throws Exception {
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

    BranchId branchId = new BranchId("ucsb-cs156-f24", "STARTER-jpa00", "branch2");

    Branch branch =
        Branch.builder().id(branchId).retrievedTime(Instant.parse("2024-05-01T00:00:00Z")).build();

    CommitDto secondCommit =
        new CommitDto(
            1L,
            branchId.org(),
            branchId.repo(),
            branchId.branchName(),
            "f0497a983b1533f4b7f9f2779030d3fa62fd6031",
            "https://github.com/ucsb-cs156/proj-frontiers/commit/f0497a983b1533f4b7f9f2779030d3fa62fd6031",
            "dj - removed files outside of our normal unit testing area",
            Instant.parse("2026-01-26T00:58:12Z"),
            "Daniel Jensen",
            "djensen2@outlook.com",
            "Division7",
            "Daniel Jensen",
            "djensen2@outlook.com",
            "Division7");

    List<CommitDto> commits = List.of(secondCommit);

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(branchRepository.findByIdIn(List.of(branchId))).thenReturn(List.of(branch));
    when(commitRepository.findByBranchIdInAndCommitTimeBetween(
            List.of(branchId),
            Instant.parse("2023-01-01T00:00:00Z"),
            Instant.parse("2023-01-02T00:00:00Z")))
        .thenReturn(commits);

    when(csvBuilderFactory.build(any())).thenCallRealMethod();

    MvcResult response =
        mockMvc
            .perform(
                post("/api/github/graphql/csv")
                    .param("courseId", "1")
                    .param("start", "2023-01-01T00:00:00Z")
                    .param("end", "2023-01-02T00:00:00Z")
                    .param("skipMergeCommits", "false")
                    .content(objectMapper.writeValueAsString(List.of(branchId)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .with(csrf()))
            .andExpect(request().asyncStarted())
            .andExpect(status().isOk())
            .andReturn();

    MvcResult asyncResponse =
        mockMvc.perform(asyncDispatch(response)).andExpect(status().isOk()).andReturn();

    verify(commitRepository, times(1))
        .findByBranchIdInAndCommitTimeBetween(
            List.of(branchId),
            Instant.parse("2023-01-01T00:00:00Z"),
            Instant.parse("2023-01-02T00:00:00Z"));
    String responseString = asyncResponse.getResponse().getContentAsString();
    assertFalse(responseString.isBlank());
  }
}
