package edu.ucsb.cs156.frontiers.services;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.models.CanvasGroup;
import edu.ucsb.cs156.frontiers.models.CanvasGroupSet;
import edu.ucsb.cs156.frontiers.models.CanvasStudent;
import edu.ucsb.cs156.frontiers.utilities.CanonicalFormConverter;
import edu.ucsb.cs156.frontiers.validators.HasLinkedCanvasCourse;
import java.util.ArrayList;
import java.util.List;
import org.springframework.graphql.client.HttpSyncGraphQlClient;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.RestClient;

/**
 * Service for interacting with the Canvas API.
 *
 * <p>Note that the Canvas API uses a GraphQL endpoint, which allows for more flexible queries
 * compared to traditional REST APIs.
 *
 * <p>For more information on the Canvas API, visit the official documentation at
 * https://canvas.instructure.com/doc/api/.
 *
 * <p>You can typically interact with Canvas API GraphQL endpoints interactively by appending
 * /graphiql to the URL of the Canvas instance.
 *
 * <p>For example, for UCSB Canvas, use: https://ucsb.instructure.com/graphiql
 */
@Service
@Validated
public class CanvasService {

  private HttpSyncGraphQlClient graphQlClient;
  private ObjectMapper mapper;

  private static final String CANVAS_GRAPHQL_URL = "https://ucsb.instructure.com/api/graphql";

  public CanvasService(ObjectMapper mapper, RestClient.Builder builder) {
    this.graphQlClient =
        HttpSyncGraphQlClient.builder(builder.baseUrl(CANVAS_GRAPHQL_URL).build()).build();
    this.mapper = mapper;
    this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  public List<CanvasGroupSet> getCanvasGroupSets(@HasLinkedCanvasCourse Course course) {
    String query =
        """
        query GetGroupSets($courseId: ID!) {
          course(id: $courseId) {
            groupSets {
              _id
              name
              id
            }
          }
        }
        """;

    HttpSyncGraphQlClient authedClient =
        graphQlClient
            .mutate()
            .header("Authorization", "Bearer " + course.getCanvasApiToken())
            .build();

    List<CanvasGroupSet> groupSets =
        authedClient
            .document(query)
            .variable("courseId", course.getCanvasCourseId())
            .retrieveSync("course.groupSets")
            .toEntityList(CanvasGroupSet.class);
    return groupSets;
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

  public List<CanvasGroup> getCanvasGroups(
      @HasLinkedCanvasCourse Course course, String groupSetId) {
    String query =
        """
            query GetTeams($groupId: ID!) {
              node(id: $groupId) {
                ... on GroupSet {
                  id
                  name
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
            .variable("groupId", groupSetId)
            .retrieveSync("node.groups")
            .toEntityList(JsonNode.class);

    List<CanvasGroup> parsedGroups =
        groups.stream()
            .map(
                group -> {
                  CanvasGroup canvasGroup =
                      CanvasGroup.builder()
                          .name(group.get("name").asText())
                          .id(group.get("_id").asInt())
                          .members(new ArrayList<>())
                          .build();
                  group
                      .get("membersConnection")
                      .get("edges")
                      .forEach(
                          edge -> {
                            canvasGroup
                                .getMembers()
                                .add(
                                    CanonicalFormConverter.convertToValidEmail(
                                        edge.path("node").path("user").get("email").asText()));
                          });
                  return canvasGroup;
                })
            .toList();

    return parsedGroups;
  }
}
