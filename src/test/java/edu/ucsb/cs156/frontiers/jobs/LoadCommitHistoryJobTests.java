package edu.ucsb.cs156.frontiers.jobs;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.ucsb.cs156.frontiers.entities.Branch;
import edu.ucsb.cs156.frontiers.entities.BranchId;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.services.GithubGraphQLService;
import edu.ucsb.cs156.frontiers.services.jobs.JobContext;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class LoadCommitHistoryJobTests {

  @Mock private GithubGraphQLService githubService;

  @Mock private JobContext ctx;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void simple_pass_through_test() throws Exception {
    BranchId branchId = new BranchId("ucsb-cs156", "proj-frontiers", "main");
    Course course = Course.builder().courseName("banana").build();
    LoadCommitHistoryJob job =
        LoadCommitHistoryJob.builder()
            .course(course)
            .branches(List.of(branchId))
            .githubService(githubService)
            .build();

    Branch history =
        Branch.builder().id(branchId).retrievedTime(Instant.parse("2023-03-21T08:00:00Z")).build();

    when(githubService.loadCommitHistory(eq(course), eq(branchId))).thenReturn(history);

    job.accept(ctx);

    verify(githubService, times(1)).loadCommitHistory(eq(course), eq(branchId));
    verify(ctx).log(eq("Loading commit history for course: banana"));
    verify(ctx)
        .log(
            eq(
                "Commit history loaded or updated for branch: BranchId[org=ucsb-cs156, repo=proj-frontiers, branchName=main]"));
  }
}
