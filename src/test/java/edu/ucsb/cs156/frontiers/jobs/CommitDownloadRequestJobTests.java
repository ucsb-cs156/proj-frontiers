package edu.ucsb.cs156.frontiers.jobs;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.DownloadRequest;
import edu.ucsb.cs156.frontiers.services.GithubGraphQLService;
import edu.ucsb.cs156.frontiers.services.jobs.JobContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class CommitDownloadRequestJobTests {

  @Mock private GithubGraphQLService githubService;

  @Mock private JobContext ctx;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void simple_pass_through_test() throws Exception {
    DownloadRequest request =
        DownloadRequest.builder()
            .id(1L)
            .course(Course.builder().courseName("banana").build())
            .build();

    CommitDownloadRequestJob job =
        CommitDownloadRequestJob.builder().request(request).githubService(githubService).build();

    job.accept(ctx);

    verify(githubService, times(1)).downloadCommitHistory(eq(request));

    verify(ctx, times(1)).log(contains("Starting download for course banana"));
    verify(ctx, times(1)).log(contains("Download completed successfully"));
  }
}
