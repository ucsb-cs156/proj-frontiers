package edu.ucsb.cs156.frontiers.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.annotations.WithInstructorCoursePermissions;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.DownloadRequest;
import edu.ucsb.cs156.frontiers.entities.DownloadedCommit;
import edu.ucsb.cs156.frontiers.entities.Job;
import edu.ucsb.cs156.frontiers.enums.DownloadRequestType;
import edu.ucsb.cs156.frontiers.jobs.CommitDownloadRequestJob;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.DownloadRequestRepository;
import edu.ucsb.cs156.frontiers.repositories.DownloadedCommitRepository;
import edu.ucsb.cs156.frontiers.services.GithubGraphQLService;
import edu.ucsb.cs156.frontiers.services.jobs.JobService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = DownloadRequestController.class)
public class DownloadRequestControllerTests extends ControllerTestCase {

  @MockitoBean DownloadedCommitRepository downloadedCommitRepository;
  @MockitoBean DownloadRequestRepository downloadRequestRepository;
  @MockitoBean CourseRepository courseRepository;
  @MockitoBean JobService jobService;
  @MockitoBean GithubGraphQLService githubGraphQLService;

  @Test
  @WithInstructorCoursePermissions
  public void testCreateDownloadRequest() throws Exception {

    Course course = Course.builder().id(1L).build();
    DownloadRequest request =
        DownloadRequest.builder()
            .downloadType(DownloadRequestType.COMMITS)
            .org("ucsb-cs156")
            .repo("proj-frontiers")
            .branch("main")
            .course(course)
            .build();

    DownloadRequest requestSaved =
        DownloadRequest.builder()
            .id(1L)
            .downloadType(DownloadRequestType.COMMITS)
            .org("ucsb-cs156")
            .repo("proj-frontiers")
            .branch("main")
            .course(course)
            .build();

    Job job = Job.builder().id(1L).build();

    DownloadRequest finalRequest =
        DownloadRequest.builder()
            .id(1L)
            .downloadType(DownloadRequestType.COMMITS)
            .org("ucsb-cs156")
            .repo("proj-frontiers")
            .branch("main")
            .course(course)
            .job(job)
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(downloadRequestRepository.save(request)).thenReturn(requestSaved);
    when(downloadRequestRepository.save(finalRequest)).thenReturn(finalRequest);
    when(jobService.runAsJob(any(CommitDownloadRequestJob.class))).thenReturn(job);

    MvcResult result =
        mockMvc
            .perform(
                post("/api/downloads/create")
                    .with(csrf())
                    .param("courseId", "1")
                    .param("type", "COMMITS")
                    .param("org", "ucsb-cs156")
                    .param("repo", "proj-frontiers"))
            .andExpect(status().isAccepted())
            .andReturn();
    verify(downloadRequestRepository).save(finalRequest);
    verify(downloadRequestRepository).save(request);
    verify(jobService).runAsJob(any(CommitDownloadRequestJob.class));

    String responseString = result.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(finalRequest);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithInstructorCoursePermissions
  public void test_download_request_non_default_branch() throws Exception {

    Course course = Course.builder().id(1L).build();
    DownloadRequest request =
        DownloadRequest.builder()
            .downloadType(DownloadRequestType.COMMITS)
            .org("ucsb-cs156")
            .repo("proj-frontiers")
            .branch("trunk")
            .course(course)
            .build();

    DownloadRequest requestSaved =
        DownloadRequest.builder()
            .id(1L)
            .downloadType(DownloadRequestType.COMMITS)
            .org("ucsb-cs156")
            .repo("proj-frontiers")
            .branch("trunk")
            .course(course)
            .build();

    Job job = Job.builder().id(1L).build();

    DownloadRequest finalRequest =
        DownloadRequest.builder()
            .id(1L)
            .downloadType(DownloadRequestType.COMMITS)
            .org("ucsb-cs156")
            .repo("proj-frontiers")
            .branch("trunk")
            .course(course)
            .job(job)
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(downloadRequestRepository.save(request)).thenReturn(requestSaved);
    when(downloadRequestRepository.save(finalRequest)).thenReturn(finalRequest);
    when(jobService.runAsJob(any(CommitDownloadRequestJob.class))).thenReturn(job);

    MvcResult result =
        mockMvc
            .perform(
                post("/api/downloads/create")
                    .with(csrf())
                    .param("courseId", "1")
                    .param("type", "COMMITS")
                    .param("org", "ucsb-cs156")
                    .param("repo", "proj-frontiers")
                    .param("branch", "trunk"))
            .andExpect(status().isAccepted())
            .andReturn();
    verify(downloadRequestRepository).save(finalRequest);
    verify(downloadRequestRepository).save(request);
    verify(jobService).runAsJob(any(CommitDownloadRequestJob.class));

    String responseString = result.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(finalRequest);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithInstructorCoursePermissions
  public void testCreateDownloadRequestWithInvalidCourseId() throws Exception {

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(
                post("/api/downloads/create")
                    .with(csrf())
                    .param("courseId", "1")
                    .param("type", "COMMITS")
                    .param("org", "ucsb-cs156")
                    .param("repo", "proj-frontiers"))
            .andExpect(status().isNotFound())
            .andReturn();

    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "type", "EntityNotFoundException",
            "message", "Course with id 1 not found");
    Map<String, String> actualMap =
        mapper.readValue(responseString, new TypeReference<Map<String, String>>() {});
    assertEquals(expectedMap, actualMap);
  }

  @Test
  @WithInstructorCoursePermissions
  public void test_download_request_not_found() throws Exception {
    when(downloadRequestRepository.findById(eq(1L))).thenReturn(Optional.empty());
    MvcResult response =
        mockMvc
            .perform(post("/api/downloads/1").with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "type", "EntityNotFoundException",
            "message", "DownloadRequest with id 1 not found");
    Map<String, String> actualMap =
        mapper.readValue(responseString, new TypeReference<Map<String, String>>() {});
    assertEquals(expectedMap, actualMap);
  }

  @Test
  @WithInstructorCoursePermissions
  public void test_not_done() throws Exception {
    DownloadRequest request =
        DownloadRequest.builder()
            .id(1L)
            .job(Job.builder().id(1L).status("running").build())
            .build();
    when(downloadRequestRepository.findById(eq(1L))).thenReturn(Optional.of(request));

    MvcResult response =
        mockMvc
            .perform(post("/api/downloads/1").with(csrf()))
            .andExpect(status().isAccepted())
            .andReturn();

    Map<String, String> expectedMap =
        Map.of("message", "Download request is not complete. Please try again later.");
    Map<String, String> actualMap =
        mapper.readValue(
            response.getResponse().getContentAsString(),
            new TypeReference<Map<String, String>>() {});
    assertEquals(expectedMap, actualMap);
  }

  @Test
  @WithInstructorCoursePermissions
  public void test_job_error() throws Exception {
    DownloadRequest request =
        DownloadRequest.builder().id(1L).job(Job.builder().id(1L).status("error").build()).build();

    when(downloadRequestRepository.findById(eq(1L))).thenReturn(Optional.of(request));

    MvcResult response =
        mockMvc
            .perform(post("/api/downloads/1").with(csrf()))
            .andExpect(status().isInternalServerError())
            .andReturn();

    Map<String, String> expectedMap =
        Map.of(
            "message",
            "Download request failed with error; please check the job log for more information.");
    Map<String, String> actualMap =
        mapper.readValue(
            response.getResponse().getContentAsString(),
            new TypeReference<Map<String, String>>() {});
    assertEquals(expectedMap, actualMap);
  }

  final String writeout_commit =
      """
      "COMMITURL","ID"
      "https://github.com/ucsb-cs156/proj-frontiers/commit/9df9217b7f66102d0dcaecf48ef48af16facb058","1"
      "https://github.com/ucsb-cs156/proj-frontiers/commit/f0497a983b1533f4b7f9f2779030d3fa62fd6031","2"
      """;

  @Test
  @WithInstructorCoursePermissions
  public void downloadRequestSuccess() throws Exception {
    DownloadRequest request =
        DownloadRequest.builder()
            .id(1L)
            .job(Job.builder().id(1L).status("complete").build())
            .org("ucsb-cs156")
            .repo("proj-frontiers")
            .build();

    DownloadedCommit firstCommit =
        DownloadedCommit.builder()
            .request(request)
            .id(1L)
            .commitUrl(
                "https://github.com/ucsb-cs156/proj-frontiers/commit/9df9217b7f66102d0dcaecf48ef48af16facb058")
            .build();

    DownloadedCommit secondCommit =
        DownloadedCommit.builder()
            .request(request)
            .id(2L)
            .commitUrl(
                "https://github.com/ucsb-cs156/proj-frontiers/commit/f0497a983b1533f4b7f9f2779030d3fa62fd6031")
            .build();

    when(downloadRequestRepository.findById(eq(1L))).thenReturn(Optional.of(request));
    when(downloadedCommitRepository.findByRequest(eq(request)))
        .thenReturn(List.of(firstCommit, secondCommit));

    mockMvc
        .perform(post("/api/downloads/1").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(content().string(writeout_commit))
        .andExpect(content().contentType("text/csv"))
        .andExpect(
            header()
                .string(
                    "Content-Disposition",
                    "attachment; filename=\"ucsb-cs156_proj-frontiers.csv\""));
  }
}
