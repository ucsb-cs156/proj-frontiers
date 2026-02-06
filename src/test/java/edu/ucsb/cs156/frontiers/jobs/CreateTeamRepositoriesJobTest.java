package edu.ucsb.cs156.frontiers.jobs;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.mockito.ArgumentCaptor;
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

    ArgumentCaptor<Course> courseCaptor = ArgumentCaptor.forClass(Course.class);
    ArgumentCaptor<Team> teamCaptor = ArgumentCaptor.forClass(Team.class);

    verify(service, times(2))
        .createTeamRepository(
            courseCaptor.capture(),
            teamCaptor.capture(),
            contains("repo-prefix"),
            eq(false),
            eq(RepositoryPermissions.WRITE));

    courseCaptor
        .getAllValues()
        .forEach(
            c -> {
              assertThat(c.getOrgName()).isEqualTo("ucsb-cs156");
              assertThat(c.getInstallationId()).isEqualTo("1234");
            });
    assertThat(teamCaptor.getAllValues().get(0).getName()).isEqualTo("test-team1");
    assertThat(teamCaptor.getAllValues().get(1).getName()).isEqualTo("test-team2");
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

    ArgumentCaptor<Course> courseCaptor = ArgumentCaptor.forClass(Course.class);
    ArgumentCaptor<Team> teamCaptor = ArgumentCaptor.forClass(Team.class);

    verify(service, times(2))
        .createTeamRepository(
            courseCaptor.capture(),
            teamCaptor.capture(),
            contains("repo-prefix"),
            eq(true),
            eq(RepositoryPermissions.WRITE));

    courseCaptor
        .getAllValues()
        .forEach(
            c -> {
              assertThat(c.getOrgName()).isEqualTo("ucsb-cs156");
              assertThat(c.getInstallationId()).isEqualTo("1234");
            });
    assertThat(teamCaptor.getAllValues().get(0).getName()).isEqualTo("test-team1");
    assertThat(teamCaptor.getAllValues().get(1).getName()).isEqualTo("test-team2");
  }
}
