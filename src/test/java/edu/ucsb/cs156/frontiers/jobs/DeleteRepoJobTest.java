package edu.ucsb.cs156.frontiers.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.Job;
import edu.ucsb.cs156.frontiers.services.JwtService;
import edu.ucsb.cs156.frontiers.services.jobs.JobContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
public class DeleteRepoJobTest {

  @Mock private JwtService jwtService;
  @Mock private RestTemplate restTemplate;
  @Captor private ArgumentCaptor<HttpEntity<String>> entityCaptor;

  private final ObjectMapper mapper = new ObjectMapper();

  Job jobStarted = Job.builder().build();
  JobContext ctx = new JobContext(null, jobStarted);

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void test_getCourse_returnsCourse() {
    Course course = Course.builder().id(1L).courseName("Test Course").build();
    DeleteRepoJob job = DeleteRepoJob.builder().course(course).build();
    assertEquals(course, job.getCourse());
  }

  @Test
  public void test_delete_empty_repo_success() throws Exception {
    Course course = Course.builder().orgName("ucsb-cs156").build();
    when(jwtService.getInstallationToken(course)).thenReturn("dummy-token");

    String reposUrl = "https://api.github.com/orgs/ucsb-cs156/repos?per_page=100";
    String reposJson = "[{\"name\": \"repo-prefix-student1\"}, {\"name\": \"other-repo\"}]";

    // Mock the repos endpoint
    ResponseEntity<String> reposResponse = new ResponseEntity<>(reposJson, HttpStatus.OK);
    when(restTemplate.exchange(
            eq(reposUrl), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
        .thenReturn(reposResponse);

    // Mock the commits endpoint (Returns 409 Conflict for empty repos)
    String commitsUrl = "https://api.github.com/repos/ucsb-cs156/repo-prefix-student1/commits";
    when(restTemplate.exchange(
            eq(commitsUrl), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
        .thenThrow(new HttpClientErrorException(HttpStatus.CONFLICT));

    // Mock the delete endpoint
    String deleteUrl = "https://api.github.com/repos/ucsb-cs156/repo-prefix-student1";
    when(restTemplate.exchange(
            eq(deleteUrl), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(String.class)))
        .thenReturn(new ResponseEntity<>("", HttpStatus.NO_CONTENT));

    DeleteRepoJob job =
        DeleteRepoJob.builder()
            .course(course)
            .prefix("repo-prefix-")
            .jwtService(jwtService)
            .restTemplate(restTemplate)
            .mapper(mapper)
            .build();

    long startTime = System.nanoTime();
    job.accept(ctx);
    long endTime = System.nanoTime();
    long elapsedMs = (endTime - startTime) / 1_000_000;

    String log = jobStarted.getLog();
    assertTrue(log.contains("1 repos found with prefix repo-prefix-"));
    assertTrue(log.contains("1 repos deleted"));
    assertTrue(log.contains("0 repos retained"));
    assertTrue(log.contains("0 errors"));
    assertFalse(log.contains("-1")); // Kills the increment mutator

    // Kills Thread.sleep mutator (gives a 50ms buffer for OS thread scheduling)
    assertTrue(elapsedMs >= 950, "Job must sleep for at least 1000ms");

    // KILLS HEADER MUTATORS: Capture the entity sent to the DELETE request and check its headers
    verify(restTemplate, times(1))
        .exchange(eq(deleteUrl), eq(HttpMethod.DELETE), entityCaptor.capture(), eq(String.class));
    HttpHeaders capturedHeaders = entityCaptor.getValue().getHeaders();
    assertEquals("Bearer dummy-token", capturedHeaders.getFirst("Authorization"));
    assertEquals("application/vnd.github+json", capturedHeaders.getFirst("Accept"));
    assertEquals("2022-11-28", capturedHeaders.getFirst("X-GitHub-Api-Version"));
  }

  @Test
  public void test_retains_repo_with_commits() throws Exception {
    Course course = Course.builder().orgName("ucsb-cs156").build();
    when(jwtService.getInstallationToken(course)).thenReturn("dummy-token");

    String reposUrl = "https://api.github.com/orgs/ucsb-cs156/repos?per_page=100";
    String reposJson = "[{\"name\": \"repo-prefix-student2\"}]";

    // Mock the repos endpoint
    ResponseEntity<String> reposResponse = new ResponseEntity<>(reposJson, HttpStatus.OK);
    when(restTemplate.exchange(
            eq(reposUrl), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
        .thenReturn(reposResponse);

    // Mock the commits endpoint (Returns 200 OK with array of commits)
    String commitsUrl = "https://api.github.com/repos/ucsb-cs156/repo-prefix-student2/commits";
    String commitsJson = "[{\"sha\": \"dummy-sha\"}]";
    ResponseEntity<String> commitsResponse = new ResponseEntity<>(commitsJson, HttpStatus.OK);
    when(restTemplate.exchange(
            eq(commitsUrl), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
        .thenReturn(commitsResponse);

    DeleteRepoJob job =
        DeleteRepoJob.builder()
            .course(course)
            .prefix("repo-prefix-")
            .jwtService(jwtService)
            .restTemplate(restTemplate)
            .mapper(mapper)
            .build();

    long startTime = System.nanoTime();
    job.accept(ctx);
    long endTime = System.nanoTime();
    long elapsedMs = (endTime - startTime) / 1_000_000;

    String log = jobStarted.getLog();
    assertTrue(log.contains("1 repos found with prefix repo-prefix-"));
    assertTrue(log.contains("Repo repo-prefix-student2 not delete; commits exist."));
    assertTrue(log.contains("0 repos deleted"));
    assertTrue(log.contains("1 repos retained"));
    assertTrue(log.contains("0 errors"));
    assertFalse(log.contains("-1")); // Kills the increment mutator

    // Kills Thread.sleep mutator
    assertTrue(elapsedMs >= 950, "Job must sleep for at least 1000ms");

    // Ensure delete was NEVER called
    verify(restTemplate, times(0))
        .exchange(
            any(String.class), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(String.class));
  }

  @Test
  public void test_pagination_and_error_handling() throws Exception {
    Course course = Course.builder().orgName("ucsb-cs156").build();
    when(jwtService.getInstallationToken(course)).thenReturn("dummy-token");

    String reposUrlPage1 = "https://api.github.com/orgs/ucsb-cs156/repos?per_page=100";
    String reposJsonPage1 = "[{\"name\": \"test-1\"}]";
    String reposUrlPage2 = "https://api.github.com/orgs/ucsb-cs156/repos?page=2";
    String reposJsonPage2 = "[{\"name\": \"test-2\"}]";

    // Mock Page 1 with Link header
    HttpHeaders headersPage1 = new HttpHeaders();
    headersPage1.add("Link", "<" + reposUrlPage2 + ">; rel=\"next\"");
    ResponseEntity<String> reposResponsePage1 =
        new ResponseEntity<>(reposJsonPage1, headersPage1, HttpStatus.OK);
    when(restTemplate.exchange(
            eq(reposUrlPage1), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
        .thenReturn(reposResponsePage1);

    // Mock Page 2 with no next link
    ResponseEntity<String> reposResponsePage2 = new ResponseEntity<>(reposJsonPage2, HttpStatus.OK);
    when(restTemplate.exchange(
            eq(reposUrlPage2), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
        .thenReturn(reposResponsePage2);

    // Mock commits for test-1: API Error (e.g., 500 Server Error)
    String commitsUrl1 = "https://api.github.com/repos/ucsb-cs156/test-1/commits";
    when(restTemplate.exchange(
            eq(commitsUrl1), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
        .thenThrow(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

    // Mock commits for test-2: Empty repo (409 Conflict)
    String commitsUrl2 = "https://api.github.com/repos/ucsb-cs156/test-2/commits";
    when(restTemplate.exchange(
            eq(commitsUrl2), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
        .thenThrow(new HttpClientErrorException(HttpStatus.CONFLICT));

    // Mock delete for test-2
    String deleteUrl2 = "https://api.github.com/repos/ucsb-cs156/test-2";
    when(restTemplate.exchange(
            eq(deleteUrl2), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(String.class)))
        .thenReturn(new ResponseEntity<>("", HttpStatus.NO_CONTENT));

    DeleteRepoJob job =
        DeleteRepoJob.builder()
            .course(course)
            .prefix("test-")
            .jwtService(jwtService)
            .restTemplate(restTemplate)
            .mapper(mapper)
            .build();

    long startTime = System.nanoTime();
    job.accept(ctx);
    long endTime = System.nanoTime();
    long elapsedMs = (endTime - startTime) / 1_000_000;

    String log = jobStarted.getLog();
    assertTrue(log.contains("2 repos found with prefix test-"));
    assertTrue(log.contains("Error processing repo test-1: 500 INTERNAL_SERVER_ERROR"));
    assertTrue(log.contains("1 repos deleted"));
    assertTrue(log.contains("0 repos retained"));
    assertTrue(log.contains("1 errors"));
    assertFalse(log.contains("-1 repos deleted"));
    assertFalse(log.contains("-1 repos retained"));
    assertFalse(log.contains("-1 errors"));

    // Kills Thread.sleep mutator for 2 items in the loop (should be ~2000ms)
    assertTrue(elapsedMs >= 1900, "Job must sleep for at least 2000ms for 2 items");
  }

  @Test
  public void test_no_repos_match_prefix() throws Exception {
    Course course = Course.builder().orgName("ucsb-cs156").build();
    when(jwtService.getInstallationToken(course)).thenReturn("dummy-token");

    String reposUrl = "https://api.github.com/orgs/ucsb-cs156/repos?per_page=100";
    String reposJson = "[{\"name\": \"unrelated-repo\"}, {\"name\": \"another-unrelated\"}]";

    when(restTemplate.exchange(
            eq(reposUrl), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
        .thenReturn(new ResponseEntity<>(reposJson, HttpStatus.OK));

    DeleteRepoJob job =
        DeleteRepoJob.builder()
            .course(course)
            .prefix("repo-prefix-")
            .jwtService(jwtService)
            .restTemplate(restTemplate)
            .mapper(mapper)
            .build();

    job.accept(ctx);

    String log = jobStarted.getLog();
    assertTrue(log.contains("0 repos found with prefix repo-prefix-"));
    assertTrue(log.contains("0 repos deleted"));
    assertTrue(log.contains("0 repos retained"));
    assertTrue(log.contains("0 errors"));

    // Delete should never be called
    verify(restTemplate, times(0))
        .exchange(
            any(String.class), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(String.class));
  }

  @Test
  public void test_link_header_present_but_no_next_rel() throws Exception {
    Course course = Course.builder().orgName("ucsb-cs156").build();
    when(jwtService.getInstallationToken(course)).thenReturn("dummy-token");

    String reposUrl = "https://api.github.com/orgs/ucsb-cs156/repos?per_page=100";
    String reposJson = "[]"; // No matching repos — we only need the pagination logic here

    // Link header with only rel="prev" (i.e., this is the last page)
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.add(
        "Link", "<https://api.github.com/orgs/ucsb-cs156/repos?page=1>; rel=\"prev\"");
    when(restTemplate.exchange(
            eq(reposUrl), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
        .thenReturn(new ResponseEntity<>(reposJson, responseHeaders, HttpStatus.OK));

    DeleteRepoJob job =
        DeleteRepoJob.builder()
            .course(course)
            .prefix("repo-prefix-")
            .jwtService(jwtService)
            .restTemplate(restTemplate)
            .mapper(mapper)
            .build();

    job.accept(ctx);

    // Only one GET call — the loop must have stopped after the first page
    verify(restTemplate, times(1))
        .exchange(eq(reposUrl), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
    assertTrue(jobStarted.getLog().contains("0 repos found with prefix repo-prefix-"));
  }

  @Test
  public void test_multi_part_link_header_prev_before_next() throws Exception {
    Course course = Course.builder().orgName("ucsb-cs156").build();
    when(jwtService.getInstallationToken(course)).thenReturn("dummy-token");

    String reposUrlPage1 = "https://api.github.com/orgs/ucsb-cs156/repos?per_page=100";
    String reposUrlPage2 = "https://api.github.com/orgs/ucsb-cs156/repos?page=2";

    // Multi-part Link header: rel="prev" part comes FIRST, rel="next" part comes second
    // This forces the if (part.contains("rel=\"next\"")) to evaluate false on the first iteration
    HttpHeaders headersPage1 = new HttpHeaders();
    headersPage1.add(
        "Link",
        "<https://api.github.com/orgs/ucsb-cs156/repos?page=1>; rel=\"prev\","
            + " <"
            + reposUrlPage2
            + ">; rel=\"next\"");
    when(restTemplate.exchange(
            eq(reposUrlPage1), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
        .thenReturn(new ResponseEntity<>("[]", headersPage1, HttpStatus.OK));

    when(restTemplate.exchange(
            eq(reposUrlPage2), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
        .thenReturn(new ResponseEntity<>("[]", HttpStatus.OK));

    DeleteRepoJob job =
        DeleteRepoJob.builder()
            .course(course)
            .prefix("repo-prefix-")
            .jwtService(jwtService)
            .restTemplate(restTemplate)
            .mapper(mapper)
            .build();

    job.accept(ctx);

    verify(restTemplate, times(1))
        .exchange(eq(reposUrlPage1), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
    verify(restTemplate, times(1))
        .exchange(eq(reposUrlPage2), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
    assertTrue(jobStarted.getLog().contains("0 repos found with prefix repo-prefix-"));
  }

  @Test
  public void test_delete_repo_with_empty_commits_array_200() throws Exception {
    Course course = Course.builder().orgName("ucsb-cs156").build();
    when(jwtService.getInstallationToken(course)).thenReturn("dummy-token");

    String reposUrl = "https://api.github.com/orgs/ucsb-cs156/repos?per_page=100";
    String reposJson = "[{\"name\": \"repo-prefix-empty\"}]";
    when(restTemplate.exchange(
            eq(reposUrl), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
        .thenReturn(new ResponseEntity<>(reposJson, HttpStatus.OK));

    // 200 OK with an empty array — no 409 Conflict, but no commits either
    String commitsUrl = "https://api.github.com/repos/ucsb-cs156/repo-prefix-empty/commits";
    when(restTemplate.exchange(
            eq(commitsUrl), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
        .thenReturn(new ResponseEntity<>("[]", HttpStatus.OK));

    String deleteUrl = "https://api.github.com/repos/ucsb-cs156/repo-prefix-empty";
    when(restTemplate.exchange(
            eq(deleteUrl), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(String.class)))
        .thenReturn(new ResponseEntity<>("", HttpStatus.NO_CONTENT));

    DeleteRepoJob job =
        DeleteRepoJob.builder()
            .course(course)
            .prefix("repo-prefix-")
            .jwtService(jwtService)
            .restTemplate(restTemplate)
            .mapper(mapper)
            .build();

    job.accept(ctx);

    String log = jobStarted.getLog();
    assertTrue(log.contains("1 repos found with prefix repo-prefix-"));
    assertTrue(log.contains("1 repos deleted"));
    assertTrue(log.contains("0 repos retained"));
    assertTrue(log.contains("0 errors"));

    // Confirm delete was called exactly once
    verify(restTemplate, times(1))
        .exchange(eq(deleteUrl), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(String.class));
  }
}
