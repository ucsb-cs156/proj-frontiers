package edu.ucsb.cs156.frontiers.jobs;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PullTeamsFromGithubJobTests {

  @Mock private CourseRepository courseRepository;
  @Mock private TeamRepository teamRepository;
  @Mock private TeamMemberRepository teamMemberRepository;
  @Mock private GithubTeamService githubTeamService;

  Job jobStarted = Job.builder().build();
  JobContext ctx = new JobContext(null, jobStarted);

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
  }

  @Test
  public void testAccept_CourseWithoutInstallationId() throws Exception {
    Long courseId = 1L;
    Course course =
        Course.builder().id(courseId).courseName("Test Course").orgName("test-org").build();
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
  public void testAccept_UpsertsTeamsByGithubIdAndName() throws Exception {
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
    Team unchanged = Team.builder().name("same-team").githubTeamId(333).course(course).build();

    List<GithubTeamInfo> githubTeams =
        Arrays.asList(
            new GithubTeamInfo(111, "team-by-name"),
            new GithubTeamInfo(222, "renamed-team"),
            new GithubTeamInfo(333, "same-team"),
            new GithubTeamInfo(444, "new-team"));

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
        .save(argThat(t -> t.getName().equals("team-by-name") && t.getGithubTeamId().equals(111)));
    verify(teamRepository)
        .save(argThat(t -> t.getName().equals("renamed-team") && t.getGithubTeamId().equals(222)));
    verify(teamRepository)
        .save(argThat(t -> t == existingById && t.getName().equals("renamed-team")));
    verify(teamRepository)
        .save(
            argThat(
                t ->
                    t.getName().equals("new-team")
                        && t.getGithubTeamId().equals(444)
                        && t.getCourse().equals(course)));
    verify(teamRepository, times(3)).save(any(Team.class));

    verify(teamRepository, never())
        .save(argThat(t -> t.getName().equals("same-team") && t.getGithubTeamId().equals(333)));
    verify(githubTeamService, never()).getTeamMemberships(any(), any());
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
        Arrays.asList(new GithubTeamInfo(111, "team-by-name"), new GithubTeamInfo(999, "new-team"));

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
                        && t.getGithubTeamId().equals(111)));
    verify(teamRepository)
        .save(
            argThat(
                t ->
                    t != existingByNameWithGithubId
                        && t.getName().equals("new-team")
                        && t.getGithubTeamId().equals(999)
                        && t.getCourse().equals(course)));
    verify(teamRepository, times(2)).save(any(Team.class));
    verify(githubTeamService, never()).getTeamMemberships(any(), any());
    assertTrue(jobStarted.getLog().contains("created: 1, updated: 1, unchanged: 0"));
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

    Team localTeam = Team.builder().name("team-a").githubTeamId(111).course(course).build();
    List<GithubTeamInfo> githubTeams = Arrays.asList(new GithubTeamInfo(111, "team-a"));
    TeamMember existingTeamMember =
        TeamMember.builder().team(localTeam).rosterStudent(existingStudent).build();

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(githubTeamService.getAllTeams(course)).thenReturn(githubTeams);
    when(teamRepository.findByCourseId(courseId)).thenReturn(Arrays.asList(localTeam));
    when(githubTeamService.getTeamMemberships(111, course))
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

    verify(githubTeamService).getTeamMemberships(111, course);

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
    assertTrue(jobStarted.getLog().contains("members created: 1, members updated: 1"));
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

    List<GithubTeamInfo> githubTeams = Arrays.asList(new GithubTeamInfo(111, "new-team"));

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(githubTeamService.getAllTeams(course)).thenReturn(githubTeams);
    when(teamRepository.findByCourseId(courseId)).thenReturn(Arrays.asList());
    when(githubTeamService.getTeamMemberships(111, course))
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
    assertTrue(jobStarted.getLog().contains("members created: 1, members updated: 0"));
  }
}
