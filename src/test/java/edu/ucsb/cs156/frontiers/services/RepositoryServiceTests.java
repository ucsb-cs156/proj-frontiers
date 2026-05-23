package edu.ucsb.cs156.frontiers.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.CourseStaff;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.Team;
import edu.ucsb.cs156.frontiers.entities.TeamMember;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.enums.RepositoryPermissions;
import edu.ucsb.cs156.frontiers.repositories.TeamRepository;
import edu.ucsb.cs156.frontiers.services.wiremock.WiremockService;
import edu.ucsb.cs156.frontiers.testconfig.TestConfig;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;

@RestClientTest(RepositoryService.class)
@Import(TestConfig.class)
public class RepositoryServiceTests {
  @MockitoBean private JwtService jwtService;
  @MockitoBean private GithubTeamService githubTeamService;
  @MockitoBean private TeamRepository teamRepository;

  @Autowired private MockRestServiceServer mockRestServiceServer;

  @MockitoBean private WiremockService wiremockService;

  @Autowired RepositoryService repositoryService;

  @Autowired private ObjectMapper objectMapper;

  Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();

  @BeforeEach
  public void setup() throws Exception {
    doReturn("real.installation.token").when(jwtService).getInstallationToken(eq(course));
  }

  @Test
  public void repo_already_exists_test() throws Exception {
    mockRestServiceServer
        .expect(requestTo("https://api.github.com/repos/ucsb-cs156/repo1-student1"))
        .andExpect(header("Authorization", "Bearer real.installation.token"))
        .andExpect(header("Accept", "application/vnd.github+json"))
        .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess());

    Map<String, Object> provisionBody = new HashMap<>();
    provisionBody.put("permission", "push");
    String provisionBodyJson = objectMapper.writeValueAsString(provisionBody);
    mockRestServiceServer
        .expect(
            requestTo(
                "https://api.github.com/repos/ucsb-cs156/repo1-student1/collaborators/student1"))
        .andExpect(header("Authorization", "Bearer real.installation.token"))
        .andExpect(header("Accept", "application/vnd.github+json"))
        .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
        .andExpect(method(HttpMethod.PUT))
        .andExpect(content().json(provisionBodyJson))
        .andRespond(withNoContent());

    RosterStudent student = RosterStudent.builder().githubLogin("student1").build();

