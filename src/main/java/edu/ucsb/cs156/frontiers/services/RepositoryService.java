package edu.ucsb.cs156.frontiers.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.CourseStaff;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.Team;
import edu.ucsb.cs156.frontiers.enums.RepositoryPermissions;
import edu.ucsb.cs156.frontiers.repositories.TeamRepository;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class RepositoryService {
  /*
   * A RestTemplate built with no timeout blocks its calling thread forever on a hung external
   * call. That's a real incident lib-jobs' single-threaded jobsExecutor hit on another app
   * (proj-scaffold): a job stuck this way permanently wedged the executor, with no way to recover
   * short of restarting the app (see lib-jobs DESIGN.md 9 -- cooperative job cancellation only
   * helps a job that reaches another checkpoint, which a truly hung thread never will). Generous
   * but finite: long enough to never trip on legitimate slowness, short enough to guarantee a job
   * can't hang forever.
   */
  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
  private static final Duration READ_TIMEOUT = Duration.ofSeconds(60);

  private final JwtService jwtService;
  private final GithubTeamService githubTeamService;
  private final TeamRepository teamRepository;
  private final RestTemplate restTemplate;
  private final ObjectMapper mapper;

  public record GithubRepository(String name, String fullName) {}

  /**
   * Result of a create-repository call: the repository name and whether the repository was newly
   * created (true) or already existed and was potentially updated (false).
   */
  public record RepositoryCreationResult(String repoName, boolean created) {}

  private HttpHeaders githubHeaders(String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Bearer " + token);
    headers.add("Accept", "application/vnd.github+json");
    headers.add("X-GitHub-Api-Version", "2022-11-28");
    return headers;
  }

  /**
   * Creates a GitHub repository for a user (student or staff), given only their GitHub login.
   *
   * <p>This helper method contains the shared logic used by both {@link
   * #createStudentRepository(Course, RosterStudent, String, Boolean, RepositoryPermissions)} and
   * {@link #createStaffRepository(Course, CourseStaff, String, Boolean, RepositoryPermissions)}.
   *
   * <ul>
   *   <li>Checks whether the repository already exists.
   *   <li>If not, creates a new repository under the course's organization.
   *   <li>Adds the user as a collaborator with the given permission level.
   * </ul>
   *
   * @param course the course whose organization the repo belongs to
   * @param githubLogin GitHub username of the student or staff member
   * @param repoPrefix prefix for the repository name (repoPrefix-githubLogin)
   * @param isPrivate whether the created repository should be private
   * @param permissions collaborator permissions to grant the user
   * @return a {@link RepositoryCreationResult} indicating the repository name and whether it was
   *     newly created, or {@link Optional#empty()} if the existence check failed unexpectedly
   * @throws NoSuchAlgorithmException if signing fails
   * @throws InvalidKeySpecException if signing fails
   * @throws JsonProcessingException if JSON serialization fails
   */
  private Optional<RepositoryCreationResult> createRepositoryForStudentOrStaff(
      Course course,
      String githubLogin,
      String repoPrefix,
      Boolean isPrivate,
      RepositoryPermissions permissions)
      throws NoSuchAlgorithmException, InvalidKeySpecException, JsonProcessingException {

    String newRepoName = repoPrefix + "-" + githubLogin;
    String token = jwtService.getInstallationToken(course);

    String existenceEndpoint =
        "https://api.github.com/repos/" + course.getOrgName() + "/" + newRepoName;
    String createEndpoint = "https://api.github.com/orgs/" + course.getOrgName() + "/repos";
    String provisionEndpoint =
        "https://api.github.com/repos/"
            + course.getOrgName()
            + "/"
            + newRepoName
            + "/collaborators/"
            + githubLogin;

    HttpHeaders existenceHeaders = githubHeaders(token);

    HttpEntity<String> existenceEntity = new HttpEntity<>(existenceHeaders);

    boolean created = false;
    try {
      restTemplate.exchange(existenceEndpoint, HttpMethod.GET, existenceEntity, String.class);
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
        HttpHeaders createHeaders = githubHeaders(token);

        Map<String, Object> body = new HashMap<>();
        body.put("name", newRepoName);
        body.put("private", isPrivate);
        String bodyAsJson = mapper.writeValueAsString(body);

        HttpEntity<String> createEntity = new HttpEntity<>(bodyAsJson, createHeaders);

        restTemplate.exchange(createEndpoint, HttpMethod.POST, createEntity, String.class);
        created = true;
      } else {
        log.warn(
            "Unexpected response code {} when checking for existence of repository {}",
            e.getStatusCode(),
            newRepoName);
        return Optional.empty();
      }
    }

    try {
      Map<String, Object> provisionBody = new HashMap<>();
      provisionBody.put("permission", permissions.getApiName());
      String provisionAsJson = mapper.writeValueAsString(provisionBody);

      HttpEntity<String> provisionEntity = new HttpEntity<>(provisionAsJson, existenceHeaders);
      restTemplate.exchange(provisionEndpoint, HttpMethod.PUT, provisionEntity, String.class);
    } catch (HttpClientErrorException ignored) {
      // silently ignore if provisioning fails (same as before)
    }
    return Optional.of(new RepositoryCreationResult(newRepoName, created));
  }

  public RepositoryService(
      JwtService jwtService,
      GithubTeamService githubTeamService,
      TeamRepository teamRepository,
      RestTemplateBuilder restTemplateBuilder,
      ObjectMapper mapper) {
    this.jwtService = jwtService;
    this.githubTeamService = githubTeamService;
    this.teamRepository = teamRepository;
    this.restTemplate =
        restTemplateBuilder.connectTimeout(CONNECT_TIMEOUT).readTimeout(READ_TIMEOUT).build();
    this.mapper = mapper;
  }

  /**
   * Creates a single student repository if it doesn't already exist, and provisions access to the
   * repository by that student
   *
   * @param course The Course in question
   * @param student RosterStudent of the student the repository should be created for
   * @param repoPrefix Name of the project or assignment. Used to title the repository, in the
   *     format repoPrefix-githubLogin
   * @param isPrivate Whether the repository is private or not
   * @return a {@link RepositoryCreationResult} indicating the repository name and whether it was
   *     newly created, or {@link Optional#empty()} if the existence check failed unexpectedly
   */
  public Optional<RepositoryCreationResult> createStudentRepository(
      Course course,
      RosterStudent student,
      String repoPrefix,
      Boolean isPrivate,
      RepositoryPermissions permissions)
      throws NoSuchAlgorithmException, InvalidKeySpecException, JsonProcessingException {
    return createRepositoryForStudentOrStaff(
        course, student.getGithubLogin(), repoPrefix, isPrivate, permissions);
  }

  /**
   * Creates a single staff repository if it doesn't already exist, and provisions access to the
   * repository by that staff member
   *
   * @param course The Course in question
   * @param staff CourseStaff of the staff the repository should be created for
   * @param repoPrefix Name of the project or assignment. Used to title the repository, in the
   *     format repoPrefix-githubLogin
   * @param isPrivate Whether the repository is private or not
   * @return a {@link RepositoryCreationResult} indicating the repository name and whether it was
   *     newly created, or {@link Optional#empty()} if the existence check failed unexpectedly
   */
  public Optional<RepositoryCreationResult> createStaffRepository(
      Course course,
      CourseStaff staff,
      String repoPrefix,
      Boolean isPrivate,
      RepositoryPermissions permissions)
      throws NoSuchAlgorithmException, InvalidKeySpecException, JsonProcessingException {

    return createRepositoryForStudentOrStaff(
        course, staff.getGithubLogin(), repoPrefix, isPrivate, permissions);
  }

  /** The name of the ruleset that requires signed commits on a repository. */
  public static final String SIGNED_COMMITS_RULESET_NAME = "Require Signed Commits";

  /**
   * Makes a repository require signed commits, or not, by giving it a ruleset named {@value
   * #SIGNED_COMMITS_RULESET_NAME}, or removing that ruleset. The ruleset requires signatures on
   * every branch except gh-pages, and is active.
   *
   * <ul>
   *   <li>required, and the repository has no such ruleset: it is created.
   *   <li>required, and the repository already has it: it is updated in place, so that it has the
   *       settings above and the repository is never without it.
   *   <li>not required, and the repository has it: it is deleted.
   *   <li>not required, and it does not: nothing is changed.
   * </ul>
   *
   * Only rulesets of the repository itself are considered, not ones inherited from the
   * organization.
   *
   * @param course the course whose organization the repository belongs to
   * @param repositoryName the name of the repository
   * @param required whether commits must be signed
   * @throws HttpStatusCodeException if GitHub refuses a request, for example because the GitHub App
   *     is not allowed to administer repositories
   */
  public void setSignedCommitsRequired(Course course, String repositoryName, boolean required)
      throws NoSuchAlgorithmException, InvalidKeySpecException, JsonProcessingException {
    String token = jwtService.getInstallationToken(course);
    String rulesetsEndpoint =
        "https://api.github.com/repos/" + course.getOrgName() + "/" + repositoryName + "/rulesets";

    ResponseEntity<String> listResponse =
        restTemplate.exchange(
            rulesetsEndpoint + "?includes_parents=false&per_page=100",
            HttpMethod.GET,
            new HttpEntity<>(githubHeaders(token)),
            String.class);
    Long existingId = null;
    for (JsonNode ruleset : mapper.readTree(listResponse.getBody())) {
      if (SIGNED_COMMITS_RULESET_NAME.equals(ruleset.path("name").asText())) {
        existingId = ruleset.path("id").asLong();
        break;
      }
    }

    if (!required) {
      if (existingId != null) {
        restTemplate.exchange(
            rulesetsEndpoint + "/" + existingId,
            HttpMethod.DELETE,
            new HttpEntity<>(githubHeaders(token)),
            String.class);
      }
      return;
    }

    Map<String, Object> body = new HashMap<>();
    body.put("name", SIGNED_COMMITS_RULESET_NAME);
    body.put("target", "branch");
    body.put("enforcement", "active");
    body.put(
        "conditions",
        Map.of(
            "ref_name",
            Map.of("include", List.of("~ALL"), "exclude", List.of("refs/heads/gh-pages"))));
    body.put("rules", List.of(Map.of("type", "required_signatures")));
    HttpEntity<String> entity =
        new HttpEntity<>(mapper.writeValueAsString(body), githubHeaders(token));
    if (existingId == null) {
      restTemplate.exchange(rulesetsEndpoint, HttpMethod.POST, entity, String.class);
    } else {
      restTemplate.exchange(
          rulesetsEndpoint + "/" + existingId, HttpMethod.PUT, entity, String.class);
    }
  }

  /**
   * Lists repositories in the course GitHub organization whose names start with the provided
   * prefix.
   */
  public List<GithubRepository> getRepositoriesMatchingPrefix(Course course, String prefix)
      throws NoSuchAlgorithmException, InvalidKeySpecException, JsonProcessingException {
    String token = jwtService.getInstallationToken(course);
    String endpoint = "https://api.github.com/orgs/" + course.getOrgName() + "/repos?per_page=100";

    HttpEntity<String> entity = new HttpEntity<>(githubHeaders(token));
    ResponseEntity<String> response =
        restTemplate.exchange(endpoint, HttpMethod.GET, entity, String.class);

    JsonNode repos = mapper.readTree(response.getBody());
    List<GithubRepository> matchingRepos = new ArrayList<>();
    for (JsonNode repo : repos) {
      String name = repo.path("name").asText();
      if (name.startsWith(prefix)) {
        matchingRepos.add(new GithubRepository(name, repo.path("full_name").asText()));
      }
    }
    return matchingRepos;
  }

  /** Returns true when the repository has no commits. */
  public boolean isRepositoryEmpty(Course course, String repositoryName)
      throws NoSuchAlgorithmException, InvalidKeySpecException, JsonProcessingException {
    String token = jwtService.getInstallationToken(course);
    String endpoint =
        "https://api.github.com/repos/"
            + course.getOrgName()
            + "/"
            + repositoryName
            + "/commits?per_page=1";

    HttpEntity<String> entity = new HttpEntity<>(githubHeaders(token));
    try {
      ResponseEntity<String> response =
          restTemplate.exchange(endpoint, HttpMethod.GET, entity, String.class);
      JsonNode commits = mapper.readTree(response.getBody());
      return commits.isArray() && commits.isEmpty();
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode().equals(HttpStatus.CONFLICT)) {
        return true;
      }
      throw e;
    }
  }

  /** Deletes a repository only when it is empty. Returns true when a delete request was sent. */
  public boolean deleteRepositoryIfEmpty(Course course, String repositoryName)
      throws NoSuchAlgorithmException, InvalidKeySpecException, JsonProcessingException {
    if (!isRepositoryEmpty(course, repositoryName)) {
      return false;
    }

    String token = jwtService.getInstallationToken(course);
    String endpoint = "https://api.github.com/repos/" + course.getOrgName() + "/" + repositoryName;

    HttpEntity<String> entity = new HttpEntity<>(githubHeaders(token));
    restTemplate.exchange(endpoint, HttpMethod.DELETE, entity, String.class);
    return true;
  }

  /**
   * Creates a GitHub repository for a team (student or staff), given only their team name.
   *
   * <ul>
   *   <li>Checks whether the repository already exists.
   *   <li>If not, creates a new repository under the course's organization.
   *   <li>Adds all team members as collaborators with the given permission level.
   * </ul>
   *
   * @param course the course whose organization the repo belongs to
   * @param team the team for which the repo is being created
   * @param repoPrefix prefix for the repository name (repoPrefix-teamSlug)
   * @param isPrivate whether the created repository should be private
   * @param permissions collaborator permissions to grant the user
   * @param orgId GitHub organization ID used for team-based repo provisioning
   * @return a {@link RepositoryCreationResult} indicating the repository name and whether it was
   *     newly created, or {@link Optional#empty()} if the existence check failed unexpectedly
   * @throws NoSuchAlgorithmException if signing fails
   * @throws InvalidKeySpecException if signing fails
   * @throws JsonProcessingException if JSON serialization fails
   */
  public Optional<RepositoryCreationResult> createTeamRepository(
      Course course,
      Team team,
      String repoPrefix,
      Boolean isPrivate,
      RepositoryPermissions permissions,
      Integer orgId)
      throws NoSuchAlgorithmException, InvalidKeySpecException, JsonProcessingException {
    String teamSlug = getOrFetchTeamSlug(course, team, orgId);
    String newRepoName = repoPrefix + "-" + teamSlug;
    String token = jwtService.getInstallationToken(course);

    String existenceEndpoint =
        "https://api.github.com/repos/" + course.getOrgName() + "/" + newRepoName;
    String createEndpoint = "https://api.github.com/orgs/" + course.getOrgName() + "/repos";
    String provisionEndpoint =
        "https://api.github.com/organizations/"
            + orgId
            + "/team/"
            + team.getGithubTeamId()
            + "/repos/"
            + course.getOrgName()
            + "/"
            + newRepoName;

    HttpHeaders existenceHeaders = githubHeaders(token);

    HttpEntity<String> existenceEntity = new HttpEntity<>(existenceHeaders);

    boolean created = false;
    try {
      restTemplate.exchange(existenceEndpoint, HttpMethod.GET, existenceEntity, String.class);
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
        HttpHeaders createHeaders = githubHeaders(token);

        Map<String, Object> body = new HashMap<>();
        body.put("name", newRepoName);
        body.put("private", isPrivate);
        String bodyAsJson = mapper.writeValueAsString(body);

        HttpEntity<String> createEntity = new HttpEntity<>(bodyAsJson, createHeaders);

        restTemplate.exchange(createEndpoint, HttpMethod.POST, createEntity, String.class);
        created = true;
      } else {
        log.warn(
            "Unexpected response code {} when checking for existence of repository {}",
            e.getStatusCode(),
            newRepoName);
        return Optional.empty();
      }
    }
    try {
      Map<String, Object> provisionBody = new HashMap<>();
      provisionBody.put("permission", permissions.getApiName());
      String provisionAsJson = mapper.writeValueAsString(provisionBody);

      HttpEntity<String> provisionEntity = new HttpEntity<>(provisionAsJson, existenceHeaders);
      restTemplate.exchange(provisionEndpoint, HttpMethod.PUT, provisionEntity, String.class);
    } catch (HttpClientErrorException ignored) {
      // silently ignore if provisioning fails (same as before)
    }
    return Optional.of(new RepositoryCreationResult(newRepoName, created));
  }

  private String getOrFetchTeamSlug(Course course, Team team, Integer orgId)
      throws JsonProcessingException, NoSuchAlgorithmException, InvalidKeySpecException {
    if (team.getGithubTeamSlug() != null && !team.getGithubTeamSlug().isBlank()) {
      return team.getGithubTeamSlug();
    }

    if (team.getGithubTeamId() == null) {
      throw new IllegalStateException(
          "Cannot create team repository without a GitHub team ID for team '"
              + team.getName()
              + "'");
    }

    GithubTeamService.GithubTeamInfo teamInfo =
        githubTeamService.getTeamInfoById(orgId, team.getGithubTeamId(), course);

    if (teamInfo == null || teamInfo.slug() == null || teamInfo.slug().isBlank()) {
      throw new IllegalStateException(
          "Cannot determine GitHub team slug for team '" + team.getName() + "'");
    }

    team.setGithubTeamSlug(teamInfo.slug());
    teamRepository.save(team);
    return teamInfo.slug();
  }
}
