package edu.ucsb.cs156.frontiers.jobs;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.TeamMember;
import edu.ucsb.cs156.frontiers.enums.TeamStatus;
import edu.ucsb.cs156.frontiers.repositories.TeamMemberRepository;
import edu.ucsb.cs156.frontiers.services.GithubTeamService;
import edu.ucsb.cs156.frontiers.services.jobs.JobContext;
import edu.ucsb.cs156.frontiers.services.jobs.JobContextConsumer;
import lombok.Builder;

@Builder
public class AddTeamMemberToGithubJob implements JobContextConsumer {
  String memberGithubLogin;
  Integer githubTeamId;
  Long teamMemberId;
  Course course;
  GithubTeamService githubTeamService;
  TeamMemberRepository teamMemberRepository;

  @Override
  public void accept(JobContext ctx) throws Exception {
    ctx.log(
        "Starting add team member to GitHub job for team ID "
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
      TeamStatus newStatus =
          githubTeamService.addMemberToGithubTeam(
              memberGithubLogin, githubTeamId, "member", course, orgId);
      ctx.log(
          "Successfully added " + memberGithubLogin + " to Github team with status: " + newStatus);

      if (teamMemberId != null && teamMemberRepository != null) {
        TeamMember teamMember = teamMemberRepository.findById(teamMemberId).orElse(null);
        if (teamMember != null) {
          teamMember.setTeamStatus(newStatus);
          teamMemberRepository.save(teamMember);
          ctx.log("Updated team member status in database");
        }
      }
    } catch (Exception e) {
      ctx.log("ERROR: Failed to add user to GitHub team: " + e.getMessage());
    }

    ctx.log("Done");
  }
}
