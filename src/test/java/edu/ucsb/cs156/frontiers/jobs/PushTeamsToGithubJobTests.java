package edu.ucsb.cs156.frontiers.jobs;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.Job;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.Team;
import edu.ucsb.cs156.frontiers.entities.TeamMember;
import edu.ucsb.cs156.frontiers.enums.TeamStatus;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.TeamMemberRepository;
import edu.ucsb.cs156.frontiers.repositories.TeamRepository;
import edu.ucsb.cs156.frontiers.services.GithubTeamService;
import edu.ucsb.cs156.frontiers.services.jobs.JobContext;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PushTeamsToGithubJobTests {

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
  public void testAccept_CourseNotFound() throws Exception {
    // Arrange
    Long courseId = 1L;
    when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

    PushTeamsToGithubJob job =
        PushTeamsToGithubJob.builder()
            .courseId(courseId)
            .courseRepository(courseRepository)
            .teamRepository(teamRepository)
            .teamMemberRepository(teamMemberRepository)
            .githubTeamService(githubTeamService)
            .build();

    // Act
    job.accept(ctx);

    // Assert
    verify(courseRepository).findById(courseId);
    verifyNoInteractions(teamRepository, teamMemberRepository, githubTeamService);
  }

  @Test
  public void testAccept_CourseWithoutGithubOrg() throws Exception {
    // Arrange
    Long courseId = 1L;
    Course course = Course.builder().id(courseId).courseName("Test Course").build();
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

    PushTeamsToGithubJob job =
        PushTeamsToGithubJob.builder()
            .courseId(courseId)
            .courseRepository(courseRepository)
            .teamRepository(teamRepository)
            .teamMemberRepository(teamMemberRepository)
            .githubTeamService(githubTeamService)
            .build();

    // Act
    job.accept(ctx);

    // Assert
    verify(courseRepository).findById(courseId);
    verifyNoInteractions(teamRepository, teamMemberRepository, githubTeamService);
  }

  @Test
  public void testAccept_CourseWithOrgNameButNoInstallationId() throws Exception {
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
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

    PushTeamsToGithubJob job =
        PushTeamsToGithubJob.builder()
            .courseId(courseId)
            .courseRepository(courseRepository)
            .teamRepository(teamRepository)
            .teamMemberRepository(teamMemberRepository)
            .githubTeamService(githubTeamService)
            .build();

    // Act
    job.accept(ctx);

    // Assert
    verify(courseRepository).findById(courseId);
    verifyNoInteractions(teamRepository, teamMemberRepository, githubTeamService);
  }

  @Test
  public void testAccept_SuccessfulTeamCreationAndMemberProcessing() throws Exception {
    // Arrange
    Long courseId = 1L;
    Course course =
        Course.builder()
            .id(courseId)
            .courseName("Test Course")
            .orgName("test-org")
            .installationId("123")
            .build();

    RosterStudent student1 =
        RosterStudent.builder().email("student1@test.com").githubLogin("student1").build();

    RosterStudent student2 =
        RosterStudent.builder().email("student2@test.com").githubLogin(null).build();

    TeamMember teamMember1 = TeamMember.builder().rosterStudent(student1).build();
    TeamMember teamMember2 = TeamMember.builder().rosterStudent(student2).build();

    Team team1 =
        Team.builder()
            .name("team1")
            .githubTeamId(null)
            .teamMembers(Set.of(teamMember1, teamMember2))
            .build();

    Team team2 = Team.builder().name("team2").githubTeamId(456).teamMembers(Set.of()).build();

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(teamRepository.findByCourseId(courseId)).thenReturn(Arrays.asList(team1, team2));

    // Mock GitHub service calls
    when(githubTeamService.createOrGetTeamId(team1, course)).thenReturn(123);
    when(githubTeamService.createOrGetTeamId(team2, course)).thenReturn(456);
    when(githubTeamService.getTeamMembershipStatus("student1", 123, course))
        .thenReturn(TeamStatus.NOT_ORG_MEMBER);
    when(githubTeamService.addMemberToGithubTeam("student1", 123, "member", course, 1))
        .thenReturn(TeamStatus.TEAM_MEMBER);
    when(githubTeamService.getOrgId("test-org", course)).thenReturn(1);

    PushTeamsToGithubJob job =
        PushTeamsToGithubJob.builder()
            .courseId(courseId)
            .courseRepository(courseRepository)
            .teamRepository(teamRepository)
            .teamMemberRepository(teamMemberRepository)
            .githubTeamService(githubTeamService)
            .build();

    // Act
    job.accept(ctx);

    // Assert
    verify(courseRepository).findById(courseId);
    verify(teamRepository).findByCourseId(courseId);
    verify(githubTeamService).createOrGetTeamId(team1, course);
    verify(githubTeamService).createOrGetTeamId(team2, course);
    verify(githubTeamService).getTeamMembershipStatus("student1", 123, course);
    verify(githubTeamService).addMemberToGithubTeam("student1", 123, "member", course, 1);

    // Verify team1 was updated with GitHub team ID
    verify(teamRepository)
        .save(argThat(t -> t.getName().equals("team1") && t.getGithubTeamId().equals(123)));

    // Verify team members were updated with correct status
    verify(teamMemberRepository)
        .save(
            argThat(
                tm ->
                    tm.getRosterStudent().equals(student1)
                        && tm.getTeamStatus().equals(TeamStatus.TEAM_MEMBER)));
    verify(teamMemberRepository)
        .save(
            argThat(
                tm ->
                    tm.getRosterStudent().equals(student2)
                        && tm.getTeamStatus().equals(TeamStatus.NO_GITHUB_ID)));
  }

  @Test
  public void testAccept_ExistingTeamMember() throws Exception {
    // Test the TEAM_MEMBER branch in the condition (first part of OR)
    // Arrange
    Long courseId = 1L;
    Course course =
        Course.builder()
            .id(courseId)
            .courseName("Test Course")
            .orgName("test-org")
            .installationId("123")
            .build();

    RosterStudent student =
        RosterStudent.builder().email("student@test.com").githubLogin("student").build();

    TeamMember teamMember = TeamMember.builder().rosterStudent(student).build();

    Team team =
        Team.builder().name("team1").githubTeamId(123).teamMembers(Set.of(teamMember)).build();

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(teamRepository.findByCourseId(courseId)).thenReturn(Arrays.asList(team));
    when(githubTeamService.createOrGetTeamId(team, course)).thenReturn(123);
    // This specifically tests the TEAM_MEMBER branch of the condition (first part of ||)
    when(githubTeamService.getTeamMembershipStatus("student", 123, course))
        .thenReturn(TeamStatus.TEAM_MEMBER);

    PushTeamsToGithubJob job =
        PushTeamsToGithubJob.builder()
            .courseId(courseId)
            .courseRepository(courseRepository)
            .teamRepository(teamRepository)
            .teamMemberRepository(teamMemberRepository)
            .githubTeamService(githubTeamService)
            .build();

    // Act
    job.accept(ctx);

    // Assert
    verify(githubTeamService).getTeamMembershipStatus("student", 123, course);
    // Should not try to add member since they're already a member
    verify(githubTeamService, never()).addMemberToGithubTeam(any(), any(), any(), any(), any());
    verify(teamMemberRepository)
        .save(argThat(tm -> tm.getTeamStatus().equals(TeamStatus.TEAM_MEMBER)));
  }

  @Test
  public void testAccept_ExistingTeamMaintainer() throws Exception {
    // Test the TEAM_MAINTAINER branch in the condition
    // Arrange
    Long courseId = 1L;
    Course course =
        Course.builder()
            .id(courseId)
            .courseName("Test Course")
            .orgName("test-org")
            .installationId("123")
            .build();

    RosterStudent student =
        RosterStudent.builder().email("student@test.com").githubLogin("student").build();

    TeamMember teamMember = TeamMember.builder().rosterStudent(student).build();

    Team team =
        Team.builder().name("team1").githubTeamId(123).teamMembers(Set.of(teamMember)).build();

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(teamRepository.findByCourseId(courseId)).thenReturn(Arrays.asList(team));
    when(githubTeamService.createOrGetTeamId(team, course)).thenReturn(123);
    // This specifically tests the TEAM_MAINTAINER branch of the condition
    when(githubTeamService.getTeamMembershipStatus("student", 123, course))
        .thenReturn(TeamStatus.TEAM_MAINTAINER);

    PushTeamsToGithubJob job =
        PushTeamsToGithubJob.builder()
            .courseId(courseId)
            .courseRepository(courseRepository)
            .teamRepository(teamRepository)
            .teamMemberRepository(teamMemberRepository)
            .githubTeamService(githubTeamService)
            .build();

    // Act
    job.accept(ctx);

    // Assert
    verify(githubTeamService).getTeamMembershipStatus("student", 123, course);
    // Should not try to add member since they're already a maintainer
    verify(githubTeamService, never()).addMemberToGithubTeam(any(), any(), any(), any(), any());
    verify(teamMemberRepository)
        .save(argThat(tm -> tm.getTeamStatus().equals(TeamStatus.TEAM_MAINTAINER)));
  }

  @Test
  public void testAccept_TeamCreationFailure() throws Exception {
    // Test exception handling when team creation fails (lines 59-60)
    // Arrange
    Long courseId = 1L;
    Course course =
        Course.builder()
            .id(courseId)
            .courseName("Test Course")
            .orgName("test-org")
            .installationId("123")
            .build();

    Team team = Team.builder().name("team1").githubTeamId(null).teamMembers(Set.of()).build();

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(teamRepository.findByCourseId(courseId)).thenReturn(Arrays.asList(team));
    when(githubTeamService.createOrGetTeamId(team, course))
        .thenThrow(new RuntimeException("GitHub API error"));

    PushTeamsToGithubJob job =
        PushTeamsToGithubJob.builder()
            .courseId(courseId)
            .courseRepository(courseRepository)
            .teamRepository(teamRepository)
            .teamMemberRepository(teamMemberRepository)
            .githubTeamService(githubTeamService)
            .build();

    // Act
    job.accept(ctx);

    // Assert
    verify(courseRepository).findById(courseId);
    verify(teamRepository).findByCourseId(courseId);
    verify(githubTeamService).createOrGetTeamId(team, course);
    // Should not save team or process members when creation fails
    verify(teamRepository, never()).save(any());
  }

  @Test
  public void testAccept_TeamWithNoGithubTeamId() throws Exception {
    // Test skipping team members when team has no GitHub team ID (lines 66-68)
    // Arrange
    Long courseId = 1L;
    Course course =
        Course.builder()
            .id(courseId)
            .courseName("Test Course")
            .orgName("test-org")
            .installationId("123")
            .build();

    RosterStudent student =
        RosterStudent.builder().email("student@test.com").githubLogin("student").build();
    TeamMember teamMember = TeamMember.builder().rosterStudent(student).build();

    // Team with null GitHub team ID - this should cause member processing to be skipped
    Team team =
        Team.builder().name("team1").githubTeamId(null).teamMembers(Set.of(teamMember)).build();

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(teamRepository.findByCourseId(courseId)).thenReturn(Arrays.asList(team));
    // Make team creation fail so GitHub team ID remains null
    when(githubTeamService.createOrGetTeamId(team, course))
        .thenThrow(new RuntimeException("Creation failed"));

    PushTeamsToGithubJob job =
        PushTeamsToGithubJob.builder()
            .courseId(courseId)
            .courseRepository(courseRepository)
            .teamRepository(teamRepository)
            .teamMemberRepository(teamMemberRepository)
            .githubTeamService(githubTeamService)
            .build();

    // Act
    job.accept(ctx);

    // Assert
    verify(courseRepository).findById(courseId);
    verify(teamRepository).findByCourseId(courseId);
    verify(githubTeamService).createOrGetTeamId(team, course);
    // Should not process team members when team has no GitHub team ID
    verify(githubTeamService, never()).getTeamMembershipStatus(any(), any(), any());
    verify(githubTeamService, never()).addMemberToGithubTeam(any(), any(), any(), any(), any());
    verify(teamMemberRepository, never()).save(any());
  }

  @Test
  public void testAccept_TeamMemberProcessingFailure() throws Exception {
    // Test exception handling when team member processing fails (lines 106-115)
    // Arrange
    Long courseId = 1L;
    Course course =
        Course.builder()
            .id(courseId)
            .courseName("Test Course")
            .orgName("test-org")
            .installationId("123")
            .build();

    RosterStudent student =
        RosterStudent.builder().email("student@test.com").githubLogin("student").build();
    TeamMember teamMember = TeamMember.builder().rosterStudent(student).build();

    Team team =
        Team.builder().name("team1").githubTeamId(123).teamMembers(Set.of(teamMember)).build();

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(teamRepository.findByCourseId(courseId)).thenReturn(Arrays.asList(team));
    when(githubTeamService.createOrGetTeamId(team, course)).thenReturn(123);
    // Make team member processing fail
    when(githubTeamService.getTeamMembershipStatus("student", 123, course))
        .thenThrow(new RuntimeException("GitHub API error"));

    PushTeamsToGithubJob job =
        PushTeamsToGithubJob.builder()
            .courseId(courseId)
            .courseRepository(courseRepository)
            .teamRepository(teamRepository)
            .teamMemberRepository(teamMemberRepository)
            .githubTeamService(githubTeamService)
            .build();

    // Act
    job.accept(ctx);

    // Assert
    verify(courseRepository).findById(courseId);
    verify(teamRepository).findByCourseId(courseId);
    verify(githubTeamService).createOrGetTeamId(team, course);
    verify(githubTeamService).getTeamMembershipStatus("student", 123, course);
    // Should set status to NOT_ORG_MEMBER when processing fails
    verify(teamMemberRepository)
        .save(argThat(tm -> tm.getTeamStatus().equals(TeamStatus.NOT_ORG_MEMBER)));
  }

  @Test
  public void test_Accept_GetOrgIdFailure() throws Exception {
    // Arrange
    Long courseId = 1L;
    Course course =
        Course.builder()
            .id(courseId)
            .courseName("Test Course")
            .orgName("test-org")
            .installationId("123")
            .build();

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(githubTeamService.getOrgId(course.getOrgName(), course))
        .thenThrow(new RuntimeException("GitHub API error"));

    PushTeamsToGithubJob job =
        PushTeamsToGithubJob.builder()
            .courseId(courseId)
            .courseRepository(courseRepository)
            .teamRepository(teamRepository)
            .teamMemberRepository(teamMemberRepository)
            .githubTeamService(githubTeamService)
            .build();

    // Act
    job.accept(ctx);

    // Assert
    verify(courseRepository).findById(courseId);
    verify(githubTeamService).getOrgId(course.getOrgName(), course);
    verifyNoInteractions(teamRepository, teamMemberRepository);
  }
}
