package edu.ucsb.cs156.frontiers.services;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.models.CanvasGroup;
import edu.ucsb.cs156.frontiers.models.CanvasGroupDetail;
import edu.ucsb.cs156.frontiers.models.CanvasGroupSet;
import edu.ucsb.cs156.frontiers.models.CanvasGroupSetDetail;
import edu.ucsb.cs156.frontiers.models.CanvasStudent;
import edu.ucsb.cs156.frontiers.utilities.CanonicalFormConverter;
import edu.ucsb.cs156.frontiers.validators.HasLinkedCanvasCourse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.graphql.client.ClientGraphQlResponse;
import org.springframework.graphql.client.GraphQlClient;
import org.springframework.graphql.client.HttpSyncGraphQlClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.RestClient;

/**
 * Service for interacting with the Canvas API.
 *
 * <p>Note that the Canvas API uses a GraphQL endpoint, which allows for more flexible queries
 * compared to traditional REST APIs.
 *
 * <p>For more information on the Canvas API, visit the official documentation at <a
 * href="https://canvas.instructure.com/doc/api/">...</a>.
 *
 * <p>You can typically interact with Canvas API GraphQL endpoints interactively by appending
 * /graphiql to the URL of the Canvas instance.
 *
 * <p>For example, for UCSB Canvas, use: <a href="https://ucsb.instructure.com/graphiql">...</a>
 *
 * <p>Every Canvas GraphQL connection is paginated: without a {@code first} argument Canvas returns
 * only the first page (20 items by default), and it never returns more than {@link #PAGE_SIZE} per
 * request. Every connection read here therefore asks for {@link #PAGE_SIZE} items and follows
 * {@code pageInfo.endCursor} until {@code hasNextPage} is false.
 */
@Service
@Validated
public class CanvasService {

  /** The largest page Canvas allows (its default_max_page_size). */
  static final int PAGE_SIZE = 100;

  private HttpSyncGraphQlClient graphQlClient;
  private RestClient restClient;
  private ObjectMapper mapper;
  private CanvasApiTokenSecurityService canvasApiTokenSecurityService;

  public CanvasService(
      ObjectMapper mapper,
      RestClient.Builder builder,
      CanvasApiTokenSecurityService canvasApiTokenSecurityService) {
    this.graphQlClient = HttpSyncGraphQlClient.builder(builder.build()).build();
    this.restClient = builder.build();
    this.mapper = mapper;
    this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    this.canvasApiTokenSecurityService = canvasApiTokenSecurityService;
  }

  public List<CanvasGroupSet> getCanvasGroupSets(@HasLinkedCanvasCourse Course course) {
    // language=GraphQL
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

    HttpSyncGraphQlClient authedClient = authedClient(course);

    List<CanvasGroupSet> groupSets =
        authedClient
            .document(query)
            .variable("courseId", course.getCanvasCourseId())
            .retrieveSync("course.groupSets")
            .toEntityList(CanvasGroupSet.class);
    return groupSets;
  }

  /**
   * Fetches the roster of students from Canvas for the given course, following pagination so that
   * every enrolled student is returned.
   *
   * @param course the Course entity containing canvasApiToken and canvasCourseId
   * @return list of RosterStudent objects from Canvas
   */
  public List<RosterStudent> getCanvasRoster(@HasLinkedCanvasCourse Course course) {
    String nodeFields =
        """
            firstName
            lastName
            sisId
            email
            integrationId
            enrollments(courseId: $courseId) {
              section {
                name
              }
            }
            """;

    List<CanvasStudent> students =
        studentEdges(course, nodeFields).stream()
            .map(edge -> toCanvasStudent(edge.get("node")))
            .toList();

    return students.stream()
        .map(
            student ->
                RosterStudent.builder()
                    .firstName(student.getFirstName())
                    .lastName(student.getLastName())
                    .studentId(student.getStudentId())
                    .email(student.getEmail())
                    .section(student.getSection() != null ? student.getSection() : "")
                    .build())
        .toList();
  }

