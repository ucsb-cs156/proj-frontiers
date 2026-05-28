package edu.ucsb.cs156.frontiers.jobs;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.services.RepositoryService;
import edu.ucsb.cs156.frontiers.services.jobs.JobContext;
import edu.ucsb.cs156.frontiers.services.jobs.JobContextConsumer;
import java.util.List;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Builder
@Slf4j
public class DeleteRepoJob implements JobContextConsumer {

  private final Course course;
  private final String prefix;
  private final RepositoryService repositoryService;

  @Override
  public Course getCourse() {
    return course;
  }

  @Override
  public void accept(JobContext ctx) throws Exception {
    ctx.log(
        String.format(
            "Starting DeleteRepoJob for course %s with prefix %s", course.getCourseName(), prefix));

    List<String> matchingRepos = repositoryService.getRepoNamesWithPrefix(course, prefix);

    ctx.log(String.format("%d repos found with prefix %s", matchingRepos.size(), prefix));

    int reposDeleted = 0;
    int reposRetained = 0;
    int repoErrors = 0;

    for (String repoName : matchingRepos) {
      try {
        // Sleep to prevent GitHub API rate limiting
        Thread.sleep(1000);

        boolean hasCommits = repositoryService.repoHasCommits(course, repoName);

        if (hasCommits) {
          reposRetained++;
          ctx.log(String.format("Repo %s not deleted; commits exist.", repoName));
        } else {
          repositoryService.deleteRepository(course, repoName);
          reposDeleted++;
          ctx.log(String.format("Deleted repo %s", repoName));
        }
      } catch (Exception e) {
        repoErrors++;
        ctx.log(String.format("Error processing repo %s: %s", repoName, e.getMessage()));
      }
    }

    ctx.log(String.format("%d repos deleted", reposDeleted));
    ctx.log(String.format("%d repos retained", reposRetained));
    ctx.log(String.format("%d errors", repoErrors));
  }
}
