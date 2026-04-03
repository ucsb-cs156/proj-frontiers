package edu.ucsb.cs156.frontiers.jobs;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.Team;
import edu.ucsb.cs156.frontiers.enums.RepositoryPermissions;
import edu.ucsb.cs156.frontiers.services.GithubTeamService;
import edu.ucsb.cs156.frontiers.services.RepositoryService;
import edu.ucsb.cs156.frontiers.services.jobs.JobContext;
import edu.ucsb.cs156.frontiers.services.jobs.JobContextConsumer;
import lombok.Builder;

@Builder
public class CreateTeamRepositoriesJob implements JobContextConsumer {
  Course course;
  RepositoryService repositoryService;
  GithubTeamService githubTeamService;
  String repositoryPrefix;
  Boolean isPrivate;
  RepositoryPermissions permissions;

  @Override
  public Course getCourse() {
    return course;
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
      repositoryService.createTeamRepository(
          course, team, repositoryPrefix, isPrivate, permissions, orgId);
    }
    ctx.log("Done");
  }
}
