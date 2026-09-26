package edu.ucsb.cs156.frontiers.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.Team;
import edu.ucsb.cs156.frontiers.entities.TeamMember;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.enums.RepositoryPermissions;
import edu.ucsb.cs156.frontiers.services.GithubTeamService;
import edu.ucsb.cs156.frontiers.services.RepositoryService;
import edu.ucsb.cs156.frontiers.services.RepositoryService.RepositoryCreationResult;
import edu.ucsb.cs156.jobs.entities.Job;
import edu.ucsb.cs156.jobs.errors.JobCancelledException;
import edu.ucsb.cs156.jobs.repositories.JobsRepository;
import edu.ucsb.cs156.jobs.services.JobContext;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

@ExtendWith(MockitoExtension.class)
public class CreateTeamRepositoriesJobTest {

  @Mock private RepositoryService service;
  @Mock private GithubTeamService githubTeamService;

  Job jobStarted = Job.builder().build();
  JobContext ctx = new JobContext(null, jobStarted);

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void test_getScope_returnsCourseScope() {
    Course course = Course.builder().id(1L).courseName("Test Course").build();

    CreateTeamRepositoriesJob job = CreateTeamRepositoriesJob.builder().course(course).build();

    assertEquals("course", job.getScopeType());
    assertEquals(course.getId(), job.getScopeId());
  }

  @Test
  public void testCreateTeamRepository_public() throws Exception {
    Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();
    RosterStudent student1 =
        RosterStudent.builder().githubLogin("student1").orgStatus(OrgStatus.MEMBER).build();
    TeamMember member1 = TeamMember.builder().rosterStudent(student1).build();
    Team team1 = Team.builder().name("test-team1").build();
    team1.setTeamMembers(List.of(member1));

    RosterStudent student2 =
        RosterStudent.builder().githubLogin("student2").orgStatus(OrgStatus.MEMBER).build();
    TeamMember member2 = TeamMember.builder().rosterStudent(student2).build();
    RosterStudent student3 =
        RosterStudent.builder().githubLogin("student3").orgStatus(OrgStatus.MEMBER).build();
    TeamMember member3 = TeamMember.builder().rosterStudent(student3).build();
    Team team2 = Team.builder().name("test-team2").build();
    team2.setTeamMembers(List.of(member2, member3));

    course.setTeams(List.of(team1, team2));
    when(githubTeamService.getOrgId("ucsb-cs156", course)).thenReturn(1);
    when(service.createTeamRepository(
            eq(course),
            eq(team1),
            eq("repo-prefix"),
            eq(false),
            eq(RepositoryPermissions.WRITE),
            eq(1)))
        .thenReturn(Optional.of(new RepositoryCreationResult("repo-prefix-test-team1", true)));
    when(service.createTeamRepository(
            eq(course),
            eq(team2),
            eq("repo-prefix"),
            eq(false),
            eq(RepositoryPermissions.WRITE),
            eq(1)))
        .thenReturn(Optional.of(new RepositoryCreationResult("repo-prefix-test-team2", false)));

    var repoJob =
        spy(
            CreateTeamRepositoriesJob.builder()
                .repositoryService(service)
                .githubTeamService(githubTeamService)
                .repositoryPrefix("repo-prefix")
                .course(course)
                .isPrivate(false)
                .permissions(RepositoryPermissions.WRITE)
                .build());

    repoJob.accept(ctx);
    String expected =
        """
        Creating team repositories...
        repositoryPrefix=repo-prefix
        isPrivate=false
        permissions=WRITE
        teamRegex=null
         created repo repo-prefix-test-team1
          updated repo repo-prefix-test-team2
        Summary:
           1 repos created
           1 repos updated
           2 repos total
        Done""";
    assertEquals(expected, jobStarted.getLog());

    verify(service, times(1))
        .createTeamRepository(
            eq(course),
            eq(team1),
            contains("repo-prefix"),
            eq(false),
            eq(RepositoryPermissions.WRITE),
            eq(1));
    verify(service, times(1))
        .createTeamRepository(
            eq(course),
            eq(team2),
            contains("repo-prefix"),
            eq(false),
            eq(RepositoryPermissions.WRITE),
            eq(1));
  }

