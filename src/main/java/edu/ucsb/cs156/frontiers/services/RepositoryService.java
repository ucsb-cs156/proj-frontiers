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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class RepositoryService {
  private final JwtService jwtService;
  private final GithubTeamService githubTeamService;
  private final TeamRepository teamRepository;
  private final RestTemplate restTemplate;
  private final ObjectMapper mapper;

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
   * @throws NoSuchAlgorithmException if signing fails
   * @throws InvalidKeySpecException if signing fails
   * @throws JsonProcessingException if JSON serialization fails
   */
  private void createRepositoryForStudentOrStaff(
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

    HttpHeaders existenceHeaders = new HttpHeaders();
    existenceHeaders.add("Authorization", "Bearer " + token);
    existenceHeaders.add("Accept", "application/vnd.github+json");
    existenceHeaders.add("X-GitHub-Api-Version", "2022-11-28");

    HttpEntity<String> existenceEntity = new HttpEntity<>(existenceHeaders);

    try {
      restTemplate.exchange(existenceEndpoint, HttpMethod.GET, existenceEntity, String.class);
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
        HttpHeaders createHeaders = new HttpHeaders();
        createHeaders.add("Authorization", "Bearer " + token);
        createHeaders.add("Accept", "application/vnd.github+json");
        createHeaders.add("X-GitHub-Api-Version", "2022-11-28");

        Map<String, Object> body = new HashMap<>();
        body.put("name", newRepoName);
        body.put("private", isPrivate);
        String bodyAsJson = mapper.writeValueAsString(body);

        HttpEntity<String> createEntity = new HttpEntity<>(bodyAsJson, createHeaders);

        restTemplate.exchange(createEndpoint, HttpMethod.POST, createEntity, String.class);
      } else {
        log.warn(
            "Unexpected response code {} when checking for existence of repository {}",
            e.getStatusCode(),
            newRepoName);
        return;
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
    this.restTemplate = restTemplateBuilder.build();
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
   */
  public void createStudentRepository(
      Course course,
      RosterStudent student,
      String repoPrefix,
      Boolean isPrivate,
      RepositoryPermissions permissions)
      throws NoSuchAlgorithmException, InvalidKeySpecException, JsonProcessingException {
    createRepositoryForStudentOrStaff(
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
   */
  public void createStaffRepository(
      Course course,
      CourseStaff staff,
      String repoPrefix,
      Boolean isPrivate,
      RepositoryPermissions permissions)
      throws NoSuchAlgorithmException, InvalidKeySpecException, JsonProcessingException {

    createRepositoryForStudentOrStaff(
        course, staff.getGithubLogin(), repoPrefix, isPrivate, permissions);
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
   * @throws NoSuchAlgorithmException if signing fails
   * @throws InvalidKeySpecException if signing fails
   * @throws JsonProcessingException if JSON serialization fails
   */
  public void createTeamRepository(
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

    HttpHeaders existenceHeaders = new HttpHeaders();
    existenceHeaders.add("Authorization", "Bearer " + token);
    existenceHeaders.add("Accept", "application/vnd.github+json");
    existenceHeaders.add("X-GitHub-Api-Version", "2022-11-28");

    HttpEntity<String> existenceEntity = new HttpEntity<>(existenceHeaders);

    try {
      restTemplate.exchange(existenceEndpoint, HttpMethod.GET, existenceEntity, String.class);
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
        HttpHeaders createHeaders = new HttpHeaders();
        createHeaders.add("Authorization", "Bearer " + token);
        createHeaders.add("Accept", "application/vnd.github+json");
        createHeaders.add("X-GitHub-Api-Version", "2022-11-28");

        Map<String, Object> body = new HashMap<>();
        body.put("name", newRepoName);
        body.put("private", isPrivate);
        String bodyAsJson = mapper.writeValueAsString(body);

        HttpEntity<String> createEntity = new HttpEntity<>(bodyAsJson, createHeaders);

        restTemplate.exchange(createEndpoint, HttpMethod.POST, createEntity, String.class);
      } else {
        log.warn(
            "Unexpected response code {} when checking for existence of repository {}",
            e.getStatusCode(),
            newRepoName);
        return;
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

  /** Gets all repositories in the course's organization that start with the given prefix. */
  public List<String> getRepoNamesWithPrefix(Course course, String prefix)
      throws JsonProcessingException, NoSuchAlgorithmException, InvalidKeySpecException {
    String token = jwtService.getInstallationToken(course);
    // Grabs up to 100 repos (adjust if your org has more repos and you need pagination)
    String endpoint = "https://api.github.com/orgs/" + course.getOrgName() + "/repos?per_page=100";

    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Bearer " + token);
    headers.add("Accept", "application/vnd.github+json");
    headers.add("X-GitHub-Api-Version", "2022-11-28");

    HttpEntity<String> entity = new HttpEntity<>(headers);
    ResponseEntity<String> response =
        restTemplate.exchange(endpoint, HttpMethod.GET, entity, String.class);

    JsonNode root = mapper.readTree(response.getBody());
    List<String> matchingRepos = new ArrayList<>();

    for (JsonNode repoNode : root) {
      String name = repoNode.get("name").asText();
      if (name.startsWith(prefix)) {
        matchingRepos.add(name);
      }
    }
    return matchingRepos;
  }

  /** Checks if a repository has any commits. */
  public boolean repoHasCommits(Course course, String repoName)
      throws JsonProcessingException, NoSuchAlgorithmException, InvalidKeySpecException {
    String token = jwtService.getInstallationToken(course);
    String endpoint =
        "https://api.github.com/repos/"
            + course.getOrgName()
            + "/"
            + repoName
            + "/commits?per_page=1";

    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Bearer " + token);
    headers.add("Accept", "application/vnd.github+json");
    headers.add("X-GitHub-Api-Version", "2022-11-28");

    HttpEntity<String> entity = new HttpEntity<>(headers);

    try {
      ResponseEntity<String> response =
          restTemplate.exchange(endpoint, HttpMethod.GET, entity, String.class);
      // If it returns 200 OK, there are commits
      return response.getStatusCode().is2xxSuccessful();
    } catch (HttpClientErrorException e) {
      // GitHub throws a 409 Conflict when you ask for commits on a completely empty repo
      if (e.getStatusCode() == HttpStatus.CONFLICT) {
        return false;
      }
      throw e; // Re-throw if it's a different error (like 404 Not Found)
    }
  }

  /** Deletes a repository using the GitHub REST API. */
  public void deleteRepository(Course course, String repoName)
      throws JsonProcessingException, NoSuchAlgorithmException, InvalidKeySpecException {
    String token = jwtService.getInstallationToken(course);
    String endpoint = "https://api.github.com/repos/" + course.getOrgName() + "/" + repoName;

    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Bearer " + token);
    headers.add("Accept", "application/vnd.github+json");
    headers.add("X-GitHub-Api-Version", "2022-11-28");

    HttpEntity<String> entity = new HttpEntity<>(headers);
    restTemplate.exchange(endpoint, HttpMethod.DELETE, entity, String.class);
  }
}
