package edu.ucsb.cs156.frontiers.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import edu.ucsb.cs156.frontiers.entities.Branch;
import edu.ucsb.cs156.frontiers.entities.BranchId;
import edu.ucsb.cs156.frontiers.entities.Commit;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.fixtures.GithubGraphQLFixtures;
import edu.ucsb.cs156.frontiers.repositories.BranchRepository;
import edu.ucsb.cs156.frontiers.repositories.CommitRepository;
import edu.ucsb.cs156.frontiers.testconfig.TestConfig;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.auditing.DateTimeProvider;
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

  @MockitoBean private DateTimeProvider provider;

  @MockitoBean private BranchRepository branchRepository;

  @MockitoBean private CommitRepository commitRepository;

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
  final String existingBranchResponse =
      """
          {
            "name": "main",
            "commit": {
              "sha": "9df9217b7f66102d0dcaecf48ef48af16facb058",
              "url": "https://github.com/ucsb-cs156/proj-frontiers/commit/9df9217b7f66102d0dcaecf48ef48af16facb058"
            }
          }
      """;

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
                             "parents": {
                               "totalCount": 1
                             },
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
                             "parents": {
                               "totalCount": 1
                             },
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

  @Test
  public void can_parse_two_pages() throws Exception {
    Instant retrievedTime = Instant.parse("2023-01-01T00:00:00Z");
    BranchId branchId = new BranchId("ucsb-cs156", "proj-frontiers", "main");
    Branch finishedHistory = Branch.builder().id(branchId).retrievedTime(retrievedTime).build();
    Commit firstCommit =
        Commit.builder()
            .commitTime(Instant.parse("2026-01-27T00:55:00Z"))
            .message("dj - added actual constraint validating dependency, modified unit tes…")
            .authorEmail("djensen2@outlook.com")
            .authorLogin("Division7")
            .authorName("Daniel Jensen")
            .branch(finishedHistory)
            .url(
                "https://github.com/ucsb-cs156/proj-frontiers/commit/9df9217b7f66102d0dcaecf48ef48af16facb058")
            .committerEmail("djensen2@outlook.com")
            .committerLogin("Division7")
            .committerName("Daniel Jensen")
            .sha("9df9217b7f66102d0dcaecf48ef48af16facb058")
            .isMergeCommit(false)
            .build();

    Commit secondCommit =
        Commit.builder()
            .commitTime(Instant.parse("2026-01-26T00:58:12Z"))
            .message("dj - removed files outside of our normal unit testing area")
            .authorEmail("djensen2@outlook.com")
            .authorLogin("Division7")
            .authorName("Daniel Jensen")
            .branch(finishedHistory)
            .url(
                "https://github.com/ucsb-cs156/proj-frontiers/commit/f0497a983b1533f4b7f9f2779030d3fa62fd6031")
            .committerEmail("djensen2@outlook.com")
            .committerLogin("Division7")
            .committerName("Daniel Jensen")
            .sha("f0497a983b1533f4b7f9f2779030d3fa62fd6031")
            .isMergeCommit(false)
            .build();

    when(provider.getNow()).thenReturn(Optional.of(retrievedTime));
    when(jwtService.getInstallationToken(eq(course))).thenReturn("mocked-token");
    when(branchRepository.findById(branchId)).thenReturn(Optional.empty());
    when(branchRepository.save(eq(finishedHistory))).thenReturn(finishedHistory);

    mockServer
        .expect(requestTo("https://api.github.com/graphql"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer mocked-token"))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andRespond(withSuccess(responsePageOne, MediaType.APPLICATION_JSON));

    mockServer
        .expect(requestTo("https://api.github.com/graphql"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer mocked-token"))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.variables.after").value("page2"))
        .andRespond(withSuccess(responsePageTwo, MediaType.APPLICATION_JSON));

    Branch returned = githubGraphQLService.loadCommitHistory(course, branchId);

    verify(commitRepository, times(1)).saveAll(List.of(firstCommit, secondCommit));

    assertEquals(finishedHistory, returned);
    mockServer.verify();
  }

  @Test
  public void short_circuit_exit_behaves() throws Exception {

    Instant retrievedTime = Instant.parse("2023-01-01T00:00:00Z");
    BranchId branchId = new BranchId("ucsb-cs156", "proj-frontiers", "main");
    Branch history =
        Branch.builder().id(branchId).retrievedTime(Instant.parse("2022-05-01T00:00:00Z")).build();

    Branch updatedRetrieveTime = Branch.builder().id(branchId).retrievedTime(retrievedTime).build();

    when(provider.getNow()).thenReturn(Optional.of(Instant.parse("2023-01-01T00:00:00Z")));
    when(jwtService.getInstallationToken(eq(course))).thenReturn("mocked-token");
    when(branchRepository.findById(branchId)).thenReturn(Optional.of(history));
    when(commitRepository.existsByBranchAndSha(
            eq(history), eq("9df9217b7f66102d0dcaecf48ef48af16facb058")))
        .thenReturn(true);

    mockServer
        .expect(requestTo("https://api.github.com/repos/ucsb-cs156/proj-frontiers/branches/main"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("Authorization", "Bearer mocked-token"))
        .andRespond(withSuccess(existingBranchResponse, MediaType.APPLICATION_JSON));

    Branch returned = githubGraphQLService.loadCommitHistory(course, branchId);

    verify(branchRepository, times(1)).findById(branchId);
    verify(branchRepository, times(1)).save(eq(updatedRetrieveTime));
    verify(commitRepository, times(1))
        .existsByBranchAndSha(eq(history), eq("9df9217b7f66102d0dcaecf48ef48af16facb058"));
    verifyNoMoreInteractions(commitRepository, branchRepository);
    assertEquals(history, returned);
    mockServer.verify();
  }

  @Test
  public void loadCommitHistory_skips_existing_commits() throws Exception {
    Instant retrievedTime = Instant.parse("2023-01-01T00:00:00Z");
    BranchId branchId = new BranchId("ucsb-cs156", "proj-frontiers", "main");
    Branch startHistory =
        Branch.builder().id(branchId).retrievedTime(Instant.parse("2022-05-01T00:00:00Z")).build();
    Branch finishedHistory = Branch.builder().id(branchId).retrievedTime(retrievedTime).build();
    Commit firstCommit =
        Commit.builder()
            .commitTime(Instant.parse("2026-01-27T00:55:00Z"))
            .message("dj - added actual constraint validating dependency, modified unit tes…")
            .authorEmail("djensen2@outlook.com")
            .authorLogin("Division7")
            .authorName("Daniel Jensen")
            .branch(finishedHistory)
            .url(
                "https://github.com/ucsb-cs156/proj-frontiers/commit/9df9217b7f66102d0dcaecf48ef48af16facb058")
            .committerEmail("djensen2@outlook.com")
            .committerLogin("Division7")
            .committerName("Daniel Jensen")
            .sha("9df9217b7f66102d0dcaecf48ef48af16facb058")
            .isMergeCommit(false)
            .build();

    Commit secondCommit =
        Commit.builder()
            .commitTime(Instant.parse("2026-01-26T00:58:12Z"))
            .message("dj - removed files outside of our normal unit testing area")
            .authorEmail("djensen2@outlook.com")
            .authorLogin("Division7")
            .authorName("Daniel Jensen")
            .branch(finishedHistory)
            .url(
                "https://github.com/ucsb-cs156/proj-frontiers/commit/f0497a983b1533f4b7f9f2779030d3fa62fd6031")
            .committerEmail("djensen2@outlook.com")
            .committerLogin("Division7")
            .committerName("Daniel Jensen")
            .sha("f0497a983b1533f4b7f9f2779030d3fa62fd6031")
            .isMergeCommit(false)
            .build();

    when(provider.getNow()).thenReturn(Optional.of(Instant.parse("2023-01-01T00:00:00Z")));
    when(jwtService.getInstallationToken(eq(course))).thenReturn("mocked-token");
    when(branchRepository.findById(branchId)).thenReturn(Optional.of(startHistory));
    when(commitRepository.existsByBranchAndSha(
            eq(startHistory), eq("9df9217b7f66102d0dcaecf48ef48af16facb058")))
        .thenReturn(false);
    when(commitRepository.streamByBranch(eq(startHistory))).thenReturn(Stream.of(secondCommit));
    when(branchRepository.save(eq(finishedHistory))).thenReturn(finishedHistory);

    mockServer
        .expect(requestTo("https://api.github.com/repos/ucsb-cs156/proj-frontiers/branches/main"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("Authorization", "Bearer mocked-token"))
        .andRespond(withSuccess(existingBranchResponse, MediaType.APPLICATION_JSON));

    mockServer
        .expect(requestTo("https://api.github.com/graphql"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer mocked-token"))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andRespond(withSuccess(responsePageOne, MediaType.APPLICATION_JSON));

    mockServer
        .expect(requestTo("https://api.github.com/graphql"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer mocked-token"))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.variables.after").value("page2"))
        .andRespond(withSuccess(responsePageTwo, MediaType.APPLICATION_JSON));

    Branch response = githubGraphQLService.loadCommitHistory(course, branchId);

    verify(commitRepository, times(1)).saveAll(eq(List.of(firstCommit)));
    verify(branchRepository, times(1)).save(eq(finishedHistory));
  }
}
