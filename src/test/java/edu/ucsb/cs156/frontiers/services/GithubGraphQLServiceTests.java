package edu.ucsb.cs156.frontiers.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.fixtures.GithubGraphQLFixtures;
import edu.ucsb.cs156.frontiers.testconfig.TestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.client.MockRestServiceServer;

@RestClientTest(GithubGraphQLService.class)
@Import({TestConfig.class})
public class GithubGraphQLServiceTests {

  @Autowired private MockRestServiceServer mockServer;

  @Autowired private GithubGraphQLService githubGraphQLService;

  @MockitoBean private JwtService jwtService;

  Course course =
      Course.builder()
          .id(1L)
          .installationId("12345")
          .orgName("test-org")
          .courseName("Test Course")
          .term("Fall 2023")
          .school("UCSB")
          .build();

  @Test
  public void testGetDefaultBranchName() throws Exception {
    when(jwtService.getInstallationToken(eq(course))).thenReturn("mocked-token");

    String graphqlResponse =
        """
        {"data": {"repository": {"defaultBranchRef": {"name": "main"}}}}
        """;

    mockServer
        .expect(requestTo("https://api.github.com/graphql"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer mocked-token"))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andRespond(withSuccess(graphqlResponse, MediaType.APPLICATION_JSON));

    String result = githubGraphQLService.getDefaultBranchName(course, "test-owner", "test-repo");

    mockServer.verify();
    assertEquals("main", result);
  }

  @Test
  public void testGetCommits() throws Exception {
    when(jwtService.getInstallationToken(eq(course))).thenReturn("mocked-token");

    String graphqlResponse =
        String.format("{\"data\": %s}", GithubGraphQLFixtures.COMMITS_RESPONSE.trim());

    mockServer
        .expect(requestTo("https://api.github.com/graphql"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer mocked-token"))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andRespond(withSuccess(graphqlResponse, MediaType.APPLICATION_JSON));

    String commitsJson =
        githubGraphQLService.getCommits(course, "test-owner", "test-repo", "main", 10, null);

    mockServer.verify();
    assertNotNull(commitsJson);
    assertEquals(GithubGraphQLFixtures.COMMITS_RESPONSE.trim(), commitsJson.trim());
  }
}
