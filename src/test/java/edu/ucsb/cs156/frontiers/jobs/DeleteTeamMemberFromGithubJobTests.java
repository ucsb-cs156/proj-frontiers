package edu.ucsb.cs156.frontiers.jobs;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.Job;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.Team;
import edu.ucsb.cs156.frontiers.entities.TeamMember;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.TeamMemberRepository;
import edu.ucsb.cs156.frontiers.repositories.TeamRepository;
import edu.ucsb.cs156.frontiers.services.GithubTeamService;
import edu.ucsb.cs156.frontiers.services.jobs.JobContext;
import java.util.Arrays;
import java.util.Optional;
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
  public void test_successfully_delete_team_member() throws Exception {
    Course course = Course.builder().orgName("test-org").installationId("123").build();
    Team team = Team.builder().id(1234L).name("team-1").githubTeamId(456).course(course).build();
    RosterStudent rosterStudent =
        RosterStudent.builder().id(99L).githubLogin("testuser").course(course).build();
    TeamMember teamMember =
        TeamMember.builder().id(10L).team(team).rosterStudent(rosterStudent).build();

    when(teamMemberRepository.findById(10L)).thenReturn(Optional.of(teamMember));

    DeleteTeamMemberFromGithubJob job =
        DeleteTeamMemberFromGithubJob.builder()
            .teamMemberId(10L)
            .teamId(1234L)
            .teamMemberRepository(teamMemberRepository)
            .githubTeamService(githubTeamService)
            .build();

    job.accept(ctx);

    verify(githubTeamService, times(1))
        .removeMemberFromGithubTeam(eq("testuser"), eq(456), eq(course));
  }

  @Test
  public void test_CourseWithoutGithubOrg() throws Exception {
    Course course = Course.builder().id(1L).courseName("Test Course").build();
    Team team = Team.builder().id(1234L).name("team-1").githubTeamId(456).course(course).build();
    RosterStudent rosterStudent =
        RosterStudent.builder().id(99L).githubLogin("testuser").course(course).build();
    Long teamMemberId = 10L;
    TeamMember teamMember =
        TeamMember.builder().id(teamMemberId).team(team).rosterStudent(rosterStudent).build();

    when(teamMemberRepository.findById(teamMemberId)).thenReturn(Optional.of(teamMember));

    DeleteTeamMemberFromGithubJob job =
        DeleteTeamMemberFromGithubJob.builder()
            .teamMemberId(teamMemberId)
            .teamId(1234L)
            .teamMemberRepository(teamMemberRepository)
            .githubTeamService(githubTeamService)
            .build();

    // Act
    job.accept(ctx);

    // Assert
    verify(teamMemberRepository).findById(teamMemberId);
    verifyNoInteractions(githubTeamService);
  }

  @Test
  public void test_CourseWithOrgNameButNoInstallationId() throws Exception {
    // Test case where orgName is not null but installationId is null
    // Arrange
    Long courseId = 1L;
    Course course =
        Course.builder()
            .id(courseId)
            .courseName("Test Course")
            .orgName("test-org")
            .installationId(null)
            .build();
    Team team = Team.builder().id(1234L).name("team-1").githubTeamId(456).course(course).build();
    RosterStudent rosterStudent =
        RosterStudent.builder().id(99L).githubLogin("testuser").course(course).build();
    Long teamMemberId = 10L;
    TeamMember teamMember =
        TeamMember.builder().id(teamMemberId).team(team).rosterStudent(rosterStudent).build();

    when(teamMemberRepository.findById(teamMemberId)).thenReturn(Optional.of(teamMember));

    DeleteTeamMemberFromGithubJob job =
        DeleteTeamMemberFromGithubJob.builder()
            .teamMemberId(teamMemberId)
            .teamId(1234L)
            .teamMemberRepository(teamMemberRepository)
            .githubTeamService(githubTeamService)
            .build();

    // Act
    job.accept(ctx);

    // Assert
    verify(teamMemberRepository).findById(teamMemberId);
    verifyNoInteractions(githubTeamService);
  }

  @Test
  public void test_TeamMemberRemovalFailure() throws Exception {
    // Test exception handling when team member removal fails
    // Arrange
    Long courseId = 1L;
    Course course =
        Course.builder()
            .id(courseId)
            .courseName("Test Course")
            .orgName("test-org")
            .installationId("123")
            .build();

    Team team1 =
        Team.builder().name("team1").githubTeamId(null).teamMembers(Arrays.asList()).build();
    Team team =
        Team.builder()
            .id(1234L)
            .name("team-1")
            .githubTeamId(456)
            .teamMembers(Arrays.asList())
            .course(course)
            .build();
    RosterStudent rosterStudent =
        RosterStudent.builder().id(99L).githubLogin("testuser").course(course).build();
    Long teamMemberId = 10L;
    TeamMember teamMember =
        TeamMember.builder().id(teamMemberId).team(team).rosterStudent(rosterStudent).build();

    when(teamMemberRepository.findById(teamMemberId)).thenReturn(Optional.of(teamMember));
    doThrow(new RuntimeException("GitHub API error"))
        .when(githubTeamService)
        .removeMemberFromGithubTeam("testuser", 456, course);

    DeleteTeamMemberFromGithubJob job =
        DeleteTeamMemberFromGithubJob.builder()
            .teamMemberId(teamMemberId)
            .teamId(1234L)
            .teamMemberRepository(teamMemberRepository)
            .githubTeamService(githubTeamService)
            .build();

    // Act
    job.accept(ctx);

    // Assert
    verify(teamMemberRepository).findById(teamMemberId);
    verify(githubTeamService).removeMemberFromGithubTeam("testuser", 456, course);
    // Should not save team or process members when creation fails
    verify(teamRepository, never()).save(any());
  }

  @Test
  public void test_TeamMemberNotFound() throws Exception {
    // Arrange
    Long teamMemberId = 1L;
    when(teamMemberRepository.findById(teamMemberId)).thenReturn(Optional.empty());

    DeleteTeamMemberFromGithubJob job =
        DeleteTeamMemberFromGithubJob.builder()
            .teamMemberId(teamMemberId)
            .teamId(1234L)
            .teamMemberRepository(teamMemberRepository)
            .githubTeamService(githubTeamService)
            .build();

    // Act
    job.accept(ctx);

    // Assert
    verify(teamMemberRepository).findById(teamMemberId);
    verifyNoInteractions(teamRepository, githubTeamService);
  }
}
