package edu.ucsb.cs156.frontiers.jobs;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.services.GithubTeamService;
import edu.ucsb.cs156.frontiers.services.jobs.JobContext;
import edu.ucsb.cs156.frontiers.services.jobs.JobContextConsumer;
import lombok.Builder;

@Builder
public class DeleteTeamFromGithubJob implements JobContextConsumer {
  Integer githubTeamId;
  Course course;
  GithubTeamService githubTeamService;

  @Override
  public void accept(JobContext ctx) throws Exception {
    ctx.log("Starting delete team from GitHub job for team ID " + githubTeamId);

    if (githubTeamId == null) {
      ctx.log("ERROR: Team has no GitHub team ID");
      return;
    }

    if (course.getOrgName() == null || course.getInstallationId() == null) {
      ctx.log("ERROR: Course has no linked GitHub organization");
      return;
    }
    // Get the organization id
    Integer orgId = null;
    try {
      orgId = githubTeamService.getOrgId(course.getOrgName(), course);

    } catch (Exception e) {
      ctx.log(
          "ERROR: Failed to get organization ID for org: "
              + course.getOrgName()
              + " - "
              + e.getMessage());
      return;
    }

    try {
      githubTeamService.deleteGithubTeam(orgId, githubTeamId, course);
      ctx.log("Successfully deleted GitHub team");
    } catch (Exception e) {
      ctx.log("ERROR: Failed to delete GitHub team: " + e.getMessage());
    }

    ctx.log("Done");
  }
}
