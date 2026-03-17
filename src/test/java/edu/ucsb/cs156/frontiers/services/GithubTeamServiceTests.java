package edu.ucsb.cs156.frontiers.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.Team;
import edu.ucsb.cs156.frontiers.enums.TeamStatus;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
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
  public void testGithubTeamSlugIsNullByDefault() {
    GithubTeamService.GithubTeamInfo teamInfo = new GithubTeamService.GithubTeamInfo(123, "team-a");

    assertEquals(123, teamInfo.id());
    assertEquals("team-a", teamInfo.name());
    assertNull(teamInfo.slug());
  }

  @Test
  public void testCreateOrGetTeamId_WhenTeamExists() throws Exception {
    // Arrange
    Course course = Course.builder().orgName("test-org").installationId("123").build();
    Team team = Team.builder().name("test-team").githubTeamSlug("test-team").build();
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
  public void testCreateOrGetTeamInfo_WhenTeamExists() throws Exception {
    Course course = Course.builder().orgName("test-org").installationId("123").build();
    Team team = Team.builder().name("test-team").githubTeamSlug("test-team").build();
    String token = "test-token";
    String existingTeamResponse = "{\"id\": 456, \"name\": \"test-team\", \"slug\": \"test-team\"}";

    when(jwtService.getInstallationToken(course)).thenReturn(token);
    when(restTemplate.exchange(
            eq("https://api.github.com/orgs/test-org/teams/test-team"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)))
        .thenReturn(new ResponseEntity<>(existingTeamResponse, HttpStatus.OK));

    GithubTeamService.GithubTeamInfo result = githubTeamService.createOrGetTeamInfo(team, course);

    assertEquals(456, result.id());
    assertEquals("test-team", result.slug());
  }

  @Test
  public void testCreateOrGetTeamId_WhenTeamDoesNotExist() throws Exception {
    // Arrange
    Course course = Course.builder().orgName("test-org").installationId("123").build();
    Team team = Team.builder().name("test-team").githubTeamSlug("test-team").build();
    String token = "test-token";
    String createTeamResponse = "{\"id\": 789, \"name\": \"test-team\", \"slug\": \"test-team\"}";

    when(jwtService.getInstallationToken(course)).thenReturn(token);
    when(restTemplate.exchange(
            eq("https://api.github.com/orgs/test-org/teams/test-team"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)))
        .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));
    when(restTemplate.exchange(
            eq("https://api.github.com/orgs/test-org/teams?per_page=100"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)))
        .thenReturn(new ResponseEntity<>("[]", HttpStatus.OK));
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
    verify(jwtService, times(3)).getInstallationToken(course);
    verify(restTemplate)
        .exchange(
            eq("https://api.github.com/orgs/test-org/teams/test-team"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class));
    verify(restTemplate)
        .exchange(
            eq("https://api.github.com/orgs/test-org/teams?per_page=100"),
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
  public void testCreateTeamInfo_WhenTeamDoesNotExist() throws Exception {
    Course course = Course.builder().orgName("test-org").installationId("123").build();
    String token = "test-token";
    String createTeamResponse = "{\"id\": 789, \"name\": \"test-team\", \"slug\": \"test-team\"}";

    when(jwtService.getInstallationToken(course)).thenReturn(token);
    when(restTemplate.exchange(
            eq("https://api.github.com/orgs/test-org/teams"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(String.class)))
        .thenReturn(new ResponseEntity<>(createTeamResponse, HttpStatus.CREATED));

    GithubTeamService.GithubTeamInfo result = githubTeamService.createTeamInfo("test-team", course);

    assertEquals(789, result.id());
    assertEquals("test-team", result.slug());
  }

  @Test
  public void testCreateTeam_ReturnsIdWhenTeamDoesNotExist() throws Exception {
    Course course = Course.builder().orgName("test-org").installationId("123").build();
    String token = "test-token";
    String createTeamResponse = "{\"id\": 789, \"name\": \"test-team\", \"slug\": \"test-team\"}";

    when(jwtService.getInstallationToken(course)).thenReturn(token);
    when(restTemplate.exchange(
            eq("https://api.github.com/orgs/test-org/teams"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(String.class)))
        .thenReturn(new ResponseEntity<>(createTeamResponse, HttpStatus.CREATED));

    Integer result = githubTeamService.createTeam("test-team", course);

    assertEquals(789, result);
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
  public void testGetTeamInfo_WhenTeamExists() throws Exception {
    Course course = Course.builder().orgName("test-org").installationId("123").build();
    String token = "test-token";
    String response = "{\"id\": 456, \"name\": \"test-team\", \"slug\": \"test-team\"}";

    when(jwtService.getInstallationToken(course)).thenReturn(token);
    when(restTemplate.exchange(
            eq("https://api.github.com/orgs/test-org/teams/test-team"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)))
        .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

    GithubTeamService.GithubTeamInfo result = githubTeamService.getTeamInfo("test-team", course);

    assertEquals(456, result.id());
    assertEquals("test-team", result.slug());
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
    TeamStatus result = githubTeamService.getTeamMembershipStatus(null, 456, course, 1);

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
            eq("https://api.github.com/organizations/1/team/456/memberships/testuser"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)))
        .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

    // Act
    TeamStatus result = githubTeamService.getTeamMembershipStatus("testuser", 456, course, 1);

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
            eq("https://api.github.com/organizations/1/team/456/memberships/testuser"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)))
        .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

    // Act
    TeamStatus result = githubTeamService.getTeamMembershipStatus("testuser", 456, course, 1);

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
            eq("https://api.github.com/organizations/1/team/456/memberships/testuser"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)))
        .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

    // Act
    TeamStatus result = githubTeamService.getTeamMembershipStatus("testuser", 456, course, 1);

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
            eq("https://api.github.com/organizations/1/team/456/memberships/testuser"),
            eq(HttpMethod.PUT),
            any(HttpEntity.class),
            eq(String.class)))
        .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

    // Act
    TeamStatus result =
        githubTeamService.addMemberToGithubTeam("testuser", 456, "member", course, 1);

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
            eq("https://api.github.com/organizations/1/team/456/memberships/testuser"),
            eq(HttpMethod.PUT),
            any(HttpEntity.class),
            eq(String.class)))
        .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

    // Act
    TeamStatus result =
        githubTeamService.addMemberToGithubTeam("testuser", 456, "maintainer", course, 1);

    // Assert
    assertEquals(TeamStatus.TEAM_MAINTAINER, result);
  }

  @Test
  public void testGetTeamId_NonNotFoundError() throws Exception {
    // Test that non-404 errors are re-thrown
    // Arrange
    Course course = Course.builder().orgName("test-org").installationId("123").build();
    String token = "test-token";

    when(jwtService.getInstallationToken(course)).thenReturn(token);
    when(restTemplate.exchange(
            eq("https://api.github.com/orgs/test-org/teams/test-team"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)))
        .thenThrow(new HttpClientErrorException(HttpStatus.FORBIDDEN));

    // Act & Assert
    assertThrows(
        HttpClientErrorException.class,
        () -> {
          githubTeamService.getTeamId("test-team", course);
        });
  }

  @Test
  public void testGetTeamMembershipStatus_NonNotFoundError() throws Exception {
    // Test that non-404 errors are re-thrown
    // Arrange
    Course course = Course.builder().orgName("test-org").installationId("123").build();
    String token = "test-token";

    when(jwtService.getInstallationToken(course)).thenReturn(token);
    when(restTemplate.exchange(
            eq("https://api.github.com/organizations/1/team/456/memberships/testuser"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)))
        .thenThrow(new HttpClientErrorException(HttpStatus.FORBIDDEN));

    // Act & Assert
    assertThrows(
        HttpClientErrorException.class,
        () -> {
          githubTeamService.getTeamMembershipStatus("testuser", 456, course, 1);
        });
  }

  @Test
  public void testGetTeamId_VerifyHeaders() throws Exception {
    // Arrange
    Course course = Course.builder().orgName("test-org").installationId("123").build();
    String token = "test-token";
    String response = "{\"id\": 456, \"name\": \"test-team\"}";
    ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);

    when(jwtService.getInstallationToken(course)).thenReturn(token);
    when(restTemplate.exchange(
            eq("https://api.github.com/orgs/test-org/teams/test-team"),
            eq(HttpMethod.GET),
            entityCaptor.capture(),
            eq(String.class)))
        .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

    // Act
    githubTeamService.getTeamId("test-team", course);

    // Assert
    HttpEntity<String> capturedEntity = entityCaptor.getValue();
    HttpHeaders headers = capturedEntity.getHeaders();
    assertEquals("Bearer " + token, headers.getFirst("Authorization"));
    assertEquals("application/vnd.github+json", headers.getFirst("Accept"));
    assertEquals("2022-11-28", headers.getFirst("X-GitHub-Api-Version"));
  }

  @Test
  public void testCreateTeam_VerifyHeaders() throws Exception {
    // Arrange
    Course course = Course.builder().orgName("test-org").installationId("123").build();
    Team team = Team.builder().name("test-team").githubTeamSlug("test-team").build();
    String token = "test-token";
    String createTeamResponse = "{\"id\": 789, \"name\": \"test-team\", \"slug\": \"test-team\"}";
    ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);

    when(jwtService.getInstallationToken(course)).thenReturn(token);
    // First call to getTeamId returns 404
    when(restTemplate.exchange(
            eq("https://api.github.com/orgs/test-org/teams/test-team"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)))
        .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));
    when(restTemplate.exchange(
            eq("https://api.github.com/orgs/test-org/teams?per_page=100"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)))
        .thenReturn(new ResponseEntity<>("[]", HttpStatus.OK));
    // Second call to createTeam
    when(restTemplate.exchange(
            eq("https://api.github.com/orgs/test-org/teams"),
            eq(HttpMethod.POST),
            entityCaptor.capture(),
            eq(String.class)))
        .thenReturn(new ResponseEntity<>(createTeamResponse, HttpStatus.CREATED));

    // Act
    githubTeamService.createOrGetTeamId(team, course);

    // Assert
    HttpEntity<String> capturedEntity = entityCaptor.getValue();
    HttpHeaders headers = capturedEntity.getHeaders();
    assertEquals("Bearer " + token, headers.getFirst("Authorization"));
    assertEquals("application/vnd.github+json", headers.getFirst("Accept"));
    assertEquals("2022-11-28", headers.getFirst("X-GitHub-Api-Version"));
  }

  @Test
  public void testCreateOrGetTeamInfo_FallsBackToLookupByNameWhenSlugMissing() throws Exception {
    Course course = Course.builder().orgName("test-org").installationId("123").build();
    Team team = Team.builder().name("Display Name").build();
    String token = "test-token";
    String allTeamsResponse =
        "[{\"id\": 456, \"name\": \"Display Name\", \"slug\": \"display-name\"}]";

    when(jwtService.getInstallationToken(course)).thenReturn(token);
    when(restTemplate.exchange(
            eq("https://api.github.com/orgs/test-org/teams?per_page=100"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)))
        .thenReturn(new ResponseEntity<>(allTeamsResponse, HttpStatus.OK));

    GithubTeamService.GithubTeamInfo result = githubTeamService.createOrGetTeamInfo(team, course);

    assertEquals(456, result.id());
    assertEquals("display-name", result.slug());
    verify(restTemplate, never())
        .exchange(
            eq("https://api.github.com/orgs/test-org/teams"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(String.class));
  }

  @Test
  public void testGetTeamMembershipStatus_VerifyHeaders() throws Exception {
    // Arrange
    Course course = Course.builder().orgName("test-org").installationId("123").build();
    String token = "test-token";
    String response = "{\"role\": \"member\", \"state\": \"active\"}";
    ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);

    when(jwtService.getInstallationToken(course)).thenReturn(token);
    when(restTemplate.exchange(
            eq("https://api.github.com/organizations/1/team/456/memberships/testuser"),
            eq(HttpMethod.GET),
            entityCaptor.capture(),
            eq(String.class)))
        .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

    // Act
    githubTeamService.getTeamMembershipStatus("testuser", 456, course, 1);

    // Assert
    HttpEntity<String> capturedEntity = entityCaptor.getValue();
    HttpHeaders headers = capturedEntity.getHeaders();
    assertEquals("Bearer " + token, headers.getFirst("Authorization"));
    assertEquals("application/vnd.github+json", headers.getFirst("Accept"));
    assertEquals("2022-11-28", headers.getFirst("X-GitHub-Api-Version"));
  }

  @Test
  public void testAddTeamMember_VerifyHeaders() throws Exception {
    // Arrange
    Course course = Course.builder().orgName("test-org").installationId("123").build();
    String token = "test-token";
    String response = "{\"role\": \"member\", \"state\": \"active\"}";
    ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);

    when(jwtService.getInstallationToken(course)).thenReturn(token);
    when(restTemplate.exchange(
            eq("https://api.github.com/organizations/1/team/456/memberships/testuser"),
            eq(HttpMethod.PUT),
            entityCaptor.capture(),
            eq(String.class)))
        .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

    // Act
    githubTeamService.addMemberToGithubTeam("testuser", 456, "member", course, 1);

    // Assert
    HttpEntity<String> capturedEntity = entityCaptor.getValue();
    HttpHeaders headers = capturedEntity.getHeaders();
    assertEquals("Bearer " + token, headers.getFirst("Authorization"));
    assertEquals("application/vnd.github+json", headers.getFirst("Accept"));
    assertEquals("2022-11-28", headers.getFirst("X-GitHub-Api-Version"));
  }

  @Test
  public void test_getOrgId_whenOrgExists() throws Exception {
    // Arrange
    Course course = Course.builder().orgName("test-org").installationId("123").build();
    String token = "test-token";
    when(jwtService.getInstallationToken(course)).thenReturn(token);
    when(restTemplate.exchange(
            eq("https://api.github.com/orgs/test-org"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)))
        .thenReturn(new ResponseEntity<>("{\"id\": 12345}", HttpStatus.OK));

    // Act
    Integer orgId = githubTeamService.getOrgId("test-org", course);

    // Assert
    assertEquals(Integer.valueOf(12345), orgId);
    verify(jwtService).getInstallationToken(course);
    verify(restTemplate)
        .exchange(
            eq("https://api.github.com/orgs/test-org"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class));
  }

  @Test
  public void testGetOrgId_VerifyHeaders() throws Exception {
    // Arrange
    Course course = Course.builder().orgName("test-org").installationId("123").build();
    String token = "test-token";
    ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);

    when(jwtService.getInstallationToken(course)).thenReturn(token);
    when(restTemplate.exchange(
            eq("https://api.github.com/orgs/test-org"),
            eq(HttpMethod.GET),
            entityCaptor.capture(),
            eq(String.class)))
        .thenReturn(new ResponseEntity<>("{\"id\": 12345}", HttpStatus.OK));

    // Act
    githubTeamService.getOrgId("test-org", course);

    // Assert
    HttpEntity<String> capturedEntity = entityCaptor.getValue();
    HttpHeaders headers = capturedEntity.getHeaders();
    assertEquals("Bearer " + token, headers.getFirst("Authorization"));
    assertEquals("application/vnd.github+json", headers.getFirst("Accept"));
    assertEquals("2022-11-28", headers.getFirst("X-GitHub-Api-Version"));
  }

  @Test
  public void testGetAllTeams_SinglePage() throws Exception {
    Course course = Course.builder().orgName("test-org").installationId("123").build();
    String token = "test-token";
    String response = "[{\"id\": 11, \"name\": \"team-a\"}, {\"id\": 12, \"name\": \"team-b\"}]";

    when(jwtService.getInstallationToken(course)).thenReturn(token);
    when(restTemplate.exchange(
            eq("https://api.github.com/orgs/test-org/teams?per_page=100"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)))
        .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

    List<GithubTeamService.GithubTeamInfo> teams = githubTeamService.getAllTeams(course);

    assertEquals(2, teams.size());
    assertEquals(Integer.valueOf(11), teams.get(0).id());
    assertEquals("team-a", teams.get(0).name());
    assertEquals(Integer.valueOf(12), teams.get(1).id());
    assertEquals("team-b", teams.get(1).name());
  }

  @Test
  public void testGetAllTeams_VerifyHeaders() throws Exception {
    Course course = Course.builder().orgName("test-org").installationId("123").build();
    String token = "test-token";
    ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);

    when(jwtService.getInstallationToken(course)).thenReturn(token);
    when(restTemplate.exchange(
            eq("https://api.github.com/orgs/test-org/teams?per_page=100"),
            eq(HttpMethod.GET),
            entityCaptor.capture(),
            eq(String.class)))
        .thenReturn(new ResponseEntity<>("[{\"id\": 11, \"name\": \"team-a\"}]", HttpStatus.OK));

    githubTeamService.getAllTeams(course);

    HttpEntity<String> capturedEntity = entityCaptor.getValue();
    HttpHeaders headers = capturedEntity.getHeaders();
    assertEquals("Bearer " + token, headers.getFirst("Authorization"));
    assertEquals("application/vnd.github+json", headers.getFirst("Accept"));
    assertEquals("2022-11-28", headers.getFirst("X-GitHub-Api-Version"));
  }

  @Test
  public void testGetAllTeams_WithPagination() throws Exception {
    Course course = Course.builder().orgName("test-org").installationId("123").build();
    String token = "test-token";
    String firstPage = "[{\"id\": 11, \"name\": \"team-a\"}]";
    String secondPage = "[{\"id\": 12, \"name\": \"team-b\"}]";
    String nextUrl = "https://api.github.com/orgs/test-org/teams?per_page=100&page=2";

    HttpHeaders firstHeaders = new HttpHeaders();
    firstHeaders.add(
        "link",
        "<"
            + nextUrl
            + ">; rel=\"next\", <https://api.github.com/orgs/test-org/teams?per_page=100&page=2>; rel=\"last\"");
    HttpHeaders secondHeaders = new HttpHeaders();
    secondHeaders.add(
        "link",
        "<https://api.github.com/orgs/test-org/teams?per_page=100&page=1>; rel=\"previous\"");

    when(jwtService.getInstallationToken(course)).thenReturn(token);
    when(restTemplate.exchange(
            eq("https://api.github.com/orgs/test-org/teams?per_page=100"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)))
        .thenReturn(new ResponseEntity<>(firstPage, firstHeaders, HttpStatus.OK));
    when(restTemplate.exchange(
            eq(nextUrl), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
        .thenReturn(new ResponseEntity<>(secondPage, secondHeaders, HttpStatus.OK));

    List<GithubTeamService.GithubTeamInfo> teams = githubTeamService.getAllTeams(course);

    assertEquals(2, teams.size());
    assertEquals(Integer.valueOf(11), teams.get(0).id());
    assertEquals(Integer.valueOf(12), teams.get(1).id());
    verify(restTemplate)
        .exchange(
            eq("https://api.github.com/orgs/test-org/teams?per_page=100"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class));
    verify(restTemplate)
        .exchange(eq(nextUrl), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
  }

  @Test
  public void testGetTeamMemberships_MemberAndMaintainer() throws Exception {
    Course course = Course.builder().orgName("test-org").installationId("123").build();
    String token = "test-token";
    String memberResponse = "[{\"login\": \"student-a\"}]";
    String maintainerResponse = "[{\"login\": \"maintainer-a\"}]";

    when(jwtService.getInstallationToken(course)).thenReturn(token);
    when(restTemplate.exchange(
            eq(
                "https://api.github.com/orgs/test-org/teams/team-a/members?per_page=100&role=member"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)))
        .thenReturn(new ResponseEntity<>(memberResponse, HttpStatus.OK));
    when(restTemplate.exchange(
            eq(
                "https://api.github.com/orgs/test-org/teams/team-a/members?per_page=100&role=maintainer"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)))
        .thenReturn(new ResponseEntity<>(maintainerResponse, HttpStatus.OK));

    Map<String, TeamStatus> memberships = githubTeamService.getTeamMemberships("team-a", course);

    assertEquals(2, memberships.size());
    assertEquals(TeamStatus.TEAM_MEMBER, memberships.get("student-a"));
    assertEquals(TeamStatus.TEAM_MAINTAINER, memberships.get("maintainer-a"));
  }

  @Test
  public void testGetTeamMemberships_VerifyHeaders() throws Exception {
    Course course = Course.builder().orgName("test-org").installationId("123").build();
    String token = "test-token";
    String membersUrl =
        "https://api.github.com/orgs/test-org/teams/team-a/members?per_page=100&role=member";
    String maintainersUrl =
        "https://api.github.com/orgs/test-org/teams/team-a/members?per_page=100&role=maintainer";

    when(jwtService.getInstallationToken(course)).thenReturn(token);
    when(restTemplate.exchange(
            eq(membersUrl), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
        .thenReturn(new ResponseEntity<>("[{\"login\": \"student-a\"}]", HttpStatus.OK));
    when(restTemplate.exchange(
            eq(maintainersUrl), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
        .thenReturn(new ResponseEntity<>("[{\"login\": \"maintainer-a\"}]", HttpStatus.OK));

    githubTeamService.getTeamMemberships("team-a", course);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<HttpEntity<String>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    verify(restTemplate)
        .exchange(eq(membersUrl), eq(HttpMethod.GET), entityCaptor.capture(), eq(String.class));
    verify(restTemplate)
        .exchange(eq(maintainersUrl), eq(HttpMethod.GET), entityCaptor.capture(), eq(String.class));

    for (HttpEntity<String> capturedEntity : entityCaptor.getAllValues()) {
      HttpHeaders headers = capturedEntity.getHeaders();
      assertEquals("Bearer " + token, headers.getFirst("Authorization"));
      assertEquals("application/vnd.github+json", headers.getFirst("Accept"));
      assertEquals("2022-11-28", headers.getFirst("X-GitHub-Api-Version"));
    }
  }

  @Test
  public void testGetTeamMemberships_WithPagination() throws Exception {
    Course course = Course.builder().orgName("test-org").installationId("123").build();
    String token = "test-token";
    String memberPage1 = "[{\"login\": \"student-a\"}]";
    String memberPage2 = "[{\"login\": \"student-b\"}]";
    String memberNextUrl =
        "https://api.github.com/orgs/test-org/teams/team-a/members?per_page=100&role=member&page=2";
    String maintainerResponse = "[{\"login\": \"maintainer-a\"}]";

    HttpHeaders memberHeaders = new HttpHeaders();
    memberHeaders.add(
        "link",
        "<"
            + memberNextUrl
            + ">; rel=\"next\", <https://api.github.com/orgs/test-org/teams/team-a/members?per_page=100&role=member&page=2>; rel=\"last\"");
    HttpHeaders secondPageHeaders = new HttpHeaders();
    secondPageHeaders.add(
        "link",
        "<https://api.github.com/orgs/test-org/teams/team-a/members?per_page=100&role=member&page=1>; rel=\"previous\"");

    when(jwtService.getInstallationToken(course)).thenReturn(token);
    when(restTemplate.exchange(
            eq(
                "https://api.github.com/orgs/test-org/teams/team-a/members?per_page=100&role=member"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)))
        .thenReturn(new ResponseEntity<>(memberPage1, memberHeaders, HttpStatus.OK));
    when(restTemplate.exchange(
            eq(memberNextUrl), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
        .thenReturn(new ResponseEntity<>(memberPage2, secondPageHeaders, HttpStatus.OK));
    when(restTemplate.exchange(
            eq(
                "https://api.github.com/orgs/test-org/teams/team-a/members?per_page=100&role=maintainer"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)))
        .thenReturn(new ResponseEntity<>(maintainerResponse, HttpStatus.OK));

    Map<String, TeamStatus> memberships = githubTeamService.getTeamMemberships("team-a", course);

    assertEquals(3, memberships.size());
    assertEquals(TeamStatus.TEAM_MEMBER, memberships.get("student-a"));
    assertEquals(TeamStatus.TEAM_MEMBER, memberships.get("student-b"));
    assertEquals(TeamStatus.TEAM_MAINTAINER, memberships.get("maintainer-a"));
  }

  @Test
  public void testGetTeamMemberships_ThrowsExceptionWhenTeamSlugIsNull() {
    Course course = Course.builder().orgName("test-org").installationId("123").build();

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> githubTeamService.getTeamMemberships(null, course));

    assertEquals("teamSlug must be provided", exception.getMessage());
    verifyNoInteractions(jwtService);
    verifyNoInteractions(restTemplate);
  }

  @Test
  public void testGetTeamMemberships_ThrowsExceptionWhenTeamSlugIsBlank() {
    Course course = Course.builder().orgName("test-org").installationId("123").build();

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> githubTeamService.getTeamMemberships("   ", course));

    assertEquals("teamSlug must be provided", exception.getMessage());
    verifyNoInteractions(jwtService);
    verifyNoInteractions(restTemplate);
  }

  @Test
  public void testRemoveTeamMember() throws Exception {
    // Arrange
    Course course = Course.builder().orgName("test-org").installationId("123").build();
    String token = "test-token";
    String response = "{\"role\": \"member\", \"state\": \"active\"}";

    when(jwtService.getInstallationToken(course)).thenReturn(token);
    when(restTemplate.exchange(
            eq("https://api.github.com/organizations/1/team/11/memberships/testuser"),
            eq(HttpMethod.DELETE),
            any(HttpEntity.class),
            eq(String.class)))
        .thenReturn(new ResponseEntity<>(response, HttpStatus.NO_CONTENT));

    // Act
    githubTeamService.removeMemberFromGithubTeam(1, "testuser", 11, course);
  }

  @Test
  public void testRemoveTeamMember_VerifyHeaders() throws Exception {
    // Arrange
    Course course = Course.builder().orgName("test-org").installationId("123").build();
    String token = "test-token";
    String response = "{\"role\": \"member\", \"state\": \"active\"}";
    ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);

    when(jwtService.getInstallationToken(course)).thenReturn(token);
    when(restTemplate.exchange(
            eq("https://api.github.com/organizations/1/team/11/memberships/testuser"),
            eq(HttpMethod.DELETE),
            entityCaptor.capture(),
            eq(String.class)))
        .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

    // Act
    githubTeamService.removeMemberFromGithubTeam(1, "testuser", 11, course);

    // Assert
    HttpEntity<String> capturedEntity = entityCaptor.getValue();
    HttpHeaders headers = capturedEntity.getHeaders();
    assertEquals("Bearer " + token, headers.getFirst("Authorization"));
    assertEquals("application/vnd.github+json", headers.getFirst("Accept"));
    assertEquals("2022-11-28", headers.getFirst("X-GitHub-Api-Version"));
  }

  @Test
  public void testRemoveTeam() throws Exception {
    // Arrange
    Course course = Course.builder().orgName("test-org").installationId("123").build();
    String token = "test-token";
    String response = "{\"role\": \"member\", \"state\": \"active\"}";

    when(jwtService.getInstallationToken(course)).thenReturn(token);
    when(restTemplate.exchange(
            eq("https://api.github.com/organizations/1/team/11"),
            eq(HttpMethod.DELETE),
            any(HttpEntity.class),
            eq(String.class)))
        .thenReturn(new ResponseEntity<>(response, HttpStatus.NO_CONTENT));

    // Act
    githubTeamService.deleteGithubTeam(1, 11, course);
  }

  @Test
  public void testRemoveTeam_VerifyHeaders() throws Exception {
    // Arrange
    Course course = Course.builder().orgName("test-org").installationId("123").build();
    String token = "test-token";
    String response = "{\"role\": \"member\", \"state\": \"active\"}";
    ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);

    when(jwtService.getInstallationToken(course)).thenReturn(token);
    when(restTemplate.exchange(
            eq("https://api.github.com/organizations/1/team/11"),
            eq(HttpMethod.DELETE),
            entityCaptor.capture(),
            eq(String.class)))
        .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

    // Act
    githubTeamService.deleteGithubTeam(1, 11, course);

    // Assert
    HttpEntity<String> capturedEntity = entityCaptor.getValue();
    HttpHeaders headers = capturedEntity.getHeaders();
    assertEquals("Bearer " + token, headers.getFirst("Authorization"));
    assertEquals("application/vnd.github+json", headers.getFirst("Accept"));
    assertEquals("2022-11-28", headers.getFirst("X-GitHub-Api-Version"));
  }
}
