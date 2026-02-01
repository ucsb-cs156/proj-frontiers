package edu.ucsb.cs156.frontiers.jobs;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.Team;
import edu.ucsb.cs156.frontiers.entities.TeamMember;
import edu.ucsb.cs156.frontiers.repositories.TeamMemberRepository;
import edu.ucsb.cs156.frontiers.services.GithubTeamService;
import edu.ucsb.cs156.frontiers.services.jobs.JobContext;
import edu.ucsb.cs156.frontiers.services.jobs.JobContextConsumer;
import java.util.Optional;
import lombok.Builder;

@Builder
public class DeleteTeamMemberFromGithubJob implements JobContextConsumer {
  Long teamMemberId;
  Long teamId;
  TeamMemberRepository teamMemberRepository;
  GithubTeamService githubTeamService;

  @Override
  public void accept(JobContext ctx) throws Exception {
    ctx.log(
        "Starting delete team member from GitHub job for team ID, member ID: "
            + teamId
            + ", "
            + teamMemberId);

    // Get the team member
    Optional<TeamMember> teamMemberOpt = teamMemberRepository.findById(teamMemberId);
    if (teamMemberOpt.isEmpty()) {
      ctx.log("ERROR: Team member with ID " + teamMemberId + " not found");
      return;
    }
    TeamMember teamMember = teamMemberOpt.get();
    RosterStudent rosterStudent = teamMember.getRosterStudent();
    Team team = teamMember.getTeam();
    Course course = team.getCourse();

    ctx.log(
        "Processing team member: "
            + rosterStudent.getGithubLogin()
            + " in team: "
            + team.getName());

    if (course.getOrgName() == null || course.getInstallationId() == null) {
      ctx.log("ERROR: Course has no linked GitHub organization");
      return;
    }

    try {
      githubTeamService.removeMemberFromGithubTeam(
          rosterStudent.getGithubLogin(), team.getGithubTeamId(), course);
      ctx.log("Successfully removed user from GitHub team");
    } catch (Exception e) {
      ctx.log("ERROR: Failed to remove user from GitHub team: " + e.getMessage());
    }

    ctx.log("Done");
  }
}
