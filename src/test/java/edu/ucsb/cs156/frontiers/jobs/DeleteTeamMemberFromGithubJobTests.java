package edu.ucsb.cs156.frontiers.jobs;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.Job;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.TeamMemberRepository;
import edu.ucsb.cs156.frontiers.repositories.TeamRepository;
import edu.ucsb.cs156.frontiers.services.GithubTeamService;
import edu.ucsb.cs156.frontiers.services.jobs.JobContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DeleteTeamMemberFromGithubJobTests {

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
  public void testGetCourse_returnsCoursePassedToBuilder() {
    Course course =
        Course.builder()
            .id(1L)
            .courseName("Test Course")
            .orgName("test-org")
            .installationId("123")
            .build();

    DeleteTeamMemberFromGithubJob job =
        DeleteTeamMemberFromGithubJob.builder()
            .memberGithubLogin("testuser")
            .githubTeamId(456)
            .course(course)
            .githubTeamService(githubTeamService)
            .build();

    assertEquals(course, job.getCourse());
  }

  @Test
  public void test_successfully_delete_team_member() throws Exception {
    Course course = Course.builder().orgName("test-org").installationId("123").build();
    when(githubTeamService.getOrgId("test-org", course)).thenReturn(1);

    DeleteTeamMemberFromGithubJob job =
        DeleteTeamMemberFromGithubJob.builder()
            .memberGithubLogin("testuser")
            .githubTeamId(456)
            .course(course)
            .githubTeamService(githubTeamService)
            .build();

    job.accept(ctx);

    verify(githubTeamService, times(1))
        .removeMemberFromGithubTeam(eq(1), eq("testuser"), eq(456), eq(course));
  }

  @Test
  public void test_githubTeamIdIsNull() throws Exception {
    // Test exception handling when github team id is null
    Course course = Course.builder().orgName("test-org").installationId("123").build();

    DeleteTeamMemberFromGithubJob job =
        DeleteTeamMemberFromGithubJob.builder()
            .memberGithubLogin("testuser")
            .githubTeamId(null)
            .course(course)
            .githubTeamService(githubTeamService)
            .build();

    job.accept(ctx);

    verify(githubTeamService, never())
        .removeMemberFromGithubTeam(anyInt(), anyString(), anyInt(), any(Course.class));
    assertTrue(jobStarted.getLog().contains("ERROR: Team has no GitHub team ID"));
  }

  @Test
  public void test_memberGithubLoginIsNull() throws Exception {
    // Test exception handling when member github login is null
    Course course = Course.builder().orgName("test-org").installationId("123").build();

    DeleteTeamMemberFromGithubJob job =
        DeleteTeamMemberFromGithubJob.builder()
            .memberGithubLogin(null)
            .githubTeamId(456)
            .course(course)
            .githubTeamService(githubTeamService)
            .build();

    job.accept(ctx);

    verify(githubTeamService, never())
        .removeMemberFromGithubTeam(anyInt(), anyString(), anyInt(), any(Course.class));
    assertTrue(jobStarted.getLog().contains("ERROR: Team member has no GitHub login"));
  }

  @Test
  public void test_CourseWithoutGithubOrg() throws Exception {
    // Test case where course orgName is null
    Course course = Course.builder().id(1L).courseName("Test Course").build();

    DeleteTeamMemberFromGithubJob job =
        DeleteTeamMemberFromGithubJob.builder()
            .memberGithubLogin("testuser")
            .githubTeamId(456)
            .course(course)
            .githubTeamService(githubTeamService)
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

    DeleteTeamMemberFromGithubJob job =
        DeleteTeamMemberFromGithubJob.builder()
            .memberGithubLogin("testuser")
            .githubTeamId(456)
            .course(course)
            .githubTeamService(githubTeamService)
            .build();

    job.accept(ctx);

    verifyNoInteractions(githubTeamService);
    assertTrue(jobStarted.getLog().contains("ERROR: Course has no linked GitHub organization"));
  }

  @Test
  public void test_TeamMemberRemovalFailure() throws Exception {
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
        .removeMemberFromGithubTeam(1, "testuser", 456, course);

    DeleteTeamMemberFromGithubJob job =
        DeleteTeamMemberFromGithubJob.builder()
            .memberGithubLogin("testuser")
            .githubTeamId(456)
            .course(course)
            .githubTeamService(githubTeamService)
            .build();

    job.accept(ctx);

    verify(githubTeamService).removeMemberFromGithubTeam(1, "testuser", 456, course);
    assertTrue(
        jobStarted
            .getLog()
            .contains("ERROR: Failed to remove user from GitHub team: GitHub API error"));
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

    DeleteTeamMemberFromGithubJob job =
        DeleteTeamMemberFromGithubJob.builder()
            .memberGithubLogin("testuser")
            .githubTeamId(456)
            .course(course)
            .githubTeamService(githubTeamService)
            .build();

    job.accept(ctx);

    verify(githubTeamService).getOrgId("test-org", course);
    verify(githubTeamService, never())
        .removeMemberFromGithubTeam(anyInt(), anyString(), anyInt(), any(Course.class));
    assertTrue(
        jobStarted
            .getLog()
            .contains("ERROR: Failed to get organization ID for org: test-org - GitHub API error"));
  }
}
