package edu.ucsb.cs156.frontiers.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs156.frontiers.entities.Branch;
import edu.ucsb.cs156.frontiers.entities.BranchId;
import edu.ucsb.cs156.frontiers.entities.Commit;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.errors.NoLinkedOrganizationException;
import edu.ucsb.cs156.frontiers.repositories.BranchRepository;
import edu.ucsb.cs156.frontiers.repositories.CommitRepository;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
public class GithubGraphQLService {

  private final HttpSyncGraphQlClient graphQlClient;

  private final JwtService jwtService;

  private final String githubBaseUrl = "https://api.github.com/graphql";

  private final DateTimeProvider dateTimeProvider;
  private final ObjectMapper jacksonObjectMapper;
  private final CommitRepository commitRepository;
  private final BranchRepository branchRepository;
  private final RestClient client;

  public GithubGraphQLService(
      RestClient.Builder builder,
      JwtService jwtService,
      DateTimeProvider dateTimeProvider,
      ObjectMapper jacksonObjectMapper,
      CommitRepository commitRepository,
      BranchRepository branchRepository) {
    this.jwtService = jwtService;
    this.graphQlClient =
        HttpSyncGraphQlClient.builder(builder.baseUrl(githubBaseUrl).build())
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    this.dateTimeProvider = dateTimeProvider;
    this.jacksonObjectMapper = jacksonObjectMapper;
    this.commitRepository = commitRepository;
    this.branchRepository = branchRepository;
    this.client = builder.baseUrl("https://api.github.com/").build();
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
                            parents {
                              totalCount
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

  /**
   * Loads the commit history for a specified repository branch. The method retrieves information
   * about the branch from the database, determines if the branch information is up to date, and
   * updates the commit history if necessary by fetching data from GitHub. If the branch does not
   * exist in the database, it is created.
   *
   * @param course the course to be authenticated against
   * @param branch the identifier of the branch to load commit history for
   * @return the {@code Branch} object representing the branch with its latest commit history
   *     information
   * @throws Exception if an error occurs while loading or updating the commit history
   */
  @Transactional(propagation = Propagation.NESTED)
  public Branch loadCommitHistory(Course course, BranchId branch) throws Exception {
    Instant retrievedTime = Instant.from(dateTimeProvider.getNow().get());

    HashMap<String, Commit> existingCommits = new HashMap<>(4000);

    Branch selectedBranch;

    Optional<Branch> existingBranch = branchRepository.findById(branch);

    if (existingBranch.isPresent()) {
      selectedBranch = existingBranch.get();
      String currentHead = getMostRecentCommitSha(course, branch);
      log.info("Branch {} already exists in database", branch);
      if (commitRepository.existsByBranchAndSha(selectedBranch, currentHead)) {
        log.info("Branch {} already exists in database and is up to date", branch);
        selectedBranch.setRetrievedTime(retrievedTime);
        branchRepository.save(selectedBranch);
        return selectedBranch;
      } else {
        log.info("Branch {} already exists in database but is out of date, updating", branch);
        commitRepository
            .streamByBranch(selectedBranch)
            .forEach(commit -> existingCommits.put(commit.getSha(), commit));
      }
    } else {
      selectedBranch = Branch.builder().id(branch).build();
      log.info("Branch {} does not exist in database, creating new branch", branch);
    }
    selectedBranch.setRetrievedTime(retrievedTime);
    branchRepository.save(selectedBranch);
    String pointer = null;
    ArrayList<Commit> commitsToBeSaved = new ArrayList<>(4000);
    boolean hasNextPage;
    do {
      JsonNode currentPage =
          jacksonObjectMapper.readTree(
              getCommits(course, branch.org(), branch.repo(), branch.branchName(), 100, pointer));
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
        String sha = node.get("node").get("oid").asText();
        if (existingCommits.containsKey(sha)) {
          continue;
        } else {
          Commit commit = jacksonObjectMapper.treeToValue(node.get("node"), Commit.class);
          commit.setBranch(selectedBranch);
          commitsToBeSaved.add(commit);
        }
      }

    } while (hasNextPage);
    commitRepository.saveAll(commitsToBeSaved);
    return selectedBranch;
  }

  public String getMostRecentCommitSha(Course course, BranchId branch) throws Exception {
    String token = jwtService.getInstallationToken(course);
    ResponseEntity<JsonNode> response =
        client
            .get()
            .uri(
                "/repos/" + branch.org() + "/" + branch.repo() + "/branches/" + branch.branchName())
            .header("Authorization", "Bearer " + token)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .retrieve()
            .toEntity(JsonNode.class);
    String commitSha = response.getBody().path("commit").path("sha").asText();
    return commitSha;
  }
}