  @Test
  public void testCreateTeamRepository_private() throws Exception {
    Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();
    RosterStudent student1 =
        RosterStudent.builder().githubLogin("student1").orgStatus(OrgStatus.MEMBER).build();
    TeamMember member1 = TeamMember.builder().rosterStudent(student1).build();
    Team team1 = Team.builder().name("test-team1").build();
    team1.setTeamMembers(List.of(member1));

    RosterStudent student2 =
        RosterStudent.builder().githubLogin("student2").orgStatus(OrgStatus.MEMBER).build();
    TeamMember member2 = TeamMember.builder().rosterStudent(student2).build();
    RosterStudent student3 =
        RosterStudent.builder().githubLogin("student3").orgStatus(OrgStatus.MEMBER).build();
    TeamMember member3 = TeamMember.builder().rosterStudent(student3).build();
    Team team2 = Team.builder().name("test-team2").build();
    team2.setTeamMembers(List.of(member2, member3));

    course.setTeams(List.of(team1, team2));
    when(githubTeamService.getOrgId("ucsb-cs156", course)).thenReturn(1);
    when(service.createTeamRepository(
            eq(course),
            eq(team1),
            eq("repo-prefix"),
            eq(true),
            eq(RepositoryPermissions.WRITE),
            eq(1)))
        .thenReturn(Optional.of(new RepositoryCreationResult("repo-prefix-test-team1", true)));
    when(service.createTeamRepository(
            eq(course),
            eq(team2),
            eq("repo-prefix"),
            eq(true),
            eq(RepositoryPermissions.WRITE),
            eq(1)))
        .thenReturn(Optional.of(new RepositoryCreationResult("repo-prefix-test-team2", true)));

    var repoJob =
        spy(
            CreateTeamRepositoriesJob.builder()
                .repositoryService(service)
                .githubTeamService(githubTeamService)
                .repositoryPrefix("repo-prefix")
                .course(course)
                .isPrivate(true)
                .permissions(RepositoryPermissions.WRITE)
                .build());

    repoJob.accept(ctx);
    String expected =
        """
        Creating team repositories...
        repositoryPrefix=repo-prefix
        isPrivate=true
        permissions=WRITE
        teamRegex=null
         created repo repo-prefix-test-team1
         created repo repo-prefix-test-team2
        Summary:
           2 repos created
           0 repos updated
           2 repos total
        Done""";
    assertEquals(expected, jobStarted.getLog());

    verify(service, times(1))
        .createTeamRepository(
            eq(course),
            eq(team1),
            contains("repo-prefix"),
            eq(true),
            eq(RepositoryPermissions.WRITE),
            eq(1));
    verify(service, times(1))
        .createTeamRepository(
            eq(course),
            eq(team2),
            contains("repo-prefix"),
            eq(true),
            eq(RepositoryPermissions.WRITE),
            eq(1));
  }

  @Test
  public void testCreateTeamRepository_logsAndReturnsWhenGetOrgIdFails() throws Exception {
    Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();
    Team team = Team.builder().name("test-team1").build();
    course.setTeams(List.of(team));

    when(githubTeamService.getOrgId("ucsb-cs156", course))
        .thenThrow(new RuntimeException("GitHub API error"));

    var repoJob =
        spy(
            CreateTeamRepositoriesJob.builder()
                .repositoryService(service)
                .githubTeamService(githubTeamService)
                .repositoryPrefix("repo-prefix")
                .course(course)
                .isPrivate(false)
                .permissions(RepositoryPermissions.WRITE)
                .build());

    IllegalStateException e = assertThrows(IllegalStateException.class, () -> repoJob.accept(ctx));

    assertTrue(jobStarted.getLog().contains("Creating team repositories..."));
    assertEquals(
        "Failed to get organization ID for org: ucsb-cs156 - GitHub API error", e.getMessage());
    verify(githubTeamService).getOrgId("ucsb-cs156", course);
    verifyNoInteractions(service);
  }

  @Test
  public void testCreateTeamRepository_withRegex_onlyMatchingTeams() throws Exception {
    Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();

    Team team1 = Team.builder().name("test-team1").build();
    Team team2 = Team.builder().name("test-team2").build();

    course.setTeams(List.of(team1, team2));
    when(githubTeamService.getOrgId("ucsb-cs156", course)).thenReturn(1);

    var repoJob =
        spy(
            CreateTeamRepositoriesJob.builder()
                .repositoryService(service)
                .githubTeamService(githubTeamService)
                .repositoryPrefix("repo-prefix")
                .course(course)
                .isPrivate(false)
                .permissions(RepositoryPermissions.WRITE)
                .teamRegex("test-team1")
                .build());

    repoJob.accept(ctx);
    String expected =
        """
        Creating team repositories...
        repositoryPrefix=repo-prefix
        isPrivate=false
        permissions=WRITE
        teamRegex=test-team1
        Summary:
           0 repos created
           0 repos updated
           0 repos total
        Done""";
    assertEquals(expected, jobStarted.getLog());

    verify(service, times(1))
        .createTeamRepository(
            eq(course),
            eq(team1),
            contains("repo-prefix"),
            eq(false),
            eq(RepositoryPermissions.WRITE),
            eq(1));
    verify(service, never())
        .createTeamRepository(eq(course), eq(team2), any(), any(), any(), any());
  }

