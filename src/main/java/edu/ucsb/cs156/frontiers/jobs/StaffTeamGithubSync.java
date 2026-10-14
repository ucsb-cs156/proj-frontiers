package edu.ucsb.cs156.frontiers.jobs;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.CourseStaff;
import edu.ucsb.cs156.frontiers.entities.Team;
import edu.ucsb.cs156.frontiers.enums.TeamStatus;
import edu.ucsb.cs156.frontiers.services.GithubTeamService;
import edu.ucsb.cs156.jobs.services.JobContext;
import java.util.List;

/**
 * Puts the course staff on the GitHub team of a staff team (see {@link Team#getIsStaffTeam()}).
 * Shared by the jobs that push teams to GitHub, so that a staff team is handled the same way
 * whether it was just created or is being pushed with all of the other teams.
 */
public final class StaffTeamGithubSync {

  private StaffTeamGithubSync() {}

  /**
   * Adds every member of the course staff who has a GitHub login to the team's GitHub team, as a
   * member, unless they are already on it. A staff member without a GitHub login, and a staff
   * member GitHub refuses, are logged and skipped; nothing is stored in Frontiers, since staff
   * members have no team member records.
   *
   * @param ctx the job context, for logging
   * @param course the course; its staff are the members of the team
   * @param team the staff team, which must already have a GitHub team id
   * @param orgId the GitHub id of the course's organization
   * @param githubTeamService the service that talks to GitHub
   */
  public static void addStaffMembers(
      JobContext ctx,
      Course course,
      Team team,
      Integer orgId,
      GithubTeamService githubTeamService) {
    List<CourseStaff> staff = course.getCourseStaff() == null ? List.of() : course.getCourseStaff();
    ctx.log(
        "Processing staff members for staff team: "
            + team.getName()
            + " ("
            + staff.size()
            + " staff member(s))");
    for (CourseStaff member : staff) {
      String login = member.getGithubLogin();
      if (login == null) {
        ctx.log("Staff member " + member.getEmail() + " has no GitHub login - skipped");
        continue;
      }
      try {
        TeamStatus currentStatus =
            githubTeamService.getTeamMembershipStatus(login, team.getGithubTeamId(), course, orgId);
        if (currentStatus == TeamStatus.TEAM_MEMBER
            || currentStatus == TeamStatus.TEAM_MAINTAINER) {
          ctx.log("Staff member " + login + " already has status: " + currentStatus);
        } else {
          TeamStatus newStatus =
              githubTeamService.addMemberToGithubTeam(
                  login, team.getGithubTeamId(), "member", course, orgId);
          ctx.log("Added staff member " + login + " to team with status: " + newStatus);
        }
      } catch (Exception e) {
        ctx.log(
            "ERROR: Failed to process staff member "
                + login
                + " for team '"
                + team.getName()
                + "': "
                + e.getMessage());
      }
    }
  }
}
