package edu.ucsb.cs156.frontiers.jobs;

import com.fasterxml.jackson.databind.JsonNode;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.Team;
import edu.ucsb.cs156.frontiers.entities.TeamMember;
import edu.ucsb.cs156.frontiers.enums.TeamStatus;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.TeamRepository;
import edu.ucsb.cs156.frontiers.services.CanvasService;
import edu.ucsb.cs156.frontiers.services.jobs.JobContext;
import edu.ucsb.cs156.frontiers.services.jobs.JobContextConsumer;
import edu.ucsb.cs156.frontiers.utilities.CanonicalFormConverter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import lombok.Builder;

@Builder
public class ImportCanvasTeamsJob implements JobContextConsumer {
  Long courseId;
  CourseRepository courseRepository;
  TeamRepository teamRepository;
  CanvasService canvasService;

  @Override
  public void accept(JobContext ctx) throws Exception {
    ctx.log("Starting import teams from Canvas job for course ID: " + courseId);

    // Get the course
    Optional<Course> courseOpt = courseRepository.findById(courseId);
    if (courseOpt.isEmpty()) {
      ctx.log("ERROR: Course with ID " + courseId + " not found");
      return;
    }
    Course course = courseOpt.get();
    ctx.log("Processing course: " + course.getCourseName());

    // Check if Canvas is linked
    if (course.getCanvasCourseId() == null || course.getCanvasApiToken() == null) {
      ctx.log("ERROR: Course has no linked Canvas course. Please configure Canvas API settings.");
      return;
    }

    // Fetch teams from Canvas
    ctx.log("Fetching teams from Canvas...");
    List<JsonNode> groups;
    try {
      groups = canvasService.fetchCanvasTeamGroups(course);
      ctx.log("Found " + groups.size() + " groups in Canvas");
    } catch (Exception e) {
      ctx.log("ERROR: Failed to fetch teams from Canvas: " + e.getMessage());
      return;
    }

    // Build lookup maps for roster students and existing teams
    HashMap<String, RosterStudent> mappedStudents = new HashMap<>();
    HashMap<String, Team> mappedTeams = new HashMap<>();
    course.getRosterStudents().forEach(student -> mappedStudents.put(student.getEmail(), student));
    course.getTeams().forEach(team -> mappedTeams.put(team.getName(), team));
    ctx.log("Found " + mappedStudents.size() + " roster students");
    ctx.log("Found " + mappedTeams.size() + " existing teams");

    List<Team> createdTeams = new ArrayList<>();
    int teamsCreated = 0;
    int teamsUpdated = 0;
    int membersAdded = 0;
    int membersSkipped = 0;

    for (JsonNode group : groups) {
      String teamName = group.get("name").asText().trim();
      Integer canvasId = group.get("_id").asInt();
      ctx.log("Processing group: " + teamName + " (Canvas ID: " + canvasId + ")");

      // Find or create the team
      Team team = mappedTeams.get(teamName);
      boolean isNewTeam = team == null;

      if (isNewTeam) {
        team = Team.builder().name(teamName).teamMembers(new ArrayList<>()).course(course).build();
        teamsCreated++;
        ctx.log("  Creating new team: " + teamName);
      } else {
        teamsUpdated++;
        ctx.log("  Updating existing team: " + teamName);
      }

      team.setCanvasId(canvasId);

      // Process team members
      JsonNode membersEdges = group.path("membersConnection").get("edges");
      if (membersEdges != null) {
        for (JsonNode edge : membersEdges) {
          String memberEmail = edge.path("node").path("user").get("email").asText();
          String canonicalEmail = CanonicalFormConverter.convertToValidEmail(memberEmail);

          RosterStudent student = mappedStudents.get(canonicalEmail);
          if (student == null) {
            ctx.log("  Skipping member " + canonicalEmail + " - not found in roster");
            membersSkipped++;
            continue;
          }

          // Check if student is already a member of this team
          final Team finalTeam = team;
          boolean alreadyMember =
              student.getTeamMembers() != null
                  && student.getTeamMembers().stream()
                      .anyMatch(tm -> tm.getTeam() != null && tm.getTeam().equals(finalTeam));

          if (alreadyMember) {
            ctx.log("  Skipping member " + canonicalEmail + " - already in team");
            membersSkipped++;
          } else {
            TeamMember newMember =
                TeamMember.builder()
                    .teamStatus(TeamStatus.NO_GITHUB_ID)
                    .team(team)
                    .rosterStudent(student)
                    .build();
            team.getTeamMembers().add(newMember);
            membersAdded++;
            ctx.log("  Added member: " + canonicalEmail);
          }
        }
      }

      createdTeams.add(team);
    }

    // Save all teams
    ctx.log("Saving " + createdTeams.size() + " teams...");
    teamRepository.saveAll(createdTeams);

    ctx.log("Import complete!");
    ctx.log("Summary:");
    ctx.log("  Teams created: " + teamsCreated);
    ctx.log("  Teams updated: " + teamsUpdated);
    ctx.log("  Members added: " + membersAdded);
    ctx.log("  Members skipped: " + membersSkipped);
  }
}
