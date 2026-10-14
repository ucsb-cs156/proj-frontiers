package edu.ucsb.cs156.frontiers.jobs;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.CourseStaff;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.Team;
import edu.ucsb.cs156.frontiers.entities.TeamMember;
import edu.ucsb.cs156.frontiers.enums.TeamStatus;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.TeamMemberRepository;
import edu.ucsb.cs156.frontiers.repositories.TeamRepository;
import edu.ucsb.cs156.frontiers.services.GithubTeamService;
import edu.ucsb.cs156.frontiers.services.GithubTeamService.GithubTeamInfo;
import edu.ucsb.cs156.jobs.services.JobContext;
import edu.ucsb.cs156.jobs.services.JobContextConsumer;
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
  public String getScopeType() {
    return "course";
  }

  @Override
  public Long getScopeId() {
    return courseId;
  }

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
    Map<String, CourseStaff> staffByGithubLogin = new HashMap<>();
    if (course.getCourseStaff() != null) {
      for (CourseStaff staff : course.getCourseStaff()) {
        if (staff.getGithubLogin() != null) {
          staffByGithubLogin.put(staff.getGithubLogin(), staff);
        }
      }
    }

    int created = 0;
    int updated = 0;
    int unchanged = 0;

    for (GithubTeamInfo githubTeam : githubTeams) {
      // The common case on a re-sync is a team that's already up to date on every field and
      // has no membership changes either -- that iteration never calls ctx.log() at all.
      // checkCancellation() gives this loop its own checkpoint independent of how many teams
      // in a row turn out to be unchanged (this is the same failure mode that drove lib-jobs
      // v0.3.2: see SyncCourseWithPlRepoJob in proj-scaffold).
      ctx.checkCancellation();
      Team localTeam = localByGithubId.get(githubTeam.id());
      if (localTeam == null) {
        localTeam = localByName.get(githubTeam.name());
      }

      // The members are needed before the team is created, to tell a staff team (every member
      // is on the course staff) from a regular one. Without any students or staff with GitHub
      // logins, nobody could be matched, so the call is skipped.
      Map<String, TeamStatus> githubMemberships = Map.of();
      if (!localStudentsByGithubLogin.isEmpty() || !staffByGithubLogin.isEmpty()) {
        githubMemberships = githubTeamService.getTeamMemberships(githubTeam.slug(), course);
      }
      boolean staffOnly =
          !githubMemberships.isEmpty()
              && staffByGithubLogin.keySet().containsAll(githubMemberships.keySet());

      boolean teamCreated = false;
      if (localTeam == null) {
        Team newTeam =
            Team.builder()
                .name(githubTeam.name())
                .course(course)
                .githubTeamId(githubTeam.id())
                .githubTeamSlug(githubTeam.slug())
                .isStaffTeam(staffOnly)
                .build();
        teamRepository.save(newTeam);
        localByGithubId.put(githubTeam.id(), newTeam);
        localByName.put(githubTeam.name(), newTeam);
        created++;
        ctx.log(
            String.format(
                "Created local team '%s' with GitHub team ID: %d%s",
                githubTeam.name(),
                githubTeam.id(),
                staffOnly ? " as a staff team (every member is on the course staff)" : ""));
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
        if (localTeam.getGithubTeamSlug() == null
            || !githubTeam.slug().equals(localTeam.getGithubTeamSlug())) {
          localTeam.setGithubTeamSlug(githubTeam.slug());
          teamUnchanged.set(false);
        }
        // A regular team with no members whose GitHub team has only staff on it becomes a staff
        // team. One that already has student members is left alone, since those members would
        // otherwise be hidden; the staff on its GitHub team are noted below instead.
        if (staffOnly
            && !Boolean.TRUE.equals(localTeam.getIsStaffTeam())
            && (localTeam.getTeamMembers() == null || localTeam.getTeamMembers().isEmpty())) {
          localTeam.setIsStaffTeam(true);
          teamUnchanged.set(false);
          ctx.log(
              String.format(
                  "Local team '%s' is now a staff team (every member of its GitHub team is on the course staff)",
                  localTeam.getName()));
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

      Team currentTeam = localTeam;
      boolean isStaffTeam = Boolean.TRUE.equals(currentTeam.getIsStaffTeam());
      githubMemberships.forEach(
          (githubLogin, membershipStatus) -> {
            // Same reasoning as the outer team loop: a member with no status change never
            // logs. checkCancellation() throws JobCancelledException, which is unchecked --
            // required here since BiConsumer's functional interface can't declare checked
            // exceptions.
            ctx.checkCancellation();
            if (isStaffTeam) {
              // The members of a staff team are the course staff; nothing is stored per member.
              if (!staffByGithubLogin.containsKey(githubLogin)) {
                ctx.log(
                    String.format(
                        "Note: '%s' is on the GitHub team of staff team '%s' but is not on the course staff",
                        githubLogin, currentTeam.getName()));
              }
              return;
            }
            RosterStudent student = localStudentsByGithubLogin.get(githubLogin);
            if (student == null) {
              // Only students become members of a regular team; anyone else is noted here.
              ctx.log(
                  String.format(
                      "Note: '%s' is on GitHub team '%s' but is %s, so was not added to the local team",
                      githubLogin,
                      currentTeam.getName(),
                      staffByGithubLogin.containsKey(githubLogin)
                          ? "on the course staff, not a student"
                          : "not a student of this course"));
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
