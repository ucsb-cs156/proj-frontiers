package edu.ucsb.cs156.frontiers.services;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import edu.ucsb.cs156.frontiers.entities.Job;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.jobs.TestJob;
import edu.ucsb.cs156.frontiers.models.CurrentUser;
import edu.ucsb.cs156.frontiers.repositories.JobsRepository;
import edu.ucsb.cs156.frontiers.services.jobs.JobContext;
import edu.ucsb.cs156.frontiers.services.jobs.JobContextFactory;
import edu.ucsb.cs156.frontiers.services.jobs.JobService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class JobServiceTests {

  @Mock private JobsRepository jobRepository;

  @Mock JobContextFactory contextFactory;

  @Mock private JobService injectedJobService;

  @Mock private CurrentUserService currentUserService;

  @InjectMocks private JobService jobService;

  CurrentUser user;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
    user =
        CurrentUser.builder()
            .roles(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
            .user(User.builder().id(1L).build())
            .build();
  }

  @Test
  void test_getJobLogs_with_log() {
    // Arrange
    Long jobId = 1L;
    Job job = Job.builder().build();
    job.setLog("This is a job log");
    when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

    // Act
    String result = jobService.getJobLogs(jobId);

    // Assert
    assertEquals("This is a job log", result);
  }

  @Test
  void test_getJobLogs_with_null_log() {
    // Arrange
    Long jobId = 2L;
    Job job = Job.builder().build();
    job.setLog(null);
    when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

    // Act
    String result = jobService.getJobLogs(jobId);

    // Assert
    assertEquals("", result);
  }

  @Test
  void test_getJobLogs_job_not_found() {
    // Arrange
    Long jobId = 3L;
    when(jobRepository.findById(jobId)).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(IllegalArgumentException.class, () -> jobService.getJobLogs(jobId));
  }

  @Test
  void runAsJob_fires_correctly() {
    TestJob job = TestJob.builder().fail(false).sleepMs(0).build();

    doNothing().when(injectedJobService).runJobAsync(any(), any());
    when(currentUserService.getUser()).thenReturn(user.getUser());
    when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

    Job result = jobService.runAsJob(job);

    // Verify the created Job has correct fields
    ArgumentCaptor<Job> savedJobCaptor = ArgumentCaptor.forClass(Job.class);
    verify(jobRepository).save(savedJobCaptor.capture());
    Job savedJob = savedJobCaptor.getValue();
    assertEquals("TestJob", savedJob.getJobName());
    assertEquals("running", savedJob.getStatus());
    assertEquals(user.getUser(), savedJob.getCreatedBy());

    // Verify runJobAsync was called with correct args
    ArgumentCaptor<Job> asyncJobCaptor = ArgumentCaptor.forClass(Job.class);
    verify(injectedJobService).runJobAsync(asyncJobCaptor.capture(), eq(job));
    assertEquals("TestJob", asyncJobCaptor.getValue().getJobName());
    assertEquals("running", asyncJobCaptor.getValue().getStatus());
  }

  @Test
  void runAsyncJob_fires_correctly() throws Exception {
    TestJob job = mock(TestJob.class);
    JobContext context = mock(JobContext.class);

    Job passedJob = Job.builder().status("running").build();

    doNothing().when(job).accept(any());

    when(contextFactory.createContext(any(Job.class))).thenReturn(context);
    doNothing().when(job).accept(eq(context));

    jobService.runJobAsync(passedJob, job);

    // Verify save was called and the status was updated to "complete"
    ArgumentCaptor<Job> savedJobCaptor = ArgumentCaptor.forClass(Job.class);
    await()
        .atMost(2, SECONDS)
        .untilAsserted(() -> verify(jobRepository).save(savedJobCaptor.capture()));
    assertEquals("complete", savedJobCaptor.getValue().getStatus());

    verify(job).accept(eq(context));
    verify(contextFactory).createContext(any(Job.class));
  }

  @Test
  void runAsyncJob_handles_error() throws Exception {
    TestJob job = mock(TestJob.class);
    JobContext context = mock(JobContext.class);

    Job passedJob = Job.builder().status("running").build();
    doNothing().when(job).accept(any());

    when(contextFactory.createContext(any(Job.class))).thenReturn(context);
    doThrow(new Exception("fail!")).when(job).accept(eq(context));

    jobService.runJobAsync(passedJob, job);
    await().atMost(2, SECONDS).untilAsserted(() -> verify(context).log(contains("fail!")));
    verify(job).accept(eq(context));
    verify(contextFactory).createContext(any(Job.class));
    assertEquals("error", passedJob.getStatus());
  }
}
