package edu.ucsb.cs156.frontiers.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.Team;
import edu.ucsb.cs156.frontiers.enums.TeamStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
public class GithubTeamServiceTests {

  @Mock private JwtService jwtService;
  @Mock private RestTemplate restTemplate;
  @Mock private RestTemplateBuilder restTemplateBuilder;

  private GithubTeamService githubTeamService;
  private ObjectMapper objectMapper;

  @BeforeEach
  public void setup() {
    when(restTemplateBuilder.build()).thenReturn(restTemplate);
    objectMapper = new ObjectMapper();
    githubTeamService = new GithubTeamService(jwtService, objectMapper, restTemplateBuilder);
  }

  @Test
  public void testCreateOrGetTeamId_WhenTeamExists() throws Exception {
    // Arrange
    Course course = Course.builder().orgName("test-org").installationId("123").build();
    Team team = Team.builder().name("test-team").build();
    String token = "test-token";
    String existingTeamResponse = "{\"id\": 456, \"name\": \"test-team\"}";

    when(jwtService.getInstallationToken(course)).thenReturn(token);
    when(restTemplate.exchange(
            eq("https://api.github.com/orgs/test-org/teams/test-team"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)))
        .thenReturn(new ResponseEntity<>(existingTeamResponse, HttpStatus.OK));

    // Act
    Integer result = githubTeamService.createOrGetTeamId(team, course);

    // Assert
    assertEquals(456, result);
    verify(jwtService).getInstallationToken(course);
    verify(restTemplate)
        .exchange(
            eq("https://api.github.com/orgs/test-org/teams/test-team"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class));
  }