    repositoryService.createStudentRepository(
        course, student, "repo1", false, RepositoryPermissions.WRITE);
    mockRestServiceServer.verify();
  }

  @Test
  public void successfully_creates_repo_public() throws Exception {
    mockRestServiceServer
        .expect(requestTo("https://api.github.com/repos/ucsb-cs156/repo1-student1"))
        .andExpect(header("Authorization", "Bearer real.installation.token"))
        .andExpect(header("Accept", "application/vnd.github+json"))
        .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withResourceNotFound());

    Map<String, Object> createBody = new HashMap<>();
    createBody.put("name", "repo1-student1");
    createBody.put("private", false);
    String createBodyJson = objectMapper.writeValueAsString(createBody);

    mockRestServiceServer
        .expect(requestTo("https://api.github.com/orgs/ucsb-cs156/repos"))
        .andExpect(header("Authorization", "Bearer real.installation.token"))
        .andExpect(header("Accept", "application/vnd.github+json"))
        .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().json(createBodyJson))
        .andRespond(withSuccess());

    Map<String, Object> provisionBody = new HashMap<>();
    provisionBody.put("permission", "admin");
    String provisionBodyJson = objectMapper.writeValueAsString(provisionBody);
    mockRestServiceServer
        .expect(
            requestTo(
                "https://api.github.com/repos/ucsb-cs156/repo1-student1/collaborators/student1"))
        .andExpect(header("Authorization", "Bearer real.installation.token"))
        .andExpect(header("Accept", "application/vnd.github+json"))
        .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
        .andExpect(method(HttpMethod.PUT))
        .andExpect(content().json(provisionBodyJson))
        .andRespond(withSuccess());

    RosterStudent student = RosterStudent.builder().githubLogin("student1").build();

    repositoryService.createStudentRepository(
        course, student, "repo1", false, RepositoryPermissions.ADMIN);

    mockRestServiceServer.verify();
  }

  @Test
  public void successfully_creates_repo_private() throws Exception {
    mockRestServiceServer
        .expect(requestTo("https://api.github.com/repos/ucsb-cs156/repo1-student1"))
        .andExpect(header("Authorization", "Bearer real.installation.token"))
        .andExpect(header("Accept", "application/vnd.github+json"))
        .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withResourceNotFound());

    Map<String, Object> createBody = new HashMap<>();
    createBody.put("name", "repo1-student1");
    createBody.put("private", true);
    String createBodyJson = objectMapper.writeValueAsString(createBody);

    mockRestServiceServer
        .expect(requestTo("https://api.github.com/orgs/ucsb-cs156/repos"))
        .andExpect(header("Authorization", "Bearer real.installation.token"))
        .andExpect(header("Accept", "application/vnd.github+json"))
        .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().json(createBodyJson))
        .andRespond(withSuccess());

    Map<String, Object> provisionBody = new HashMap<>();
    provisionBody.put("permission", "push");
    String provisionBodyJson = objectMapper.writeValueAsString(provisionBody);
    mockRestServiceServer
        .expect(
            requestTo(
                "https://api.github.com/repos/ucsb-cs156/repo1-student1/collaborators/student1"))
        .andExpect(header("Authorization", "Bearer real.installation.token"))
        .andExpect(header("Accept", "application/vnd.github+json"))
        .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
        .andExpect(method(HttpMethod.PUT))
        .andExpect(content().json(provisionBodyJson))
        .andRespond(withSuccess());

    RosterStudent student = RosterStudent.builder().githubLogin("student1").build();

    repositoryService.createStudentRepository(
        course, student, "repo1", true, RepositoryPermissions.WRITE);

    mockRestServiceServer.verify();
  }

  @Test
  public void exits_if_not_not_found() throws Exception {
    mockRestServiceServer
        .expect(requestTo("https://api.github.com/repos/ucsb-cs156/repo1-student1"))
        .andExpect(header("Authorization", "Bearer real.installation.token"))
        .andExpect(header("Accept", "application/vnd.github+json"))
        .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withForbiddenRequest());

    RosterStudent student = RosterStudent.builder().githubLogin("student1").build();

    repositoryService.createStudentRepository(
        course, student, "repo1", false, RepositoryPermissions.WRITE);
    mockRestServiceServer.verify();
  }

  @Test
  public void warn_on_not_no_content() throws Exception {
    mockRestServiceServer
        .expect(requestTo("https://api.github.com/repos/ucsb-cs156/repo1-student1"))
        .andExpect(header("Authorization", "Bearer real.installation.token"))
        .andExpect(header("Accept", "application/vnd.github+json"))
        .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess());

    Map<String, Object> provisionBody = new HashMap<>();
    provisionBody.put("permission", "push");
    String provisionBodyJson = objectMapper.writeValueAsString(provisionBody);
    mockRestServiceServer
        .expect(
            requestTo(
                "https://api.github.com/repos/ucsb-cs156/repo1-student1/collaborators/student1"))
        .andExpect(header("Authorization", "Bearer real.installation.token"))
        .andExpect(header("Accept", "application/vnd.github+json"))
        .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
        .andExpect(method(HttpMethod.PUT))
        .andExpect(content().json(provisionBodyJson))
        .andRespond(withForbiddenRequest());

    RosterStudent student = RosterStudent.builder().githubLogin("student1").build();

    repositoryService.createStudentRepository(
        course, student, "repo1", false, RepositoryPermissions.WRITE);
    mockRestServiceServer.verify();
  }

  @Test
  public void successfully_creates_staff_repo_public() throws Exception {
    mockRestServiceServer
        .expect(requestTo("https://api.github.com/repos/ucsb-cs156/repo1-staff1"))
        .andExpect(header("Authorization", "Bearer real.installation.token"))
        .andExpect(header("Accept", "application/vnd.github+json"))
        .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withResourceNotFound());

    Map<String, Object> createBody = new HashMap<>();
    createBody.put("name", "repo1-staff1");
    createBody.put("private", false);
    String createBodyJson = objectMapper.writeValueAsString(createBody);

    mockRestServiceServer
        .expect(requestTo("https://api.github.com/orgs/ucsb-cs156/repos"))
        .andExpect(header("Authorization", "Bearer real.installation.token"))
        .andExpect(header("Accept", "application/vnd.github+json"))
        .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().json(createBodyJson))
        .andRespond(withSuccess());

    Map<String, Object> provisionBody = new HashMap<>();
    provisionBody.put("permission", "admin");
    String provisionBodyJson = objectMapper.writeValueAsString(provisionBody);

    mockRestServiceServer
        .expect(
            requestTo("https://api.github.com/repos/ucsb-cs156/repo1-staff1/collaborators/staff1"))
        .andExpect(header("Authorization", "Bearer real.installation.token"))
        .andExpect(header("Accept", "application/vnd.github+json"))
        .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
        .andExpect(method(HttpMethod.PUT))
        .andExpect(content().json(provisionBodyJson))
        .andRespond(withSuccess());

    CourseStaff staff = CourseStaff.builder().githubLogin("staff1").build();

    repositoryService.createStaffRepository(
        course, staff, "repo1", false, RepositoryPermissions.ADMIN);

    mockRestServiceServer.verify();
  }

  @Test
  public void team_repo_already_exists_test() throws Exception {
    mockRestServiceServer
        .expect(requestTo("https://api.github.com/repos/ucsb-cs156/repo1-test-team"))
        .andExpect(header("Authorization", "Bearer real.installation.token"))
        .andExpect(header("Accept", "application/vnd.github+json"))
        .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess());

    Map<String, Object> provisionBody = new HashMap<>();
    provisionBody.put("permission", "push");
    String provisionBodyJson = objectMapper.writeValueAsString(provisionBody);
    mockRestServiceServer
        .expect(
            requestTo(
                "https://api.github.com/organizations/1/team/12345/repos/ucsb-cs156/repo1-test-team"))
        .andExpect(header("Authorization", "Bearer real.installation.token"))
        .andExpect(header("Accept", "application/vnd.github+json"))
        .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
        .andExpect(method(HttpMethod.PUT))
        .andExpect(content().json(provisionBodyJson))
        .andRespond(withNoContent());

    RosterStudent student = RosterStudent.builder().githubLogin("student1").build();
    Team team = Team.builder().name("test-team").githubTeamSlug("test-team").build();
    TeamMember member = TeamMember.builder().rosterStudent(student).build();
    team.setTeamMembers(List.of(member));
    team.setGithubTeamId(12345);

    repositoryService.createTeamRepository(
        course, team, "repo1", false, RepositoryPermissions.WRITE, 1);
    mockRestServiceServer.verify();
  }

  @Test
  public void successfully_creates_team_repo_public() throws Exception {
    mockRestServiceServer
        .expect(requestTo("https://api.github.com/repos/ucsb-cs156/repo1-test-team1"))
        .andExpect(header("Authorization", "Bearer real.installation.token"))
        .andExpect(header("Accept", "application/vnd.github+json"))
        .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withResourceNotFound());

    Map<String, Object> createBody = new HashMap<>();
    createBody.put("name", "repo1-test-team1");
    createBody.put("private", false);
    String createBodyJson = objectMapper.writeValueAsString(createBody);

    mockRestServiceServer
        .expect(requestTo("https://api.github.com/orgs/ucsb-cs156/repos"))
        .andExpect(header("Authorization", "Bearer real.installation.token"))
        .andExpect(header("Accept", "application/vnd.github+json"))
        .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().json(createBodyJson))
        .andRespond(withSuccess());

    Map<String, Object> provisionBody = new HashMap<>();
    provisionBody.put("permission", "admin");
    String provisionBodyJson = objectMapper.writeValueAsString(provisionBody);
    mockRestServiceServer
        .expect(
            requestTo(
                "https://api.github.com/organizations/1/team/123456/repos/ucsb-cs156/repo1-test-team1"))
        .andExpect(header("Authorization", "Bearer real.installation.token"))
        .andExpect(header("Accept", "application/vnd.github+json"))
        .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
        .andExpect(method(HttpMethod.PUT))
        .andExpect(content().json(provisionBodyJson))
        .andRespond(withSuccess());

    RosterStudent student1 =
        RosterStudent.builder().githubLogin("student1").orgStatus(OrgStatus.MEMBER).build();
    TeamMember member1 = TeamMember.builder().rosterStudent(student1).build();
    RosterStudent student2 =
        RosterStudent.builder().githubLogin("student2").orgStatus(OrgStatus.MEMBER).build();
    TeamMember member2 = TeamMember.builder().rosterStudent(student2).build();
    RosterStudent student3 =
        RosterStudent.builder().githubLogin("student3").orgStatus(OrgStatus.MEMBER).build();
    TeamMember member3 = TeamMember.builder().rosterStudent(student3).build();
    Team team1 = Team.builder().name("test-team1").githubTeamSlug("test-team1").build();
    team1.setTeamMembers(List.of(member1, member2, member3));
    team1.setGithubTeamId(123456);

    repositoryService.createTeamRepository(
        course, team1, "repo1", false, RepositoryPermissions.ADMIN, 1);
    mockRestServiceServer.verify();
  }

  @Test
  public void successfully_creates_team_repo_private() throws Exception {
    mockRestServiceServer
        .expect(requestTo("https://api.github.com/repos/ucsb-cs156/repo1-test-team"))
        .andExpect(header("Authorization", "Bearer real.installation.token"))
        .andExpect(header("Accept", "application/vnd.github+json"))
        .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withResourceNotFound());

    Map<String, Object> createBody = new HashMap<>();
    createBody.put("name", "repo1-test-team");
    createBody.put("private", true);
    String createBodyJson = objectMapper.writeValueAsString(createBody);

    mockRestServiceServer
        .expect(requestTo("https://api.github.com/orgs/ucsb-cs156/repos"))
        .andExpect(header("Authorization", "Bearer real.installation.token"))
        .andExpect(header("Accept", "application/vnd.github+json"))
        .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().json(createBodyJson))
        .andRespond(withSuccess());

    Map<String, Object> provisionBody = new HashMap<>();
    provisionBody.put("permission", "push");
    String provisionBodyJson = objectMapper.writeValueAsString(provisionBody);
    mockRestServiceServer
        .expect(
            requestTo(
                "https://api.github.com/organizations/1/team/1234567/repos/ucsb-cs156/repo1-test-team"))
        .andExpect(header("Authorization", "Bearer real.installation.token"))
        .andExpect(header("Accept", "application/vnd.github+json"))
        .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
        .andExpect(method(HttpMethod.PUT))
        .andExpect(content().json(provisionBodyJson))
        .andRespond(withSuccess());

    RosterStudent student = RosterStudent.builder().githubLogin("student1").build();
    Team team = Team.builder().name("test-team").githubTeamSlug("test-team").build();
    TeamMember member = TeamMember.builder().rosterStudent(student).build();
    team.setTeamMembers(List.of(member));
    team.setGithubTeamId(1234567);

    repositoryService.createTeamRepository(
        course, team, "repo1", true, RepositoryPermissions.WRITE, 1);
    mockRestServiceServer.verify();
  }

  @Test
  public void exits_if_team_repo_not_not_found() throws Exception {
    mockRestServiceServer
        .expect(requestTo("https://api.github.com/repos/ucsb-cs156/repo1-test-team"))
        .andExpect(header("Authorization", "Bearer real.installation.token"))
        .andExpect(header("Accept", "application/vnd.github+json"))
        .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withForbiddenRequest());

    RosterStudent student = RosterStudent.builder().githubLogin("student1").build();
    Team team = Team.builder().name("test-team").githubTeamSlug("test-team").build();
    TeamMember member = TeamMember.builder().rosterStudent(student).build();
    team.setTeamMembers(List.of(member));

    repositoryService.createTeamRepository(
        course, team, "repo1", false, RepositoryPermissions.WRITE, 1);
    mockRestServiceServer.verify();
  }

  @Test
  public void warn_on_team_repo_not_no_content() throws Exception {
    mockRestServiceServer
        .expect(requestTo("https://api.github.com/repos/ucsb-cs156/repo1-test-team"))
        .andExpect(header("Authorization", "Bearer real.installation.token"))
        .andExpect(header("Accept", "application/vnd.github+json"))
        .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess());

    Map<String, Object> provisionBody = new HashMap<>();
    provisionBody.put("permission", "push");
    String provisionBodyJson = objectMapper.writeValueAsString(provisionBody);
    mockRestServiceServer
        .expect(
            requestTo(
                "https://api.github.com/organizations/1/team/12345/repos/ucsb-cs156/repo1-test-team"))
        .andExpect(header("Authorization", "Bearer real.installation.token"))
        .andExpect(header("Accept", "application/vnd.github+json"))
        .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
        .andExpect(method(HttpMethod.PUT))
        .andExpect(content().json(provisionBodyJson))
        .andRespond(withForbiddenRequest());

    RosterStudent student = RosterStudent.builder().githubLogin("student1").build();
    Team team = Team.builder().name("test-team").githubTeamSlug("test-team").build();
    TeamMember member = TeamMember.builder().rosterStudent(student).build();
    team.setTeamMembers(List.of(member));
    team.setGithubTeamId(12345);

    repositoryService.createTeamRepository(
        course, team, "repo1", false, RepositoryPermissions.WRITE, 1);
    mockRestServiceServer.verify();
  }

  @Test
  public void team_repo_fetches_and_persists_slug_when_missing() throws Exception {
    mockRestServiceServer
        .expect(requestTo("https://api.github.com/repos/ucsb-cs156/repo1-actual-team-slug"))
        .andExpect(header("Authorization", "Bearer real.installation.token"))
        .andExpect(header("Accept", "application/vnd.github+json"))
        .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess());

    Map<String, Object> provisionBody = new HashMap<>();
    provisionBody.put("permission", "push");
    String provisionBodyJson = objectMapper.writeValueAsString(provisionBody);
    mockRestServiceServer
        .expect(
            requestTo(
                "https://api.github.com/organizations/1/team/12345/repos/ucsb-cs156/repo1-actual-team-slug"))
        .andExpect(header("Authorization", "Bearer real.installation.token"))
        .andExpect(header("Accept", "application/vnd.github+json"))
        .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
        .andExpect(method(HttpMethod.PUT))
        .andExpect(content().json(provisionBodyJson))
        .andRespond(withNoContent());

    Team team = Team.builder().name("Display Name").githubTeamId(12345).build();

    doReturn(new GithubTeamService.GithubTeamInfo(12345, "Display Name", "actual-team-slug"))
        .when(githubTeamService)
        .getTeamInfoById(1, 12345, course);
    doReturn(team).when(teamRepository).save(team);

    repositoryService.createTeamRepository(
        course, team, "repo1", false, RepositoryPermissions.WRITE, 1);

    mockRestServiceServer.verify();
    verify(teamRepository).save(team);
    assertEquals("actual-team-slug", team.getGithubTeamSlug());
  }

  @Test
  public void team_repo_uses_existing_slug_without_lookup_or_save() throws Exception {
    mockRestServiceServer
        .expect(requestTo("https://api.github.com/repos/ucsb-cs156/repo1-existing-slug"))
        .andExpect(header("Authorization", "Bearer real.installation.token"))
        .andExpect(header("Accept", "application/vnd.github+json"))
        .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess());

    Map<String, Object> provisionBody = new HashMap<>();
    provisionBody.put("permission", "push");
    String provisionBodyJson = objectMapper.writeValueAsString(provisionBody);
    mockRestServiceServer
        .expect(
            requestTo(
                "https://api.github.com/organizations/1/team/12345/repos/ucsb-cs156/repo1-existing-slug"))
        .andExpect(header("Authorization", "Bearer real.installation.token"))
        .andExpect(header("Accept", "application/vnd.github+json"))
        .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
        .andExpect(method(HttpMethod.PUT))
        .andExpect(content().json(provisionBodyJson))
        .andRespond(withNoContent());

    Team team =
        Team.builder()
            .name("Display Name")
            .githubTeamId(12345)
            .githubTeamSlug("existing-slug")
            .build();

    repositoryService.createTeamRepository(
        course, team, "repo1", false, RepositoryPermissions.WRITE, 1);

    mockRestServiceServer.verify();
    verify(githubTeamService, never()).getTeamInfoById(any(), any(), any());
    verify(teamRepository, never()).save(any());
  }

  @Test
  public void team_repo_falls_back_when_existing_slug_is_blank() throws Exception {
    mockRestServiceServer
        .expect(requestTo("https://api.github.com/repos/ucsb-cs156/repo1-actual-team-slug"))
        .andExpect(header("Authorization", "Bearer real.installation.token"))
        .andExpect(header("Accept", "application/vnd.github+json"))
        .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess());

    Map<String, Object> provisionBody = new HashMap<>();
    provisionBody.put("permission", "push");
    String provisionBodyJson = objectMapper.writeValueAsString(provisionBody);
    mockRestServiceServer
        .expect(
            requestTo(
                "https://api.github.com/organizations/1/team/12345/repos/ucsb-cs156/repo1-actual-team-slug"))
        .andExpect(header("Authorization", "Bearer real.installation.token"))
        .andExpect(header("Accept", "application/vnd.github+json"))
        .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
        .andExpect(method(HttpMethod.PUT))
        .andExpect(content().json(provisionBodyJson))
        .andRespond(withNoContent());

    Team team =
        Team.builder().name("Display Name").githubTeamId(12345).githubTeamSlug("   ").build();

    doReturn(new GithubTeamService.GithubTeamInfo(12345, "Display Name", "actual-team-slug"))
        .when(githubTeamService)
        .getTeamInfoById(1, 12345, course);
    doReturn(team).when(teamRepository).save(team);

    repositoryService.createTeamRepository(
        course, team, "repo1", false, RepositoryPermissions.WRITE, 1);

    mockRestServiceServer.verify();
    verify(githubTeamService).getTeamInfoById(1, 12345, course);
    verify(teamRepository).save(team);
    assertEquals("actual-team-slug", team.getGithubTeamSlug());
  }

  @Test
  public void team_repo_throws_when_slug_missing_and_team_id_missing() {
    Team team = Team.builder().name("Display Name").build();

    IllegalStateException e =
        assertThrows(
            IllegalStateException.class,
            () ->
                repositoryService.createTeamRepository(
                    course, team, "repo1", false, RepositoryPermissions.WRITE, 1));

    assertEquals(
        "Cannot create team repository without a GitHub team ID for team 'Display Name'",
        e.getMessage());
    verifyNoInteractions(githubTeamService, teamRepository);
  }

  @Test
  public void team_repo_throws_when_fetched_team_info_is_null() throws Exception {
    Team team = Team.builder().name("Display Name").githubTeamId(12345).build();

    doReturn(null).when(githubTeamService).getTeamInfoById(1, 12345, course);

    IllegalStateException e =
        assertThrows(
            IllegalStateException.class,
            () ->
                repositoryService.createTeamRepository(
                    course, team, "repo1", false, RepositoryPermissions.WRITE, 1));

    assertEquals("Cannot determine GitHub team slug for team 'Display Name'", e.getMessage());
    verify(teamRepository, never()).save(any());
  }

  @Test
  public void team_repo_throws_when_fetched_team_slug_is_blank() throws Exception {
    Team team = Team.builder().name("Display Name").githubTeamId(12345).build();

    doReturn(new GithubTeamService.GithubTeamInfo(12345, "Display Name", "   "))
        .when(githubTeamService)
        .getTeamInfoById(1, 12345, course);

    IllegalStateException e =
        assertThrows(
            IllegalStateException.class,
            () ->
                repositoryService.createTeamRepository(
                    course, team, "repo1", false, RepositoryPermissions.WRITE, 1));

    assertEquals("Cannot determine GitHub team slug for team 'Display Name'", e.getMessage());
    verify(teamRepository, never()).save(any());
  }

  @Test
  public void team_repo_throws_when_fetched_team_slug_is_null() throws Exception {
    Team team = Team.builder().name("Display Name").githubTeamId(12345).build();

    doReturn(new GithubTeamService.GithubTeamInfo(12345, "Display Name", null))
        .when(githubTeamService)
        .getTeamInfoById(1, 12345, course);

    IllegalStateException e =
        assertThrows(
            IllegalStateException.class,
            () ->
                repositoryService.createTeamRepository(
                    course, team, "repo1", false, RepositoryPermissions.WRITE, 1));

    assertEquals("Cannot determine GitHub team slug for team 'Display Name'", e.getMessage());
    verify(teamRepository, never()).save(any());
  }

  @Test
  public void getRepositoriesMatchingPrefix_filters_matching_repositories() throws Exception {
    String response =
        """
        [
          {"name": "lab01-student1", "full_name": "ucsb-cs156/lab01-student1"},
          {"name": "lab01-student2", "full_name": "ucsb-cs156/lab01-student2"},
          {"name": "other-student3", "full_name": "ucsb-cs156/other-student3"}
        ]
        """;

    mockRestServiceServer
        .expect(requestTo("https://api.github.com/orgs/ucsb-cs156/repos?per_page=100"))
        .andExpect(header("Authorization", "Bearer real.installation.token"))
        .andExpect(header("Accept", "application/vnd.github+json"))
        .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

    List<RepositoryService.GithubRepository> repos =
        repositoryService.getRepositoriesMatchingPrefix(course, "lab01");

    assertEquals(2, repos.size());
    assertEquals("lab01-student1", repos.get(0).name());
    assertEquals("ucsb-cs156/lab01-student1", repos.get(0).fullName());
    assertEquals("lab01-student2", repos.get(1).name());
    mockRestServiceServer.verify();
  }

  @Test
  public void getRepositoriesMatchingPrefix_returns_empty_list_when_no_repositories_match()
      throws Exception {
    String response =
        """
        [
          {"name": "other-student1", "full_name": "ucsb-cs156/other-student1"}
        ]
        """;

    mockRestServiceServer
        .expect(requestTo("https://api.github.com/orgs/ucsb-cs156/repos?per_page=100"))
        .andExpect(header("Authorization", "Bearer real.installation.token"))
        .andExpect(header("Accept", "application/vnd.github+json"))
        .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

    List<RepositoryService.GithubRepository> repos =
        repositoryService.getRepositoriesMatchingPrefix(course, "lab01");

    assertTrue(repos.isEmpty());
    mockRestServiceServer.verify();
  }

  @Test
  public void getRepositoriesMatchingPrefix_propagates_github_api_errors() {
    mockRestServiceServer
        .expect(requestTo("https://api.github.com/orgs/ucsb-cs156/repos?per_page=100"))
        .andExpect(header("Authorization", "Bearer real.installation.token"))
        .andExpect(header("Accept", "application/vnd.github+json"))
        .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withForbiddenRequest());

    assertThrows(
        HttpClientErrorException.class,
        () -> repositoryService.getRepositoriesMatchingPrefix(course, "lab01"));
    mockRestServiceServer.verify();
  }

  @Test
  public void isRepositoryEmpty_returns_true_when_commits_response_is_empty_array()
      throws Exception {
    mockRestServiceServer
        .expect(
            requestTo("https://api.github.com/repos/ucsb-cs156/lab01-student1/commits?per_page=1"))
        .andExpect(header("Authorization", "Bearer real.installation.token"))
        .andExpect(header("Accept", "application/vnd.github+json"))
        .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

    assertTrue(repositoryService.isRepositoryEmpty(course, "lab01-student1"));
    mockRestServiceServer.verify();
  }

  @Test
  public void isRepositoryEmpty_returns_true_when_github_reports_empty_repository()
      throws Exception {
    mockRestServiceServer
        .expect(
            requestTo("https://api.github.com/repos/ucsb-cs156/lab01-student1/commits?per_page=1"))
        .andExpect(header("Authorization", "Bearer real.installation.token"))
        .andExpect(header("Accept", "application/vnd.github+json"))
        .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withStatus(HttpStatus.CONFLICT));

    assertTrue(repositoryService.isRepositoryEmpty(course, "lab01-student1"));
    mockRestServiceServer.verify();
  }

  @Test
  public void isRepositoryEmpty_returns_false_when_commits_exist() throws Exception {
    String response = """
        [
          {"sha": "abc123"}
        ]
        """;

    mockRestServiceServer
        .expect(
            requestTo("https://api.github.com/repos/ucsb-cs156/lab01-student1/commits?per_page=1"))
        .andExpect(header("Authorization", "Bearer real.installation.token"))
        .andExpect(header("Accept", "application/vnd.github+json"))
        .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

    assertFalse(repositoryService.isRepositoryEmpty(course, "lab01-student1"));
    mockRestServiceServer.verify();
  }

  @Test
  public void isRepositoryEmpty_propagates_unexpected_github_api_errors() {
    mockRestServiceServer
        .expect(
            requestTo("https://api.github.com/repos/ucsb-cs156/lab01-student1/commits?per_page=1"))
        .andExpect(header("Authorization", "Bearer real.installation.token"))
        .andExpect(header("Accept", "application/vnd.github+json"))
        .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withForbiddenRequest());

    assertThrows(
        HttpClientErrorException.class,
        () -> repositoryService.isRepositoryEmpty(course, "lab01-student1"));
    mockRestServiceServer.verify();
  }

  @Test
  public void deleteRepositoryIfEmpty_deletes_empty_repository() throws Exception {
    mockRestServiceServer
        .expect(
            requestTo("https://api.github.com/repos/ucsb-cs156/lab01-student1/commits?per_page=1"))
        .andExpect(header("Authorization", "Bearer real.installation.token"))
        .andExpect(header("Accept", "application/vnd.github+json"))
        .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

    mockRestServiceServer
        .expect(requestTo("https://api.github.com/repos/ucsb-cs156/lab01-student1"))
        .andExpect(header("Authorization", "Bearer real.installation.token"))
        .andExpect(header("Accept", "application/vnd.github+json"))
        .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
        .andExpect(method(HttpMethod.DELETE))
        .andRespond(withNoContent());

    assertTrue(repositoryService.deleteRepositoryIfEmpty(course, "lab01-student1"));
    mockRestServiceServer.verify();
  }

  @Test
  public void deleteRepositoryIfEmpty_does_not_delete_non_empty_repository() throws Exception {
    String response = """
        [
          {"sha": "abc123"}
        ]
        """;

    mockRestServiceServer
        .expect(
            requestTo("https://api.github.com/repos/ucsb-cs156/lab01-student1/commits?per_page=1"))
        .andExpect(header("Authorization", "Bearer real.installation.token"))
        .andExpect(header("Accept", "application/vnd.github+json"))
        .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

    assertFalse(repositoryService.deleteRepositoryIfEmpty(course, "lab01-student1"));
    mockRestServiceServer.verify();
  }

  @Test
  public void deleteRepositoryIfEmpty_propagates_delete_errors() {
    mockRestServiceServer
        .expect(
            requestTo("https://api.github.com/repos/ucsb-cs156/lab01-student1/commits?per_page=1"))
        .andExpect(header("Authorization", "Bearer real.installation.token"))
        .andExpect(header("Accept", "application/vnd.github+json"))
        .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

    mockRestServiceServer
        .expect(requestTo("https://api.github.com/repos/ucsb-cs156/lab01-student1"))
        .andExpect(header("Authorization", "Bearer real.installation.token"))
        .andExpect(header("Accept", "application/vnd.github+json"))
        .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
        .andExpect(method(HttpMethod.DELETE))
        .andRespond(withForbiddenRequest());

    assertThrows(
        HttpClientErrorException.class,
        () -> repositoryService.deleteRepositoryIfEmpty(course, "lab01-student1"));
    mockRestServiceServer.verify();
  }
}
