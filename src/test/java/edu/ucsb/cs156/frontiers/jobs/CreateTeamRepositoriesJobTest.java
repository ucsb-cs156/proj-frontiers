package edu.ucsb.cs156.frontiers.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

  Job jobStarted = Job.builder().build();
  JobContext ctx = new JobContext(null, jobStarted);

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void testGetCourse_returnsCoursePassedToBuilder() {
    Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();

    var repoJob =
        CreateTeamRepositoriesJob.builder()
            .repositoryService(service)
            .repositoryPrefix("repo-prefix")
            .course(course)
            .isPrivate(false)
            .permissions(RepositoryPermissions.WRITE)
            .build();

    assertEquals(course, repoJob.getCourse());
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

    var repoJob =
        spy(
            CreateTeamRepositoriesJob.builder()
                .repositoryService(service)
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
            eq(RepositoryPermissions.WRITE));
    verify(service, times(1))
        .createTeamRepository(
            eq(course),
            eq(team2),
            contains("repo-prefix"),
            eq(false),
            eq(RepositoryPermissions.WRITE));
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

    var repoJob =
        spy(
            CreateTeamRepositoriesJob.builder()
                .repositoryService(service)
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
            eq(RepositoryPermissions.WRITE));
    verify(service, times(1))
        .createTeamRepository(
            eq(course),
            eq(team2),
            contains("repo-prefix"),
            eq(true),
            eq(RepositoryPermissions.WRITE));
  }
}
