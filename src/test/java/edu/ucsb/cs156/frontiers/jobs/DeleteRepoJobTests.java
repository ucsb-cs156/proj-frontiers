package edu.ucsb.cs156.frontiers.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.services.RepositoryService;
import edu.ucsb.cs156.frontiers.services.jobs.JobContext;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DeleteRepoJobTests {

  @Mock RepositoryService repositoryService;
  @Mock JobContext ctx;
  @Captor ArgumentCaptor<String> logCaptor;

  @Test
  void test_get_course() {
    Course course = Course.builder().courseName("CS156").build();

    DeleteRepoJob job =
        DeleteRepoJob.builder()
            .course(course)
            .prefix("lab01")
            .repositoryService(repositoryService)
            .build();

    assertEquals(course, job.getCourse());
  }

  @Test
  void test_delete_repo_job_success_mixed_repos() throws Exception {
    Course course = Course.builder().courseName("CS156").build();
    DeleteRepoJob job =
        DeleteRepoJob.builder()
            .course(course)
            .prefix("lab01")
            .repositoryService(repositoryService)
            .build();

    when(repositoryService.getRepoNamesWithPrefix(course, "lab01"))
        .thenReturn(List.of("lab01-brian", "lab01-empty"));

    when(repositoryService.repoHasCommits(course, "lab01-brian")).thenReturn(true);
    when(repositoryService.repoHasCommits(course, "lab01-empty")).thenReturn(false);

    long startTime = System.currentTimeMillis();
    job.accept(ctx);
    long endTime = System.currentTimeMillis();

    assertTrue(endTime - startTime >= 1900, "Job should sleep for 1000ms per repo");

    verify(ctx, times(7)).log(logCaptor.capture());
    List<String> logs = logCaptor.getAllValues();

    assertEquals("Starting DeleteRepoJob for course CS156 with prefix lab01", logs.get(0));
    assertEquals("2 repos found with prefix lab01", logs.get(1));
    assertEquals("Repo lab01-brian not deleted; commits exist.", logs.get(2));
    assertEquals("Deleted repo lab01-empty", logs.get(3));

    assertEquals("1 repos deleted", logs.get(4));
    assertEquals("1 repos retained", logs.get(5));
    assertEquals("0 errors", logs.get(6));

    verify(repositoryService, never()).deleteRepository(course, "lab01-brian");
    verify(repositoryService, times(1)).deleteRepository(course, "lab01-empty");
  }

  @Test
  void test_delete_repo_job_handles_exceptions() throws Exception {
    Course course = Course.builder().courseName("CS156").build();
    DeleteRepoJob job =
        DeleteRepoJob.builder()
            .course(course)
            .prefix("lab01")
            .repositoryService(repositoryService)
            .build();

    when(repositoryService.getRepoNamesWithPrefix(course, "lab01"))
        .thenReturn(List.of("lab01-error"));

    when(repositoryService.repoHasCommits(course, "lab01-error"))
        .thenThrow(new RuntimeException("GitHub API down"));

    long startTime = System.currentTimeMillis();
    job.accept(ctx);
    long endTime = System.currentTimeMillis();

    assertTrue(endTime - startTime >= 900, "Job should sleep for 1000ms per repo");

    verify(ctx, times(6)).log(logCaptor.capture());
    List<String> logs = logCaptor.getAllValues();

    assertEquals("Error processing repo lab01-error: GitHub API down", logs.get(2));
    assertEquals("0 repos deleted", logs.get(3));
    assertEquals("0 repos retained", logs.get(4));
    assertEquals("1 errors", logs.get(5));

    verify(repositoryService, never()).deleteRepository(eq(course), anyString());
  }

  @Test
  void test_delete_repo_job_no_repos_found() throws Exception {
    Course course = Course.builder().courseName("CS156").build();
    DeleteRepoJob job =
        DeleteRepoJob.builder()
            .course(course)
            .prefix("lab01")
            .repositoryService(repositoryService)
            .build();

    when(repositoryService.getRepoNamesWithPrefix(course, "lab01")).thenReturn(List.of());

    job.accept(ctx);

    verify(ctx, times(5)).log(logCaptor.capture());
    List<String> logs = logCaptor.getAllValues();

    assertEquals("Starting DeleteRepoJob for course CS156 with prefix lab01", logs.get(0));
    assertEquals("0 repos found with prefix lab01", logs.get(1));
    assertEquals("0 repos deleted", logs.get(2));
    assertEquals("0 repos retained", logs.get(3));
    assertEquals("0 errors", logs.get(4));

    verify(repositoryService, never()).deleteRepository(any(), anyString());
  }
}
