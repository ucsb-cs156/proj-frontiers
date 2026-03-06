package edu.ucsb.cs156.frontiers.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.DownloadRequest;
import edu.ucsb.cs156.frontiers.entities.DownloadedCommit;
import edu.ucsb.cs156.frontiers.errors.NoLinkedOrganizationException;
import edu.ucsb.cs156.frontiers.repositories.DownloadedCommitRepository;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.GraphQlResponse;
import org.springframework.graphql.client.HttpSyncGraphQlClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
public class GithubGraphQLService {

  private final HttpSyncGraphQlClient graphQlClient;

  private final JwtService jwtService;

  private final String githubBaseUrl = "https://api.github.com/graphql";

  private final ObjectMapper jacksonObjectMapper;
  private final DownloadedCommitRepository downloadedCommitRepository;

  public GithubGraphQLService(
      RestClient.Builder builder,
      JwtService jwtService,
      ObjectMapper jacksonObjectMapper,
      DownloadedCommitRepository downloadedCommitRepository) {
    this.jwtService = jwtService;
    this.graphQlClient =
        HttpSyncGraphQlClient.builder(builder.baseUrl(githubBaseUrl).build())
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    this.jacksonObjectMapper = jacksonObjectMapper;
    this.downloadedCommitRepository = downloadedCommitRepository;
  }

  /**
   * Retrieves the name of the default branch for a given GitHub repository.
   *
   * @param owner The owner (username or organization) of the repository.
   * @param repo The name of the repository.
   * @return A Mono emitting the default branch name, or an empty Mono if not found.
   */
  public String getDefaultBranchName(Course course, String owner, String repo)
      throws JsonProcessingException,
          NoSuchAlgorithmException,
          InvalidKeySpecException,
          NoLinkedOrganizationException {
    log.info(
        "getDefaultBranchName called with course.getId(): {} owner: {}, repo: {}",
        course.getId(),
        owner,
        repo);
    String githubToken = jwtService.getInstallationToken(course);

    // language=GraphQL
    String query =
        """
        query getDefaultBranch($owner: String!, $repo: String!) {
          repository(owner: $owner, name: $repo) {
            defaultBranchRef {
              name
            }
          }
        }
        """;

    return graphQlClient
        .mutate()
        .header("Authorization", "Bearer " + githubToken)
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build()
        .document(query)
        .variable("owner", owner)
        .variable("repo", repo)
        .retrieveSync("repository.defaultBranchRef.name")
        .toEntity(String.class);
  }

  public String getCommits(
      Course course, String owner, String repo, String branch, int first, String after)
      throws JsonProcessingException,
          NoSuchAlgorithmException,
          InvalidKeySpecException,
          NoLinkedOrganizationException {
    return getCommits(course, owner, repo, branch, null, null, first, after);
  }

  /**
   * Retrieves the commit history for a specified branch of a GitHub repository within a given time
   * range.
   *
   * @param course The course entity, used to fetch the associated GitHub installation token.
   * @param owner The owner of the GitHub repository.
   * @param repo The name of the GitHub repository.
   * @param branch The branch of the repository for which the commit history is retrieved.
   * @param since The start time for fetching commits (inclusive). Optional. Can be null.
   * @param until The end time for fetching commits (exclusive). Optional. Can be null.
   * @param size The maximum number of commits to retrieve in one request.
   * @param cursor The pagination cursor pointing to the start of the commit history to fetch.
   *     Optional. Can be null.
   * @return A JSON string representing the commit history and associated metadata.
   * @throws NoLinkedOrganizationException If no linked organization exists for the specified
   *     course.
   */
  public String getCommits(
      Course course,
      String owner,
      String repo,
      String branch,
      Instant since,
      Instant until,
      int size,
      String cursor)
      throws JsonProcessingException,
          NoSuchAlgorithmException,
          InvalidKeySpecException,
          NoLinkedOrganizationException {
    String githubToken = jwtService.getInstallationToken(course);
    // language=GraphQL
    String query =
        """
            query GetBranchCommits($owner: String!, $repo: String!, $branch: String!, $first: Int!, $after: String, $since: GitTimestamp, $until: GitTimestamp) {
              repository(owner: $owner, name: $repo) {
                ref(qualifiedName: $branch) {
                  target {
                    ... on Commit {
                      history(first: $first, after: $after, since: $since, until: $until) {
                        pageInfo {
                          hasNextPage
                          endCursor
                        }
                        edges {
                          node {
                            oid
                            url
                            messageHeadline
                            committedDate
                            author {
                              name
                              email
                              user {
                                login
                              }
                            }
                            committer {
                              name
                              email
                              user {
                                login
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
            """;

    GraphQlResponse response =
        graphQlClient
            .mutate()
            .header("Authorization", "Bearer " + githubToken)
            .build()
            .document(query)
            .variable("owner", owner)
            .variable("repo", repo)
            .variable("branch", branch)
            .variable("first", size)
            .variable("after", cursor)
            .variable("since", since)
            .variable("until", until)
            .executeSync();

    Map<String, Object> data = response.getData();
    String jsonData = jacksonObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
    return jsonData;
  }

  public void downloadCommitHistory(DownloadRequest downloadRequest)
      throws NoSuchAlgorithmException, InvalidKeySpecException, JsonProcessingException {
    String pointer = null;
    boolean hasNextPage;
    List<DownloadedCommit> downloadedCommits = new ArrayList<>(4000);
    do {
      JsonNode currentPage =
          jacksonObjectMapper.readTree(
              getCommits(
                  downloadRequest.getCourse(),
                  downloadRequest.getOrg(),
                  downloadRequest.getRepo(),
                  downloadRequest.getBranch(),
                  downloadRequest.getStartDate(),
                  downloadRequest.getEndDate(),
                  100,
                  pointer));
      pointer =
          currentPage
              .path("repository")
              .path("ref")
              .path("target")
              .path("history")
              .path("pageInfo")
              .path("endCursor")
              .asText();
      hasNextPage =
          currentPage
              .path("repository")
              .path("ref")
              .path("target")
              .path("history")
              .path("pageInfo")
              .path("hasNextPage")
              .asBoolean();
      JsonNode commits =
          currentPage.path("repository").path("ref").path("target").path("history").path("edges");
      for (JsonNode node : commits) {
        DownloadedCommit newCommit =
            jacksonObjectMapper.treeToValue(node.get("node"), DownloadedCommit.class);
        newCommit.setRequest(downloadRequest);
        downloadedCommits.add(newCommit);
      }

    } while (hasNextPage);
    downloadedCommitRepository.saveAll(downloadedCommits);
  }
}
