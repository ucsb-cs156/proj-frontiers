package edu.ucsb.cs156.frontiers.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.DownloadRequest;
import edu.ucsb.cs156.frontiers.entities.DownloadedCommit;
import edu.ucsb.cs156.frontiers.fixtures.GithubGraphQLFixtures;
import edu.ucsb.cs156.frontiers.repositories.DownloadedCommitRepository;
import edu.ucsb.cs156.frontiers.testconfig.TestConfig;
import java.time.Instant;
import java.util.List;
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

  @MockitoBean private DownloadedCommitRepository downloadedCommitRepository;

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

  // language=JSON
  final String responsePageOne =
      """
          {
             "data": {
               "repository": {
                 "ref": {
                   "target": {
                     "history": {
                       "pageInfo": {
                         "hasNextPage": true,
                         "endCursor": "page2"
                       },
                       "edges": [
                         {
                           "node": {
                             "oid": "9df9217b7f66102d0dcaecf48ef48af16facb058",
                             "url": "https://github.com/ucsb-cs156/proj-frontiers/commit/9df9217b7f66102d0dcaecf48ef48af16facb058",
                             "messageHeadline": "dj - added actual constraint validating dependency, modified unit tes…",
                             "committedDate": "2026-01-27T00:55:00Z",
                             "author": {
                               "name": "Daniel Jensen",
                               "email": "djensen2@outlook.com",
                               "user": {
                                 "login": "Division7"
                               }
                             },
                             "committer": {
                               "name": "Daniel Jensen",
                               "email": "djensen2@outlook.com",
                               "user": {
                                 "login": "Division7"
                               }
                             }
                           }
                         }
                       ]
                     }
                   }
                 }
               }
             }
           }
          """;

  // language=JSON
  final String responsePageTwo =
      """
          {
             "data": {
               "repository": {
                 "ref": {
                   "target": {
                     "history": {
                       "pageInfo": {
                         "hasNextPage": false,
                         "endCursor": "thereIsStillAnEndCursor"
                       },
                       "edges": [
                         {
                           "node": {
                             "oid": "f0497a983b1533f4b7f9f2779030d3fa62fd6031",
                             "url": "https://github.com/ucsb-cs156/proj-frontiers/commit/f0497a983b1533f4b7f9f2779030d3fa62fd6031",
                             "messageHeadline": "dj - removed files outside of our normal unit testing area",
                             "committedDate": "2026-01-26T00:58:12Z",
                             "author": {
                               "name": "Daniel Jensen",
                               "email": "djensen2@outlook.com",
                               "user": {
                                 "login": "Division7"
                               }
                             },
                             "committer": {
                               "name": "Daniel Jensen",
                               "email": "djensen2@outlook.com",
                               "user": {
                                 "login": "Division7"
                               }
                             }
                           }
                         }
                       ]
                     }
                   }
                 }
               }
             }
           }
          """;

  public void handles_two_pages() throws Exception {
    mockServer
        .expect(requestTo("https://api.github.com/graphql"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer mocked-token"))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.variables.owner").value("ucsb-cs156"))
        .andExpect(jsonPath("$.variables.repo").value("proj-frontiers"))
        .andExpect(jsonPath("$.variables.branch").value("main"))
        .andExpect(jsonPath("$.variables.first").value(100))
        .andExpect(jsonPath("$.variables.after").value("pageOne"))
        .andExpect(jsonPath("$.variables.since").value("2023-03-11T00:00:00Z"))
        .andExpect(jsonPath("$.variables.until").value("2023-03-21T00:00:00Z"))
        .andRespond(withSuccess(responsePageOne, MediaType.APPLICATION_JSON));

    mockServer
        .expect(requestTo("https://api.github.com/graphql"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer mocked-token"))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.variables.owner").value("ucsb-cs156"))
        .andExpect(jsonPath("$.variables.repo").value("proj-frontiers"))
        .andExpect(jsonPath("$.variables.branch").value("main"))
        .andExpect(jsonPath("$.variables.first").value(100))
        .andExpect(jsonPath("$.variables.after").value("thereIsStillAnEndCursor"))
        .andExpect(jsonPath("$.variables.since").value("2023-03-11T00:00:00Z"))
        .andExpect(jsonPath("$.variables.until").value("2023-03-21T00:00:00Z"))
        .andRespond(withSuccess(responsePageOne, MediaType.APPLICATION_JSON));

    DownloadRequest request =
        DownloadRequest.builder()
            .org("ucsb-cs156")
            .repo("proj-frontiers")
            .branch("main")
            .course(course)
            .startDate(Instant.parse("2023-03-11T00:00:00Z"))
            .endDate(Instant.parse("2023-03-21T00:00:00Z"))
            .build();

    when(jwtService.getInstallationToken(eq(course))).thenReturn("mocked-token");

    DownloadedCommit firstCommit =
        DownloadedCommit.builder()
            .request(request)
            .commitUrl(
                "https://github.com/ucsb-cs156/proj-frontiers/commit/9df9217b7f66102d0dcaecf48ef48af16facb058")
            .build();

    DownloadedCommit secondCommit =
        DownloadedCommit.builder()
            .request(request)
            .commitUrl(
                "https://github.com/ucsb-cs156/proj-frontiers/commit/f0497a983b1533f4b7f9f2779030d3fa62fd6031")
            .build();

    githubGraphQLService.downloadCommitHistory(request);

    verify(downloadedCommitRepository, times(1)).saveAll(List.of(firstCommit, secondCommit));
  }

  @Test
  public void handles_null_since_and_until() throws Exception {
    mockServer
        .expect(requestTo("https://api.github.com/graphql"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer mocked-token"))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.variables.owner").value("ucsb-cs156"))
        .andExpect(jsonPath("$.variables.repo").value("proj-frontiers"))
        .andExpect(jsonPath("$.variables.branch").value("main"))
        .andExpect(jsonPath("$.variables.first").value(100))
        .andExpect(jsonPath("$.variables.after").value("pageOne"))
        .andExpect(jsonPath("$.variables.since").value("2023-03-11T00:00:00Z"))
        .andExpect(jsonPath("$.variables.until").value("2023-03-21T00:00:00Z"))
        .andRespond(withSuccess(responsePageOne, MediaType.APPLICATION_JSON));

    mockServer
        .expect(requestTo("https://api.github.com/graphql"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer mocked-token"))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.variables.owner").value("ucsb-cs156"))
        .andExpect(jsonPath("$.variables.repo").value("proj-frontiers"))
        .andExpect(jsonPath("$.variables.branch").value("main"))
        .andExpect(jsonPath("$.variables.first").value(100))
        .andExpect(jsonPath("$.variables.after").value("thereIsStillAnEndCursor"))
        .andExpect(jsonPath("$.variables.since").value("null"))
        .andExpect(jsonPath("$.variables.until").value("null"))
        .andRespond(withSuccess(responsePageTwo, MediaType.APPLICATION_JSON));

    DownloadRequest request =
        DownloadRequest.builder()
            .org("ucsb-cs156")
            .repo("proj-frontiers")
            .branch("main")
            .course(course)
            .startDate(null)
            .endDate(null)
            .build();

    when(jwtService.getInstallationToken(eq(course))).thenReturn("mocked-token");

    DownloadedCommit firstCommit =
        DownloadedCommit.builder()
            .request(request)
            .commitUrl(
                "https://github.com/ucsb-cs156/proj-frontiers/commit/9df9217b7f66102d0dcaecf48ef48af16facb058")
            .build();

    DownloadedCommit secondCommit =
        DownloadedCommit.builder()
            .request(request)
            .commitUrl(
                "https://github.com/ucsb-cs156/proj-frontiers/commit/f0497a983b1533f4b7f9f2779030d3fa62fd6031")
            .build();

    githubGraphQLService.downloadCommitHistory(request);

    verify(downloadedCommitRepository, times(1)).saveAll(List.of(firstCommit, secondCommit));
  }
}
