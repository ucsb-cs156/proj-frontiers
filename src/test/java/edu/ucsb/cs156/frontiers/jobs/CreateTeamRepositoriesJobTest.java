package edu.ucsb.cs156.frontiers.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.Job;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.Team;
import edu.ucsb.cs156.frontiers.entities.TeamMember;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.enums.RepositoryPermissions;
import edu.ucsb.cs156.frontiers.services.GithubTeamService;
import edu.ucsb.cs156.frontiers.services.RepositoryService;
import edu.ucsb.cs156.frontiers.services.jobs.JobContext;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

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
  public void test_getCourse_returnsCourse() {
    Course course = Course.builder().id(1L).courseName("Test Course").build();

    CreateTeamRepositoriesJob job = CreateTeamRepositoriesJob.builder().course(course).build();

    assertEquals(course, job.getCourse());
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
    String expected = """
        Creating team repositories...
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
    String expected = """
        Creating team repositories...
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
}
