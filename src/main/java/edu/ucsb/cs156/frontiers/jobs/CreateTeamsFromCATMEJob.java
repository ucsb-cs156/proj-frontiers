package edu.ucsb.cs156.frontiers.jobs;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.Team;
import edu.ucsb.cs156.frontiers.entities.TeamMember;
import edu.ucsb.cs156.frontiers.enums.RosterStatus;
import edu.ucsb.cs156.frontiers.models.CATMETeamAssignment;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.repositories.TeamMemberRepository;
import edu.ucsb.cs156.frontiers.repositories.TeamRepository;
import edu.ucsb.cs156.jobs.services.JobContext;
import edu.ucsb.cs156.jobs.services.JobContextConsumer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;

/**
 * Creates teams in Frontiers from the team assignments of a CATME TeamMaker CSV export, and adds
 * each student to their team. Teams that already exist are reused; students already on their team
 * are left alone. Nothing is pushed to GitHub: that is a separate step (Push Teams to GitHub, on
 * the Teams tab).
 *
 * <p>The assignments are expected to have passed the CATME audit first, so every student in them
 * should be an enrolled student of the course; any that is not is logged and skipped.
 */
@Builder
public class CreateTeamsFromCATMEJob implements JobContextConsumer {

  Course course;
  List<CATMETeamAssignment> assignments;
  CourseRepository courseRepository;
  RosterStudentRepository rosterStudentRepository;
  TeamRepository teamRepository;
  TeamMemberRepository teamMemberRepository;

  /**
   * @return the team assignments this job was given
   */
  public List<CATMETeamAssignment> getAssignments() {
    return assignments;
  }

  @Override
  public String getScopeType() {
    return "course";
  }

  @Override
  public Long getScopeId() {
    return course.getId();
  }

  @Override
  public void accept(JobContext ctx) throws Exception {
    Course currentCourse = courseRepository.findById(course.getId()).orElseThrow();
    ctx.log("Creating teams from CATME file (teams are NOT pushed to GitHub by this job)");

    Map<String, RosterStudent> studentsById = new HashMap<>();
    for (RosterStudent student : rosterStudentRepository.findByCourseId(currentCourse.getId())) {
      if (student.getStudentId() != null
          && (student.getRosterStatus() == RosterStatus.ROSTER
              || student.getRosterStatus() == RosterStatus.MANUAL)) {
        studentsById.put(student.getStudentId(), student);
      }
    }

    // Group the assignments by team, keeping the order of the file
    Map<String, List<CATMETeamAssignment>> assignmentsByTeam = new LinkedHashMap<>();
    int withoutTeam = 0;
    for (CATMETeamAssignment assignment : assignments) {
      String teamName = assignment.teamName() == null ? "" : assignment.teamName().strip();
      if (teamName.isEmpty()) {
        withoutTeam++;
        continue;
      }
      assignmentsByTeam.computeIfAbsent(teamName, name -> new ArrayList<>()).add(assignment);
    }
    if (withoutTeam > 0) {
      ctx.log("%d student(s) in the file have no team name; skipped".formatted(withoutTeam));
    }

    int created = 0;
    int added = 0;
    for (Map.Entry<String, List<CATMETeamAssignment>> entry : assignmentsByTeam.entrySet()) {
      String teamName = entry.getKey();
      Team team =
          teamRepository.findByCourseIdAndName(currentCourse.getId(), teamName).orElse(null);
      if (team == null) {
        team =
            teamRepository.save(
                Team.builder()
                    .name(teamName)
                    .course(currentCourse)
                    .teamMembers(new ArrayList<>())
                    .build());
        created++;
        ctx.log("Created team %s".formatted(teamName));
      } else {
        ctx.log("Team %s already exists".formatted(teamName));
      }

      for (CATMETeamAssignment assignment : entry.getValue()) {
        RosterStudent student = studentsById.get(assignment.studentId());
        if (student == null) {
          ctx.log(
              "Student %s (%s) is not an enrolled student of this course; skipped"
                  .formatted(assignment.name(), assignment.studentId()));
          continue;
        }
        if (teamMemberRepository.findByTeamAndRosterStudent(team, student).isPresent()) {
          ctx.log(
              "%s (%s) is already on team %s"
                  .formatted(assignment.name(), assignment.studentId(), teamName));
          continue;
        }
        teamMemberRepository.save(TeamMember.builder().team(team).rosterStudent(student).build());
        added++;
        ctx.log(
            "Added %s (%s) to team %s"
                .formatted(assignment.name(), assignment.studentId(), teamName));
      }
    }
    ctx.log(
        "Done: %d team(s) created, %d student(s) added to teams. Teams have NOT been pushed to GitHub; use Push Teams to GitHub on the Teams tab when ready."
            .formatted(created, added));
  }
}
