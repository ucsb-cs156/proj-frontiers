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
public class DeleteTeamFromGithubJobTests {

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
  public void test_successfully_delete_team() throws Exception {
    Course course = Course.builder().orgName("test-org").installationId("123").build();
    when(githubTeamService.getOrgId("test-org", course)).thenReturn(1);

    DeleteTeamFromGithubJob job =
        DeleteTeamFromGithubJob.builder()
            .githubTeamId(456)
            .course(course)
            .githubTeamService(githubTeamService)
            .build();

    job.accept(ctx);

    verify(githubTeamService, times(1)).deleteGithubTeam(eq(1), eq(456), eq(course));
  }

  @Test
  public void test_githubTeamIdIsNull() throws Exception {
    // Test exception handling when github team id is null
    Course course = Course.builder().orgName("test-org").installationId("123").build();

    DeleteTeamFromGithubJob job =
        DeleteTeamFromGithubJob.builder()
            .githubTeamId(null)
            .course(course)
            .githubTeamService(githubTeamService)
            .build();

    job.accept(ctx);

    verify(githubTeamService, never()).deleteGithubTeam(anyInt(), anyInt(), any(Course.class));
    assertTrue(jobStarted.getLog().contains("ERROR: Team has no GitHub team ID"));
  }

  @Test
  public void test_CourseWithoutGithubOrg() throws Exception {
    // Test case where course orgName is null
    Course course = Course.builder().id(1L).courseName("Test Course").build();

    DeleteTeamFromGithubJob job =
        DeleteTeamFromGithubJob.builder()
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

    DeleteTeamFromGithubJob job =
        DeleteTeamFromGithubJob.builder()
            .githubTeamId(456)
            .course(course)
            .githubTeamService(githubTeamService)
            .build();

    job.accept(ctx);

    verifyNoInteractions(githubTeamService);
    assertTrue(jobStarted.getLog().contains("ERROR: Course has no linked GitHub organization"));
  }

  @Test
  public void test_TeamDeletionFailure() throws Exception {
    // Test exception handling when team deletion fails
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
        .deleteGithubTeam(1, 456, course);

    DeleteTeamFromGithubJob job =
        DeleteTeamFromGithubJob.builder()
            .githubTeamId(456)
            .course(course)
            .githubTeamService(githubTeamService)
            .build();

    job.accept(ctx);

    verify(githubTeamService).deleteGithubTeam(1, 456, course);
    assertTrue(
        jobStarted.getLog().contains("ERROR: Failed to delete GitHub team: GitHub API error"));
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

    DeleteTeamFromGithubJob job =
        DeleteTeamFromGithubJob.builder()
            .githubTeamId(456)
            .course(course)
            .githubTeamService(githubTeamService)
            .build();

    job.accept(ctx);

    verify(githubTeamService).getOrgId("test-org", course);
    verify(githubTeamService, never()).deleteGithubTeam(anyInt(), anyInt(), any(Course.class));
    assertTrue(
        jobStarted
            .getLog()
            .contains("ERROR: Failed to get organization ID for org: test-org - GitHub API error"));
  }
}
