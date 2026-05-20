package edu.ucsb.cs156.frontiers.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.Job;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.services.RepositoryService;
import edu.ucsb.cs156.frontiers.services.jobs.JobContext;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DeleteReposJobTests {

  @Mock private CourseRepository courseRepository;
  @Mock private RepositoryService repositoryService;

  @Test
  public void getCourse_returnsCourseWhenPresent() {
    Course course = Course.builder().id(7L).build();
    when(courseRepository.findById(7L)).thenReturn(Optional.of(course));

    DeleteReposJob job =
        DeleteReposJob.builder()
            .courseId(7L)
            .courseRepository(courseRepository)
            .repositoryService(repositoryService)
            .prefix("lab")
            .sleepMillis(0L)
            .build();

    assertEquals(course, job.getCourse());
  }

  @Test
  public void getCourse_returnsNullWhenNotPresent() {
    when(courseRepository.findById(7L)).thenReturn(Optional.empty());

    DeleteReposJob job =
        DeleteReposJob.builder()
            .courseId(7L)
            .courseRepository(courseRepository)
            .repositoryService(repositoryService)
            .prefix("lab")
            .sleepMillis(0L)
            .build();

    assertNull(job.getCourse());
  }

  @Test
  public void accept_logsAndReturnsWhenCourseMissing() throws Exception {
    when(courseRepository.findById(8L)).thenReturn(Optional.empty());

    Job jobStarted = Job.builder().build();
    JobContext ctx = new JobContext(null, jobStarted);

    DeleteReposJob job =
        DeleteReposJob.builder()
            .courseId(8L)
            .courseRepository(courseRepository)
            .repositoryService(repositoryService)
            .prefix("lab")
            .sleepMillis(0L)
            .build();

    job.accept(ctx);

    assertEquals("ERROR: Course with ID 8 not found", jobStarted.getLog());
    verifyNoInteractions(repositoryService);
  }

  @Test
  public void accept_logsAndReturnsWhenCourseHasNoLinkedOrg() throws Exception {
    Course course = Course.builder().id(8L).courseName("CMPSC 156").build();
    when(courseRepository.findById(8L)).thenReturn(Optional.of(course));

    Job jobStarted = Job.builder().build();
    JobContext ctx = new JobContext(null, jobStarted);

    DeleteReposJob job =
        DeleteReposJob.builder()
            .courseId(8L)
            .courseRepository(courseRepository)
            .repositoryService(repositoryService)
            .prefix("lab")
            .sleepMillis(0L)
            .build();

    job.accept(ctx);

    assertEquals("ERROR: Course has no linked GitHub organization", jobStarted.getLog());
    verifyNoInteractions(repositoryService);
  }

  @Test
  public void accept_deletesEmptyRepos_andCountsRetainedAndErrors() throws Exception {
    Course course = Course.builder().id(9L).orgName("ucsb-cs156").installationId("1234").build();
    when(courseRepository.findById(9L)).thenReturn(Optional.of(course));
    when(repositoryService.getRepositoryNamesWithPrefix(course, "lab"))
        .thenReturn(List.of("lab-a", "lab-b", "lab-c"));
    when(repositoryService.repositoryHasCommits(course, "lab-a")).thenReturn(false);
    when(repositoryService.repositoryHasCommits(course, "lab-b")).thenReturn(true);
    when(repositoryService.repositoryHasCommits(course, "lab-c"))
        .thenThrow(new RuntimeException("GitHub API error"));

    Job jobStarted = Job.builder().build();
    JobContext ctx = new JobContext(null, jobStarted);

    DeleteReposJob job =
        DeleteReposJob.builder()
            .courseId(9L)
            .courseRepository(courseRepository)
            .repositoryService(repositoryService)
            .prefix("lab")
            .sleepMillis(0L)
            .build();

    job.accept(ctx);

    String expected =
        """
        3 repos found with prefix lab
        Repo lab-b not deleted; commits exist.
        ERROR deleting repo lab-c: GitHub API error
        1 repos deleted
        1 repos retained
        1 errors""";
    assertEquals(expected, jobStarted.getLog());
    verify(repositoryService).deleteRepository(course, "lab-a");
    verify(repositoryService, never()).deleteRepository(course, "lab-b");
    verify(repositoryService, never()).deleteRepository(course, "lab-c");
  }
}