  @Test
  public void testCreateTeamRepository_withRegex_noMatches() throws Exception {
    Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();

    Team team1 = Team.builder().name("test-team1").build();
    Team team2 = Team.builder().name("test-team2").build();

    course.setTeams(List.of(team1, team2));
    when(githubTeamService.getOrgId("ucsb-cs156", course)).thenReturn(1);

    var repoJob =
        spy(
            CreateTeamRepositoriesJob.builder()
                .repositoryService(service)
                .githubTeamService(githubTeamService)
                .repositoryPrefix("repo-prefix")
                .course(course)
                .isPrivate(false)
                .permissions(RepositoryPermissions.WRITE)
                .teamRegex("no_matches_regex")
                .build());

    repoJob.accept(ctx);
    String expected =
        """
        Creating team repositories...
        repositoryPrefix=repo-prefix
        isPrivate=false
        permissions=WRITE
        teamRegex=no_matches_regex
        Summary:
           0 repos created
           0 repos updated
           0 repos total
        Done""";
    assertEquals(expected, jobStarted.getLog());

    verify(service, never())
        .createTeamRepository(eq(course), eq(team1), any(), any(), any(), any());
    verify(service, never())
        .createTeamRepository(eq(course), eq(team2), any(), any(), any(), any());
  }

  // ────────────────────── checkCancellation checkpoint ──────────────────────
  // A team skipped by teamRegex never logs anything -- without its own ctx.checkCancellation()
  // checkpoint, this loop would give cancellation no opportunity to fire no matter how many
  // teams it skips. Exactly 5 real checkpoints precede the loop's own check: accept()'s opening
  // "Creating team repositories..." log line, and the 4 lines that echo the parameters
  // (repositoryPrefix/isPrivate/permissions/teamRegex); each ctx.log() call checks cancellation.

  @Test
  public void checkCancellation_stops_the_team_loop_before_calling_repositoryService()
      throws Exception {
    Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();
    Team team = Team.builder().name("test-team1").build();
    course.setTeams(List.of(team));
    when(githubTeamService.getOrgId("ucsb-cs156", course)).thenReturn(1);

    JobsRepository jobsRepository = mock(JobsRepository.class);
    Job runningJob = Job.builder().id(99L).status("running").build();
    Job cancellingJob = Job.builder().id(99L).status("cancelling").build();
    when(jobsRepository.findById(99L))
        .thenReturn(
            Optional.of(runningJob),
            Optional.of(runningJob),
            Optional.of(runningJob),
            Optional.of(runningJob),
            Optional.of(runningJob),
            Optional.of(cancellingJob));
    Job job = Job.builder().id(99L).build();
    JobContext cancellingCtx = new JobContext(null, job, null, jobsRepository);

    var repoJob =
        CreateTeamRepositoriesJob.builder()
            .repositoryService(service)
            .githubTeamService(githubTeamService)
            .repositoryPrefix("repo-prefix")
            .course(course)
            .isPrivate(false)
            .permissions(RepositoryPermissions.WRITE)
            .build();

    assertThrows(JobCancelledException.class, () -> repoJob.accept(cancellingCtx));

    verify(service, never()).createTeamRepository(any(), any(), any(), any(), any(), any());
  }

  private CreateTeamRepositoriesJob.CreateTeamRepositoriesJobBuilder signedCommitsJob(
      Course course, Boolean requireSignedCommit) {
    return CreateTeamRepositoriesJob.builder()
        .repositoryService(service)
        .githubTeamService(githubTeamService)
        .repositoryPrefix("repo-prefix")
        .course(course)
        .isPrivate(false)
        .permissions(RepositoryPermissions.WRITE)
        .requireSignedCommit(requireSignedCommit);
  }

  private Course courseWithTwoTeams(Team team1, Team team2) throws Exception {
    Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();
    course.setTeams(List.of(team1, team2));
    when(githubTeamService.getOrgId("ucsb-cs156", course)).thenReturn(1);
    when(service.createTeamRepository(eq(course), eq(team1), any(), any(), any(), eq(1)))
        .thenReturn(Optional.of(new RepositoryCreationResult("repo-prefix-test-team1", true)));
    when(service.createTeamRepository(eq(course), eq(team2), any(), any(), any(), eq(1)))
        .thenReturn(Optional.of(new RepositoryCreationResult("repo-prefix-test-team2", false)));
    return course;
  }

