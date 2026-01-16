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
  public void testCreateTeamRepository_public() throws Exception {
    Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();
    RosterStudent student =
        RosterStudent.builder().githubLogin("studentLogin").orgStatus(OrgStatus.MEMBER).build();
    Team team = Team.builder().name("test-team").build();
    TeamMember member = TeamMember.builder().rosterStudent(student).build();
    team.setTeamMembers(List.of(member));
    course.setTeams(List.of(team));

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
            eq(team),
            contains("repo-prefix"),
            eq(false),
            eq(RepositoryPermissions.WRITE));
  }

  @Test
  public void testCreateTeamRepository_private() throws Exception {
    Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();
    RosterStudent student =
        RosterStudent.builder().githubLogin("studentLogin").orgStatus(OrgStatus.MEMBER).build();
    Team team = Team.builder().name("test-team").build();
    TeamMember member = TeamMember.builder().rosterStudent(student).team(team).build();
    team.setTeamMembers(List.of(member));
    course.setTeams(List.of(team));

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
            eq(team),
            contains("repo-prefix"),
            eq(true),
            eq(RepositoryPermissions.WRITE));
  }
}
