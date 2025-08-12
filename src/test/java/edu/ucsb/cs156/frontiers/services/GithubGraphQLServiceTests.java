package edu.ucsb.cs156.frontiers.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.fixtures.GithubGraphQLFixtures;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.graphql.client.ClientGraphQlResponse;
import org.springframework.graphql.client.GraphQlClient.RequestSpec;
import org.springframework.graphql.client.GraphQlClient.RetrieveSyncSpec;
import org.springframework.graphql.client.HttpSyncGraphQlClient;

@ExtendWith(MockitoExtension.class)
public class GithubGraphQLServiceTests {

  @Mock private JwtService jwtService;

  @Mock private HttpSyncGraphQlClient graphQlClient;

  @Mock private HttpSyncGraphQlClient.Builder graphQlClientBuilder;

  @Mock private RequestSpec requestSpec;

  @Mock private RetrieveSyncSpec retrieveSyncSpec;

  @Mock private ClientGraphQlResponse clientGraphQlResponse;

  Course course =
      Course.builder()
          .id(1L)
          .installationId("12345")
          .orgName("test-org")
          .courseName("Test Course")
          .term("Fall 2023")
          .school("UCSB")
          .build();

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
  }

  @AfterEach
  public void tearDown() {
    Mockito.reset(
        jwtService,
        graphQlClient,
        graphQlClientBuilder,
        requestSpec,
        retrieveSyncSpec,
        clientGraphQlResponse); // Reset specific mocks
  }

  @Test
  public void testGetDefaultBranchName() throws Exception {
    assertNotNull(course);
    when(jwtService.getInstallationToken(eq(course))).thenReturn("mocked-token");

    when(graphQlClient.mutate()).thenReturn(graphQlClientBuilder);
    when(graphQlClientBuilder.header(anyString(), anyString())).thenReturn(graphQlClientBuilder);
    when(graphQlClientBuilder.build()).thenReturn(graphQlClient);
    when(graphQlClient.document(anyString())).thenReturn(requestSpec);
    when(requestSpec.variable(anyString(), anyString())).thenReturn(requestSpec);
    when(requestSpec.retrieveSync(anyString())).thenReturn(retrieveSyncSpec);
    when(retrieveSyncSpec.toEntity(eq(String.class))).thenReturn("main");

    GithubGraphQLService githubGraphQLService = new GithubGraphQLService(graphQlClient, jwtService);
    String defaultBranchName =
        githubGraphQLService.getDefaultBranchName(course, "test-owner", "test-repo");
    assertEquals("main", defaultBranchName);
  }

  @Test
  public void testGetCommits() throws Exception {

    TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {};

    when(clientGraphQlResponse.getData()).thenReturn(GithubGraphQLFixtures.COMMITS_RESPONSE_MAP);

    when(graphQlClient.mutate()).thenReturn(graphQlClientBuilder);
    when(graphQlClientBuilder.header(anyString(), anyString())).thenReturn(graphQlClientBuilder);
    when(graphQlClientBuilder.build()).thenReturn(graphQlClient);
    when(graphQlClient.document(anyString())).thenReturn(requestSpec);
    when(requestSpec.variable(anyString(), any())).thenReturn(requestSpec);
    when(requestSpec.executeSync()).thenReturn(clientGraphQlResponse);
    when(requestSpec.retrieveSync(anyString())).thenReturn(retrieveSyncSpec);

    assertNotNull(course);
    when(jwtService.getInstallationToken(eq(course))).thenReturn("mocked-token");

    when(graphQlClient.mutate()).thenReturn(graphQlClientBuilder);
    when(graphQlClientBuilder.header(anyString(), anyString())).thenReturn(graphQlClientBuilder);
    when(graphQlClientBuilder.build()).thenReturn(graphQlClient);
    when(graphQlClient.document(anyString())).thenReturn(requestSpec);
    when(requestSpec.variable(anyString(), anyString())).thenReturn(requestSpec);
    when(requestSpec.executeSync()).thenReturn(clientGraphQlResponse);
    when(requestSpec.retrieveSync(anyString())).thenReturn(retrieveSyncSpec);

    when(clientGraphQlResponse.getData()).thenReturn(GithubGraphQLFixtures.COMMITS_RESPONSE_MAP);

    GithubGraphQLService githubGraphQLService = new GithubGraphQLService(graphQlClient, jwtService);
    String commitsJson =
        githubGraphQLService.getCommits(course, "test-owner", "test-repo", "main", 10, null);

    verify(graphQlClient).mutate();
    verify(graphQlClientBuilder).header("Authorization", "Bearer mocked-token");
    verify(graphQlClientBuilder).header("Content-Type", "application/json");
    verify(graphQlClientBuilder).build();
    verify(requestSpec).variable("owner", "test-owner");
    verify(requestSpec).variable("repo", "test-repo");
    verify(requestSpec).variable("branch", "main");
    verify(requestSpec).variable("first", 10);
    verify(requestSpec).variable("after", null);
    verify(requestSpec).executeSync();
    verify(clientGraphQlResponse).getData();

    assertEquals(GithubGraphQLFixtures.COMMITS_RESPONSE.trim(), commitsJson.trim());
  }
}