  @Test
  public void requireSignedCommit_true_is_logged_and_applied_to_created_and_updated_repos()
      throws Exception {
    Team team1 = Team.builder().name("test-team1").build();
    Team team2 = Team.builder().name("test-team2").build();
    Course course = courseWithTwoTeams(team1, team2);

    signedCommitsJob(course, true).build().accept(ctx);

    String expected =
        """
        Creating team repositories...
        repositoryPrefix=repo-prefix
        isPrivate=false
        permissions=WRITE
        teamRegex=null
        requireSignedCommit=true
         created repo repo-prefix-test-team1
          updated repo repo-prefix-test-team2
        Summary:
           1 repos created
           1 repos updated
           2 repos total
        Done""";
    assertEquals(expected, jobStarted.getLog());
    verify(service, times(1))
        .setSignedCommitsRequired(eq(course), eq("repo-prefix-test-team1"), eq(true));
    verify(service, times(1))
        .setSignedCommitsRequired(eq(course), eq("repo-prefix-test-team2"), eq(true));
  }

  @Test
  public void requireSignedCommit_false_is_logged_and_clears_the_rule_on_the_repos()
      throws Exception {
    Team team1 = Team.builder().name("test-team1").build();
    Team team2 = Team.builder().name("test-team2").build();
    Course course = courseWithTwoTeams(team1, team2);

    signedCommitsJob(course, false).build().accept(ctx);

    assertTrue(jobStarted.getLog().contains("\nrequireSignedCommit=false\n"));
    verify(service, times(1))
        .setSignedCommitsRequired(eq(course), eq("repo-prefix-test-team1"), eq(false));
    verify(service, times(1))
        .setSignedCommitsRequired(eq(course), eq("repo-prefix-test-team2"), eq(false));
  }

  @Test
  public void requireSignedCommit_null_leaves_the_rulesets_alone_and_is_not_logged()
      throws Exception {
    Team team1 = Team.builder().name("test-team1").build();
    Team team2 = Team.builder().name("test-team2").build();
    Course course = courseWithTwoTeams(team1, team2);

    signedCommitsJob(course, null).build().accept(ctx);

    assertFalse(jobStarted.getLog().contains("requireSignedCommit"));
    verify(service, never()).setSignedCommitsRequired(any(), any(), anyBoolean());
  }

  @Test
  public void a_repo_that_github_refuses_the_rule_for_is_logged_and_the_job_carries_on()
      throws Exception {
    Team team1 = Team.builder().name("test-team1").build();
    Team team2 = Team.builder().name("test-team2").build();
    Course course = courseWithTwoTeams(team1, team2);
    doThrow(
            HttpClientErrorException.create(
                HttpStatus.FORBIDDEN,
                "Forbidden",
                HttpHeaders.EMPTY,
                "{\"message\":\"Resource not accessible by integration\"}".getBytes(),
                null))
        .when(service)
        .setSignedCommitsRequired(eq(course), eq("repo-prefix-test-team1"), eq(true));

    signedCommitsJob(course, true).build().accept(ctx);

    String expected =
        """
        Creating team repositories...
        repositoryPrefix=repo-prefix
        isPrivate=false
        permissions=WRITE
        teamRegex=null
        requireSignedCommit=true
         created repo repo-prefix-test-team1
          could not require signed commits on repo-prefix-test-team1: 403 FORBIDDEN {"message":"Resource not accessible by integration"}
          updated repo repo-prefix-test-team2
        Summary:
           1 repos created
           1 repos updated
           2 repos total
           1 repos where signed commits could not be set
        Done""";
    assertEquals(expected, jobStarted.getLog());
    // it went on to the next repo
    verify(service, times(1))
        .setSignedCommitsRequired(eq(course), eq("repo-prefix-test-team2"), eq(true));
  }

  @Test
  public void a_refusal_to_clear_the_rule_says_so() throws Exception {
    Team team1 = Team.builder().name("test-team1").build();
    Team team2 = Team.builder().name("test-team2").build();
    Course course = courseWithTwoTeams(team1, team2);
    lenient()
        .doThrow(
            HttpClientErrorException.create(
                HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, new byte[0], null))
        .when(service)
        .setSignedCommitsRequired(eq(course), eq("repo-prefix-test-team2"), eq(false));

    signedCommitsJob(course, false).build().accept(ctx);

    assertTrue(
        jobStarted
            .getLog()
            .contains(
                "  could not stop requiring signed commits on repo-prefix-test-team2: 404 NOT_FOUND \n"));
    assertTrue(jobStarted.getLog().contains("   1 repos where signed commits could not be set\n"));
  }

  @Test
  public void a_repo_whose_creation_failed_gets_no_signed_commits_call() throws Exception {
    Team team1 = Team.builder().name("test-team1").build();
    Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();
    course.setTeams(List.of(team1));
    when(githubTeamService.getOrgId("ucsb-cs156", course)).thenReturn(1);
    when(service.createTeamRepository(eq(course), eq(team1), any(), any(), any(), eq(1)))
        .thenReturn(Optional.empty());

    signedCommitsJob(course, true).build().accept(ctx);

    verify(service, never()).setSignedCommitsRequired(any(), any(), anyBoolean());
    assertFalse(jobStarted.getLog().contains("could not"));
  }
}