  private CanvasStudent toCanvasStudent(JsonNode userNode) {
    CanvasStudent canvasStudent = mapper.convertValue(userNode, CanvasStudent.class);
    JsonNode enrollments = userNode.path("enrollments");
    if (enrollments.isArray() && !enrollments.isEmpty()) {
      JsonNode sectionName = enrollments.get(0).path("section").path("name");
      if (sectionName.isTextual()) {
        String name = sectionName.asText();
        canvasStudent.setSection(name.substring(0, Math.min(5, name.length())));
      }
    }
    return canvasStudent;
  }

  /**
   * Fetches the groups in a Canvas group set with their members' emails, as used by the pull-teams
   * job. This is a view of {@link #getCanvasGroupSetDetail}.
   *
   * @param course the course, which must be linked to Canvas
   * @param groupSetId the group set's GraphQL relay id or numeric Canvas id
   * @return the groups, each with its members' canonical emails
   */
  public List<CanvasGroup> getCanvasGroups(
      @HasLinkedCanvasCourse Course course, String groupSetId) {
    List<CanvasGroup> groups = new ArrayList<>();
    for (CanvasGroupDetail group : getCanvasGroupSetDetail(course, groupSetId).getGroups()) {
      groups.add(
          CanvasGroup.builder()
              .name(group.getName())
              .id(group.getId())
              .members(new ArrayList<>(group.getMemberUserIdsByEmail().keySet()))
              .build());
    }
    return groups;
  }

  /**
   * Fetches a Canvas group set with everything the push-to-Canvas job needs: the numeric ids of the
   * group set and its groups, and each group's members as canonical email to numeric Canvas user
   * id. Group membership is paginated, so groups with more than {@link #PAGE_SIZE} members are
   * completed with follow-up queries.
   *
   * @param course the course, which must be linked to Canvas
   * @param groupSetId the group set's GraphQL relay id (as returned by getCanvasGroupSets) or its
   *     numeric Canvas id (as shown in Canvas URLs)
   * @return the group set detail
   */
  public CanvasGroupSetDetail getCanvasGroupSetDetail(
      @HasLinkedCanvasCourse Course course, String groupSetId) {
    String query =
        "query GetGroupSetDetail($groupId: ID!, $first: Int!) { groupSet: "
            + groupSetSelector(groupSetId)
            + """
                 {
                ... on GroupSet {
                  _id
                  name
                  groups {
                    _id
                    name
                    membersConnection(first: $first) {
                      edges {
                        node {
                          user {
                            _id
                            email
                          }
                        }
                      }
                      pageInfo {
                        hasNextPage
                        endCursor
                      }
                    }
                  }
                }
              }
            }
            """;

    HttpSyncGraphQlClient client = authedClient(course);
    JsonNode node =
        client
            .document(query)
            .variable("groupId", groupSetId)
            .variable("first", PAGE_SIZE)
            .retrieveSync("groupSet")
            .toEntity(JsonNode.class);

    List<CanvasGroupDetail> groups = new ArrayList<>();
    for (JsonNode group : node.path("groups")) {
      Integer groupId = group.path("_id").asInt();
      List<JsonNode> edges = new ArrayList<>();
      group.path("membersConnection").path("edges").forEach(edges::add);
      JsonNode pageInfo = group.path("membersConnection").path("pageInfo");
      if (pageInfo.path("hasNextPage").asBoolean()) {
        edges.addAll(
            remainingGroupMemberEdges(client, groupId, pageInfo.path("endCursor").asText()));
      }
      Map<String, Integer> members = new LinkedHashMap<>();
      for (JsonNode edge : edges) {
        JsonNode user = edge.path("node").path("user");
        members.put(
            CanonicalFormConverter.convertToValidEmail(user.path("email").asText()),
            user.path("_id").asInt());
      }
      groups.add(
          CanvasGroupDetail.builder()
              .id(groupId)
              .name(group.path("name").asText())
              .memberUserIdsByEmail(members)
              .build());
    }
    return CanvasGroupSetDetail.builder()
        .id(node.path("_id").asInt())
        .name(node.path("name").asText())
        .groups(groups)
        .build();
  }