  @Test
  public void testCreateOrGetTeamId_WhenTeamDoesNotExist() throws Exception {
    // Arrange
    Course course = Course.builder().orgName("test-org").installationId("123").build();
    Team team = Team.builder().name("test-team").build();
    String token = "test-token";
    String createTeamResponse = "{\"id\": 789, \"name\": \"test-team\"}";

    when(jwtService.getInstallationToken(course)).thenReturn(token);
    when(restTemplate.exchange(
            eq("https://api.github.com/orgs/test-org/teams/test-team"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)))
        .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));
    when(restTemplate.exchange(
            eq("https://api.github.com/orgs/test-org/teams"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(String.class)))
        .thenReturn(new ResponseEntity<>(createTeamResponse, HttpStatus.CREATED));

    // Act
    Integer result = githubTeamService.createOrGetTeamId(team, course);

    // Assert
    assertEquals(789, result);
    verify(jwtService, times(2)).getInstallationToken(course);
    verify(restTemplate)
        .exchange(
            eq("https://api.github.com/orgs/test-org/teams/test-team"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class));
    verify(restTemplate)
        .exchange(
            eq("https://api.github.com/orgs/test-org/teams"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(String.class));
  }

  @Test
  public void testGetTeamId_WhenTeamExists() throws Exception {
    // Arrange
    Course course = Course.builder().orgName("test-org").installationId("123").build();
    String token = "test-token";
    String response = "{\"id\": 456, \"name\": \"test-team\"}";

    when(jwtService.getInstallationToken(course)).thenReturn(token);
    when(restTemplate.exchange(
            eq("https://api.github.com/orgs/test-org/teams/test-team"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)))
        .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

    // Act
    Integer result = githubTeamService.getTeamId("test-team", course);

    // Assert
    assertEquals(456, result);
  }

  @Test
  public void testGetTeamId_WhenTeamDoesNotExist() throws Exception {
    // Arrange
    Course course = Course.builder().orgName("test-org").installationId("123").build();
    String token = "test-token";

    when(jwtService.getInstallationToken(course)).thenReturn(token);
    when(restTemplate.exchange(
            eq("https://api.github.com/orgs/test-org/teams/test-team"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)))
        .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

    // Act
    Integer result = githubTeamService.getTeamId("test-team", course);

    // Assert
    assertNull(result);
  }

  @Test
  public void testGetTeamMembershipStatus_NoGithubId() throws Exception {
    // Arrange
    Course course = Course.builder().orgName("test-org").installationId("123").build();

    // Act
    TeamStatus result = githubTeamService.getTeamMembershipStatus(null, 456, course);

    // Assert
    assertEquals(TeamStatus.NO_GITHUB_ID, result);
  }

  @Test
  public void testGetTeamMembershipStatus_TeamMember() throws Exception {
    // Arrange
    Course course = Course.builder().orgName("test-org").installationId("123").build();
    String token = "test-token";
    String response = "{\"role\": \"member\", \"state\": \"active\"}";

    when(jwtService.getInstallationToken(course)).thenReturn(token);
    when(restTemplate.exchange(
            eq("https://api.github.com/orgs/test-org/teams/456/memberships/testuser"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)))
        .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

    // Act
    TeamStatus result = githubTeamService.getTeamMembershipStatus("testuser", 456, course);

    // Assert
    assertEquals(TeamStatus.TEAM_MEMBER, result);
  }

  @Test
  public void testGetTeamMembershipStatus_TeamMaintainer() throws Exception {
    // Arrange
    Course course = Course.builder().orgName("test-org").installationId("123").build();
    String token = "test-token";
    String response = "{\"role\": \"maintainer\", \"state\": \"active\"}";

    when(jwtService.getInstallationToken(course)).thenReturn(token);
    when(restTemplate.exchange(
            eq("https://api.github.com/orgs/test-org/teams/456/memberships/testuser"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)))
        .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

    // Act
    TeamStatus result = githubTeamService.getTeamMembershipStatus("testuser", 456, course);

    // Assert
    assertEquals(TeamStatus.TEAM_MAINTAINER, result);
  }

  @Test
  public void testGetTeamMembershipStatus_NotOrgMember() throws Exception {
    // Arrange
    Course course = Course.builder().orgName("test-org").installationId("123").build();
    String token = "test-token";

    when(jwtService.getInstallationToken(course)).thenReturn(token);
    when(restTemplate.exchange(
            eq("https://api.github.com/orgs/test-org/teams/456/memberships/testuser"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)))
        .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

    // Act
    TeamStatus result = githubTeamService.getTeamMembershipStatus("testuser", 456, course);

    // Assert
    assertEquals(TeamStatus.NOT_ORG_MEMBER, result);
  }

  @Test
  public void testAddTeamMember_AsMember() throws Exception {
    // Arrange
    Course course = Course.builder().orgName("test-org").installationId("123").build();
    String token = "test-token";
    String response = "{\"role\": \"member\", \"state\": \"active\"}";

    when(jwtService.getInstallationToken(course)).thenReturn(token);
    when(restTemplate.exchange(
            eq("https://api.github.com/orgs/test-org/teams/456/memberships/testuser"),
            eq(HttpMethod.PUT),
            any(HttpEntity.class),
            eq(String.class)))
        .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

    // Act
    TeamStatus result = githubTeamService.addTeamMember("testuser", 456, "member", course);

    // Assert
    assertEquals(TeamStatus.TEAM_MEMBER, result);
  }

  @Test
  public void testAddTeamMember_AsMaintainer() throws Exception {
    // Arrange
    Course course = Course.builder().orgName("test-org").installationId("123").build();
    String token = "test-token";
    String response = "{\"role\": \"maintainer\", \"state\": \"active\"}";

    when(jwtService.getInstallationToken(course)).thenReturn(token);
    when(restTemplate.exchange(
            eq("https://api.github.com/orgs/test-org/teams/456/memberships/testuser"),
            eq(HttpMethod.PUT),
            any(HttpEntity.class),
            eq(String.class)))
        .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

    // Act
    TeamStatus result = githubTeamService.addTeamMember("testuser", 456, "maintainer", course);

    // Assert
    assertEquals(TeamStatus.TEAM_MAINTAINER, result);
  }
}
