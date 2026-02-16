package edu.ucsb.cs156.frontiers.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.errors.NoLinkedOrganizationException;
import edu.ucsb.cs156.frontiers.mongo.documents.Commit;
import edu.ucsb.cs156.frontiers.mongo.documents.CommitHistory;
import edu.ucsb.cs156.frontiers.mongo.repositories.CommitHistoryRepository;
import edu.ucsb.cs156.frontiers.mongo.repositories.CommitRepository;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.Instant;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.graphql.GraphQlResponse;
import org.springframework.graphql.client.HttpSyncGraphQlClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
public class GithubGraphQLService {

  private final HttpSyncGraphQlClient graphQlClient;

  private final JwtService jwtService;

  private final String githubBaseUrl = "https://api.github.com/graphql";

  private final DateTimeProvider dateTimeProvider;
  private final ObjectMapper jacksonObjectMapper;
  private final RestClient client;
  private final CommitHistoryRepository commitHistoryRepository;
  private final CommitRepository commitRepository;

  public GithubGraphQLService(
      RestClient.Builder builder,
      JwtService jwtService,
      DateTimeProvider dateTimeProvider,
      ObjectMapper jacksonObjectMapper,
      CommitHistoryRepository commitHistoryRepository,
      CommitRepository commitRepository) {
    this.jwtService = jwtService;
    this.graphQlClient =
        HttpSyncGraphQlClient.builder(builder.baseUrl(githubBaseUrl).build())
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    this.dateTimeProvider = dateTimeProvider;
    this.jacksonObjectMapper = jacksonObjectMapper;
    this.client = builder.baseUrl("https://api.github.com/").build();
    this.commitHistoryRepository = commitHistoryRepository;
    this.commitRepository = commitRepository;
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
    log.info(
        "getCommits called with course.getId(): {} owner: {}, repo: {}, branch: {}, first: {}, after: {}",
        course.getId(),
        owner,
        repo,
        branch,
        first,
        after);
    String githubToken = jwtService.getInstallationToken(course);

    String query =
        """
            query GetBranchCommits($owner: String!, $repo: String!, $branch: String!, $first: Int!, $after: String) {
              repository(owner: $owner, name: $repo) {
                ref(qualifiedName: $branch) {
                  target {
                    ... on Commit {
                      history(first: $first, after: $after) {
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
            .variable("first", first)
            .variable("after", after)
            .executeSync();

    Map<String, Object> data = response.getData();
    String jsonData = jacksonObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
    return jsonData;
  }

  public CommitHistory updateCommitHistory(Course course, String owner, String repo, String branch)
      throws Exception {
    Optional<CommitHistory> existingHistory =
        commitHistoryRepository.findByOwnerAndRepoAndBranch(owner, repo, branch);
    String currentSha = getMostRecentCommitUrl(course, owner, repo, branch);
    CommitHistory matchingHistory;
    Instant retrievedTime = Instant.from(dateTimeProvider.getNow().get());
    if (existingHistory.isPresent()) {
      log.info(
          "Discovered cached commit history for branch {}, with head at {}",
          branch,
          existingHistory.get().getHeadSha());
      String cachedSha = existingHistory.get().getHeadSha();
      if (cachedSha.equals(currentSha)) {
        log.info("Returning cached commit history for branch {}, commit URLs match.", branch);
        return existingHistory.get();
      } else {
        log.info("Cached commit history for branch {} is stale, retrieving new history.", branch);
        matchingHistory = existingHistory.get();
        matchingHistory.setHeadSha(currentSha);
        matchingHistory.setRetrievedTime(retrievedTime);
      }
    } else {
      CommitHistory history =
          CommitHistory.builder()
              .owner(owner)
              .repo(repo)
              .branch(branch)
              .retrievedTime(retrievedTime)
              .headSha(currentSha)
              .build();
      commitHistoryRepository.save(history);
      matchingHistory = history;
    }
    String pointer = null;
    boolean hasNextPage;
    LinkedList<Commit> history = new LinkedList<>();
    do {
      JsonNode currentPage =
          jacksonObjectMapper.readTree(getCommits(course, owner, repo, branch, 100, pointer));
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
        Commit current = jacksonObjectMapper.treeToValue(node.get("node"), Commit.class);
        current.setParentBranch(matchingHistory.getId());
        history.add(current);
      }
    } while (hasNextPage);
    commitRepository.saveAll(history);
    return matchingHistory;
  }

  public String getMostRecentCommitUrl(Course course, String owner, String repo, String branch)
      throws Exception {
    String token = jwtService.getInstallationToken(course);
    ResponseEntity<JsonNode> response =
        client
            .get()
            .uri("/repos/" + owner + "/" + repo + "/branches/" + branch)
            .header("Authorization", "Bearer " + token)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .retrieve()
            .toEntity(JsonNode.class);
    String commitSha = response.getBody().path("commit").path("sha").asText();
    return commitSha;
  }
}
