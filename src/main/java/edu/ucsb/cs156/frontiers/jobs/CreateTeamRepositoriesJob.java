package edu.ucsb.cs156.frontiers.jobs;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.Team;
import edu.ucsb.cs156.frontiers.enums.RepositoryPermissions;
import edu.ucsb.cs156.frontiers.services.GithubTeamService;
import edu.ucsb.cs156.frontiers.services.RepositoryService;
import edu.ucsb.cs156.jobs.services.JobContext;
import edu.ucsb.cs156.jobs.services.JobContextConsumer;
import lombok.Builder;

@Builder
public class CreateTeamRepositoriesJob implements JobContextConsumer {
  Course course;
  RepositoryService repositoryService;
  GithubTeamService githubTeamService;
  String repositoryPrefix;
  Boolean isPrivate;
  RepositoryPermissions permissions;
  String teamRegex;

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

    Integer orgId;
    try {
      orgId = githubTeamService.getOrgId(course.getOrgName(), course);
    } catch (Exception e) {
      throw new IllegalStateException(
          "Failed to get organization ID for org: " + course.getOrgName() + " - " + e.getMessage(),
          e);
    }

    for (Team team : course.getTeams()) {
      // A team skipped by teamRegex never logs anything -- checkCancellation() gives this
      // loop its own checkpoint independent of whether an iteration does any work.
      ctx.checkCancellation();
      if (teamRegex != null && !team.getName().matches(teamRegex)) {
        continue;
      }
      repositoryService.createTeamRepository(
          course, team, repositoryPrefix, isPrivate, permissions, orgId);
    }
    ctx.log("Done");
  }
}
