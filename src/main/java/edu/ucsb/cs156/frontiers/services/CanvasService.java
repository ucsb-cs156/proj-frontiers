package edu.ucsb.cs156.frontiers.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.models.CanvasStudent;
import java.util.List;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.graphql.client.HttpSyncGraphQlClient;
import org.springframework.stereotype.Service;

@Service
public class CanvasService {

  private HttpSyncGraphQlClient graphQlClient;
  private ObjectMapper mapper;

  public CanvasService(RestTemplateBuilder templateBuilder, HttpSyncGraphQlClient graphQlClient) {
    this.graphQlClient =
        graphQlClient.mutate().url("https://ucsb.instructure.com/api/graphql").build();
    this.mapper = new ObjectMapper();
  }

  /**
   * Fetches the roster of students from Canvas for the given course.
   *
   * @param course the Course entity containing canvasApiToken and canvasCourseId
   * @return list of RosterStudent objects from Canvas
   */
  public List<RosterStudent> getCanvasRoster(Course course) {
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
}
