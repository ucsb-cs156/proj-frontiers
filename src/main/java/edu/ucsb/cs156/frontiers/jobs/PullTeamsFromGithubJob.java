package edu.ucsb.cs156.frontiers.jobs;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.Team;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.TeamRepository;
import edu.ucsb.cs156.frontiers.services.GithubTeamService;
import edu.ucsb.cs156.frontiers.services.GithubTeamService.GithubTeamInfo;
import edu.ucsb.cs156.frontiers.services.jobs.JobContext;
import edu.ucsb.cs156.frontiers.services.jobs.JobContextConsumer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Builder;

@Builder
public class PullTeamsFromGithubJob implements JobContextConsumer {
  Long courseId;
  CourseRepository courseRepository;
  TeamRepository teamRepository;
  GithubTeamService githubTeamService;

  @Override
  public void accept(JobContext ctx) throws Exception {
    ctx.log("Starting pull teams from GitHub job for course ID: " + courseId);

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

    List<GithubTeamInfo> githubTeams;
    try {
      githubTeams = githubTeamService.getAllTeams(course);
    } catch (Exception e) {
      ctx.log("ERROR: Failed to pull teams from GitHub: " + e.getMessage());
      return;
    }

    Map<Integer, Team> localByGithubId = new HashMap<>();
    Map<String, Team> localByName = new HashMap<>();
    for (Team localTeam : teamRepository.findByCourseId(courseId)) {
      if (localTeam.getGithubTeamId() != null) {
        localByGithubId.put(localTeam.getGithubTeamId(), localTeam);
      }
      localByName.put(localTeam.getName(), localTeam);
    }

    int created = 0;
    int updated = 0;
    int unchanged = 0;

    for (GithubTeamInfo githubTeam : githubTeams) {
      Team localTeam = localByGithubId.get(githubTeam.id());
      if (localTeam == null) {
        localTeam = localByName.get(githubTeam.name());
      }

      if (localTeam == null) {
        Team newTeam =
            Team.builder()
                .name(githubTeam.name())
                .course(course)
                .githubTeamId(githubTeam.id())
                .build();
        teamRepository.save(newTeam);
        localByGithubId.put(githubTeam.id(), newTeam);
        localByName.put(githubTeam.name(), newTeam);
        created++;
        ctx.log(
            "Created local team '"
                + githubTeam.name()
                + "' with GitHub team ID: "
                + githubTeam.id());
        continue;
      }

      boolean changed = false;
      if (!githubTeam.name().equals(localTeam.getName())) {
        localByName.remove(localTeam.getName());
        localTeam.setName(githubTeam.name());
        changed = true;
      }
      if (!githubTeam.id().equals(localTeam.getGithubTeamId())) {
        if (localTeam.getGithubTeamId() != null) {
          localByGithubId.remove(localTeam.getGithubTeamId());
        }
        localTeam.setGithubTeamId(githubTeam.id());
        changed = true;
      }

      if (changed) {
        teamRepository.save(localTeam);
        updated++;
        ctx.log(
            "Updated local team '"
                + localTeam.getName()
                + "' with GitHub team ID: "
                + githubTeam.id());
      } else {
        unchanged++;
      }

      localByGithubId.put(githubTeam.id(), localTeam);
      localByName.put(localTeam.getName(), localTeam);
    }

    ctx.log(
        "Completed pull teams from GitHub job for course ID: "
            + courseId
            + " (GitHub teams: "
            + githubTeams.size()
            + ", created: "
            + created
            + ", updated: "
            + updated
            + ", unchanged: "
            + unchanged
            + ")");
  }
}
