package edu.ucsb.cs156.frontiers.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.CourseStaff;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.Team;
import edu.ucsb.cs156.frontiers.entities.TeamMember;
import edu.ucsb.cs156.frontiers.enums.RepositoryPermissions;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
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
      JwtService jwtService, RestTemplateBuilder restTemplateBuilder, ObjectMapper mapper) {
    this.jwtService = jwtService;
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
   * @param repoPrefix prefix for the repository name (repoPrefix-teamName)
   * @param isPrivate whether the created repository should be private
   * @param permissions collaborator permissions to grant the user
   * @throws NoSuchAlgorithmException if signing fails
   * @throws InvalidKeySpecException if signing fails
   * @throws JsonProcessingException if JSON serialization fails
   */
  public void createTeamRepository(
      Course course,
      Team team,
      String repoPrefix,
      Boolean isPrivate,
      RepositoryPermissions permissions)
      throws NoSuchAlgorithmException, InvalidKeySpecException, JsonProcessingException {

    String newRepoName = repoPrefix + "-" + team.getName();
    String token = jwtService.getInstallationToken(course);

    String existenceEndpoint =
        "https://api.github.com/repos/" + course.getOrgName() + "/" + newRepoName;
    String createEndpoint = "https://api.github.com/orgs/" + course.getOrgName() + "/repos";

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

    if (team.getTeamMembers() != null) {
      for (TeamMember member : team.getTeamMembers()) {
        if (member.getRosterStudent() != null
            && member.getRosterStudent().getGithubLogin() != null) {
          String githubLogin = member.getRosterStudent().getGithubLogin();
          String provisionEndpoint =
              "https://api.github.com/repos/"
                  + course.getOrgName()
                  + "/"
                  + newRepoName
                  + "/collaborators/"
                  + githubLogin;

          try {
            Map<String, Object> provisionBody = new HashMap<>();
            provisionBody.put("permission", permissions.getApiName());
            String provisionAsJson = mapper.writeValueAsString(provisionBody);

            HttpEntity<String> provisionEntity =
                new HttpEntity<>(provisionAsJson, existenceHeaders);
            restTemplate.exchange(provisionEndpoint, HttpMethod.PUT, provisionEntity, String.class);
          } catch (HttpClientErrorException ignored) {
            // silently ignore if provisioning fails (same as before)
          }
        }
      }
    }
  }
}
