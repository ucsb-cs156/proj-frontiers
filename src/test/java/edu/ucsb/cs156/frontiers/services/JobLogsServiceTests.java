package edu.ucsb.cs156.frontiers.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import edu.ucsb.cs156.frontiers.entities.Job;
import edu.ucsb.cs156.frontiers.repositories.JobsRepository;
import edu.ucsb.cs156.frontiers.services.jobs.JobService;

public class JobLogsServiceTests {

  @Mock private JobsRepository jobRepository;

  @InjectMocks private JobService jobService;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
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
}