  /**
   * Fetches the numeric Canvas user id of every student enrolled in the Canvas course, keyed by
   * canonical email, following pagination so that every enrolled student is included.
   *
   * @param course the course, which must be linked to Canvas
   * @return map from canonical email to Canvas user id
   */
  public Map<String, Integer> getCanvasUserIdsByEmail(@HasLinkedCanvasCourse Course course) {
    Map<String, Integer> userIds = new LinkedHashMap<>();
    for (JsonNode edge : studentEdges(course, "_id\nemail\n")) {
      JsonNode user = edge.path("node");
      userIds.put(
          CanonicalFormConverter.convertToValidEmail(user.path("email").asText()),
          user.path("_id").asInt());
    }
    return userIds;
  }

  /**
   * Creates a group in a Canvas group set.
   *
   * @param course the course, which must be linked to Canvas
   * @param groupSetId the numeric Canvas id of the group set
   * @param name the name of the new group
   * @return the numeric Canvas id of the new group
   * @throws RuntimeException if Canvas reports a validation error
   */
  public Integer createCanvasGroup(
      @HasLinkedCanvasCourse Course course, Integer groupSetId, String name) {
    // language=GraphQL
    String mutation =
        """
            mutation CreateGroup($groupSetId: ID!, $name: String!) {
              createGroupInSet(input: {groupSetId: $groupSetId, name: $name}) {
                group {
                  _id
                }
                errors {
                  message
                }
              }
            }
            """;

    ClientGraphQlResponse response =
        authedClient(course)
            .document(mutation)
            .variable("groupSetId", groupSetId.toString())
            .variable("name", name)
            .executeSync();
    JsonNode payload = response.field("createGroupInSet").toEntity(JsonNode.class);
    JsonNode errors = payload.path("errors");
    if (errors.isArray() && !errors.isEmpty()) {
      throw new RuntimeException(
          "Canvas refused to create group " + name + ": " + errors.get(0).path("message").asText());
    }
    return payload.path("group").path("_id").asInt();
  }

  /**
   * Deletes a Canvas group.
   *
   * @param course the course, which must be linked to Canvas
   * @param groupId the numeric Canvas id of the group
   */
  public void deleteCanvasGroup(@HasLinkedCanvasCourse Course course, Integer groupId) {
    restClient
        .delete()
        .uri(restBaseUrl(course) + "/groups/" + groupId)
        .header("Authorization", bearer(course))
        .retrieve()
        .toBodilessEntity();
  }

  /**
   * Adds a user to a Canvas group.
   *
   * @param course the course, which must be linked to Canvas
   * @param groupId the numeric Canvas id of the group
   * @param userId the numeric Canvas id of the user
   */
  public void addCanvasGroupMember(
      @HasLinkedCanvasCourse Course course, Integer groupId, Integer userId) {
    restClient
        .post()
        .uri(restBaseUrl(course) + "/groups/" + groupId + "/memberships")
        .header("Authorization", bearer(course))
        .contentType(MediaType.APPLICATION_JSON)
        .body(Map.of("user_id", userId))
        .retrieve()
        .toBodilessEntity();
  }

  /**
   * Removes a user from a Canvas group.
   *
   * @param course the course, which must be linked to Canvas
   * @param groupId the numeric Canvas id of the group
   * @param userId the numeric Canvas id of the user
   */
  public void removeCanvasGroupMember(
      @HasLinkedCanvasCourse Course course, Integer groupId, Integer userId) {
    restClient
        .delete()
        .uri(restBaseUrl(course) + "/groups/" + groupId + "/users/" + userId)
        .header("Authorization", bearer(course))
        .retrieve()
        .toBodilessEntity();
  }

