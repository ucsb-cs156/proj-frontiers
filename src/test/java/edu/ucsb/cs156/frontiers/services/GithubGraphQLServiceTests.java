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
  //  final String response =
  //      """
  //          {
  //             "data": {
  //               "repository": {
  //                 "ref": {
  //                   "target": {
  //                     "history": {
  //                       "pageInfo": {
  //                         "hasNextPage": false,
  //                         "endCursor": "thereIsStillAnEndCursor"
  //                       },
  //                       "edges": [
  //                         {
  //                           "node": {
  //                             "oid": "9df9217b7f66102d0dcaecf48ef48af16facb058",
  //                             "url":
  // "https://github.com/ucsb-cs156/proj-frontiers/commit/9df9217b7f66102d0dcaecf48ef48af16facb058",
  //                             "messageHeadline": "dj - added actual constraint validating
  // dependency, modified unit tes…",
  //                             "committedDate": "2026-01-27T00:55:00Z",
  //                             "author": {
  //                               "name": "Daniel Jensen",
  //                               "email": "djensen2@outlook.com",
  //                               "user": {
  //                                 "login": "Division7"
  //                               }
  //                             },
  //                             "committer": {
  //                               "name": "Daniel Jensen",
  //                               "email": "djensen2@outlook.com",
  //                               "user": {
  //                                 "login": "Division7"
  //                               }
  //                             }
  //                           }
  //                         },
  //                         {
  //                           "node": {
  //                             "oid": "f0497a983b1533f4b7f9f2779030d3fa62fd6031",
  //                             "url":
  // "https://github.com/ucsb-cs156/proj-frontiers/commit/f0497a983b1533f4b7f9f2779030d3fa62fd6031",
  //                             "messageHeadline": "dj - removed files outside of our normal unit
  // testing area",
  //                             "committedDate": "2026-01-26T00:58:12Z",
  //                             "author": {
  //                               "name": "Daniel Jensen",
  //                               "email": "djensen2@outlook.com",
  //                               "user": {
  //                                 "login": "Division7"
  //                               }
  //                             },
  //                             "committer": {
  //                               "name": "Daniel Jensen",
  //                               "email": "djensen2@outlook.com",
  //                               "user": {
  //                                 "login": "Division7"
  //                               }
  //                             }
  //                           }
  //                         },
  //                         {
  //                           "node": {
  //                             "oid": "10269ddffe44c9367fb9beed5943a675ea3ba43f",
  //                             "url":
  // "https://github.com/ucsb-cs156/proj-frontiers/commit/10269ddffe44c9367fb9beed5943a675ea3ba43f",
  //                             "messageHeadline": "dj - built a currently tightly linked canvas
  // roster integration",
  //                             "committedDate": "2026-01-26T00:50:08Z",
  //                             "author": {
  //                               "name": "Daniel Jensen",
  //                               "email": "djensen2@outlook.com",
  //                               "user": {
  //                                 "login": "Division7"
  //                               }
  //                             },
  //                             "committer": {
  //                               "name": "Daniel Jensen",
  //                               "email": "djensen2@outlook.com",
  //                               "user": {
  //                                 "login": "Division7"
  //                               }
  //                             }
  //                           }
  //                         },
  //                         {
  //                           "node": {
  //                             "oid": "2497a0276ad9dcf2a7b098d30226acef61e7f069",
  //                             "url":
  // "https://github.com/ucsb-cs156/proj-frontiers/commit/2497a0276ad9dcf2a7b098d30226acef61e7f069",
  //                             "messageHeadline": "dj - refactored tests to use MockRestServer
  // hook",
  //                             "committedDate": "2026-01-25T10:16:38Z",
  //                             "author": {
  //                               "name": "Daniel Jensen",
  //                               "email": "djensen2@outlook.com",
  //                               "user": {
  //                                 "login": "Division7"
  //                               }
  //                             },
  //                             "committer": {
  //                               "name": "Daniel Jensen",
  //                               "email": "djensen2@outlook.com",
  //                               "user": {
  //                                 "login": "Division7"
  //                               }
  //                             }
  //                           }
  //                         },
  //                         {
  //                           "node": {
  //                             "oid": "e2aee37e439c609ad45abfa51e9f2edaf444c05e",
  //                             "url":
  // "https://github.com/ucsb-cs156/proj-frontiers/commit/e2aee37e439c609ad45abfa51e9f2edaf444c05e",
  //                             "messageHeadline": "Add uploadRosterFromCanvas endpoint for Canvas
  // roster upload, add tes…",
  //                             "committedDate": "2026-01-25T08:55:37Z",
  //                             "author": {
  //                               "name": "Copilot",
  //                               "email": "198982749+Copilot@users.noreply.github.com",
  //                               "user": {
  //                                 "login": "Copilot"
  //                               }
  //                             },
  //                             "committer": {
  //                               "name": "GitHub",
  //                               "email": "noreply@github.com",
  //                               "user": null
  //                             }
  //                           }
  //                         },
  //                         {
  //                           "node": {
  //                             "oid": "419b4cae580d69510ff158599cc9a3043e992c1d",
  //                             "url":
  // "https://github.com/ucsb-cs156/proj-frontiers/commit/419b4cae580d69510ff158599cc9a3043e992c1d",
  //                             "messageHeadline": "Update junie.yml",
  //                             "committedDate": "2026-01-25T08:33:34Z",
  //                             "author": {
  //                               "name": "Daniel Jensen",
  //                               "email": "djensen2@outlook.com",
  //                               "user": {
  //                                 "login": "Division7"
  //                               }
  //                             },
  //                             "committer": {
  //                               "name": "GitHub",
  //                               "email": "noreply@github.com",
  //                               "user": null
  //                             }
  //                           }
  //                         },
  //                         {
  //                           "node": {
  //                             "oid": "b4454e4ed1996590edbb7688865a99af1ff176db",
  //                             "url":
  // "https://github.com/ucsb-cs156/proj-frontiers/commit/b4454e4ed1996590edbb7688865a99af1ff176db",
  //                             "messageHeadline": "dk - Added frontend to set off create team
  // repos job (#518)",
  //                             "committedDate": "2026-01-25T08:32:51Z",
  //                             "author": {
  //                               "name": "DerekKirschbaum",
  //                               "email": "109634693+DerekKirschbaum@users.noreply.github.com",
  //                               "user": {
  //                                 "login": "DerekKirschbaum"
  //                               }
  //                             },
  //                             "committer": {
  //                               "name": "GitHub",
  //                               "email": "noreply@github.com",
  //                               "user": null
  //                             }
  //                           }
  //                         }
  //                       ]
  //                     }
  //                   }
  //                 }
  //               }
  //             }
  //           }
  //          """;
  //
  //  @Test
  //  public void onePageOnly_stops_short() throws Exception {
  //    ZonedDateTime retrievedTime = ZonedDateTime.parse("2023-01-01T00:00:00Z");
  //    CommitHistory history =
  //        CommitHistory.builder()
  //            .owner("ucsb-cs156")
  //            .repo("proj-frontiers")
  //            .retrievedTime(retrievedTime)
  //            .build();
  //    Commit firstCommit =
  //        Commit.builder()
  //            .commitTime(
  //                ZonedDateTime.parse("2026-01-27T00:55:00Z")
  //                    .withZoneSameInstant(ZoneId.systemDefault()))
  //            .message("dj - added actual constraint validating dependency, modified unit tes…")
  //            .authorEmail("djensen2@outlook.com")
  //            .authorLogin("Division7")
  //            .authorName("Daniel Jensen")
  //            .url(
  //
  // "https://github.com/ucsb-cs156/proj-frontiers/commit/9df9217b7f66102d0dcaecf48ef48af16facb058")
  //            .committerEmail("djensen2@outlook.com")
  //            .committerLogin("Division7")
  //            .committerName("Daniel Jensen")
  //            .build();
  //
  //    history.getCommits().add(firstCommit);
  //    history.setCount(1);
  //
  // when(provider.getNow()).thenReturn(Optional.of(ZonedDateTime.parse("2023-01-01T00:00:00Z")));
  //    when(jwtService.getInstallationToken(eq(course))).thenReturn("mocked-token");
  //
  //    mockServer
  //        .expect(requestTo("https://api.github.com/graphql"))
  //        .andExpect(method(HttpMethod.POST))
  //        .andExpect(header("Authorization", "Bearer mocked-token"))
  //        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
  //        .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
  //
  //    CommitHistory returned =
  //        githubGraphQLService.returnCommitHistory(course, "ucsb-cs156", "proj-frontiers", "main",
  // 1);
  //
  //    assertEquals(history, returned);
  //    mockServer.verify();
  //  }
  //
  //  // language=JSON
  //  final String responsePageOne =
  //      """
  //          {
  //             "data": {
  //               "repository": {
  //                 "ref": {
  //                   "target": {
  //                     "history": {
  //                       "pageInfo": {
  //                         "hasNextPage": true,
  //                         "endCursor": "page2"
  //                       },
  //                       "edges": [
  //                         {
  //                           "node": {
  //                             "oid": "9df9217b7f66102d0dcaecf48ef48af16facb058",
  //                             "url":
  // "https://github.com/ucsb-cs156/proj-frontiers/commit/9df9217b7f66102d0dcaecf48ef48af16facb058",
  //                             "messageHeadline": "dj - added actual constraint validating
  // dependency, modified unit tes…",
  //                             "committedDate": "2026-01-27T00:55:00Z",
  //                             "author": {
  //                               "name": "Daniel Jensen",
  //                               "email": "djensen2@outlook.com",
  //                               "user": {
  //                                 "login": "Division7"
  //                               }
  //                             },
  //                             "committer": {
  //                               "name": "Daniel Jensen",
  //                               "email": "djensen2@outlook.com",
  //                               "user": {
  //                                 "login": "Division7"
  //                               }
  //                             }
  //                           }
  //                         }
  //                       ]
  //                     }
  //                   }
  //                 }
  //               }
  //             }
  //           }
  //          """;
  //
  //  // language=JSON
  //  final String responsePageTwo =
  //      """
  //          {
  //             "data": {
  //               "repository": {
  //                 "ref": {
  //                   "target": {
  //                     "history": {
  //                       "pageInfo": {
  //                         "hasNextPage": false,
  //                         "endCursor": "thereIsStillAnEndCursor"
  //                       },
  //                       "edges": [
  //                         {
  //                           "node": {
  //                             "oid": "f0497a983b1533f4b7f9f2779030d3fa62fd6031",
  //                             "url":
  // "https://github.com/ucsb-cs156/proj-frontiers/commit/f0497a983b1533f4b7f9f2779030d3fa62fd6031",
  //                             "messageHeadline": "dj - removed files outside of our normal unit
  // testing area",
  //                             "committedDate": "2026-01-26T00:58:12Z",
  //                             "author": {
  //                               "name": "Daniel Jensen",
  //                               "email": "djensen2@outlook.com",
  //                               "user": {
  //                                 "login": "Division7"
  //                               }
  //                             },
  //                             "committer": {
  //                               "name": "Daniel Jensen",
  //                               "email": "djensen2@outlook.com",
  //                               "user": {
  //                                 "login": "Division7"
  //                               }
  //                             }
  //                           }
  //                         }
  //                       ]
  //                     }
  //                   }
  //                 }
  //               }
  //             }
  //           }
  //          """;
  //
  //  @Test
  //  public void can_parse_two_pages() throws Exception {
  //    ZonedDateTime retrievedTime = ZonedDateTime.parse("2023-01-01T00:00:00Z");
  //    CommitHistory history =
  //        CommitHistory.builder()
  //            .owner("ucsb-cs156")
  //            .repo("proj-frontiers")
  //            .retrievedTime(retrievedTime)
  //            .build();
  //    Commit firstCommit =
  //        Commit.builder()
  //            .commitTime(
  //                ZonedDateTime.parse("2026-01-27T00:55:00Z")
  //                    .withZoneSameInstant(ZoneId.systemDefault()))
  //            .message("dj - added actual constraint validating dependency, modified unit tes…")
  //            .authorEmail("djensen2@outlook.com")
  //            .authorLogin("Division7")
  //            .authorName("Daniel Jensen")
  //            .url(
  //
  // "https://github.com/ucsb-cs156/proj-frontiers/commit/9df9217b7f66102d0dcaecf48ef48af16facb058")
  //            .committerEmail("djensen2@outlook.com")
  //            .committerLogin("Division7")
  //            .committerName("Daniel Jensen")
  //            .build();
  //
  //    Commit secondCommit =
  //        Commit.builder()
  //            .commitTime(
  //                ZonedDateTime.parse("2026-01-26T00:58:12Z")
  //                    .withZoneSameInstant(ZoneId.systemDefault()))
  //            .message("dj - removed files outside of our normal unit testing area")
  //            .authorEmail("djensen2@outlook.com")
  //            .authorLogin("Division7")
  //            .authorName("Daniel Jensen")
  //            .url(
  //
  // "https://github.com/ucsb-cs156/proj-frontiers/commit/f0497a983b1533f4b7f9f2779030d3fa62fd6031")
  //            .committerEmail("djensen2@outlook.com")
  //            .committerLogin("Division7")
  //            .committerName("Daniel Jensen")
  //            .build();
  //
  //    history.getCommits().add(firstCommit);
  //    history.getCommits().add(secondCommit);
  //    history.setCount(2);
  //
  // when(provider.getNow()).thenReturn(Optional.of(ZonedDateTime.parse("2023-01-01T00:00:00Z")));
  //    when(jwtService.getInstallationToken(eq(course))).thenReturn("mocked-token");
  //
  //    mockServer
  //        .expect(requestTo("https://api.github.com/graphql"))
  //        .andExpect(method(HttpMethod.POST))
  //        .andExpect(header("Authorization", "Bearer mocked-token"))
  //        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
  //        .andRespond(withSuccess(responsePageOne, MediaType.APPLICATION_JSON));
  //
  //    mockServer
  //        .expect(requestTo("https://api.github.com/graphql"))
  //        .andExpect(method(HttpMethod.POST))
  //        .andExpect(header("Authorization", "Bearer mocked-token"))
  //        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
  //        .andExpect(jsonPath("$.variables.after").value("page2"))
  //        .andRespond(withSuccess(responsePageTwo, MediaType.APPLICATION_JSON));
  //
  //    CommitHistory returned =
  //        githubGraphQLService.returnCommitHistory(course, "ucsb-cs156", "proj-frontiers", "main",
  // 2);
  //
  //    assertEquals(history, returned);
  //    mockServer.verify();
  //  }
  //
  //  @Test
  //  public void early_return_on_done() throws Exception {
  //    ZonedDateTime retrievedTime = ZonedDateTime.parse("2023-01-01T00:00:00Z");
  //    CommitHistory history =
  //        CommitHistory.builder()
  //            .owner("ucsb-cs156")
  //            .repo("proj-frontiers")
  //            .retrievedTime(retrievedTime)
  //            .build();
  //    Commit firstCommit =
  //        Commit.builder()
  //            .commitTime(
  //                ZonedDateTime.parse("2026-01-27T00:55:00Z")
  //                    .withZoneSameInstant(ZoneId.systemDefault()))
  //            .message("dj - added actual constraint validating dependency, modified unit tes…")
  //            .authorEmail("djensen2@outlook.com")
  //            .authorLogin("Division7")
  //            .authorName("Daniel Jensen")
  //            .url(
  //
  // "https://github.com/ucsb-cs156/proj-frontiers/commit/9df9217b7f66102d0dcaecf48ef48af16facb058")
  //            .committerEmail("djensen2@outlook.com")
  //            .committerLogin("Division7")
  //            .committerName("Daniel Jensen")
  //            .build();
  //
  //    history.getCommits().add(firstCommit);
  //    history.setCount(1);
  //
  // when(provider.getNow()).thenReturn(Optional.of(ZonedDateTime.parse("2023-01-01T00:00:00Z")));
  //    when(jwtService.getInstallationToken(eq(course))).thenReturn("mocked-token");
  //
  //    mockServer
  //        .expect(requestTo("https://api.github.com/graphql"))
  //        .andExpect(method(HttpMethod.POST))
  //        .andExpect(header("Authorization", "Bearer mocked-token"))
  //        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
  //        .andRespond(withSuccess(responsePageOne, MediaType.APPLICATION_JSON));
  //
  //    CommitHistory returned =
  //        githubGraphQLService.returnCommitHistory(course, "ucsb-cs156", "proj-frontiers", "main",
  // 1);
  //
  //    assertEquals(history, returned);
  //    mockServer.verify();
  //  }
  //
  //  @Test
  //  public void behaves_correctly_on_lack_of_commits() throws Exception {
  //    ZonedDateTime retrievedTime = ZonedDateTime.parse("2023-01-01T00:00:00Z");
  //    CommitHistory history =
  //        CommitHistory.builder()
  //            .owner("ucsb-cs156")
  //            .repo("proj-frontiers")
  //            .retrievedTime(retrievedTime)
  //            .build();
  //    Commit firstCommit =
  //        Commit.builder()
  //            .commitTime(
  //                ZonedDateTime.parse("2026-01-27T00:55:00Z")
  //                    .withZoneSameInstant(ZoneId.systemDefault()))
  //            .message("dj - added actual constraint validating dependency, modified unit tes…")
  //            .authorEmail("djensen2@outlook.com")
  //            .authorLogin("Division7")
  //            .authorName("Daniel Jensen")
  //            .url(
  //
  // "https://github.com/ucsb-cs156/proj-frontiers/commit/9df9217b7f66102d0dcaecf48ef48af16facb058")
  //            .committerEmail("djensen2@outlook.com")
  //            .committerLogin("Division7")
  //            .committerName("Daniel Jensen")
  //            .build();
  //
  //    Commit secondCommit =
  //        Commit.builder()
  //            .commitTime(
  //                ZonedDateTime.parse("2026-01-26T00:58:12Z")
  //                    .withZoneSameInstant(ZoneId.systemDefault()))
  //            .message("dj - removed files outside of our normal unit testing area")
  //            .authorEmail("djensen2@outlook.com")
  //            .authorLogin("Division7")
  //            .authorName("Daniel Jensen")
  //            .url(
  //
  // "https://github.com/ucsb-cs156/proj-frontiers/commit/f0497a983b1533f4b7f9f2779030d3fa62fd6031")
  //            .committerEmail("djensen2@outlook.com")
  //            .committerLogin("Division7")
  //            .committerName("Daniel Jensen")
  //            .build();
  //
  //    history.getCommits().add(firstCommit);
  //    history.getCommits().add(secondCommit);
  //    history.setCount(2);
  //
  // when(provider.getNow()).thenReturn(Optional.of(ZonedDateTime.parse("2023-01-01T00:00:00Z")));
  //    when(jwtService.getInstallationToken(eq(course))).thenReturn("mocked-token");
  //
  //    mockServer
  //        .expect(requestTo("https://api.github.com/graphql"))
  //        .andExpect(method(HttpMethod.POST))
  //        .andExpect(header("Authorization", "Bearer mocked-token"))
  //        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
  //        .andRespond(withSuccess(responsePageOne, MediaType.APPLICATION_JSON));
  //
  //    mockServer
  //        .expect(requestTo("https://api.github.com/graphql"))
  //        .andExpect(method(HttpMethod.POST))
  //        .andExpect(header("Authorization", "Bearer mocked-token"))
  //        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
  //        .andExpect(jsonPath("$.variables.after").value("page2"))
  //        .andRespond(withSuccess(responsePageTwo, MediaType.APPLICATION_JSON));
  //
  //    CommitHistory returned =
  //        githubGraphQLService.returnCommitHistory(
  //            course, "ucsb-cs156", "proj-frontiers", "main", 10);
  //
  //    assertEquals(history, returned);
  //    mockServer.verify();
  //  }
}
