package edu.ucsb.cs156.frontiers.jobs;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.CourseStaff;
import edu.ucsb.cs156.frontiers.entities.Team;
import edu.ucsb.cs156.frontiers.enums.TeamStatus;
import edu.ucsb.cs156.frontiers.services.GithubTeamService;
import edu.ucsb.cs156.jobs.entities.Job;
import edu.ucsb.cs156.jobs.services.JobContext;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StaffTeamGithubSyncTests {

  @Mock private GithubTeamService githubTeamService;

  Job jobStarted = Job.builder().build();
  JobContext ctx = new JobContext(null, jobStarted);

  private static final Integer ORG_ID = 7;

  private static CourseStaff staff(String email, String githubLogin) {
    return CourseStaff.builder().email(email).githubLogin(githubLogin).build();
  }

  private static Team staffTeam() {
    return Team.builder().name("Staff").githubTeamId(55).isStaffTeam(true).build();
  }

  @Test
  public void addsStaffWhoAreNotYetOnTheTeam_andLeavesThoseWhoAre() throws Exception {
    CourseStaff newcomer = staff("new@ucsb.edu", "newcomer");
    CourseStaff member = staff("member@ucsb.edu", "already-member");
    CourseStaff maintainer = staff("maint@ucsb.edu", "already-maintainer");
    Course course =
        Course.builder()
            .id(1L)
            .orgName("test-org")
            .courseStaff(List.of(newcomer, member, maintainer))
            .build();
    Team team = staffTeam();
    when(githubTeamService.getTeamMembershipStatus("newcomer", 55, course, ORG_ID))
        .thenReturn(TeamStatus.NOT_ORG_MEMBER);
    when(githubTeamService.getTeamMembershipStatus("already-member", 55, course, ORG_ID))
        .thenReturn(TeamStatus.TEAM_MEMBER);
    when(githubTeamService.getTeamMembershipStatus("already-maintainer", 55, course, ORG_ID))
        .thenReturn(TeamStatus.TEAM_MAINTAINER);
    when(githubTeamService.addMemberToGithubTeam("newcomer", 55, "member", course, ORG_ID))
        .thenReturn(TeamStatus.TEAM_MEMBER);

    StaffTeamGithubSync.addStaffMembers(ctx, course, team, ORG_ID, githubTeamService);

    verify(githubTeamService).addMemberToGithubTeam("newcomer", 55, "member", course, ORG_ID);
    verify(githubTeamService, times(1))
        .addMemberToGithubTeam(any(), anyInt(), any(), any(), anyInt());
    String log = jobStarted.getLog();
    assertTrue(log.contains("Processing staff members for staff team: Staff (3 staff member(s))"));
    assertTrue(log.contains("Added staff member newcomer to team with status: TEAM_MEMBER"));
    assertTrue(log.contains("Staff member already-member already has status: TEAM_MEMBER"));
    assertTrue(log.contains("Staff member already-maintainer already has status: TEAM_MAINTAINER"));
  }

  @Test
  public void skipsStaffWithoutAGithubLogin() throws Exception {
    Course course =
        Course.builder()
            .id(1L)
            .orgName("test-org")
            .courseStaff(List.of(staff("nologin@ucsb.edu", null)))
            .build();

    StaffTeamGithubSync.addStaffMembers(ctx, course, staffTeam(), ORG_ID, githubTeamService);

    verifyNoInteractions(githubTeamService);
    assertTrue(
        jobStarted
            .getLog()
            .contains("Staff member nologin@ucsb.edu has no GitHub login - skipped"));
  }

  @Test
  public void logsAndCarriesOn_whenGithubRefusesAStaffMember() throws Exception {
    CourseStaff refused = staff("refused@ucsb.edu", "refused");
    CourseStaff fine = staff("fine@ucsb.edu", "fine");
    Course course =
        Course.builder().id(1L).orgName("test-org").courseStaff(List.of(refused, fine)).build();
    when(githubTeamService.getTeamMembershipStatus("refused", 55, course, ORG_ID))
        .thenThrow(new RuntimeException("GitHub API error"));
    when(githubTeamService.getTeamMembershipStatus("fine", 55, course, ORG_ID))
        .thenReturn(TeamStatus.NOT_ORG_MEMBER);
    when(githubTeamService.addMemberToGithubTeam("fine", 55, "member", course, ORG_ID))
        .thenReturn(TeamStatus.TEAM_MEMBER);

    StaffTeamGithubSync.addStaffMembers(ctx, course, staffTeam(), ORG_ID, githubTeamService);

    verify(githubTeamService, times(1))
        .addMemberToGithubTeam(anyString(), anyInt(), anyString(), any(), anyInt());
    verify(githubTeamService).addMemberToGithubTeam("fine", 55, "member", course, ORG_ID);
    assertTrue(
        jobStarted
            .getLog()
            .contains(
                "ERROR: Failed to process staff member refused for team 'Staff': GitHub API error"));
    assertTrue(jobStarted.getLog().contains("Added staff member fine to team with status"));
  }

  @Test
  public void treatsNoStaffListAsNoStaff() throws Exception {
    Course course = Course.builder().id(1L).orgName("test-org").courseStaff(null).build();

    StaffTeamGithubSync.addStaffMembers(ctx, course, staffTeam(), ORG_ID, githubTeamService);

    verifyNoInteractions(githubTeamService);
    assertTrue(
        jobStarted
            .getLog()
            .contains("Processing staff members for staff team: Staff (0 staff member(s))"));
  }
}
