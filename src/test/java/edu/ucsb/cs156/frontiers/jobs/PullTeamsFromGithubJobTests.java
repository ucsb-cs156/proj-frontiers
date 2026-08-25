package edu.ucsb.cs156.frontiers.jobs;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.Team;
import edu.ucsb.cs156.frontiers.entities.TeamMember;
import edu.ucsb.cs156.frontiers.enums.TeamStatus;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.TeamMemberRepository;
import edu.ucsb.cs156.frontiers.repositories.TeamRepository;
import edu.ucsb.cs156.frontiers.services.GithubTeamService;
import edu.ucsb.cs156.frontiers.services.GithubTeamService.GithubTeamInfo;
import edu.ucsb.cs156.jobs.entities.Job;
import edu.ucsb.cs156.jobs.errors.JobCancelledException;
import edu.ucsb.cs156.jobs.repositories.JobsRepository;
import edu.ucsb.cs156.jobs.services.JobContext;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PullTeamsFromGithubJobTests {

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
  public void test_getScope_returnsCourseScope() {
    Long courseId = 1L;

    PullTeamsFromGithubJob job =
        PullTeamsFromGithubJob.builder()
            .courseId(courseId)
            .courseRepository(courseRepository)
            .build();

    assertEquals("course", job.getScopeType());
    assertEquals(courseId, job.getScopeId());
  }

  @Test
  public void testAccept_CourseNotFound() throws Exception {
    Long courseId = 1L;
    when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

    PullTeamsFromGithubJob job =
        PullTeamsFromGithubJob.builder()
            .courseId(courseId)
            .courseRepository(courseRepository)
            .teamRepository(teamRepository)
            .teamMemberRepository(teamMemberRepository)
            .githubTeamService(githubTeamService)
            .build();

    job.accept(ctx);

    verify(courseRepository).findById(courseId);
    verifyNoInteractions(teamRepository, teamMemberRepository, githubTeamService);
    assertTrue(jobStarted.getLog().contains("ERROR: Course with ID 1 not found"));
  }

  @Test
  public void testAccept_CourseWithoutGithubOrg() throws Exception {
    Long courseId = 1L;
    Course course = Course.builder().id(courseId).courseName("Test Course").build();
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

    PullTeamsFromGithubJob job =
        PullTeamsFromGithubJob.builder()
            .courseId(courseId)
            .courseRepository(courseRepository)
            .teamRepository(teamRepository)
            .teamMemberRepository(teamMemberRepository)
            .githubTeamService(githubTeamService)
            .build();

    job.accept(ctx);

    verify(courseRepository).findById(courseId);
    verifyNoInteractions(teamRepository, teamMemberRepository, githubTeamService);
    assertTrue(jobStarted.getLog().contains("ERROR: Course has no linked GitHub organization"));
  }

  @Test
  public void testAccept_CourseWithOrgNameButNoInstallationId() throws Exception {
    // Test case where orgName is not null but installationId is null
    Long courseId = 1L;
    Course course =
        Course.builder()
            .id(courseId)
            .courseName("Test Course")
            .orgName("test-org")
            .installationId(null)
            .build();
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

    PullTeamsFromGithubJob job =
        PullTeamsFromGithubJob.builder()
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
  public void testAccept_GetAllTeamsFailure() throws Exception {
    Long courseId = 1L;
    Course course =
        Course.builder()
            .id(courseId)
            .courseName("Test Course")
            .orgName("test-org")
            .installationId("123")
            .build();

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(githubTeamService.getAllTeams(course)).thenThrow(new RuntimeException("GitHub API error"));

    PullTeamsFromGithubJob job =
        PullTeamsFromGithubJob.builder()
            .courseId(courseId)
            .courseRepository(courseRepository)
            .teamRepository(teamRepository)
            .teamMemberRepository(teamMemberRepository)
            .githubTeamService(githubTeamService)
            .build();

    job.accept(ctx);

    verify(courseRepository).findById(courseId);
    verify(githubTeamService).getAllTeams(course);
    verifyNoInteractions(teamMemberRepository);
  }

  @Test
  public void testAccept_UpdatesExistingTeamsByGithubIdAndNameAndCreatesTeam() throws Exception {
    Long courseId = 1L;
    Course course =
        Course.builder()
            .id(courseId)
            .courseName("Test Course")
            .orgName("test-org")
            .installationId("123")
            .build();

    Team existingByName =
        Team.builder().name("team-by-name").githubTeamId(null).course(course).build();
    Team existingById = Team.builder().name("old-name").githubTeamId(222).course(course).build();
    Team unchanged =
        Team.builder()
            .name("same-team")
            .githubTeamId(333)
            .githubTeamSlug("same-team")
            .course(course)
            .build();

    List<GithubTeamInfo> githubTeams =
        Arrays.asList(
            new GithubTeamInfo(111, "team-by-name", "team-by-name"),
            new GithubTeamInfo(222, "renamed-team", "renamed-team"),
            new GithubTeamInfo(333, "same-team", "same-team"),
            new GithubTeamInfo(444, "new-team", "new-team"));

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(githubTeamService.getAllTeams(course)).thenReturn(githubTeams);
    when(teamRepository.findByCourseId(courseId))
        .thenReturn(Arrays.asList(existingByName, existingById, unchanged));

    PullTeamsFromGithubJob job =
        PullTeamsFromGithubJob.builder()
            .courseId(courseId)
            .courseRepository(courseRepository)
            .teamRepository(teamRepository)
            .teamMemberRepository(teamMemberRepository)
            .githubTeamService(githubTeamService)
            .build();

    job.accept(ctx);

    verify(githubTeamService).getAllTeams(course);
    verify(teamRepository).findByCourseId(courseId);

    verify(teamRepository)
        .save(
            argThat(
                t ->
                    t.getName().equals("team-by-name")
                        && t.getGithubTeamId().equals(111)
                        && t.getGithubTeamSlug().equals("team-by-name")));
    verify(teamRepository)
        .save(
            argThat(
                t ->
                    t.getName().equals("renamed-team")
                        && t.getGithubTeamId().equals(222)
                        && t.getGithubTeamSlug().equals("renamed-team")));
    verify(teamRepository)
        .save(
            argThat(
                t ->
                    t == existingById
                        && t.getName().equals("renamed-team")
                        && t.getGithubTeamSlug().equals("renamed-team")));
    verify(teamRepository)
        .save(
            argThat(
                t ->
                    t.getName().equals("new-team")
                        && t.getGithubTeamId().equals(444)
                        && t.getGithubTeamSlug().equals("new-team")
                        && t.getCourse().equals(course)));
    verify(teamRepository, times(3)).save(any(Team.class));

    verify(teamRepository, never())
        .save(argThat(t -> t.getName().equals("same-team") && t.getGithubTeamId().equals(333)));
    verify(githubTeamService, never()).getTeamMemberships(any(), any());
    assertTrue(
        jobStarted.getLog().contains("Starting pull teams from GitHub job for course ID: 1"));
    assertTrue(jobStarted.getLog().contains("Processing course: Test Course (org: test-org)"));
    assertTrue(
        jobStarted.getLog().contains("Created local team 'new-team' with GitHub team ID: 444"));
    assertTrue(
        jobStarted.getLog().contains("Updated local team 'team-by-name' with GitHub team ID: 111"));
    assertTrue(jobStarted.getLog().contains("created: 1, updated: 2, unchanged: 1"));
  }

  @Test
  public void testAccept_RemovesOldGithubIdMappingWhenMatchingByName() throws Exception {
    Long courseId = 1L;
    Course course =
        Course.builder()
            .id(courseId)
            .courseName("Test Course")
            .orgName("test-org")
            .installationId("123")
            .build();

    Team existingByNameWithGithubId =
        Team.builder().name("team-by-name").githubTeamId(999).course(course).build();

    List<GithubTeamInfo> githubTeams =
        Arrays.asList(
            new GithubTeamInfo(111, "team-by-name", "team-by-name"),
            new GithubTeamInfo(999, "new-team", "new-team"));

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(githubTeamService.getAllTeams(course)).thenReturn(githubTeams);
    when(teamRepository.findByCourseId(courseId))
        .thenReturn(Arrays.asList(existingByNameWithGithubId));

    PullTeamsFromGithubJob job =
        PullTeamsFromGithubJob.builder()
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
                    t == existingByNameWithGithubId
                        && t.getName().equals("team-by-name")
                        && t.getGithubTeamId().equals(111)
                        && t.getGithubTeamSlug().equals("team-by-name")));
    verify(teamRepository)
        .save(
            argThat(
                t ->
                    t != existingByNameWithGithubId
                        && t.getName().equals("new-team")
                        && t.getGithubTeamId().equals(999)
                        && t.getGithubTeamSlug().equals("new-team")
                        && t.getCourse().equals(course)));
    verify(teamRepository, times(2)).save(any(Team.class));
    verify(githubTeamService, never()).getTeamMemberships(any(), any());
    assertTrue(jobStarted.getLog().contains("created: 1, updated: 1, unchanged: 0"));
  }

  @Test
  public void testAccept_UpdatesExistingTeamWhenOnlySlugChanges() throws Exception {
    Long courseId = 1L;
    Course course =
        Course.builder()
            .id(courseId)
            .courseName("Test Course")
            .orgName("test-org")
            .installationId("123")
            .build();

    Team localTeam =
        Team.builder()
            .name("team-a")
            .githubTeamId(111)
            .githubTeamSlug("old-slug")
            .course(course)
            .build();

    List<GithubTeamInfo> githubTeams = Arrays.asList(new GithubTeamInfo(111, "team-a", "team-a"));

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(githubTeamService.getAllTeams(course)).thenReturn(githubTeams);
    when(teamRepository.findByCourseId(courseId)).thenReturn(Arrays.asList(localTeam));

    PullTeamsFromGithubJob job =
        PullTeamsFromGithubJob.builder()
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
                    t == localTeam
                        && t.getName().equals("team-a")
                        && t.getGithubTeamId().equals(111)
                        && t.getGithubTeamSlug().equals("team-a")));
    verify(githubTeamService, never()).getTeamMemberships(any(), any());
    assertTrue(jobStarted.getLog().contains("created: 0, updated: 1, unchanged: 0"));
  }

  @Test
  public void testAccept_UpdatesExistingTeamWhenOnlyNameChanges() throws Exception {
    Long courseId = 1L;
    Course course =
        Course.builder()
            .id(courseId)
            .courseName("Test Course")
            .orgName("test-org")
            .installationId("123")
            .build();

    Team localTeam =
        Team.builder()
            .name("old-name")
            .githubTeamId(111)
            .githubTeamSlug("team-a")
            .course(course)
            .build();

    List<GithubTeamInfo> githubTeams = Arrays.asList(new GithubTeamInfo(111, "team-a", "team-a"));

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(githubTeamService.getAllTeams(course)).thenReturn(githubTeams);
    when(teamRepository.findByCourseId(courseId)).thenReturn(Arrays.asList(localTeam));

    PullTeamsFromGithubJob job =
        PullTeamsFromGithubJob.builder()
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
                    t == localTeam
                        && t.getName().equals("team-a")
                        && t.getGithubTeamId().equals(111)
                        && t.getGithubTeamSlug().equals("team-a")));
    verify(githubTeamService, never()).getTeamMemberships(any(), any());
    assertTrue(jobStarted.getLog().contains("created: 0, updated: 1, unchanged: 0"));
  }

  @Test
  public void testAccept_UpdatesExistingTeamWhenOnlyGithubIdChanges() throws Exception {
    Long courseId = 1L;
    Course course =
        Course.builder()
            .id(courseId)
            .courseName("Test Course")
            .orgName("test-org")
            .installationId("123")
            .build();

    Team localTeam =
        Team.builder()
            .name("team-a")
            .githubTeamId(999)
            .githubTeamSlug("team-a")
            .course(course)
            .build();

    List<GithubTeamInfo> githubTeams = Arrays.asList(new GithubTeamInfo(111, "team-a", "team-a"));

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(githubTeamService.getAllTeams(course)).thenReturn(githubTeams);
    when(teamRepository.findByCourseId(courseId)).thenReturn(Arrays.asList(localTeam));

    PullTeamsFromGithubJob job =
        PullTeamsFromGithubJob.builder()
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
                    t == localTeam
                        && t.getName().equals("team-a")
                        && t.getGithubTeamId().equals(111)
                        && t.getGithubTeamSlug().equals("team-a")));
    verify(githubTeamService, never()).getTeamMemberships(any(), any());
    assertTrue(jobStarted.getLog().contains("created: 0, updated: 1, unchanged: 0"));
  }

  @Test
  public void testAccept_BackfillsLocalNullSlugWhenNameAndIdAlreadyMatch() throws Exception {
    Long courseId = 1L;
    Course course =
        Course.builder()
            .id(courseId)
            .courseName("Test Course")
            .orgName("test-org")
            .installationId("123")
            .build();

    Team localTeam =
        Team.builder().name("team-a").githubTeamId(111).githubTeamSlug(null).course(course).build();

    List<GithubTeamInfo> githubTeams = Arrays.asList(new GithubTeamInfo(111, "team-a", "team-a"));

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(githubTeamService.getAllTeams(course)).thenReturn(githubTeams);
    when(teamRepository.findByCourseId(courseId)).thenReturn(Arrays.asList(localTeam));

    PullTeamsFromGithubJob job =
        PullTeamsFromGithubJob.builder()
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
                    t == localTeam
                        && t.getName().equals("team-a")
                        && t.getGithubTeamId().equals(111)
                        && t.getGithubTeamSlug().equals("team-a")));
    verify(githubTeamService, never()).getTeamMemberships(any(), any());
    assertTrue(jobStarted.getLog().contains("created: 0, updated: 1, unchanged: 0"));
  }

  @Test
  public void testAccept_AddsAndUpdatesTeamMembersForPulledTeams() throws Exception {
    Long courseId = 1L;
    RosterStudent memberStudent = RosterStudent.builder().githubLogin("member-login").build();
    RosterStudent existingStudent = RosterStudent.builder().githubLogin("existing-login").build();
    RosterStudent nonMemberStudent =
        RosterStudent.builder().githubLogin("non-member-login").build();
    RosterStudent noGithubStudent = RosterStudent.builder().githubLogin(null).build();

    Course course =
        Course.builder()
            .id(courseId)
            .courseName("Test Course")
            .orgName("test-org")
            .installationId("123")
            .rosterStudents(
                Arrays.asList(memberStudent, existingStudent, nonMemberStudent, noGithubStudent))
            .build();

    Team localTeam =
        Team.builder()
            .name("team-a")
            .githubTeamId(111)
            .githubTeamSlug("team-a")
            .course(course)
            .build();
    List<GithubTeamInfo> githubTeams = Arrays.asList(new GithubTeamInfo(111, "team-a", "team-a"));
    TeamMember existingTeamMember =
        TeamMember.builder().team(localTeam).rosterStudent(existingStudent).build();

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(githubTeamService.getAllTeams(course)).thenReturn(githubTeams);
    when(teamRepository.findByCourseId(courseId)).thenReturn(Arrays.asList(localTeam));
    when(githubTeamService.getTeamMemberships("team-a", course))
        .thenReturn(
            Map.of(
                "member-login",
                TeamStatus.TEAM_MEMBER,
                "existing-login",
                TeamStatus.TEAM_MAINTAINER,
                "not-in-local-roster",
                TeamStatus.TEAM_MEMBER));
    when(teamMemberRepository.findByTeamAndRosterStudent(localTeam, memberStudent))
        .thenReturn(Optional.empty());
    when(teamMemberRepository.findByTeamAndRosterStudent(localTeam, existingStudent))
        .thenReturn(Optional.of(existingTeamMember));

    PullTeamsFromGithubJob job =
        PullTeamsFromGithubJob.builder()
            .courseId(courseId)
            .courseRepository(courseRepository)
            .teamRepository(teamRepository)
            .teamMemberRepository(teamMemberRepository)
            .githubTeamService(githubTeamService)
            .build();

    job.accept(ctx);

    verify(githubTeamService).getTeamMemberships("team-a", course);

    verify(teamMemberRepository).findByTeamAndRosterStudent(localTeam, memberStudent);
    verify(teamMemberRepository).findByTeamAndRosterStudent(localTeam, existingStudent);
    verify(teamMemberRepository, never()).findByTeamAndRosterStudent(localTeam, nonMemberStudent);
    verify(teamMemberRepository, never()).findByTeamAndRosterStudent(localTeam, noGithubStudent);
    verify(teamMemberRepository)
        .save(
            argThat(
                tm ->
                    tm != existingTeamMember
                        && tm.getTeam().equals(localTeam)
                        && tm.getRosterStudent().equals(memberStudent)
                        && tm.getTeamStatus().equals(TeamStatus.TEAM_MEMBER)));
    verify(teamMemberRepository)
        .save(
            argThat(
                tm ->
                    tm == existingTeamMember
                        && tm.getTeam().equals(localTeam)
                        && tm.getRosterStudent().equals(existingStudent)
                        && tm.getTeamStatus().equals(TeamStatus.TEAM_MAINTAINER)));
    verify(teamMemberRepository, times(2)).save(any(TeamMember.class));
    assertTrue(
        jobStarted
            .getLog()
            .contains(
                "Created team member 'member-login' in team 'team-a' with status TEAM_MEMBER"));
    assertTrue(
        jobStarted
            .getLog()
            .contains(
                "Updated team member 'existing-login' in team 'team-a' with status"
                    + " TEAM_MAINTAINER"));
    assertTrue(jobStarted.getLog().contains("created: 0, updated: 1, unchanged: 0"));
  }

  @Test
  public void testAccept_CreatesNewTeamAndSyncsItsMembers() throws Exception {
    Long courseId = 1L;
    RosterStudent memberStudent = RosterStudent.builder().githubLogin("member-login").build();

    Course course =
        Course.builder()
            .id(courseId)
            .courseName("Test Course")
            .orgName("test-org")
            .installationId("123")
            .rosterStudents(Arrays.asList(memberStudent))
            .build();

    List<GithubTeamInfo> githubTeams =
        Arrays.asList(new GithubTeamInfo(111, "new-team", "new-team"));

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(githubTeamService.getAllTeams(course)).thenReturn(githubTeams);
    when(teamRepository.findByCourseId(courseId)).thenReturn(Arrays.asList());
    when(githubTeamService.getTeamMemberships("new-team", course))
        .thenReturn(Map.of("member-login", TeamStatus.TEAM_MEMBER));
    when(teamMemberRepository.findByTeamAndRosterStudent(any(Team.class), eq(memberStudent)))
        .thenReturn(Optional.empty());

    PullTeamsFromGithubJob job =
        PullTeamsFromGithubJob.builder()
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
                    t.getName().equals("new-team")
                        && t.getGithubTeamId().equals(111)
                        && t.getGithubTeamSlug().equals("new-team")
                        && t.getCourse().equals(course)));
    verify(teamMemberRepository)
        .findByTeamAndRosterStudent(
            argThat(t -> t.getName().equals("new-team") && t.getGithubTeamId().equals(111)),
            eq(memberStudent));
    verify(teamMemberRepository)
        .save(
            argThat(
                tm ->
                    tm.getTeam().getName().equals("new-team")
                        && tm.getTeam().getGithubTeamId().equals(111)
                        && tm.getRosterStudent().equals(memberStudent)
                        && tm.getTeamStatus().equals(TeamStatus.TEAM_MEMBER)));
    assertTrue(jobStarted.getLog().contains("created: 1, updated: 0, unchanged: 0"));
  }

  @Test
  public void testAccept_DoesNotUpdateOrLogWhenMembershipStatusIsUnchanged() throws Exception {
    Long courseId = 1L;
    RosterStudent existingStudent = RosterStudent.builder().githubLogin("existing-login").build();

    Course course =
        Course.builder()
            .id(courseId)
            .courseName("Test Course")
            .orgName("test-org")
            .installationId("123")
            .rosterStudents(Arrays.asList(existingStudent))
            .build();

    Team localTeam =
        Team.builder()
            .name("team-a")
            .githubTeamId(111)
            .githubTeamSlug("team-a")
            .course(course)
            .build();
    List<GithubTeamInfo> githubTeams = Arrays.asList(new GithubTeamInfo(111, "team-a", "team-a"));
    TeamMember existingTeamMember =
        TeamMember.builder()
            .team(localTeam)
            .rosterStudent(existingStudent)
            .teamStatus(TeamStatus.TEAM_MEMBER)
            .build();

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(githubTeamService.getAllTeams(course)).thenReturn(githubTeams);
    when(teamRepository.findByCourseId(courseId)).thenReturn(Arrays.asList(localTeam));
    when(githubTeamService.getTeamMemberships("team-a", course))
        .thenReturn(Map.of("existing-login", TeamStatus.TEAM_MEMBER));
    when(teamMemberRepository.findByTeamAndRosterStudent(localTeam, existingStudent))
        .thenReturn(Optional.of(existingTeamMember));

    PullTeamsFromGithubJob job =
        PullTeamsFromGithubJob.builder()
            .courseId(courseId)
            .courseRepository(courseRepository)
            .teamRepository(teamRepository)
            .teamMemberRepository(teamMemberRepository)
            .githubTeamService(githubTeamService)
            .build();

    job.accept(ctx);

    verify(teamMemberRepository).findByTeamAndRosterStudent(localTeam, existingStudent);
    verify(teamMemberRepository, never()).save(any(TeamMember.class));
    assertFalse(jobStarted.getLog().contains("Updated team member 'existing-login'"));
    assertTrue(jobStarted.getLog().contains("created: 0, updated: 0, unchanged: 1"));
  }

  @Test
  public void testAccept_CountsTeamUpdatedWhenOnlyMembershipUpdateOccurs() throws Exception {
    Long courseId = 1L;
    RosterStudent existingStudent = RosterStudent.builder().githubLogin("existing-login").build();

    Course course =
        Course.builder()
            .id(courseId)
            .courseName("Test Course")
            .orgName("test-org")
            .installationId("123")
            .rosterStudents(Arrays.asList(existingStudent))
            .build();

    Team localTeam =
        Team.builder()
            .name("team-a")
            .githubTeamId(111)
            .githubTeamSlug("team-a")
            .course(course)
            .build();
    List<GithubTeamInfo> githubTeams = Arrays.asList(new GithubTeamInfo(111, "team-a", "team-a"));
    TeamMember existingTeamMember =
        TeamMember.builder()
            .team(localTeam)
            .rosterStudent(existingStudent)
            .teamStatus(TeamStatus.TEAM_MEMBER)
            .build();

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(githubTeamService.getAllTeams(course)).thenReturn(githubTeams);
    when(teamRepository.findByCourseId(courseId)).thenReturn(Arrays.asList(localTeam));
    when(githubTeamService.getTeamMemberships("team-a", course))
        .thenReturn(Map.of("existing-login", TeamStatus.TEAM_MAINTAINER));
    when(teamMemberRepository.findByTeamAndRosterStudent(localTeam, existingStudent))
        .thenReturn(Optional.of(existingTeamMember));

    PullTeamsFromGithubJob job =
        PullTeamsFromGithubJob.builder()
            .courseId(courseId)
            .courseRepository(courseRepository)
            .teamRepository(teamRepository)
            .teamMemberRepository(teamMemberRepository)
            .githubTeamService(githubTeamService)
            .build();

    job.accept(ctx);

    verify(teamMemberRepository)
        .save(
            argThat(
                tm ->
                    tm == existingTeamMember
                        && tm.getTeamStatus().equals(TeamStatus.TEAM_MAINTAINER)));
    assertTrue(jobStarted.getLog().contains("created: 0, updated: 1, unchanged: 0"));
  }

  @Test
  public void testAccept_CountsTeamUpdatedWhenOnlyMembershipCreateOccurs() throws Exception {
    Long courseId = 1L;
    RosterStudent memberStudent = RosterStudent.builder().githubLogin("member-login").build();

    Course course =
        Course.builder()
            .id(courseId)
            .courseName("Test Course")
            .orgName("test-org")
            .installationId("123")
            .rosterStudents(Arrays.asList(memberStudent))
            .build();

    Team localTeam =
        Team.builder()
            .name("team-a")
            .githubTeamId(111)
            .githubTeamSlug("team-a")
            .course(course)
            .build();
    List<GithubTeamInfo> githubTeams = Arrays.asList(new GithubTeamInfo(111, "team-a", "team-a"));

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(githubTeamService.getAllTeams(course)).thenReturn(githubTeams);
    when(teamRepository.findByCourseId(courseId)).thenReturn(Arrays.asList(localTeam));
    when(githubTeamService.getTeamMemberships("team-a", course))
        .thenReturn(Map.of("member-login", TeamStatus.TEAM_MEMBER));
    when(teamMemberRepository.findByTeamAndRosterStudent(localTeam, memberStudent))
        .thenReturn(Optional.empty());

    PullTeamsFromGithubJob job =
        PullTeamsFromGithubJob.builder()
            .courseId(courseId)
            .courseRepository(courseRepository)
            .teamRepository(teamRepository)
            .teamMemberRepository(teamMemberRepository)
            .githubTeamService(githubTeamService)
            .build();

    job.accept(ctx);

    verify(teamMemberRepository)
        .save(
            argThat(
                tm ->
                    tm.getTeam().equals(localTeam)
                        && tm.getRosterStudent().equals(memberStudent)
                        && tm.getTeamStatus().equals(TeamStatus.TEAM_MEMBER)));
    assertTrue(jobStarted.getLog().contains("created: 0, updated: 1, unchanged: 0"));
  }

  // ────────────────────── checkCancellation checkpoints ──────────────────────
  // The common case on a re-sync is a team that's already up to date on every field with no
  // membership changes either -- that iteration never calls ctx.log() at all. Without their own
  // checkCancellation() checkpoints, neither the outer team loop nor the inner per-member
  // forEach would give cancellation an opportunity to fire during such a silent stretch. These
  // tests mock a JobsRepository that reports "running" for exactly the calls known to precede
  // the checkpoint under test, then "cancelling" from then on, and assert both that
  // JobCancelledException is thrown AND that a downstream call the checkpoint should have
  // pre-empted was never made.

  private static Job runningJob() {
    return Job.builder().id(99L).status("running").build();
  }

  private static Job cancellingJob() {
    return Job.builder().id(99L).status("cancelling").build();
  }

  @Test
  public void checkCancellation_stops_the_team_loop_before_looking_up_the_local_team()
      throws Exception {
    Long courseId = 1L;
    Course course =
        Course.builder()
            .id(courseId)
            .courseName("Test Course")
            .orgName("test-org")
            .installationId("123")
            .build();
    List<GithubTeamInfo> githubTeams = Arrays.asList(new GithubTeamInfo(111, "team-a", "team-a"));

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(githubTeamService.getAllTeams(course)).thenReturn(githubTeams);
    when(teamRepository.findByCourseId(courseId)).thenReturn(Arrays.asList());

    JobsRepository jobsRepository = mock(JobsRepository.class);
    // 2 real checkpoints precede the team loop's own check: accept()'s opening "Starting..." and
    // "Processing course..." log lines.
    when(jobsRepository.findById(99L))
        .thenReturn(
            Optional.of(runningJob()), Optional.of(runningJob()), Optional.of(cancellingJob()));
    Job job = Job.builder().id(99L).build();
    JobContext cancellingCtx = new JobContext(null, job, null, jobsRepository);

    PullTeamsFromGithubJob job2 =
        PullTeamsFromGithubJob.builder()
            .courseId(courseId)
            .courseRepository(courseRepository)
            .teamRepository(teamRepository)
            .teamMemberRepository(teamMemberRepository)
            .githubTeamService(githubTeamService)
            .build();

    assertThrows(JobCancelledException.class, () -> job2.accept(cancellingCtx));

    verify(teamRepository, never()).save(any());
  }

  @Test
  public void checkCancellation_stops_the_member_loop_before_looking_up_the_team_member()
      throws Exception {
    Long courseId = 1L;
    RosterStudent existingStudent = RosterStudent.builder().githubLogin("existing-login").build();
    Course course =
        Course.builder()
            .id(courseId)
            .courseName("Test Course")
            .orgName("test-org")
            .installationId("123")
            .rosterStudents(Arrays.asList(existingStudent))
            .build();
    // Already up to date on every field -- teamCreated=false and teamUnchanged stays true, so
    // this iteration of the outer loop reaches the member-processing block with no log call.
    Team localTeam =
        Team.builder()
            .name("team-a")
            .githubTeamId(111)
            .githubTeamSlug("team-a")
            .course(course)
            .build();
    List<GithubTeamInfo> githubTeams = Arrays.asList(new GithubTeamInfo(111, "team-a", "team-a"));

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(githubTeamService.getAllTeams(course)).thenReturn(githubTeams);
    when(teamRepository.findByCourseId(courseId)).thenReturn(Arrays.asList(localTeam));
    when(githubTeamService.getTeamMemberships("team-a", course))
        .thenReturn(Map.of("existing-login", TeamStatus.TEAM_MEMBER));

    JobsRepository jobsRepository = mock(JobsRepository.class);
    // 3 real checkpoints precede the member loop's own check under this setup: accept()'s two
    // opening log lines, and the outer team loop's own checkCancellation() (checked above).
    when(jobsRepository.findById(99L))
        .thenReturn(
            Optional.of(runningJob()),
            Optional.of(runningJob()),
            Optional.of(runningJob()),
            Optional.of(cancellingJob()));
    Job job = Job.builder().id(99L).build();
    JobContext cancellingCtx = new JobContext(null, job, null, jobsRepository);

    PullTeamsFromGithubJob job2 =
        PullTeamsFromGithubJob.builder()
            .courseId(courseId)
            .courseRepository(courseRepository)
            .teamRepository(teamRepository)
            .teamMemberRepository(teamMemberRepository)
            .githubTeamService(githubTeamService)
            .build();

    assertThrows(JobCancelledException.class, () -> job2.accept(cancellingCtx));

    verify(teamMemberRepository, never()).findByTeamAndRosterStudent(any(), any());
  }
}