  /**
   * Fetches every edge of the course's student enrollment connection, following pagination.
   *
   * @param course the course
   * @param nodeFields the GraphQL fields to select on each user node
   * @return all edges, in Canvas order
   */
  private List<JsonNode> studentEdges(Course course, String nodeFields) {
    String query =
        "query GetStudents($courseId: ID!, $first: Int!, $after: String) {"
            + " course(id: $courseId) {"
            + " usersConnection(filter: {enrollmentTypes: StudentEnrollment}, first: $first,"
            + " after: $after) { edges { node { "
            + nodeFields
            + " } } pageInfo { hasNextPage endCursor } } } }";
    return allEdges(
        authedClient(course),
        query,
        Map.of("courseId", course.getCanvasCourseId()),
        "course.usersConnection",
        null);
  }

  /**
   * Fetches the members of one group that did not fit in the first page of the group set query.
   *
   * @param client the authenticated client
   * @param groupId the numeric Canvas id of the group
   * @param after the end cursor of the page already read
   * @return the remaining edges
   */
  private List<JsonNode> remainingGroupMemberEdges(
      HttpSyncGraphQlClient client, Integer groupId, String after) {
    // language=GraphQL
    String query =
        """
            query GetGroupMembers($groupId: ID!, $first: Int!, $after: String) {
              legacyNode(_id: $groupId, type: Group) {
                ... on Group {
                  membersConnection(first: $first, after: $after) {
                    edges {
                      node {
                        user {
                          _id
                          email
                        }
                      }
                    }
                    pageInfo {
                      hasNextPage
                      endCursor
                    }
                  }
                }
              }
            }
            """;
    return allEdges(
        client,
        query,
        Map.of("groupId", groupId.toString()),
        "legacyNode.membersConnection",
        after);
  }

  /**
   * Reads a paginated connection to the end. The query must declare {@code $first: Int!} and {@code
   * $after: String}, pass them to the connection, and select {@code pageInfo { hasNextPage
   * endCursor }} on it.
   *
   * @param client the authenticated client
   * @param query the query
   * @param variables the query's other variables
   * @param connectionPath the path of the connection in the response
   * @param after the cursor to start after, or null to start at the beginning
   * @return every edge from {@code after} to the end, in order
   */
  private static List<JsonNode> allEdges(
      HttpSyncGraphQlClient client,
      String query,
      Map<String, Object> variables,
      String connectionPath,
      String after) {
    List<JsonNode> edges = new ArrayList<>();
    while (true) {
      GraphQlClient.RequestSpec request =
          client.document(query).variables(variables).variable("first", PAGE_SIZE);
      if (after != null) {
        request = request.variable("after", after);
      }
      JsonNode connection = request.retrieveSync(connectionPath).toEntity(JsonNode.class);
      connection.path("edges").forEach(edges::add);
      JsonNode pageInfo = connection.path("pageInfo");
      if (!pageInfo.path("hasNextPage").asBoolean()) {
        return edges;
      }
      after = pageInfo.path("endCursor").asText();
    }
  }

  /**
   * The GraphQL field that looks up a group set by the given id. Canvas's {@code node(id:)} takes
   * only the GraphQL relay id (e.g. "R3JvdXBTZXQtMTAx"); a plain numeric id, as shown in Canvas
   * URLs and by the Canvas REST API, must go through {@code legacyNode}. Both forms are accepted so
   * that instructors can paste either.
   *
   * @param groupSetId a relay id or a numeric id
   * @return the field selector, without its selection set
   */
  static String groupSetSelector(String groupSetId) {
    if (groupSetId.matches("\\d+")) {
      return "legacyNode(_id: $groupId, type: GroupSet)";
    }
    return "node(id: $groupId)";
  }

  private HttpSyncGraphQlClient authedClient(Course course) {
    return graphQlClient
        .mutate()
        .header("Authorization", bearer(course))
        .url(course.getSchool().getCanvasImplementation())
        .build();
  }

  private String bearer(Course course) {
    return "Bearer " + canvasApiTokenSecurityService.decrypt(course.getCanvasApiToken());
  }

  /**
   * The Canvas REST API base for the course's school, derived from the GraphQL endpoint: e.g.
   * https://ucsb.instructure.com/api/graphql becomes https://ucsb.instructure.com/api/v1.
   */
  private static String restBaseUrl(Course course) {
    return course.getSchool().getCanvasImplementation().replace("/api/graphql", "/api/v1");
  }
}
