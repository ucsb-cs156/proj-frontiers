package edu.ucsb.cs156.frontiers.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.graphql.client.GraphQlClient.RequestSpec;
import org.springframework.graphql.client.GraphQlClient.RetrieveSyncSpec;
import org.springframework.graphql.client.HttpSyncGraphQlClient;

@ExtendWith(MockitoExtension.class)
public class CanvasServiceTests {

  @Mock private RestTemplateBuilder restTemplateBuilder;

  @Mock private HttpSyncGraphQlClient graphQlClient;

  @Mock private HttpSyncGraphQlClient.Builder graphQlClientBuilder;

  @Mock private RequestSpec requestSpec;

  @Mock private RetrieveSyncSpec retrieveSyncSpec;

  private ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
  }

  @AfterEach
  public void tearDown() {
    Mockito.reset(
        restTemplateBuilder, graphQlClient, graphQlClientBuilder, requestSpec, retrieveSyncSpec);
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

    // Create JSON nodes that match what Canvas API would return
    String jsonResponse =
        """
        [
          {"node": {"firstName": "Alice", "lastName": "Smith", "sisId": "A111111", "email": "alice@ucsb.edu", "integrationId": null}},
          {"node": {"firstName": "Bob", "lastName": "Jones", "sisId": "A222222", "email": "bob@ucsb.edu", "integrationId": "B222222"}}
        ]
        """;
    JsonNode rootNode = objectMapper.readTree(jsonResponse);
    List<JsonNode> convertedNodes = new java.util.ArrayList<>();
    for (JsonNode node : rootNode) {
      convertedNodes.add(node);
    }

    // Set up mock chain
    when(graphQlClient.mutate()).thenReturn(graphQlClientBuilder);
    when(graphQlClientBuilder.url(anyString())).thenReturn(graphQlClientBuilder);
    when(graphQlClientBuilder.header(anyString(), anyString())).thenReturn(graphQlClientBuilder);
    when(graphQlClientBuilder.build()).thenReturn(graphQlClient);
    when(graphQlClient.document(anyString())).thenReturn(requestSpec);
    when(requestSpec.variable(anyString(), any())).thenReturn(requestSpec);
    when(requestSpec.retrieveSync(anyString())).thenReturn(retrieveSyncSpec);
    when(retrieveSyncSpec.toEntityList(eq(JsonNode.class))).thenReturn(convertedNodes);

    // Act
    CanvasService canvasService = new CanvasService(restTemplateBuilder, graphQlClient);
    List<RosterStudent> result = canvasService.getCanvasRoster(course);

    // Assert
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

    // Set up mock chain for empty result
    when(graphQlClient.mutate()).thenReturn(graphQlClientBuilder);
    when(graphQlClientBuilder.url(anyString())).thenReturn(graphQlClientBuilder);
    when(graphQlClientBuilder.header(anyString(), anyString())).thenReturn(graphQlClientBuilder);
    when(graphQlClientBuilder.build()).thenReturn(graphQlClient);
    when(graphQlClient.document(anyString())).thenReturn(requestSpec);
    when(requestSpec.variable(anyString(), any())).thenReturn(requestSpec);
    when(requestSpec.retrieveSync(anyString())).thenReturn(retrieveSyncSpec);
    when(retrieveSyncSpec.toEntityList(eq(JsonNode.class))).thenReturn(List.of());

    // Act
    CanvasService canvasService = new CanvasService(restTemplateBuilder, graphQlClient);
    List<RosterStudent> result = canvasService.getCanvasRoster(course);

    // Assert
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
    String jsonResponse =
        """
        [
          {"node": {"firstName": "Charlie", "lastName": "Brown", "sisId": "SIS123", "email": "charlie@ucsb.edu", "integrationId": null}}
        ]
        """;
    JsonNode rootNode = objectMapper.readTree(jsonResponse);
    List<JsonNode> convertedNodes = new java.util.ArrayList<>();
    for (JsonNode node : rootNode) {
      convertedNodes.add(node);
    }

    when(graphQlClient.mutate()).thenReturn(graphQlClientBuilder);
    when(graphQlClientBuilder.url(anyString())).thenReturn(graphQlClientBuilder);
    when(graphQlClientBuilder.header(anyString(), anyString())).thenReturn(graphQlClientBuilder);
    when(graphQlClientBuilder.build()).thenReturn(graphQlClient);
    when(graphQlClient.document(anyString())).thenReturn(requestSpec);
    when(requestSpec.variable(anyString(), any())).thenReturn(requestSpec);
    when(requestSpec.retrieveSync(anyString())).thenReturn(retrieveSyncSpec);
    when(retrieveSyncSpec.toEntityList(eq(JsonNode.class))).thenReturn(convertedNodes);

    // Act
    CanvasService canvasService = new CanvasService(restTemplateBuilder, graphQlClient);
    List<RosterStudent> result = canvasService.getCanvasRoster(course);

    // Assert
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
    String jsonResponse =
        """
        [
          {"node": {"firstName": "Diana", "lastName": "Prince", "sisId": "SIS456", "email": "diana@ucsb.edu", "integrationId": "INT456"}}
        ]
        """;
    JsonNode rootNode = objectMapper.readTree(jsonResponse);
    List<JsonNode> convertedNodes = new java.util.ArrayList<>();
    for (JsonNode node : rootNode) {
      convertedNodes.add(node);
    }

    when(graphQlClient.mutate()).thenReturn(graphQlClientBuilder);
    when(graphQlClientBuilder.url(anyString())).thenReturn(graphQlClientBuilder);
    when(graphQlClientBuilder.header(anyString(), anyString())).thenReturn(graphQlClientBuilder);
    when(graphQlClientBuilder.build()).thenReturn(graphQlClient);
    when(graphQlClient.document(anyString())).thenReturn(requestSpec);
    when(requestSpec.variable(anyString(), any())).thenReturn(requestSpec);
    when(requestSpec.retrieveSync(anyString())).thenReturn(retrieveSyncSpec);
    when(retrieveSyncSpec.toEntityList(eq(JsonNode.class))).thenReturn(convertedNodes);

    // Act
    CanvasService canvasService = new CanvasService(restTemplateBuilder, graphQlClient);
    List<RosterStudent> result = canvasService.getCanvasRoster(course);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals("INT456", result.get(0).getStudentId());
  }
}
