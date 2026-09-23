package edu.ucsb.cs156.frontiers.jobs;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.Team;
import edu.ucsb.cs156.frontiers.entities.TeamMember;
import edu.ucsb.cs156.frontiers.models.CanvasGroupDetail;
import edu.ucsb.cs156.frontiers.models.CanvasGroupSetDetail;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.TeamRepository;
import edu.ucsb.cs156.frontiers.services.CanvasService;
import edu.ucsb.cs156.frontiers.utilities.CanonicalFormConverter;
import edu.ucsb.cs156.jobs.services.JobContext;
import edu.ucsb.cs156.jobs.services.JobContextConsumer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Updates the groups in a Canvas group set to match the teams in Frontiers: creates a Canvas group
 * for each team that has none, adds and removes group members so that each group's membership
 * matches the team's, and deletes Canvas groups that do not correspond to any team. A team is
 * matched to a group by the team's stored Canvas id first, then by name.
 *
 * <p>Problems with a single team, group or student (a student who is not enrolled in the Canvas
 * course, a Canvas API call that fails) are logged as warnings and the job proceeds; only a failure
 * to read the group set or the Canvas roster fails the job.
 */
@Builder
@Getter
@Slf4j
public class PushTeamsToCanvasJob implements JobContextConsumer {

  Course course;
  String groupSetId;
  CanvasService canvasService;
  CourseRepository courseRepository;
  TeamRepository teamRepository;

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
    course = courseRepository.findById(course.getId()).get();
    CanvasGroupSetDetail groupSet = canvasService.getCanvasGroupSetDetail(course, groupSetId);
    Map<String, Integer> canvasUserIds = canvasService.getCanvasUserIdsByEmail(course);
    ctx.log(
        String.format(
            "Pushing %d teams to Canvas group set %s (id %d), which has %d groups",
            course.getTeams().size(),
            groupSet.getName(),
            groupSet.getId(),
            groupSet.getGroups().size()));

    Map<Integer, CanvasGroupDetail> groupsByCanvasId = new HashMap<>();
    Map<String, CanvasGroupDetail> groupsByName = new HashMap<>();
    for (CanvasGroupDetail group : groupSet.getGroups()) {
      groupsByCanvasId.put(group.getId(), group);
      groupsByName.put(group.getName().trim(), group);
    }

    List<Team> teams = new ArrayList<>(course.getTeams());
    teams.sort(Comparator.comparing(Team::getName));
    Set<Integer> matchedGroupIds = new HashSet<>();

    for (Team team : teams) {
      CanvasGroupDetail group = groupsByCanvasId.get(team.getCanvasId());
      if (group == null) {
        group = groupsByName.get(team.getName().trim());
      }
      if (group != null) {
        ctx.log(
            String.format(
                "Team %s matches Canvas group %s (id %d)",
                team.getName(), group.getName(), group.getId()));
      } else {
        try {
          Integer newId = canvasService.createCanvasGroup(course, groupSet.getId(), team.getName());
          group =
              CanvasGroupDetail.builder()
                  .id(newId)
                  .name(team.getName())
                  .memberUserIdsByEmail(new LinkedHashMap<>())
                  .build();
          ctx.log(String.format("Created Canvas group %s (id %d)", team.getName(), newId));
        } catch (Exception e) {
          warn(ctx, "Could not create Canvas group " + team.getName() + ": " + e.getMessage());
          continue;
        }
      }
      matchedGroupIds.add(group.getId());
      if (!Objects.equals(team.getCanvasId(), group.getId())) {
        team.setCanvasId(group.getId());
        teamRepository.save(team);
      }
      syncMembers(ctx, team, group, canvasUserIds);
    }

    for (CanvasGroupDetail group : groupSet.getGroups()) {
      if (!matchedGroupIds.contains(group.getId())) {
        try {
          canvasService.deleteCanvasGroup(course, group.getId());
          ctx.log(
              String.format(
                  "Deleted Canvas group %s (id %d), which has no team in Frontiers",
                  group.getName(), group.getId()));
        } catch (Exception e) {
          warn(
              ctx,
              String.format(
                  "Could not delete Canvas group %s (id %d): %s",
                  group.getName(), group.getId(), e.getMessage()));
        }
      }
    }
    ctx.log("Done pushing teams to Canvas");
  }

  private void syncMembers(
      JobContext ctx, Team team, CanvasGroupDetail group, Map<String, Integer> canvasUserIds) {
    Map<String, Integer> wanted = new LinkedHashMap<>();
    for (TeamMember member : team.getTeamMembers()) {
      RosterStudent student = member.getRosterStudent();
      String email = CanonicalFormConverter.convertToValidEmail(student.getEmail());
      Integer userId = canvasUserIds.get(email);
      if (userId == null) {
        warn(
            ctx,
            String.format(
                "Student %s on team %s is not enrolled in the Canvas course; skipping",
                email, team.getName()));
      } else {
        wanted.put(email, userId);
      }
    }

    Map<String, Integer> current = group.getMemberUserIdsByEmail();
    for (Map.Entry<String, Integer> entry : wanted.entrySet()) {
      if (!current.containsKey(entry.getKey())) {
        try {
          canvasService.addCanvasGroupMember(course, group.getId(), entry.getValue());
          ctx.log(String.format("Added %s to Canvas group %s", entry.getKey(), group.getName()));
        } catch (Exception e) {
          warn(
              ctx,
              String.format(
                  "Could not add %s to Canvas group %s: %s",
                  entry.getKey(), group.getName(), e.getMessage()));
        }
      }
    }
    for (Map.Entry<String, Integer> entry : current.entrySet()) {
      if (!wanted.containsKey(entry.getKey())) {
        try {
          canvasService.removeCanvasGroupMember(course, group.getId(), entry.getValue());
          ctx.log(
              String.format("Removed %s from Canvas group %s", entry.getKey(), group.getName()));
        } catch (Exception e) {
          warn(
              ctx,
              String.format(
                  "Could not remove %s from Canvas group %s: %s",
                  entry.getKey(), group.getName(), e.getMessage()));
        }
      }
    }
  }

  private static void warn(JobContext ctx, String message) {
    log.warn(message);
    ctx.log("WARNING: " + message);
  }
}
