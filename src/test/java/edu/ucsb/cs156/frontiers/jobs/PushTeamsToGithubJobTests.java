package edu.ucsb.cs156.frontiers.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import edu.ucsb.cs156.frontiers.services.GithubTeamService.GithubTeamInfo;
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
  public void test_getCourse_returnsCourse_whenFound() {
    Long courseId = 1L;
    Course course = Course.builder().id(courseId).courseName("Test Course").build();

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

    PushTeamsToGithubJob job =
        PushTeamsToGithubJob.builder()
            .courseId(courseId)
            .courseRepository(courseRepository)
            .build();

    Course result = job.getCourse();

    assertEquals(course, result);
    verify(courseRepository, times(1)).findById(courseId);
  }

  @Test
  public void test_getCourse_returnsNull_whenNotFound() {
    Long courseId = 1L;

    when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

    PushTeamsToGithubJob job =
        PushTeamsToGithubJob.builder()
            .courseId(courseId)
            .courseRepository(courseRepository)
            .build();

    Course result = job.getCourse();

    assertNull(result);
    verify(courseRepository, times(1)).findById(courseId);
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
            .teamMembers(Arrays.asList(teamMember1, teamMember2))
            .build();

    Team team2 =
        Team.builder().name("team2").githubTeamId(456).teamMembers(Arrays.asList()).build();

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(teamRepository.findByCourseId(courseId)).thenReturn(Arrays.asList(team1, team2));

    // Mock GitHub service calls
    when(githubTeamService.createOrGetTeamInfo(team1, course))
        .thenReturn(new GithubTeamInfo(123, "team1", "team1"));
    when(githubTeamService.createOrGetTeamInfo(team2, course))
        .thenReturn(new GithubTeamInfo(456, "team2", "team2"));
    when(githubTeamService.getTeamMembershipStatus("student1", 123, course, 1))
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
    verify(githubTeamService).createOrGetTeamInfo(team1, course);
    verify(githubTeamService).createOrGetTeamInfo(team2, course);
    verify(githubTeamService).getTeamMembershipStatus("student1", 123, course, 1);
    verify(githubTeamService).addMemberToGithubTeam("student1", 123, "member", course, 1);

    // Verify team1 was updated with GitHub team ID
    verify(teamRepository)
        .save(
            argThat(
                t ->
                    t.getName().equals("team1")
                        && t.getGithubTeamId().equals(123)
                        && t.getGithubTeamSlug().equals("team1")));

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
        Team.builder()
            .name("team1")
            .githubTeamId(123)
            .githubTeamSlug("team1")
            .teamMembers(Arrays.asList(teamMember))
            .build();

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(teamRepository.findByCourseId(courseId)).thenReturn(Arrays.asList(team));
    when(githubTeamService.createOrGetTeamInfo(team, course))
        .thenReturn(new GithubTeamInfo(123, "team1", "team1"));
    // This specifically tests the TEAM_MEMBER branch of the condition (first part of ||)
    when(githubTeamService.getTeamMembershipStatus("student", 123, course, 1))
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
    verify(githubTeamService).getTeamMembershipStatus("student", 123, course, 1);
    verify(teamRepository, never()).save(any(Team.class));
    // Should not try to add member since they're already a member
    verify(githubTeamService, never()).addMemberToGithubTeam(any(), any(), any(), any(), any());
    verify(teamMemberRepository)
        .save(argThat(tm -> tm.getTeamStatus().equals(TeamStatus.TEAM_MEMBER)));
    assertTrue(jobStarted.getLog().contains("already has correct GitHub team ID: 123"));
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
        Team.builder()
            .name("team1")
            .githubTeamId(123)
            .githubTeamSlug("team1")
            .teamMembers(Arrays.asList(teamMember))
            .build();

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(teamRepository.findByCourseId(courseId)).thenReturn(Arrays.asList(team));
    when(githubTeamService.createOrGetTeamInfo(team, course))
        .thenReturn(new GithubTeamInfo(123, "team1", "team1"));
    // This specifically tests the TEAM_MAINTAINER branch of the condition
    when(githubTeamService.getTeamMembershipStatus("student", 123, course, 1))
        .thenReturn(TeamStatus.TEAM_MAINTAINER);
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
    verify(githubTeamService).getTeamMembershipStatus("student", 123, course, 1);
    verify(teamRepository, never()).save(any(Team.class));
    // Should not try to add member since they're already a maintainer
    verify(githubTeamService, never()).addMemberToGithubTeam(any(), any(), any(), any(), any());
    verify(teamMemberRepository)
        .save(argThat(tm -> tm.getTeamStatus().equals(TeamStatus.TEAM_MAINTAINER)));
    assertTrue(jobStarted.getLog().contains("already has correct GitHub team ID: 123"));
  }

  @Test
  public void testAccept_UpdatesTeamWhenOnlyGithubTeamIdDiffers() throws Exception {
    Long courseId = 1L;
    Course course =
        Course.builder()
            .id(courseId)
            .courseName("Test Course")
            .orgName("test-org")
            .installationId("123")
            .build();

    Team team =
        Team.builder()
            .name("team1")
            .githubTeamId(999)
            .githubTeamSlug("team1")
            .teamMembers(Arrays.asList())
            .build();

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(teamRepository.findByCourseId(courseId)).thenReturn(Arrays.asList(team));
    when(githubTeamService.getOrgId("test-org", course)).thenReturn(1);
    when(githubTeamService.createOrGetTeamInfo(team, course))
        .thenReturn(new GithubTeamInfo(123, "team1", "team1"));

    PushTeamsToGithubJob job =
        PushTeamsToGithubJob.builder()
            .courseId(courseId)
            .courseRepository(courseRepository)
            .teamRepository(teamRepository)
            .teamMemberRepository(teamMemberRepository)
            .githubTeamService(githubTeamService)
            .build();

    job.accept(ctx);

    verify(teamRepository)
        .save(
            argThat(
                t ->
                    t == team
                        && t.getGithubTeamId().equals(123)
                        && t.getGithubTeamSlug().equals("team1")));
    assertTrue(jobStarted.getLog().contains("Updated team 'team1' with GitHub team ID: 123"));
  }

  @Test
  public void testAccept_UpdatesTeamWhenOnlyGithubTeamSlugDiffers() throws Exception {
    Long courseId = 1L;
    Course course =
        Course.builder()
            .id(courseId)
            .courseName("Test Course")
            .orgName("test-org")
            .installationId("123")
            .build();

    Team team =
        Team.builder()
            .name("team1")
            .githubTeamId(123)
            .githubTeamSlug("old-slug")
            .teamMembers(Arrays.asList())
            .build();

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(teamRepository.findByCourseId(courseId)).thenReturn(Arrays.asList(team));
    when(githubTeamService.getOrgId("test-org", course)).thenReturn(1);
    when(githubTeamService.createOrGetTeamInfo(team, course))
        .thenReturn(new GithubTeamInfo(123, "team1", "team1"));

    PushTeamsToGithubJob job =
        PushTeamsToGithubJob.builder()
            .courseId(courseId)
            .courseRepository(courseRepository)
            .teamRepository(teamRepository)
            .teamMemberRepository(teamMemberRepository)
            .githubTeamService(githubTeamService)
            .build();

    job.accept(ctx);

    verify(teamRepository)
        .save(
            argThat(
                t ->
                    t == team
                        && t.getGithubTeamId().equals(123)
                        && t.getGithubTeamSlug().equals("team1")));
    assertTrue(jobStarted.getLog().contains("Updated team 'team1' with GitHub team ID: 123"));
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

    Team team =
        Team.builder().name("team1").githubTeamId(null).teamMembers(Arrays.asList()).build();

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(teamRepository.findByCourseId(courseId)).thenReturn(Arrays.asList(team));
    when(githubTeamService.createOrGetTeamInfo(team, course))
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
    verify(githubTeamService).createOrGetTeamInfo(team, course);
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
        Team.builder()
            .name("team1")
            .githubTeamId(null)
            .teamMembers(Arrays.asList(teamMember))
            .build();

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(teamRepository.findByCourseId(courseId)).thenReturn(Arrays.asList(team));
    // Make team creation fail so GitHub team ID remains null
    when(githubTeamService.createOrGetTeamInfo(team, course))
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
    verify(githubTeamService).createOrGetTeamInfo(team, course);
    // Should not process team members when team has no GitHub team ID
    verify(githubTeamService, never()).getTeamMembershipStatus(any(), any(), any(), any());
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
        Team.builder()
            .name("team1")
            .githubTeamId(123)
            .teamMembers(Arrays.asList(teamMember))
            .build();

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(teamRepository.findByCourseId(courseId)).thenReturn(Arrays.asList(team));
    when(githubTeamService.createOrGetTeamInfo(team, course))
        .thenReturn(new GithubTeamInfo(123, "team1", "team1"));
    // Make team member processing fail
    when(githubTeamService.getTeamMembershipStatus("student", 123, course, 1))
        .thenThrow(new RuntimeException("GitHub API error"));
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
    verify(githubTeamService).createOrGetTeamInfo(team, course);
    verify(githubTeamService).getTeamMembershipStatus("student", 123, course, 1);
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

    IllegalStateException e = assertThrows(IllegalStateException.class, () -> job.accept(ctx));

    verify(courseRepository).findById(courseId);
    verify(githubTeamService).getOrgId(course.getOrgName(), course);
    verifyNoInteractions(teamRepository, teamMemberRepository);
    assertEquals(
        "Failed to get organization ID for org: test-org - GitHub API error", e.getMessage());
  }
}
