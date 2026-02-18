package edu.ucsb.cs156.frontiers.jobs;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.services.GithubTeamService;
import edu.ucsb.cs156.frontiers.services.jobs.JobContext;
import edu.ucsb.cs156.frontiers.services.jobs.JobContextConsumer;
import lombok.Builder;

@Builder
public class DeleteTeamMemberFromGithubJob implements JobContextConsumer {
  String memberGithubLogin;
  Integer githubTeamId;
  Course course;
  GithubTeamService githubTeamService;

  @Override
  public Course getCourse() {
    return this.course;
  }

  @Override
  public void accept(JobContext ctx) throws Exception {
    ctx.log(
        "Starting delete team member from GitHub job for team ID "
            + githubTeamId
            + " member "
            + memberGithubLogin);

    if (githubTeamId == null) {
      ctx.log("ERROR: Team has no GitHub team ID");
      return;
    }

    if (memberGithubLogin == null) {
      ctx.log("ERROR: Team member has no GitHub login");
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
      githubTeamService.removeMemberFromGithubTeam(orgId, memberGithubLogin, githubTeamId, course);
      ctx.log("Successfully removed user from GitHub team");
    } catch (Exception e) {
      ctx.log("ERROR: Failed to remove user from GitHub team: " + e.getMessage());
    }

    ctx.log("Done");
  }
}
