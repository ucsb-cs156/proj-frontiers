package edu.ucsb.cs156.frontiers.jobs;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.Team;
import edu.ucsb.cs156.frontiers.entities.TeamMember;
import edu.ucsb.cs156.frontiers.enums.TeamStatus;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.TeamMemberRepository;
import edu.ucsb.cs156.frontiers.repositories.TeamRepository;
import edu.ucsb.cs156.frontiers.services.GithubTeamService;
import edu.ucsb.cs156.frontiers.services.jobs.JobContext;
import edu.ucsb.cs156.frontiers.services.jobs.JobContextConsumer;
import java.util.Optional;
import lombok.Builder;

@Builder
public class PushTeamsToGithubJob implements JobContextConsumer {
  Long courseId;
  CourseRepository courseRepository;
  TeamRepository teamRepository;
  TeamMemberRepository teamMemberRepository;
  GithubTeamService githubTeamService;

  @Override
  public void accept(JobContext ctx) throws Exception {
    ctx.log("Starting push teams to GitHub job for course ID: " + courseId);

    // Get the course
    Optional<Course> courseOpt = courseRepository.findById(courseId);
    if (courseOpt.isEmpty()) {
      ctx.log("ERROR: Course with ID " + courseId + " not found");
      return;
    }
    Course course = courseOpt.get();
    ctx.log("Processing course: " + course.getCourseName() + " (org: " + course.getOrgName() + ")");

    if (course.getOrgName() == null || course.getInstallationId() == null) {
      ctx.log("ERROR: Course has no linked GitHub organization");
      return;
    }

    // Get all teams for this course
    Iterable<Team> teams = teamRepository.findByCourseId(courseId);

    // First pass: Create teams on GitHub and update githubTeamId
    for (Team team : teams) {
      ctx.log("Processing team: " + team.getName());
      try {
        Integer githubTeamId = githubTeamService.createOrGetTeamId(team, course);
        if (!githubTeamId.equals(team.getGithubTeamId())) {
          team.setGithubTeamId(githubTeamId);
          teamRepository.save(team);
          ctx.log("Updated team '" + team.getName() + "' with GitHub team ID: " + githubTeamId);
        } else {
          ctx.log(
              "Team '" + team.getName() + "' already has correct GitHub team ID: " + githubTeamId);
        }
      } catch (Exception e) {
        ctx.log("ERROR: Failed to create/get team '" + team.getName() + "': " + e.getMessage());
      }
    }

    // Second pass: Process team members
    for (Team team : teams) {
      if (team.getGithubTeamId() == null) {
        ctx.log("Skipping team members for '" + team.getName() + "' - no GitHub team ID");
        continue;
      }

      ctx.log("Processing members for team: " + team.getName());
      for (TeamMember teamMember : team.getTeamMembers()) {
        RosterStudent student = teamMember.getRosterStudent();
        if (student.getGithubLogin() == null) {
          // Update status to NO_GITHUB_ID
          teamMember.setTeamStatus(TeamStatus.NO_GITHUB_ID);
          teamMemberRepository.save(teamMember);
          ctx.log(
              "Student " + student.getEmail() + " has no GitHub login - marked as NO_GITHUB_ID");
          continue;
        }

        try {
          // Check current status
          TeamStatus currentStatus =
              githubTeamService.getTeamMembershipStatus(
                  student.getGithubLogin(), team.getGithubTeamId(), course);

          if (currentStatus == TeamStatus.TEAM_MEMBER
              || currentStatus == TeamStatus.TEAM_MAINTAINER) {
            // Already a member, just update the status
            teamMember.setTeamStatus(currentStatus);
            teamMemberRepository.save(teamMember);
            ctx.log(
                "Student " + student.getGithubLogin() + " already has status: " + currentStatus);
          } else {
            // Add as member
            TeamStatus newStatus =
                githubTeamService.addTeamMember(
                    student.getGithubLogin(), team.getGithubTeamId(), "member", course);
            teamMember.setTeamStatus(newStatus);
            teamMemberRepository.save(teamMember);
            ctx.log(
                "Added student " + student.getGithubLogin() + " to team with status: " + newStatus);
          }
        } catch (Exception e) {
          ctx.log(
              "ERROR: Failed to process team member "
                  + student.getGithubLogin()
                  + " for team '"
                  + team.getName()
                  + "': "
                  + e.getMessage());
          teamMember.setTeamStatus(TeamStatus.NOT_ORG_MEMBER);
          teamMemberRepository.save(teamMember);
        }
      }
    }

    ctx.log("Completed push teams to GitHub job for course ID: " + courseId);
  }
}
