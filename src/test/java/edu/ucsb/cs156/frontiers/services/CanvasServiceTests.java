package edu.ucsb.cs156.frontiers.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import edu.ucsb.cs156.frontiers.config.GithubGraphQLClientConfig;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.models.CanvasGroup;
import edu.ucsb.cs156.frontiers.models.CanvasGroupSet;
import edu.ucsb.cs156.frontiers.testconfig.TestConfig;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

@RestClientTest(CanvasService.class)
@Import({TestConfig.class, GithubGraphQLClientConfig.class})
public class CanvasServiceTests {

  @Autowired private MockRestServiceServer mockServer;

  @Autowired private CanvasService canvasService;

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
  public void testGetCanvasGroupSets_returnsGroupSets() throws Exception {
    // Arrange
    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .canvasApiToken("test-api-token")
            .canvasCourseId("12345")
            .build();

    String graphqlResponse =
        """
        {
          "data": {
            "course": {
              "groupSets": [
                {"_id": "101", "name": "Project Teams", "id": "UHJvamVjdFRlYW1z"},
                {"_id": "102", "name": "Lab Groups", "id": "TGFiR3JvdXBz"}
              ]
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
    List<CanvasGroupSet> result = canvasService.getCanvasGroupSets(course);

    // Assert
    mockServer.verify();
    assertNotNull(result);
    assertEquals(2, result.size());

    CanvasGroupSet groupSet1 = result.get(0);
    assertEquals("Project Teams", groupSet1.getName());
    assertEquals("UHJvamVjdFRlYW1z", groupSet1.getId());

    CanvasGroupSet groupSet2 = result.get(1);
    assertEquals("Lab Groups", groupSet2.getName());
    assertEquals("TGFiR3JvdXBz", groupSet2.getId());
  }

  @Test
  public void testGetCanvasGroupSets_returnsEmptyList() throws Exception {
    // Arrange
    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .canvasApiToken("test-api-token")
            .canvasCourseId("12345")
            .build();

    String graphqlResponse =
        """
        {
          "data": {
            "course": {
              "groupSets": []
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
    List<CanvasGroupSet> result = canvasService.getCanvasGroupSets(course);

    // Assert
    mockServer.verify();
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  public void testGetCanvasGroups_returnsGroups() throws Exception {
    // Arrange
    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .canvasApiToken("test-api-token")
            .canvasCourseId("12345")
            .build();

    String graphqlResponse =
        """
        {
          "data": {
            "node": {
              "id": "R3JvdXBTZXQtMTAx",
              "name": "Project Teams",
              "groups": [
                {
                  "name": "Team Alpha",
                  "_id": 201,
                  "membersConnection": {
                    "edges": [
                      {"node": {"user": {"email": "alice@ucsb.edu"}}},
                      {"node": {"user": {"email": "bob@ucsb.edu"}}}
                    ]
                  }
                },
                {
                  "name": "Team Beta",
                  "_id": 202,
                  "membersConnection": {
                    "edges": [
                      {"node": {"user": {"email": "charlie@umail.ucsb.edu"}}}
                    ]
                  }
                }
              ]
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
    List<CanvasGroup> result = canvasService.getCanvasGroups(course, "R3JvdXBTZXQtMTAx");

    // Assert
    mockServer.verify();
    assertNotNull(result);
    assertEquals(2, result.size());

    CanvasGroup group1 = result.get(0);
    assertEquals("Team Alpha", group1.getName());
    assertEquals(201, group1.getId());
    assertEquals(2, group1.getMembers().size());
    assertEquals("alice@ucsb.edu", group1.getMembers().get(0));
    assertEquals("bob@ucsb.edu", group1.getMembers().get(1));

    CanvasGroup group2 = result.get(1);
    assertEquals("Team Beta", group2.getName());
    assertEquals(202, group2.getId());
    assertEquals(1, group2.getMembers().size());
    // Email should be converted to canonical form (umail.ucsb.edu -> ucsb.edu)
    assertEquals("charlie@ucsb.edu", group2.getMembers().get(0));
  }

  @Test
  public void testGetCanvasGroups_returnsEmptyList() throws Exception {
    // Arrange
    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .canvasApiToken("test-api-token")
            .canvasCourseId("12345")
            .build();

    String graphqlResponse =
        """
        {
          "data": {
            "node": {
              "id": "R3JvdXBTZXQtMTAx",
              "name": "Project Teams",
              "groups": []
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
    List<CanvasGroup> result = canvasService.getCanvasGroups(course, "R3JvdXBTZXQtMTAx");

    // Assert
    mockServer.verify();
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  public void testGetCanvasGroups_handlesGroupWithNoMembers() throws Exception {
    // Arrange
    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .canvasApiToken("test-api-token")
            .canvasCourseId("12345")
            .build();

    String graphqlResponse =
        """
        {
          "data": {
            "node": {
              "id": "R3JvdXBTZXQtMTAx",
              "name": "Project Teams",
              "groups": [
                {
                  "name": "Empty Team",
                  "_id": 203,
                  "membersConnection": {
                    "edges": []
                  }
                }
              ]
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
    List<CanvasGroup> result = canvasService.getCanvasGroups(course, "R3JvdXBTZXQtMTAx");

    // Assert
    mockServer.verify();
    assertNotNull(result);
    assertEquals(1, result.size());

    CanvasGroup group = result.get(0);
    assertEquals("Empty Team", group.getName());
    assertEquals(203, group.getId());
    assertTrue(group.getMembers().isEmpty());
  }
}
