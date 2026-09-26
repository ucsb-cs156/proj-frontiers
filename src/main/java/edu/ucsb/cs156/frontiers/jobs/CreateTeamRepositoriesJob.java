package edu.ucsb.cs156.frontiers.jobs;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.Team;
import edu.ucsb.cs156.frontiers.enums.RepositoryPermissions;
import edu.ucsb.cs156.frontiers.services.GithubTeamService;
import edu.ucsb.cs156.frontiers.services.RepositoryService;
import edu.ucsb.cs156.frontiers.services.RepositoryService.RepositoryCreationResult;
import edu.ucsb.cs156.jobs.services.JobContext;
import edu.ucsb.cs156.jobs.services.JobContextConsumer;
import java.util.Optional;
import lombok.Builder;
import org.springframework.web.client.HttpStatusCodeException;

@Builder
public class CreateTeamRepositoriesJob implements JobContextConsumer {
  Course course;
  RepositoryService repositoryService;
  GithubTeamService githubTeamService;
  String repositoryPrefix;
  Boolean isPrivate;
  RepositoryPermissions permissions;
  String teamRegex;

  /**
   * Whether the repositories must require signed commits: true gives each repository the ruleset
   * that requires them, false removes it. Null, as for a caller that does not manage this, leaves
   * the repositories' rulesets alone.
   */
  Boolean requireSignedCommit;

  @Override
  public String getScopeType() {
    return "course";
  }

  @Override
  public Long getScopeId() {
    return course.getId();
  }

  @Override
  public void accept(JobContext ctx) throws Exception {
    ctx.log("Creating team repositories...");
    ctx.log("repositoryPrefix=" + repositoryPrefix);
    ctx.log("isPrivate=" + isPrivate);
    ctx.log("permissions=" + permissions);
    ctx.log("teamRegex=" + teamRegex);
    if (requireSignedCommit != null) {
      ctx.log("requireSignedCommit=" + requireSignedCommit);
    }

    Integer orgId;
    try {
      orgId = githubTeamService.getOrgId(course.getOrgName(), course);
    } catch (Exception e) {
      throw new IllegalStateException(
          "Failed to get organization ID for org: " + course.getOrgName() + " - " + e.getMessage(),
          e);
    }

    int reposCreated = 0;
    int reposUpdated = 0;
    int signedCommitsFailures = 0;

    for (Team team : course.getTeams()) {
      // A team skipped by teamRegex never logs anything -- checkCancellation() gives this
      // loop its own checkpoint independent of whether an iteration does any work.
      ctx.checkCancellation();
      if (teamRegex != null && !team.getName().matches(teamRegex)) {
        continue;
      }
      Optional<RepositoryCreationResult> result =
          repositoryService.createTeamRepository(
              course, team, repositoryPrefix, isPrivate, permissions, orgId);
      if (result.isPresent()) {
        if (result.get().created()) {
          ctx.log(" created repo " + result.get().repoName());
          reposCreated++;
        } else {
          ctx.log("  updated repo " + result.get().repoName());
          reposUpdated++;
        }
        if (!applySignedCommits(ctx, result.get().repoName())) {
          signedCommitsFailures++;
        }
      }
    }
    ctx.log("Summary:");
    ctx.log(String.format("%4d repos created", reposCreated));
    ctx.log(String.format("%4d repos updated", reposUpdated));
    ctx.log(String.format("%4d repos total", reposCreated + reposUpdated));
    if (signedCommitsFailures > 0) {
      ctx.log(
          String.format("%4d repos where signed commits could not be set", signedCommitsFailures));
    }
    ctx.log("Done");
  }

  /**
   * Makes a repository require signed commits, or not, as this job is set to; does nothing if it is
   * not set either way. A refusal by GitHub is logged, and does not stop the job.
   *
   * @return false if GitHub refused, true otherwise
   */
  private boolean applySignedCommits(JobContext ctx, String repoName) throws Exception {
    if (requireSignedCommit == null) {
      return true;
    }
    try {
      repositoryService.setSignedCommitsRequired(course, repoName, requireSignedCommit);
      return true;
    } catch (HttpStatusCodeException e) {
      ctx.log(
          "  could not "
              + (requireSignedCommit ? "require" : "stop requiring")
              + " signed commits on "
              + repoName
              + ": "
              + e.getStatusCode()
              + " "
              + e.getResponseBodyAsString());
      return false;
    }
  }
}
