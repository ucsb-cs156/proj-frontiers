package edu.ucsb.cs156.frontiers.jobs;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.Team;
import edu.ucsb.cs156.frontiers.repositories.TeamRepository;
import edu.ucsb.cs156.frontiers.services.GithubTeamService;
import edu.ucsb.cs156.frontiers.services.jobs.JobContext;
import edu.ucsb.cs156.frontiers.services.jobs.JobContextConsumer;
import lombok.Builder;

@Builder
public class AddTeamToGithubJob implements JobContextConsumer {
  Course course;
  String teamName;
  TeamRepository teamRepository;
  GithubTeamService githubTeamService;

  @Override
  public void accept(JobContext ctx) throws Exception {
    ctx.log("Starting add team to GitHub job for team: " + teamName);

    if (teamName == null) {
      ctx.log("ERROR: Team has no name");
      return;
    }

    if (course.getOrgName() == null || course.getInstallationId() == null) {
      ctx.log("ERROR: Course has no linked GitHub organization");
      return;
    }

    Team team = teamRepository.findByCourseIdAndName(course.getId(), teamName).orElse(null);
    if (team == null) {
      ctx.log(
          "ERROR: Team with name '"
              + teamName
              + "' not found for course "
              + course.getCourseName());
      return;
    }

    try {
      Integer githubTeamId = githubTeamService.createTeam(teamName, course);
      team.setGithubTeamId(githubTeamId);
      teamRepository.save(team);
      ctx.log(
          "Successfully added team '"
              + team.getName()
              + "' to GitHub with GitHub team ID: "
              + githubTeamId);
    } catch (Exception e) {
      ctx.log("ERROR: Failed to add team to GitHub: " + e.getMessage());
    }

    ctx.log("Done");
  }
}
