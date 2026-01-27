package edu.ucsb.cs156.frontiers.services;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.Team;
import edu.ucsb.cs156.frontiers.entities.TeamMember;
import edu.ucsb.cs156.frontiers.enums.TeamStatus;
import edu.ucsb.cs156.frontiers.models.CanvasStudent;
import edu.ucsb.cs156.frontiers.repositories.TeamRepository;
import edu.ucsb.cs156.frontiers.utilities.CanonicalFormConverter;
import edu.ucsb.cs156.frontiers.validators.HasLinkedCanvasCourse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.springframework.graphql.client.HttpSyncGraphQlClient;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.RestClient;

@Service
@Validated
public class CanvasService {

  private final TeamRepository teamRepository;
  private HttpSyncGraphQlClient graphQlClient;
  private ObjectMapper mapper;

  private static final String CANVAS_GRAPHQL_URL = "https://ucsb.instructure.com/api/graphql";

  public CanvasService(
      ObjectMapper mapper, RestClient.Builder builder, TeamRepository teamRepository) {
    this.graphQlClient =
        HttpSyncGraphQlClient.builder(builder.baseUrl(CANVAS_GRAPHQL_URL).build()).build();
    this.mapper = mapper;
    this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    this.teamRepository = teamRepository;
  }

  /**
   * Fetches the roster of students from Canvas for the given course.
   *
   * @param course the Course entity containing canvasApiToken and canvasCourseId
   * @return list of RosterStudent objects from Canvas
   */
  public List<RosterStudent> getCanvasRoster(@HasLinkedCanvasCourse Course course) {
    String query =
        """
              query GetRoster($courseId: ID!) {
              course(id: $courseId) {
                usersConnection(filter: {enrollmentTypes: StudentEnrollment}) {
                  edges {
                    node {
                      firstName
                      lastName
                      sisId
                      email
                      integrationId
                    }
                  }
                }
              }
            }
            """;

    HttpSyncGraphQlClient authedClient =
        graphQlClient
            .mutate()
            .header("Authorization", "Bearer " + course.getCanvasApiToken())
            .build();

    List<CanvasStudent> students =
        authedClient
            .document(query)
            .variable("courseId", course.getCanvasCourseId())
            .retrieveSync("course.usersConnection.edges")
            .toEntityList(JsonNode.class)
            .stream()
            .map(node -> mapper.convertValue(node.get("node"), CanvasStudent.class))
            .toList();

    return students.stream()
        .map(
            student ->
                RosterStudent.builder()
                    .firstName(student.getFirstName())
                    .lastName(student.getLastName())
                    .studentId(student.getStudentId())
                    .email(student.getEmail())
                    .build())
        .toList();
  }

  public List<Team> getCanvasTeams(@HasLinkedCanvasCourse Course course) {
    String query =
        """
        query GetTeams($courseId: ID!) {
          course(id: $courseId) {
            groupSets {
              groups {
                name
                _id
                membersConnection {
                  edges {
                    node {
                      user {
                        email
                      }
                    }
                  }
                }
              }
            }
          }
        }
        """;

    HttpSyncGraphQlClient authedClient =
        graphQlClient
            .mutate()
            .header("Authorization", "Bearer " + course.getCanvasApiToken())
            .build();

    List<JsonNode> groups =
        authedClient
            .document(query)
            .variable("courseId", course.getCanvasCourseId())
            .retrieveSync("course.groupSets[0].groups")
            .toEntityList(JsonNode.class);

    HashMap<String, RosterStudent> mappedStudents = new HashMap<>();
    HashMap<String, Team> mappedTeams = new HashMap<>();
    course.getRosterStudents().forEach(student -> mappedStudents.put(student.getEmail(), student));
    course.getTeams().forEach(team -> mappedTeams.put(team.getName(), team));

    List<Team> createdTeams = new ArrayList<>();

    for (JsonNode group : groups) {
      Team linked =
          mappedTeams.getOrDefault(
              group.get("name").asText().trim(),
              Team.builder()
                  .name(group.get("name").asText())
                  .teamMembers(new ArrayList<>())
                  .course(course)
                  .build());
      linked.setCanvasId(group.get("_id").asInt());
      group
          .path("membersConnection")
          .get("edges")
          .forEach(
              edge -> {
                RosterStudent student =
                    mappedStudents.get(
                        CanonicalFormConverter.convertToValidEmail(
                            edge.path("node").path("user").get("email").asText()));
                if (student != null) {
                  if (student.getTeamMembers().stream()
                      .anyMatch(teamMember -> teamMember.getTeam().equals(linked))) {
                    return;
                  } else {
                    linked
                        .getTeamMembers()
                        .add(
                            TeamMember.builder()
                                .teamStatus(TeamStatus.NO_GITHUB_ID)
                                .team(linked)
                                .rosterStudent(student)
                                .build());
                  }
                }
              });
      createdTeams.add(linked);
    }
    teamRepository.saveAll(createdTeams);

    return createdTeams;
  }
}
