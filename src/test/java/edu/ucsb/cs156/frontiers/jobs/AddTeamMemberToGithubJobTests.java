package edu.ucsb.cs156.frontiers.jobs;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.Job;
import edu.ucsb.cs156.frontiers.entities.TeamMember;
import edu.ucsb.cs156.frontiers.enums.TeamStatus;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.TeamMemberRepository;
import edu.ucsb.cs156.frontiers.repositories.TeamRepository;
import edu.ucsb.cs156.frontiers.services.GithubTeamService;
import edu.ucsb.cs156.frontiers.services.jobs.JobContext;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AddTeamMemberToGithubJobTests {

  @Mock private CourseRepository courseRepository;
  @Mock private TeamRepository teamRepository;
  @Mock private TeamMemberRepository teamMemberRepository;
  @Mock private GithubTeamService githubTeamService;

  Job jobStarted = Job.builder().build();
  JobContext ctx = new JobContext(null, jobStarted);

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void test_successfully_add_team_member() throws Exception {
    Course course = Course.builder().orgName("test-org").installationId("123").build();
    when(githubTeamService.getOrgId("test-org", course)).thenReturn(1);

    AddTeamMemberToGithubJob job =
        AddTeamMemberToGithubJob.builder()
            .memberGithubLogin("testuser")
            .githubTeamId(456)
            .teamMemberId(123L)
            .course(course)
            .githubTeamService(githubTeamService)
            .teamMemberRepository(teamMemberRepository)
            .build();

    job.accept(ctx);

    verify(githubTeamService, times(1))
        .addMemberToGithubTeam(eq("testuser"), eq(456), eq("member"), eq(course), eq(1));
  }

  @Test
  public void test_successfully_add_team_member_and_update_status() throws Exception {
    Course course = Course.builder().orgName("test-org").installationId("123").build();
    TeamMember teamMember = TeamMember.builder().id(123L).build();

    when(githubTeamService.getOrgId("test-org", course)).thenReturn(1);
    when(githubTeamService.addMemberToGithubTeam("testuser", 456, "member", course, 1))
        .thenReturn(TeamStatus.TEAM_MEMBER);
    when(teamMemberRepository.findById(123L)).thenReturn(Optional.of(teamMember));

    AddTeamMemberToGithubJob job =
        AddTeamMemberToGithubJob.builder()
            .memberGithubLogin("testuser")
            .githubTeamId(456)
            .teamMemberId(123L)
            .course(course)
            .githubTeamService(githubTeamService)
            .teamMemberRepository(teamMemberRepository)
            .build();

    job.accept(ctx);

    verify(githubTeamService, times(1))
        .addMemberToGithubTeam(eq("testuser"), eq(456), eq("member"), eq(course), eq(1));
    verify(teamMemberRepository, times(1)).findById(123L);
    verify(teamMemberRepository, times(1)).save(teamMember);
    assertEquals(TeamStatus.TEAM_MEMBER, teamMember.getTeamStatus());
    assertTrue(jobStarted.getLog().contains("Updated team member status in database"));
  }

  @Test
  public void test_githubTeamIdIsNull() throws Exception {
    // Test exception handling when github team id is null
    Course course = Course.builder().orgName("test-org").installationId("123").build();

    AddTeamMemberToGithubJob job =
        AddTeamMemberToGithubJob.builder()
            .memberGithubLogin("testuser")
            .githubTeamId(null)
            .teamMemberId(123L)
            .course(course)
            .githubTeamService(githubTeamService)
            .teamMemberRepository(teamMemberRepository)
            .build();

    job.accept(ctx);

    verify(githubTeamService, never())
        .addMemberToGithubTeam(anyString(), anyInt(), anyString(), any(Course.class), anyInt());
    assertTrue(jobStarted.getLog().contains("ERROR: Team has no GitHub team ID"));
  }

  @Test
  public void test_memberGithubLoginIsNull() throws Exception {
    // Test exception handling when member github login is null
    Course course = Course.builder().orgName("test-org").installationId("123").build();

    AddTeamMemberToGithubJob job =
        AddTeamMemberToGithubJob.builder()
            .memberGithubLogin(null)
            .githubTeamId(456)
            .teamMemberId(123L)
            .course(course)
            .githubTeamService(githubTeamService)
            .teamMemberRepository(teamMemberRepository)
            .build();

    job.accept(ctx);

    verify(githubTeamService, never())
        .addMemberToGithubTeam(anyString(), anyInt(), anyString(), any(Course.class), anyInt());
    assertTrue(jobStarted.getLog().contains("ERROR: Team member has no GitHub login"));
  }

  @Test
  public void test_CourseWithoutGithubOrg() throws Exception {
    // Test case where course orgName is null
    Course course = Course.builder().id(1L).courseName("Test Course").build();

    AddTeamMemberToGithubJob job =
        AddTeamMemberToGithubJob.builder()
            .memberGithubLogin("testuser")
            .githubTeamId(456)
            .teamMemberId(123L)
            .course(course)
            .githubTeamService(githubTeamService)
            .teamMemberRepository(teamMemberRepository)
            .build();

    job.accept(ctx);

    verifyNoInteractions(githubTeamService);
    assertTrue(jobStarted.getLog().contains("ERROR: Course has no linked GitHub organization"));
  }

  @Test
  public void test_CourseWithOrgNameButNoInstallationId() throws Exception {
    // Test case where orgName is not null but installationId is null
    Course course =
        Course.builder()
            .id(1L)
            .courseName("Test Course")
            .orgName("test-org")
            .installationId(null)
            .build();

    AddTeamMemberToGithubJob job =
        AddTeamMemberToGithubJob.builder()
            .memberGithubLogin("testuser")
            .githubTeamId(456)
            .teamMemberId(123L)
            .course(course)
            .githubTeamService(githubTeamService)
            .teamMemberRepository(teamMemberRepository)
            .build();

    job.accept(ctx);

    verifyNoInteractions(githubTeamService);
    assertTrue(jobStarted.getLog().contains("ERROR: Course has no linked GitHub organization"));
  }

  @Test
  public void test_AddTeamMemberFailure() throws Exception {
    // Test exception handling when team member removal fails
    Long courseId = 1L;
    Course course =
        Course.builder()
            .id(courseId)
            .courseName("Test Course")
            .orgName("test-org")
            .installationId("123")
            .build();
    when(githubTeamService.getOrgId("test-org", course)).thenReturn(1);

    doThrow(new RuntimeException("GitHub API error"))
        .when(githubTeamService)
        .addMemberToGithubTeam("testuser", 456, "member", course, 1);

    AddTeamMemberToGithubJob job =
        AddTeamMemberToGithubJob.builder()
            .memberGithubLogin("testuser")
            .githubTeamId(456)
            .teamMemberId(123L)
            .course(course)
            .githubTeamService(githubTeamService)
            .teamMemberRepository(teamMemberRepository)
            .build();

    job.accept(ctx);

    verify(githubTeamService).addMemberToGithubTeam("testuser", 456, "member", course, 1);
    assertTrue(
        jobStarted.getLog().contains("ERROR: Failed to add user to GitHub team: GitHub API error"));
  }

  @Test
  public void test_getOrgIdFailure() throws Exception {
    // Test exception handling when get org id failure occurs
    Long courseId = 1L;
    Course course =
        Course.builder()
            .id(courseId)
            .courseName("Test Course")
            .orgName("test-org")
            .installationId("123")
            .build();

    doThrow(new RuntimeException("GitHub API error"))
        .when(githubTeamService)
        .getOrgId("test-org", course);

    AddTeamMemberToGithubJob job =
        AddTeamMemberToGithubJob.builder()
            .memberGithubLogin("testuser")
            .githubTeamId(456)
            .teamMemberId(123L)
            .course(course)
            .githubTeamService(githubTeamService)
            .teamMemberRepository(teamMemberRepository)
            .build();

    job.accept(ctx);

    verify(githubTeamService).getOrgId("test-org", course);
    verify(githubTeamService, never())
        .addMemberToGithubTeam(anyString(), anyInt(), anyString(), any(Course.class), anyInt());
    assertTrue(
        jobStarted
            .getLog()
            .contains("ERROR: Failed to get organization ID for org: test-org - GitHub API error"));
  }
}
