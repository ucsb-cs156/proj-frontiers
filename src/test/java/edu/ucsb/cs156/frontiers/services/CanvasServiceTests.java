package edu.ucsb.cs156.frontiers.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.enums.School;
import edu.ucsb.cs156.frontiers.models.CanvasGroup;
import edu.ucsb.cs156.frontiers.models.CanvasGroupDetail;
import edu.ucsb.cs156.frontiers.models.CanvasGroupSet;
import edu.ucsb.cs156.frontiers.models.CanvasGroupSetDetail;
import edu.ucsb.cs156.frontiers.testconfig.TestConfig;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;

@RestClientTest(CanvasService.class)
@Import({TestConfig.class})
public class CanvasServiceTests {

  @Autowired private MockRestServiceServer mockServer;

  @Autowired private CanvasService canvasService;

  @MockitoBean private CanvasApiTokenSecurityService canvasApiTokenSecurityService;

  @BeforeEach
  public void setup() {
    mockServer.reset();
    org.mockito.Mockito.when(
            canvasApiTokenSecurityService.decrypt(org.mockito.ArgumentMatchers.any(String.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
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
            .school(School.UCSB)
            .build();

    // Create GraphQL response that matches what Canvas API would return
    String graphqlResponse =
        """
        {
          "data": {
            "course": {
              "usersConnection": {
                "edges": [
                  {"node": {"firstName": "Alice", "lastName": "Smith", "sisId": "A111111", "email": "alice@ucsb.edu", "integrationId": null, "enrollments": [{"section": {"name": "52027 [TA] F 02:00PM PHELP2524"}}]}},
                  {"node": {"firstName": "Bob", "lastName": "Jones", "sisId": "A222222", "email": "bob@ucsb.edu", "integrationId": "B222222", "enrollments": [{"section": {"name": "12345 [LEC] MW 10:00AM PHELP1401"}}]}}
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
    assertEquals("52027", student1.getSection());

    RosterStudent student2 = result.get(1);
    assertEquals("Bob", student2.getFirstName());
    assertEquals("Jones", student2.getLastName());
    assertEquals("B222222", student2.getStudentId()); // integrationId takes precedence
    assertEquals("bob@ucsb.edu", student2.getEmail());
    assertEquals("12345", student2.getSection());
  }

  @Test
  public void testGetCanvasRoster_setsEmptySectionWhenNoEnrollments() throws Exception {
    // Arrange
    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .canvasApiToken("test-api-token")
            .canvasCourseId("12345")
            .school(School.UCSB)
            .build();

    // Student with no enrollments/section info returned by Canvas
    String graphqlResponse =
        """
        {
          "data": {
            "course": {
              "usersConnection": {
                "edges": [
                  {"node": {"firstName": "Eve", "lastName": "Adams", "sisId": "E111111", "email": "eve@ucsb.edu", "integrationId": null, "enrollments": []}}
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
    assertEquals("", result.get(0).getSection());
  }

  @Test
  public void testGetCanvasRoster_setsEmptySectionWhenSectionNameMissing() throws Exception {
    // Arrange
    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .canvasApiToken("test-api-token")
            .canvasCourseId("12345")
            .school(School.UCSB)
            .build();

    // Student with an enrollment entry that has no section name info returned by Canvas
    String graphqlResponse =
        """
        {
          "data": {
            "course": {
              "usersConnection": {
                "edges": [
                  {"node": {"firstName": "Frank", "lastName": "Ng", "sisId": "F111111", "email": "frank@ucsb.edu", "integrationId": null, "enrollments": [{"section": {}}]}}
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
    assertEquals("", result.get(0).getSection());
  }

  @Test
  public void testGetCanvasRoster_truncatesSectionNameToFirstFiveCharacters() throws Exception {
    // Arrange
    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .canvasApiToken("test-api-token")
            .canvasCourseId("12345")
            .school(School.UCSB)
            .build();

    // Canvas returns section names like "52027 [TA] F 02:00PM PHELP2524"; only the first
    // five characters (the section number) should be stored.
    String graphqlResponse =
        """
        {
          "data": {
            "course": {
              "usersConnection": {
                "edges": [
                  {"node": {"firstName": "Grace", "lastName": "Lee", "sisId": "G111111", "email": "grace@ucsb.edu", "integrationId": null, "enrollments": [{"section": {"name": "52027 [TA] F 02:00PM PHELP2524"}}]}},
                  {"node": {"firstName": "Hank", "lastName": "Kim", "sisId": "H111111", "email": "hank@ucsb.edu", "integrationId": null, "enrollments": [{"section": {"name": "AB"}}]}}
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
    assertEquals(2, result.size());
    assertEquals("52027", result.get(0).getSection());
    assertEquals("AB", result.get(1).getSection());
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
            .school(School.UCSB)
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
            .school(School.UCSB)
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
            .school(School.UCSB)
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
            .school(School.UCSB)
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
            .school(School.UCSB)
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
            .school(School.UCSB)
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
            .school(School.UCSB)
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
            .school(School.UCSB)
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

  // ---- push-to-Canvas support ----

  private Course linkedCourse() {
    return Course.builder()
        .id(1L)
        .courseName("CS156")
        .canvasApiToken("test-api-token")
        .canvasCourseId("12345")
        .school(School.UCSB)
        .build();
  }

  @Test
  public void getCanvasGroupSetDetail_parsesIdsNamesAndMembers() throws Exception {
    Course course = linkedCourse();
    String graphqlResponse =
        """
        {
          "data": {
            "node": {
              "_id": "101",
              "name": "Project Teams",
              "groups": [
                {
                  "_id": "201",
                  "name": "Team Alpha",
                  "membersConnection": {
                    "edges": [
                      {"node": {"user": {"_id": "11", "email": "Alice@umail.ucsb.edu"}}},
                      {"node": {"user": {"_id": "12", "email": "bob@ucsb.edu"}}}
                    ]
                  }
                },
                {
                  "_id": "202",
                  "name": "Team Beta",
                  "membersConnection": {"edges": []}
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
        .andExpect(content().string(org.hamcrest.Matchers.containsString("R3JvdXBTZXQtMTAx")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("membersConnection")))
        .andRespond(withSuccess(graphqlResponse, MediaType.APPLICATION_JSON));

    CanvasGroupSetDetail result = canvasService.getCanvasGroupSetDetail(course, "R3JvdXBTZXQtMTAx");

    mockServer.verify();
    assertEquals(101, result.getId());
    assertEquals("Project Teams", result.getName());
    assertEquals(2, result.getGroups().size());
    CanvasGroupDetail alpha = result.getGroups().get(0);
    assertEquals(201, alpha.getId());
    assertEquals("Team Alpha", alpha.getName());
    assertEquals(Map.of("alice@ucsb.edu", 11, "bob@ucsb.edu", 12), alpha.getMemberUserIdsByEmail());
    assertEquals(
        List.of("alice@ucsb.edu", "bob@ucsb.edu"),
        List.copyOf(alpha.getMemberUserIdsByEmail().keySet()));
    CanvasGroupDetail beta = result.getGroups().get(1);
    assertEquals(202, beta.getId());
    assertEquals("Team Beta", beta.getName());
    assertEquals(Map.of(), beta.getMemberUserIdsByEmail());
  }

  @Test
  public void getCanvasGroupSetDetail_withNoGroups() throws Exception {
    Course course = linkedCourse();
    String graphqlResponse =
        """
        {"data": {"node": {"_id": "101", "name": "Project Teams", "groups": []}}}
        """;
    mockServer
        .expect(requestTo("https://ucsb.instructure.com/api/graphql"))
        .andRespond(withSuccess(graphqlResponse, MediaType.APPLICATION_JSON));

    CanvasGroupSetDetail result = canvasService.getCanvasGroupSetDetail(course, "R3JvdXBTZXQtMTAx");

    mockServer.verify();
    assertEquals(101, result.getId());
    assertEquals(List.of(), result.getGroups());
  }

  @Test
  public void getCanvasUserIdsByEmail_mapsCanonicalEmailsToIds() throws Exception {
    Course course = linkedCourse();
    String graphqlResponse =
        """
        {
          "data": {
            "course": {
              "usersConnection": {
                "edges": [
                  {"node": {"_id": "11", "email": "Alice@umail.ucsb.edu"}},
                  {"node": {"_id": "12", "email": "bob@ucsb.edu"}}
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
        .andExpect(content().string(org.hamcrest.Matchers.containsString("\"courseId\":\"12345\"")))
        .andRespond(withSuccess(graphqlResponse, MediaType.APPLICATION_JSON));

    Map<String, Integer> result = canvasService.getCanvasUserIdsByEmail(course);

    mockServer.verify();
    assertEquals(Map.of("alice@ucsb.edu", 11, "bob@ucsb.edu", 12), result);
  }

  @Test
  public void getCanvasUserIdsByEmail_withNoStudents() throws Exception {
    Course course = linkedCourse();
    mockServer
        .expect(requestTo("https://ucsb.instructure.com/api/graphql"))
        .andRespond(
            withSuccess(
                """
                {"data": {"course": {"usersConnection": {"edges": []}}}}
                """,
                MediaType.APPLICATION_JSON));

    assertEquals(Map.of(), canvasService.getCanvasUserIdsByEmail(course));
    mockServer.verify();
  }

  @Test
  public void createCanvasGroup_returnsTheNewGroupId() throws Exception {
    Course course = linkedCourse();
    mockServer
        .expect(requestTo("https://ucsb.instructure.com/api/graphql"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer test-api-token"))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("createGroupInSet")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("\"groupSetId\":\"101\"")))
        .andExpect(
            content().string(org.hamcrest.Matchers.containsString("\"name\":\"Team Alpha\"")))
        .andRespond(
            withSuccess(
                """
                {"data": {"createGroupInSet": {"group": {"_id": "201"}, "errors": null}}}
                """,
                MediaType.APPLICATION_JSON));

    Integer id = canvasService.createCanvasGroup(course, 101, "Team Alpha");

    mockServer.verify();
    assertEquals(201, id);
  }

  @Test
  public void createCanvasGroup_throwsWhenCanvasReportsErrors() throws Exception {
    Course course = linkedCourse();
    mockServer
        .expect(requestTo("https://ucsb.instructure.com/api/graphql"))
        .andRespond(
            withSuccess(
                """
                {"data": {"createGroupInSet": {"group": null, "errors": [{"message": "name taken"}]}}}
                """,
                MediaType.APPLICATION_JSON));

    RuntimeException e =
        assertThrows(
            RuntimeException.class,
            () -> canvasService.createCanvasGroup(course, 101, "Team Alpha"));

    mockServer.verify();
    assertEquals("Canvas refused to create group Team Alpha: name taken", e.getMessage());
  }

  @Test
  public void createCanvasGroup_treatsEmptyErrorsAsSuccess() throws Exception {
    Course course = linkedCourse();
    mockServer
        .expect(requestTo("https://ucsb.instructure.com/api/graphql"))
        .andRespond(
            withSuccess(
                """
                {"data": {"createGroupInSet": {"group": {"_id": "202"}, "errors": []}}}
                """,
                MediaType.APPLICATION_JSON));

    assertEquals(202, canvasService.createCanvasGroup(course, 101, "Team Beta"));
    mockServer.verify();
  }

  @Test
  public void deleteCanvasGroup_callsTheRestApi() throws Exception {
    Course course = linkedCourse();
    mockServer
        .expect(requestTo("https://ucsb.instructure.com/api/v1/groups/201"))
        .andExpect(method(HttpMethod.DELETE))
        .andExpect(header("Authorization", "Bearer test-api-token"))
        .andRespond(withSuccess("{\"id\": 201}", MediaType.APPLICATION_JSON));

    canvasService.deleteCanvasGroup(course, 201);

    mockServer.verify();
  }

  @Test
  public void deleteCanvasGroup_throwsOnHttpError() throws Exception {
    Course course = linkedCourse();
    mockServer
        .expect(requestTo("https://ucsb.instructure.com/api/v1/groups/201"))
        .andRespond(withStatus(HttpStatus.NOT_FOUND));

    assertThrows(
        HttpClientErrorException.class, () -> canvasService.deleteCanvasGroup(course, 201));
    mockServer.verify();
  }

  @Test
  public void addCanvasGroupMember_postsTheUserId() throws Exception {
    Course course = linkedCourse();
    mockServer
        .expect(requestTo("https://ucsb.instructure.com/api/v1/groups/201/memberships"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer test-api-token"))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json("{\"user_id\": 11}"))
        .andRespond(withSuccess("{\"id\": 301}", MediaType.APPLICATION_JSON));

    canvasService.addCanvasGroupMember(course, 201, 11);

    mockServer.verify();
  }

  @Test
  public void removeCanvasGroupMember_deletesTheMembership() throws Exception {
    Course course = linkedCourse();
    mockServer
        .expect(requestTo("https://ucsb.instructure.com/api/v1/groups/201/users/11"))
        .andExpect(method(HttpMethod.DELETE))
        .andExpect(header("Authorization", "Bearer test-api-token"))
        .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

    canvasService.removeCanvasGroupMember(course, 201, 11);

    mockServer.verify();
  }

  @Test
  public void restCalls_useTheSchoolsCanvasHost() throws Exception {
    Course course = linkedCourse();
    course.setSchool(School.CHICO_STATE);
    mockServer
        .expect(requestTo("https://canvas.csuchico.edu/api/v1/groups/5/users/6"))
        .andExpect(method(HttpMethod.DELETE))
        .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

    canvasService.removeCanvasGroupMember(course, 5, 6);

    mockServer.verify();
  }
}
