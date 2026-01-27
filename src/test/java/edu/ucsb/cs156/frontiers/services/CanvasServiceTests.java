package edu.ucsb.cs156.frontiers.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import edu.ucsb.cs156.frontiers.config.GithubGraphQLClientConfig;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.Team;
import edu.ucsb.cs156.frontiers.enums.TeamStatus;
import edu.ucsb.cs156.frontiers.testconfig.TestConfig;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.client.MockRestServiceServer;

@RestClientTest(CanvasService.class)
@Import({TestConfig.class, GithubGraphQLClientConfig.class})
public class CanvasServiceTests {

  @Autowired private MockRestServiceServer mockServer;

  @Autowired private CanvasService canvasService;

  @MockBean private edu.ucsb.cs156.frontiers.repositories.TeamRepository teamRepository;

  @BeforeEach
  public void setup() {
    mockServer.reset();
  }

  @Test
  public void testGetCanvasRoster_returnsStudents() throws Exception {
    // Arrange
    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .canvasApiToken("test-api-token")
            .canvasCourseId("12345")
            .build();

    // Create GraphQL response that matches what Canvas API would return
    String graphqlResponse =
        """
        {
          "data": {
            "course": {
              "usersConnection": {
                "edges": [
                  {"node": {"firstName": "Alice", "lastName": "Smith", "sisId": "A111111", "email": "alice@ucsb.edu", "integrationId": null}},
                  {"node": {"firstName": "Bob", "lastName": "Jones", "sisId": "A222222", "email": "bob@ucsb.edu", "integrationId": "B222222"}}
                ]
              }
            }
          }
        }
        """;

    mockServer
        .expect(requestTo("https://ucsb.instructure.com/api/graphql"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer test-api-token"))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andRespond(withSuccess(graphqlResponse, MediaType.APPLICATION_JSON));

    // Act
    List<RosterStudent> result = canvasService.getCanvasRoster(course);

    // Assert
    mockServer.verify();
    assertNotNull(result);
    assertEquals(2, result.size());

    RosterStudent student1 = result.get(0);
    assertEquals("Alice", student1.getFirstName());
    assertEquals("Smith", student1.getLastName());
    assertEquals("A111111", student1.getStudentId());
    assertEquals("alice@ucsb.edu", student1.getEmail());

    RosterStudent student2 = result.get(1);
    assertEquals("Bob", student2.getFirstName());
    assertEquals("Jones", student2.getLastName());
    assertEquals("B222222", student2.getStudentId()); // integrationId takes precedence
    assertEquals("bob@ucsb.edu", student2.getEmail());
  }

  @Test
  public void testGetCanvasRoster_returnsEmptyList() throws Exception {
    // Arrange
    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .canvasApiToken("test-api-token")
            .canvasCourseId("12345")
            .build();

    // GraphQL response with empty edges array
    String graphqlResponse =
        """
        {
          "data": {
            "course": {
              "usersConnection": {
                "edges": []
              }
            }
          }
        }
        """;

    mockServer
        .expect(requestTo("https://ucsb.instructure.com/api/graphql"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer test-api-token"))
        .andRespond(withSuccess(graphqlResponse, MediaType.APPLICATION_JSON));

    // Act
    List<RosterStudent> result = canvasService.getCanvasRoster(course);

    // Assert
    mockServer.verify();
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  public void testGetCanvasRoster_usesSisIdWhenIntegrationIdNull() throws Exception {
    // Arrange
    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .canvasApiToken("test-api-token")
            .canvasCourseId("12345")
            .build();

    // Student with null integrationId - should use sisId
    String graphqlResponse =
        """
        {
          "data": {
            "course": {
              "usersConnection": {
                "edges": [
                  {"node": {"firstName": "Charlie", "lastName": "Brown", "sisId": "SIS123", "email": "charlie@ucsb.edu", "integrationId": null}}
                ]
              }
            }
          }
        }
        """;

    mockServer
        .expect(requestTo("https://ucsb.instructure.com/api/graphql"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer test-api-token"))
        .andRespond(withSuccess(graphqlResponse, MediaType.APPLICATION_JSON));

    // Act
    List<RosterStudent> result = canvasService.getCanvasRoster(course);

    // Assert
    mockServer.verify();
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals("SIS123", result.get(0).getStudentId());
  }

  @Test
  public void testGetCanvasRoster_usesIntegrationIdWhenPresent() throws Exception {
    // Arrange
    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .canvasApiToken("test-api-token")
            .canvasCourseId("12345")
            .build();

    // Student with integrationId - should use integrationId over sisId
    String graphqlResponse =
        """
        {
          "data": {
            "course": {
              "usersConnection": {
                "edges": [
                  {"node": {"firstName": "Diana", "lastName": "Prince", "sisId": "SIS456", "email": "diana@ucsb.edu", "integrationId": "INT456"}}
                ]
              }
            }
          }
        }
        """;

    mockServer
        .expect(requestTo("https://ucsb.instructure.com/api/graphql"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer test-api-token"))
        .andRespond(withSuccess(graphqlResponse, MediaType.APPLICATION_JSON));

    // Act
    List<RosterStudent> result = canvasService.getCanvasRoster(course);

    // Assert
    mockServer.verify();
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals("INT456", result.get(0).getStudentId());
  }

  @Test
  public void testGetCanvasTeams_createsTeamsAndMembers() throws Exception {
    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .canvasApiToken("test-api-token")
            .canvasCourseId("12345")
            .rosterStudents(
                List.of(
                    RosterStudent.builder()
                        .email("alice@ucsb.edu")
                        .firstName("Alice")
                        .lastName("Smith")
                        .studentId("A1")
                        .teamMembers(new ArrayList<>())
                        .build(),
                    RosterStudent.builder()
                        .email("bob@ucsb.edu")
                        .firstName("Bob")
                        .lastName("Jones")
                        .studentId("B2")
                        .teamMembers(new ArrayList<>())
                        .build()))
            .teams(new ArrayList<>())
            .build();

    String graphqlResponse =
        """
        {
          "data": {
            "course": {
              "groupSets": [{
                "groups": [{
                  "name": "Team Alpha",
                  "_id": 999,
                  "membersConnection": {
                    "edges": [
                      {"node": {"user": {"email": "alice@ucsb.edu"}}},
                      {"node": {"user": {"email": "bob@ucsb.edu"}}}
                    ]
                  }
                }]
              }]
            }
          }
        }
        """;

    mockServer
        .expect(requestTo("https://ucsb.instructure.com/api/graphql"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer test-api-token"))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andRespond(withSuccess(graphqlResponse, MediaType.APPLICATION_JSON));

    List<Team> result = canvasService.getCanvasTeams(course);

    mockServer.verify();
    assertNotNull(result);
    assertEquals(1, result.size());
    Team createdTeam = result.get(0);
    assertEquals("Team Alpha", createdTeam.getName());
    assertEquals(999, createdTeam.getCanvasId());
    assertEquals(2, createdTeam.getTeamMembers().size());
    assertEquals(
        TeamStatus.NO_GITHUB_ID,
        createdTeam.getTeamMembers().get(0).getTeamStatus());
    assertEquals(course, createdTeam.getCourse());

    ArgumentCaptor<List<Team>> captor = ArgumentCaptor.forClass(List.class);
    verify(teamRepository, times(1)).saveAll(captor.capture());
    assertEquals(1, captor.getValue().size());
    assertEquals("Team Alpha", captor.getValue().get(0).getName());
  }
}
