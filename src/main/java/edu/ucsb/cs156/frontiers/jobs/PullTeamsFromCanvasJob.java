package edu.ucsb.cs156.frontiers.jobs;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.Team;
import edu.ucsb.cs156.frontiers.entities.TeamMember;
import edu.ucsb.cs156.frontiers.errors.DuplicateGroupException;
import edu.ucsb.cs156.frontiers.models.CanvasGroup;
import edu.ucsb.cs156.frontiers.repositories.TeamRepository;
import edu.ucsb.cs156.frontiers.services.CanvasService;
import edu.ucsb.cs156.frontiers.services.jobs.JobContext;
import edu.ucsb.cs156.frontiers.services.jobs.JobContextConsumer;
import edu.ucsb.cs156.frontiers.utilities.CanonicalFormConverter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import lombok.Builder;
import org.springframework.validation.annotation.Validated;

@Builder
@Validated
public class PullTeamsFromCanvasJob implements JobContextConsumer {

  Course course;
  String groupsetId;
  CanvasService canvasService;
  TeamRepository teamRepository;

  @Override
  public void accept(JobContext ctx) throws Exception {
    ctx.log("Processing...");
    List<CanvasGroup> groups = canvasService.getCanvasGroups(course, groupsetId);
    HashMap<String, RosterStudent> mappedStudents = new HashMap<>();
    HashMap<String, Team> teamsByName = new HashMap<>();
    HashMap<Integer, Team> teamsByCanvasId = new HashMap<>();
    course.getRosterStudents().forEach(student -> mappedStudents.put(student.getEmail(), student));
    course
        .getTeams()
        .forEach(
            team -> {
              teamsByName.put(team.getName(), team);
              if (team.getCanvasId() != null) {
                teamsByCanvasId.put(team.getCanvasId(), team);
              }
            });

    List<Team> createdTeams = new ArrayList<>();

    for (CanvasGroup group : groups) {
      Team linked;
      if (teamsByCanvasId.containsKey(group.getId())) {
        linked = teamsByCanvasId.get(group.getId());
      } else if (teamsByName.containsKey(group.getName().trim())) {
        linked = teamsByName.get(group.getName().trim());
      } else {
        linked =
            Team.builder()
                .name(group.getName())
                .teamMembers(new ArrayList<>())
                .course(course)
                .build();
      }
      if (linked.getCanvasId() != null && !linked.getCanvasId().equals(group.getId())) {
        ctx.log("Duplicate group found: " + group.getName() + " with canvasId: " + group.getId());
        throw new DuplicateGroupException();
      }

      linked.setCanvasId(group.getId());
      ctx.log("Processing group: " + group.getName() + " with canvasId: " + group.getId());
      Team finalLinked = linked;
      group
          .getMembers()
          .forEach(
              email -> {
                RosterStudent student =
                    mappedStudents.get(CanonicalFormConverter.convertToValidEmail(email));
                if (student != null) {
                  if (student.getTeamMembers().stream()
                      .anyMatch(teamMember -> teamMember.getTeam().equals(finalLinked))) {
                    return;
                  } else {
                    finalLinked
                        .getTeamMembers()
                        .add(TeamMember.builder().team(finalLinked).rosterStudent(student).build());
                  }
                }
              });
      createdTeams.add(finalLinked);
    }
    teamRepository.saveAll(createdTeams);
  }
}
