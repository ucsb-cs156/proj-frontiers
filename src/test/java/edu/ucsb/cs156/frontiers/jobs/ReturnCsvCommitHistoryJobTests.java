package edu.ucsb.cs156.frontiers.jobs;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.models.Commit;
import edu.ucsb.cs156.frontiers.models.CommitHistory;
import edu.ucsb.cs156.frontiers.services.GithubGraphQLService;
import edu.ucsb.cs156.frontiers.services.jobs.JobContext;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ReturnCsvCommitHistoryJobTests {

  @Mock private GithubGraphQLService githubService;

  @Mock private JobContext ctx;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void simple_pass_through_test() throws Exception {
    Course course = Course.builder().courseName("banana").build();
    ReturnCsvCommitHistoryJob job =
        ReturnCsvCommitHistoryJob.builder()
            .course(course)
            .owner("ucsb-cs156")
            .repo("proj-frontiers")
            .githubService(githubService)
            .branch("main")
            .count(10)
            .build();

    CommitHistory history =
        CommitHistory.builder()
            .owner("ucsb-cs156")
            .repo("proj-frontiers")
            .retrievedTime(ZonedDateTime.parse("2023-03-21T08:00:00Z"))
            .build();
    Commit firstCommit =
        Commit.builder()
            .commitTime(
                ZonedDateTime.parse("2026-01-27T00:55:00Z")
                    .withZoneSameInstant(ZoneId.systemDefault()))
            .message("dj - added actual constraint validating dependency, modified unit tes…")
            .authorEmail("djensen2@outlook.com")
            .authorLogin("Division7")
            .authorName("Daniel Jensen")
            .url(
                "https://github.com/ucsb-cs156/proj-frontiers/commit/9df9217b7f66102d0dcaecf48ef48af16facb058")
            .committerEmail("djensen2@outlook.com")
            .committerLogin("Division7")
            .committerName("Daniel Jensen")
            .build();

    Commit secondCommit =
        Commit.builder()
            .commitTime(
                ZonedDateTime.parse("2026-01-26T00:58:12Z")
                    .withZoneSameInstant(ZoneId.systemDefault()))
            .message("dj - removed files outside of our normal unit testing area")
            .authorEmail("djensen2@outlook.com")
            .authorLogin("Division7")
            .authorName("Daniel Jensen")
            .url(
                "https://github.com/ucsb-cs156/proj-frontiers/commit/f0497a983b1533f4b7f9f2779030d3fa62fd6031")
            .committerEmail("djensen2@outlook.com")
            .committerLogin("Division7")
            .committerName("Daniel Jensen")
            .build();

    history.getCommits().add(firstCommit);
    history.getCommits().add(secondCommit);
    history.setCount(2);

    when(githubService.returnCommitHistory(
            eq(course), eq("ucsb-cs156"), eq("proj-frontiers"), eq("main"), eq(10)))
        .thenReturn(history);

    job.accept(ctx);

    verify(githubService, times(1))
        .returnCommitHistory(
            eq(course), eq("ucsb-cs156"), eq("proj-frontiers"), eq("main"), eq(10));
    verify(ctx).log(eq("Returning CSV commit history for course: banana"));
    verify(ctx).log(eq("CSV commit history returned."));
  }
}
