package edu.ucsb.cs156.frontiers.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.Team;
import edu.ucsb.cs156.frontiers.enums.TeamStatus;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class GithubTeamService {

  public record GithubTeamInfo(Integer id, String name, String slug) {
    public GithubTeamInfo(Integer id, String name) {
      this(id, name, null);
    }
  }

  public record GithubTeamMemberInfo(String login) {}

  private final JwtService jwtService;
  private final ObjectMapper objectMapper;
  private final RestTemplate restTemplate;

  public GithubTeamService(
      JwtService jwtService, ObjectMapper objectMapper, RestTemplateBuilder builder) {
    this.jwtService = jwtService;
    this.objectMapper = objectMapper;
    this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    this.restTemplate = builder.build();
  }

  /**
   * Creates a team on GitHub if it doesn't exist, or returns the existing team ID.
   *
   * @param team The team to create
   * @param course The course containing the organization
   * @return The GitHub team ID
   * @throws JsonProcessingException if there is an error processing JSON
   * @throws NoSuchAlgorithmException if there is an algorithm error
   * @throws InvalidKeySpecException if there is a key specification error
   */
  public Integer createOrGetTeamId(Team team, Course course)
      throws JsonProcessingException, NoSuchAlgorithmException, InvalidKeySpecException {
    // First check if team already exists by getting team info
    Integer existingTeamId = getTeamId(team.getName(), course);
    if (existingTeamId != null) {
      return existingTeamId;
    }

    // Create the team if it doesn't exist
    return createTeam(team.getName(), course);
  }

  /**
   * Get the org id, given the org name.
   *
   * <p>Note: in the future, it would be better to cache this value in the Course row in the
   * database at the time the Github App is linked to the org, since it doesn't change.
   *
   * @param orgName
   * @param course
   * @return
   * @throws JsonProcessingException
   * @throws NoSuchAlgorithmException
   * @throws InvalidKeySpecException
   */
  public Integer getOrgId(String orgName, Course course)
      throws JsonProcessingException, NoSuchAlgorithmException, InvalidKeySpecException {
    String endpoint = "https://api.github.com/orgs/" + orgName;
    HttpHeaders headers = new HttpHeaders();
    String token = jwtService.getInstallationToken(course);
    headers.add("Authorization", "Bearer " + token);
    headers.add("Accept", "application/vnd.github+json");
    headers.add("X-GitHub-Api-Version", "2022-11-28");
    HttpEntity<String> entity = new HttpEntity<>(headers);

    ResponseEntity<String> response =
        restTemplate.exchange(endpoint, HttpMethod.GET, entity, String.class);
    JsonNode responseJson = objectMapper.readTree(response.getBody());
    return responseJson.get("id").asInt();
  }

  /**
   * Gets the team ID for a team name, returns null if team doesn't exist.
   *
   * @param teamName The name of the team
   * @param course The course containing the organization
   * @return The GitHub team ID or null if not found
   * @throws JsonProcessingException if there is an error processing JSON
   * @throws NoSuchAlgorithmException if there is an algorithm error
   * @throws InvalidKeySpecException if there is a key specification error
   */
  public Integer getTeamId(String teamName, Course course)
      throws JsonProcessingException, NoSuchAlgorithmException, InvalidKeySpecException {
    String endpoint = "https://api.github.com/orgs/" + course.getOrgName() + "/teams/" + teamName;
    HttpHeaders headers = new HttpHeaders();
    String token = jwtService.getInstallationToken(course);
    headers.add("Authorization", "Bearer " + token);
    headers.add("Accept", "application/vnd.github+json");
    headers.add("X-GitHub-Api-Version", "2022-11-28");
    HttpEntity<String> entity = new HttpEntity<>(headers);

    try {
      ResponseEntity<String> response =
          restTemplate.exchange(endpoint, HttpMethod.GET, entity, String.class);
      JsonNode responseJson = objectMapper.readTree(response.getBody());
      return responseJson.get("id").asInt();
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode().value() == 404) {
        return null; // Team doesn't exist
      }
      throw e;
    }
  }

  /**
   * Creates a new team on GitHub.
   *
   * @param teamName The name of the team to create
   * @param course The course containing the organization
   * @return The GitHub team ID
   * @throws JsonProcessingException if there is an error processing JSON
   * @throws NoSuchAlgorithmException if there is an algorithm error
   * @throws InvalidKeySpecException if there is a key specification error
   */
  public Integer createTeam(String teamName, Course course)
      throws JsonProcessingException, NoSuchAlgorithmException, InvalidKeySpecException {
    String endpoint = "https://api.github.com/orgs/" + course.getOrgName() + "/teams";
    HttpHeaders headers = new HttpHeaders();
    String token = jwtService.getInstallationToken(course);
    headers.add("Authorization", "Bearer " + token);
    headers.add("Accept", "application/vnd.github+json");
    headers.add("X-GitHub-Api-Version", "2022-11-28");

    Map<String, Object> body = new HashMap<>();
    body.put("name", teamName);
    body.put("privacy", "closed"); // Teams are private by default
    String bodyAsJson = objectMapper.writeValueAsString(body);
    HttpEntity<String> entity = new HttpEntity<>(bodyAsJson, headers);

    ResponseEntity<String> response =
        restTemplate.exchange(endpoint, HttpMethod.POST, entity, String.class);
    JsonNode responseJson = objectMapper.readTree(response.getBody());
    Integer teamId = responseJson.get("id").asInt();
    log.info(
        "Created team '{}' with ID {} in organization {}", teamName, teamId, course.getOrgName());
    return teamId;
  }

  /**
   * Deletes a team on GitHub.
   *
   * @param orgId The ID of the organization
   * @param githubTeamId The ID of the team to delete
   * @param course The course containing the organization
   * @throws JsonProcessingException if there is an error processing JSON
   * @throws NoSuchAlgorithmException if there is an algorithm error
   * @throws InvalidKeySpecException if there is a key specification error
   */
  public void deleteGithubTeam(Integer orgId, Integer teamId, Course course)
      throws JsonProcessingException, NoSuchAlgorithmException, InvalidKeySpecException {
    String endpoint = "https://api.github.com/organizations/" + orgId + "/team/" + teamId;
    HttpHeaders headers = new HttpHeaders();
    String token = jwtService.getInstallationToken(course);
    headers.add("Authorization", "Bearer " + token);
    headers.add("Accept", "application/vnd.github+json");
    headers.add("X-GitHub-Api-Version", "2022-11-28");
    HttpEntity<String> entity = new HttpEntity<>(headers);

    restTemplate.exchange(endpoint, HttpMethod.DELETE, entity, String.class);
    log.info("Deleted team with ID {} in organization {}", teamId, course.getOrgName());
  }

  /**
   * Gets the current team membership status for a user.
   *
   * @param githubLogin The GitHub login of the user
   * @param teamId The GitHub team ID
   * @param course The course containing the organization
   * @return The team status of the user
   * @throws JsonProcessingException if there is an error processing JSON
   * @throws NoSuchAlgorithmException if there is an algorithm error
   * @throws InvalidKeySpecException if there is a key specification error
   */
  public TeamStatus getTeamMembershipStatus(String githubLogin, Integer teamId, Course course)
      throws JsonProcessingException, NoSuchAlgorithmException, InvalidKeySpecException {
    if (githubLogin == null) {
      return TeamStatus.NO_GITHUB_ID;
    }

    String endpoint =
        "https://api.github.com/orgs/"
            + course.getOrgName()
            + "/teams/"
            + teamId
            + "/memberships/"
            + githubLogin;
    HttpHeaders headers = new HttpHeaders();
    String token = jwtService.getInstallationToken(course);
    headers.add("Authorization", "Bearer " + token);
    headers.add("Accept", "application/vnd.github+json");
    headers.add("X-GitHub-Api-Version", "2022-11-28");
    HttpEntity<String> entity = new HttpEntity<>(headers);

    try {
      ResponseEntity<String> response =
          restTemplate.exchange(endpoint, HttpMethod.GET, entity, String.class);
      JsonNode responseJson = objectMapper.readTree(response.getBody());
      String role = responseJson.get("role").asText();
      return "maintainer".equalsIgnoreCase(role)
          ? TeamStatus.TEAM_MAINTAINER
          : TeamStatus.TEAM_MEMBER;
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode().value() == 404) {
        return TeamStatus.NOT_ORG_MEMBER; // User is not a member of the team
      }
      throw e;
    }
  }

  /**
   * Adds a member to a GitHub team.
   *
   * @param githubLogin The GitHub login of the user to add
   * @param teamId The GitHub team ID
   * @param role The role to assign ("member" or "maintainer")
   * @param course The course containing the organization
   * @return The resulting team status
   * @throws JsonProcessingException if there is an error processing JSON
   * @throws NoSuchAlgorithmException if there is an algorithm error
   * @throws InvalidKeySpecException if there is a key specification error
   */
  public TeamStatus addMemberToGithubTeam(
      String githubLogin, Integer teamId, String role, Course course, Integer orgId)
      throws JsonProcessingException, NoSuchAlgorithmException, InvalidKeySpecException {
    String endpoint =
        "https://api.github.com/organizations/"
            + orgId
            + "/team/"
            + teamId
            + "/memberships/"
            + githubLogin;
    HttpHeaders headers = new HttpHeaders();
    String token = jwtService.getInstallationToken(course);
    headers.add("Authorization", "Bearer " + token);
    headers.add("Accept", "application/vnd.github+json");
    headers.add("X-GitHub-Api-Version", "2022-11-28");

    Map<String, Object> body = new HashMap<>();
    body.put("role", role);
    String bodyAsJson = objectMapper.writeValueAsString(body);
    HttpEntity<String> entity = new HttpEntity<>(bodyAsJson, headers);

    ResponseEntity<String> response =
        restTemplate.exchange(endpoint, HttpMethod.PUT, entity, String.class);
    JsonNode responseJson = objectMapper.readTree(response.getBody());
    String resultRole = responseJson.get("role").asText();
    log.info("Added user '{}' to team ID {} with role '{}'", githubLogin, teamId, resultRole);
    return "maintainer".equalsIgnoreCase(resultRole)
        ? TeamStatus.TEAM_MAINTAINER
        : TeamStatus.TEAM_MEMBER;
  }

  /**
   * Removes a member from a GitHub team
   *
   * @param orgId
   * @param githubLogin
   * @param teamId
   * @param course
   * @throws NoSuchAlgorithmException
   * @throws InvalidKeySpecException
   * @throws JsonProcessingException
   */
  public void removeMemberFromGithubTeam(
      Integer orgId, String githubLogin, Integer teamId, Course course)
      throws NoSuchAlgorithmException, InvalidKeySpecException, JsonProcessingException {

    String endpoint =
        "https://api.github.com/organizations/"
            + orgId
            + "/team/"
            + teamId
            + "/memberships/"
            + githubLogin;
    HttpHeaders headers = new HttpHeaders();
    String token = jwtService.getInstallationToken(course);
    headers.add("Authorization", "Bearer " + token);
    headers.add("Accept", "application/vnd.github+json");
    headers.add("X-GitHub-Api-Version", "2022-11-28");
    HttpEntity<String> entity = new HttpEntity<>(headers);

    restTemplate.exchange(endpoint, HttpMethod.DELETE, entity, String.class);
    log.info("Successfully removed member {} from team ID {}", githubLogin, teamId);
  }

  /**
   * Returns all team members for a GitHub team, grouped by membership role.
   *
   * @param teamId The GitHub team ID
   * @param course The course containing the organization
   * @return A map of github login to TeamStatus for members and maintainers
   * @throws NoSuchAlgorithmException if there is an algorithm error
   * @throws InvalidKeySpecException if there is a key specification error
   * @throws JsonProcessingException if there is an error processing JSON
   */
  public Map<String, TeamStatus> getTeamMemberships(String teamSlug, Course course)
      throws NoSuchAlgorithmException, InvalidKeySpecException, JsonProcessingException {
    Map<String, TeamStatus> memberships = new HashMap<>();
    for (GithubTeamMemberInfo member : getTeamMembersByRole(teamSlug, course, "member")) {
      memberships.put(member.login(), TeamStatus.TEAM_MEMBER);
    }
    for (GithubTeamMemberInfo member : getTeamMembersByRole(teamSlug, course, "maintainer")) {
      memberships.put(member.login(), TeamStatus.TEAM_MAINTAINER);
    }
    return memberships;
  }

  private List<GithubTeamMemberInfo> getTeamMembersByRole(
      String teamSlug, Course course, String role)
      throws NoSuchAlgorithmException, InvalidKeySpecException, JsonProcessingException {
    if (teamSlug == null || teamSlug.isBlank()) {
      throw new IllegalArgumentException("teamSlug must be provided");
    }
    String endpoint =
        "https://api.github.com/orgs/"
            + course.getOrgName()
            + "/teams/"
            + teamSlug
            + "/members?per_page=100&role="
            + role;
    Pattern pattern = Pattern.compile("(?<=<)([\\S]*)(?=>; rel=\"next\")");

    HttpHeaders headers = new HttpHeaders();
    String token = jwtService.getInstallationToken(course);
    headers.add("Authorization", "Bearer " + token);
    headers.add("Accept", "application/vnd.github+json");
    headers.add("X-GitHub-Api-Version", "2022-11-28");
    HttpEntity<String> entity = new HttpEntity<>(headers);

    ResponseEntity<String> response =
        restTemplate.exchange(endpoint, HttpMethod.GET, entity, String.class);
    List<GithubTeamMemberInfo> members = new ArrayList<>();

    while (true) {
      members.addAll(
          objectMapper.convertValue(
              objectMapper.readTree(response.getBody()),
              new TypeReference<List<GithubTeamMemberInfo>>() {}));

      List<String> responseLinks = response.getHeaders().getOrEmpty("link");
      if (responseLinks.isEmpty() || !responseLinks.getFirst().contains("next")) {
        break;
      }

      Matcher matcher = pattern.matcher(responseLinks.getFirst());
      if (!matcher.find()) {
        break;
      }

      response = restTemplate.exchange(matcher.group(0), HttpMethod.GET, entity, String.class);
    }

    return members;
  }

  /**
   * Returns all teams for an organization, following pagination links when present.
   *
   * @param course The course containing the organization
   * @return A list of GitHub teams with id and name
   * @throws NoSuchAlgorithmException if there is an algorithm error
   * @throws InvalidKeySpecException if there is a key specification error
   * @throws JsonProcessingException if there is an error processing JSON
   */
  public List<GithubTeamInfo> getAllTeams(Course course)
      throws NoSuchAlgorithmException, InvalidKeySpecException, JsonProcessingException {
    String endpoint = "https://api.github.com/orgs/" + course.getOrgName() + "/teams?per_page=100";
    Pattern pattern = Pattern.compile("(?<=<)([\\S]*)(?=>; rel=\"next\")");

    HttpHeaders headers = new HttpHeaders();
    String token = jwtService.getInstallationToken(course);
    headers.add("Authorization", "Bearer " + token);
    headers.add("Accept", "application/vnd.github+json");
    headers.add("X-GitHub-Api-Version", "2022-11-28");
    HttpEntity<String> entity = new HttpEntity<>(headers);

    ResponseEntity<String> response =
        restTemplate.exchange(endpoint, HttpMethod.GET, entity, String.class);
    List<GithubTeamInfo> teams = new ArrayList<>();

    while (true) {
      teams.addAll(
          objectMapper.convertValue(
              objectMapper.readTree(response.getBody()),
              new TypeReference<List<GithubTeamInfo>>() {}));

      List<String> responseLinks = response.getHeaders().getOrEmpty("link");
      if (responseLinks.isEmpty() || !responseLinks.getFirst().contains("next")) {
        break;
      }

      Matcher matcher = pattern.matcher(responseLinks.getFirst());
      if (!matcher.find()) {
        break;
      }

      response = restTemplate.exchange(matcher.group(0), HttpMethod.GET, entity, String.class);
    }

    return teams;
  }
}
