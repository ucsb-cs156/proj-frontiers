package edu.ucsb.cs156.frontiers.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.graphql.client.HttpSyncGraphQlClient;
import org.springframework.graphql.client.HttpSyncGraphQlClient.Builder;
import org.springframework.graphql.client.GraphQlClient.RequestSpec;
import org.springframework.graphql.client.GraphQlClient.RetrieveSyncSpec;

import edu.ucsb.cs156.frontiers.entities.Course;

@ExtendWith(MockitoExtension.class)
public class GithubGraphQLServiceTests {

    @Mock
    private JwtService jwtService;

    @Mock
    private HttpSyncGraphQlClient graphQlClient;

    @Mock
    private HttpSyncGraphQlClient.Builder graphQlClientBuilder;

    @Mock
    private RequestSpec requestSpec;

    @Mock
    private RetrieveSyncSpec retrieveSyncSpec;

    Course course = Course.builder()
            .id(1L)
            .installationId("12345")
            .orgName("test-org")
            .courseName("Test Course")
            .term("Fall 2023")
            .school("UCSB")
            .build();

    @Test
    public void testGetDefaultBranchName() throws Exception {
        assertNotNull(course);
        when(jwtService.getInstallationToken(eq(course))).thenReturn("mocked-token");

        when(graphQlClient.mutate()).thenReturn(graphQlClientBuilder);
        when(graphQlClientBuilder.header(anyString(), anyString()))
                .thenReturn(graphQlClientBuilder);
        when(graphQlClientBuilder.build())
                .thenReturn(graphQlClient);
        when(graphQlClient.document(anyString()))
                .thenReturn(requestSpec);
        when(requestSpec.variable(anyString(), anyString()))
                .thenReturn(requestSpec);
        when(requestSpec.retrieveSync(anyString()))
                .thenReturn(retrieveSyncSpec);
        when(retrieveSyncSpec.toEntity(eq(String.class)))
                .thenReturn("main");

        GithubGraphQLService githubGraphQLService = new GithubGraphQLService(
                graphQlClient,
                jwtService);
        String defaultBranchName = githubGraphQLService.getDefaultBranchName(course, "test-owner", "test-repo");
        assertEquals("main", defaultBranchName);
    }
   

}