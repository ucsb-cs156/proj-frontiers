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
import edu.ucsb.cs156.frontiers.services.GithubTeamService.GithubTeamInfo;
import edu.ucsb.cs156.frontiers.services.jobs.JobContext;
import edu.ucsb.cs156.frontiers.services.jobs.JobContextConsumer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Builder;

@Builder
public class PullTeamsFromGithubJob implements JobContextConsumer {
  Long courseId;
  CourseRepository courseRepository;
  TeamRepository teamRepository;
  TeamMemberRepository teamMemberRepository;
  GithubTeamService githubTeamService;

  @Override
  public void accept(JobContext ctx) throws Exception {
    ctx.log(String.format("Starting pull teams from GitHub job for course ID: %s", courseId));

    Optional<Course> courseOpt = courseRepository.findById(courseId);
    if (courseOpt.isEmpty()) {
      ctx.log(String.format("ERROR: Course with ID %s not found", courseId));
      return;
    }
    Course course = courseOpt.get();
    ctx.log(
        String.format(
            "Processing course: %s (org: %s)", course.getCourseName(), course.getOrgName()));

    if (course.getOrgName() == null || course.getInstallationId() == null) {
      ctx.log("ERROR: Course has no linked GitHub organization");
      return;
    }

    List<GithubTeamInfo> githubTeams;
    try {
      githubTeams = githubTeamService.getAllTeams(course);
    } catch (Exception e) {
      ctx.log(String.format("ERROR: Failed to pull teams from GitHub: %s", e.getMessage()));
      return;
    }

    Map<Integer, Team> localByGithubId = new HashMap<>();
    Map<String, Team> localByName = new HashMap<>();
    Map<String, RosterStudent> localStudentsByGithubLogin = new HashMap<>();
    for (Team localTeam : teamRepository.findByCourseId(courseId)) {
      if (localTeam.getGithubTeamId() != null) {
        localByGithubId.put(localTeam.getGithubTeamId(), localTeam);
      }
      localByName.put(localTeam.getName(), localTeam);
    }
    if (course.getRosterStudents() != null) {
      for (RosterStudent student : course.getRosterStudents()) {
        if (student.getGithubLogin() != null) {
          localStudentsByGithubLogin.put(student.getGithubLogin(), student);
        }
      }
    }

    int created = 0;
    int updated = 0;
    int unchanged = 0;

    for (GithubTeamInfo githubTeam : githubTeams) {
      Team localTeam = localByGithubId.get(githubTeam.id());
      if (localTeam == null) {
        localTeam = localByName.get(githubTeam.name());
      }

      boolean teamCreated = false;
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
            String.format(
                "Created local team '%s' with GitHub team ID: %d",
                githubTeam.name(), githubTeam.id()));
        localTeam = newTeam;
        teamCreated = true;
      }

      AtomicBoolean teamUnchanged = new AtomicBoolean(true);
      if (!teamCreated) {
        if (!githubTeam.name().equals(localTeam.getName())) {
          localByName.remove(localTeam.getName());
          localTeam.setName(githubTeam.name());
          teamUnchanged.set(false);
        }
        if (!githubTeam.id().equals(localTeam.getGithubTeamId())) {
          if (localTeam.getGithubTeamId() != null) {
            localByGithubId.remove(localTeam.getGithubTeamId());
          }
          localTeam.setGithubTeamId(githubTeam.id());
          teamUnchanged.set(false);
        }

        if (!teamUnchanged.get()) {
          teamRepository.save(localTeam);
          ctx.log(
              String.format(
                  "Updated local team '%s' with GitHub team ID: %d",
                  localTeam.getName(), githubTeam.id()));
        }
      }

      localByGithubId.put(githubTeam.id(), localTeam);
      localByName.put(localTeam.getName(), localTeam);

      if (!localStudentsByGithubLogin.isEmpty()) {
        Map<String, TeamStatus> githubMemberships =
            githubTeamService.getTeamMemberships(githubTeam.slug(), course);
        Team currentTeam = localTeam;
        githubMemberships.forEach(
            (githubLogin, membershipStatus) -> {
              RosterStudent student = localStudentsByGithubLogin.get(githubLogin);
              if (student == null) {
                return;
              }

              Optional<TeamMember> existingTeamMember =
                  teamMemberRepository.findByTeamAndRosterStudent(currentTeam, student);
              if (existingTeamMember.isPresent()) {
                TeamMember teamMember = existingTeamMember.get();
                if (teamMember.getTeamStatus() != membershipStatus) {
                  teamMember.setTeamStatus(membershipStatus);
                  teamMemberRepository.save(teamMember);
                  teamUnchanged.set(false);
                  ctx.log(
                      String.format(
                          "Updated team member '%s' in team '%s' with status %s",
                          githubLogin, currentTeam.getName(), membershipStatus));
                }
              } else {
                TeamMember newTeamMember =
                    TeamMember.builder()
                        .team(currentTeam)
                        .rosterStudent(student)
                        .teamStatus(membershipStatus)
                        .build();
                teamMemberRepository.save(newTeamMember);
                teamUnchanged.set(false);
                ctx.log(
                    String.format(
                        "Created team member '%s' in team '%s' with status %s",
                        githubLogin, currentTeam.getName(), membershipStatus));
              }
            });
      }

      if (!teamCreated) {
        if (teamUnchanged.get()) {
          unchanged++;
        } else {
          updated++;
        }
      }
    }

    ctx.log(
        String.format(
            "Completed pull teams from GitHub job for course ID: %s (GitHub teams: %d, created: %d, updated: %d, unchanged: %d)",
            courseId, githubTeams.size(), created, updated, unchanged));
  }
}
