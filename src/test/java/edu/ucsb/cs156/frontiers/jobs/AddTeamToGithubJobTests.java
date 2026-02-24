package edu.ucsb.cs156.frontiers.jobs;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.Job;
import edu.ucsb.cs156.frontiers.entities.Team;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
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
public class AddTeamToGithubJobTests {

  @Mock private CourseRepository courseRepository;
  @Mock private GithubTeamService githubTeamService;
  @Mock private TeamRepository teamRepository;

  Job jobStarted = Job.builder().build();
  JobContext ctx = new JobContext(null, jobStarted);

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void test_successfully_add_team() throws Exception {
    Course course = Course.builder().orgName("test-org").id(2L).installationId("123").build();
    Team team = Team.builder().name("test-team").id(1L).build();
    when(teamRepository.findByCourseIdAndName(2L, "test-team")).thenReturn(Optional.of(team));
    when(githubTeamService.createTeam("test-team", course)).thenReturn(11);
    AddTeamToGithubJob job =
        AddTeamToGithubJob.builder()
            .teamName("test-team")
            .course(course)
            .teamRepository(teamRepository)
            .githubTeamService(githubTeamService)
            .build();

    job.accept(ctx);

    verify(githubTeamService, times(1)).createTeam(eq("test-team"), eq(course));
    assertEquals(11, team.getGithubTeamId());
    verify(teamRepository, times(1)).save(team);
  }

  @Test
  public void test_teamNameIsNull() throws Exception {
    // Test exception handling when team name is null
    Course course = Course.builder().orgName("test-org").installationId("123").build();

    AddTeamToGithubJob job =
        AddTeamToGithubJob.builder()
            .teamName(null)
            .course(course)
            .githubTeamService(githubTeamService)
            .teamRepository(teamRepository)
            .build();

    job.accept(ctx);

    verify(githubTeamService, never()).createTeam(anyString(), any(Course.class));
    assertTrue(jobStarted.getLog().contains("ERROR: Team has no name"));
  }

  @Test
  public void test_CourseWithoutGithubOrg() throws Exception {
    // Test case where course orgName is null
    Course course = Course.builder().id(1L).courseName("Test Course").build();

    AddTeamToGithubJob job =
        AddTeamToGithubJob.builder()
            .teamName("test-team")
            .course(course)
            .githubTeamService(githubTeamService)
            .teamRepository(teamRepository)
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

    AddTeamToGithubJob job =
        AddTeamToGithubJob.builder()
            .teamName("test-team")
            .course(course)
            .githubTeamService(githubTeamService)
            .teamRepository(teamRepository)
            .build();

    job.accept(ctx);

    verifyNoInteractions(githubTeamService);
    assertTrue(jobStarted.getLog().contains("ERROR: Course has no linked GitHub organization"));
  }

  @Test
  public void test_teamIsNull() throws Exception {
    // Test case where team with given name is not found in team repository
    when(teamRepository.findByCourseIdAndName(1L, "test-team")).thenReturn(Optional.empty());
    Course course =
        Course.builder()
            .id(1L)
            .courseName("Test Course")
            .orgName("test-org")
            .installationId("123")
            .build();

    AddTeamToGithubJob job =
        AddTeamToGithubJob.builder()
            .teamName("test-team")
            .course(course)
            .githubTeamService(githubTeamService)
            .teamRepository(teamRepository)
            .build();

    job.accept(ctx);

    verifyNoInteractions(githubTeamService);
    assertTrue(
        jobStarted
            .getLog()
            .contains("ERROR: Team with name 'test-team' not found for course Test Course"));
  }

  @Test
  public void test_TeamCreationFailure() throws Exception {
    // Test exception handling when team creation fails
    Course course =
        Course.builder()
            .id(1L)
            .courseName("Test Course")
            .orgName("test-org")
            .installationId("123")
            .build();

    Team team = Team.builder().name("test-team").id(1L).build();
    when(teamRepository.findByCourseIdAndName(1L, "test-team")).thenReturn(Optional.of(team));

    doThrow(new RuntimeException("GitHub API error"))
        .when(githubTeamService)
        .createTeam("test-team", course);

    AddTeamToGithubJob job =
        AddTeamToGithubJob.builder()
            .teamName("test-team")
            .course(course)
            .githubTeamService(githubTeamService)
            .teamRepository(teamRepository)
            .build();

    job.accept(ctx);

    verify(githubTeamService).createTeam("test-team", course);
    assertTrue(
        jobStarted.getLog().contains("ERROR: Failed to add team to GitHub: GitHub API error"));
  }
}
