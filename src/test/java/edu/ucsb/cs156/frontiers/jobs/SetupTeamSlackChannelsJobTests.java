package edu.ucsb.cs156.frontiers.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.CourseStaff;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.Team;
import edu.ucsb.cs156.frontiers.entities.TeamMember;
import edu.ucsb.cs156.frontiers.errors.SlackApiException;
import edu.ucsb.cs156.frontiers.models.SlackChannel;
import edu.ucsb.cs156.frontiers.models.SlackUser;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseStaffRepository;
import edu.ucsb.cs156.frontiers.repositories.TeamRepository;
import edu.ucsb.cs156.frontiers.services.CanvasApiTokenSecurityService;
import edu.ucsb.cs156.frontiers.services.SlackService;
import edu.ucsb.cs156.jobs.entities.Job;
import edu.ucsb.cs156.jobs.services.JobContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SetupTeamSlackChannelsJobTests {

  @Mock private CourseRepository courseRepository;
  @Mock private TeamRepository teamRepository;
  @Mock private CourseStaffRepository courseStaffRepository;
  @Mock private SlackService slackService;
  @Mock private CanvasApiTokenSecurityService tokenSecurityService;

  private static final String TOKEN = "xoxb-test-token";

  Job jobStarted = Job.builder().build();
  JobContext ctx = new JobContext(null, jobStarted);

  private final Course course =
      Course.builder()
          .id(1L)
          .courseName("CS156")
          .instructorEmail("Prof@UCSB.edu")
          .slackBotToken("enc:v1:ciphertext")
          .build();

  private SetupTeamSlackChannelsJob job() {
    return SetupTeamSlackChannelsJob.builder()
        .course(Course.builder().id(1L).build())
        .courseRepository(courseRepository)
        .teamRepository(teamRepository)
        .courseStaffRepository(courseStaffRepository)
        .slackService(slackService)
        .tokenSecurityService(tokenSecurityService)
        .build();
  }

  private static SlackUser slackUser(String id, String realName, String email) {
    return SlackUser.builder()
        .id(id)
        .realName(realName)
        .profile(SlackUser.Profile.builder().email(email).build())
        .build();
  }

  private static RosterStudent student(String firstName, String email) {
    return RosterStudent.builder().firstName(firstName).lastName("Student").email(email).build();
  }

  private static Team team(String name, RosterStudent... students) {
    Team team = Team.builder().name(name).teamMembers(new ArrayList<>()).build();
    for (RosterStudent student : students) {
      team.getTeamMembers().add(TeamMember.builder().team(team).rosterStudent(student).build());
    }
    return team;
  }

  private void courseHasToken() {
    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
    when(tokenSecurityService.decrypt("enc:v1:ciphertext")).thenReturn(TOKEN);
  }

  private static String log(String... lines) {
    return String.join("\n", lines);
  }

  @Test
  public void scope_is_the_course() {
    SetupTeamSlackChannelsJob job =
        SetupTeamSlackChannelsJob.builder().course(Course.builder().id(42L).build()).build();
    assertEquals("course", job.getScopeType());
    assertEquals(42L, job.getScopeId());
  }

  @Test
  public void sanitizeChannelName_follows_the_rules() {
    assertEquals("", SetupTeamSlackChannelsJob.sanitizeChannelName(null));
    assertEquals("", SetupTeamSlackChannelsJob.sanitizeChannelName(""));
    assertEquals("team-01", SetupTeamSlackChannelsJob.sanitizeChannelName("Team-01"));
    assertEquals("the-a-team", SetupTeamSlackChannelsJob.sanitizeChannelName("The A Team"));
    assertEquals("f26-5pm-3", SetupTeamSlackChannelsJob.sanitizeChannelName("F26-5pm-3"));
    assertEquals("a_b-c-d--e", SetupTeamSlackChannelsJob.sanitizeChannelName("a_b c.d/#e"));
    assertEquals("caf-", SetupTeamSlackChannelsJob.sanitizeChannelName("Café"));
    assertEquals("team-the-a-team", SetupTeamSlackChannelsJob.channelNameFor("The A Team"));
    assertEquals("team-", SetupTeamSlackChannelsJob.channelNameFor(null));
  }

  @Test
  public void creates_channels_adds_members_and_instructor_and_removes_others() throws Exception {
    courseHasToken();

    SlackUser bot = slackUser("U_BOT", "Frontiers", null);
    bot.setBot(true);
    SlackUser invited = slackUser("U_EVE", "Eve Student", "eve@ucsb.edu");
    invited.setInvitedUser(true);
    when(slackService.listUsers(TOKEN))
        .thenReturn(
            List.of(
                slackUser("U_PROF", "Prof Essor", "prof@ucsb.edu"),
                slackUser("U_TA", "Tee Ay", "ta@ucsb.edu"),
                slackUser("U_ALICE", "Alice Student", "alice@ucsb.edu"),
                slackUser("U_BOB", "Bob Student", "bob@umail.ucsb.edu"),
                slackUser("U_CAROL", "Carol Student", "carol@ucsb.edu"),
                slackUser("U_DAVE", "Dave Student", "dave@ucsb.edu"),
                slackUser("U_STRANGER", "Some Stranger", "stranger@example.org"),
                slackUser("U_NOEMAIL", "No Email", null),
                bot,
                invited));

    RosterStudent alice = student("Alice", "alice@ucsb.edu");
    RosterStudent bob = student("Bob", "bob@ucsb.edu");
    RosterStudent carol = student("Carol", "carol@ucsb.edu");
    RosterStudent dave = student("Dave", "dave@ucsb.edu");
    RosterStudent eve = student("Eve", "eve@ucsb.edu");
    RosterStudent noEmail = student("NoEmail", null);
    // a team member whose roster student is missing, and a team with no members list
    Team broken = team("Broken");
    broken.getTeamMembers().add(TeamMember.builder().team(broken).build());
    Team nullMembers = Team.builder().name("Null Members").build();
    when(teamRepository.findByCourseIdOrderByNameAsc(1L))
        .thenReturn(
            List.of(
                team("The A Team", alice, bob, eve, noEmail),
                team("Team B!", carol),
                team("archived", dave),
                team("bad/name"),
                broken,
                nullMembers));

    when(slackService.listPublicChannels(TOKEN))
        .thenReturn(
            List.of(
                SlackChannel.builder().id("C_B").name("team-team-b-").build(),
                SlackChannel.builder().id("C_GENERAL").name("general").build(),
                SlackChannel.builder().id("C_ARCH").name("team-archived").archived(true).build()));
    when(slackService.createPublicChannel(TOKEN, "team-the-a-team"))
        .thenReturn(SlackChannel.builder().id("C_A").name("team-the-a-team").build());
    when(slackService.createPublicChannel(TOKEN, "team-bad-name"))
        .thenThrow(new SlackApiException("invalid_name_specials"));
    when(slackService.createPublicChannel(TOKEN, "team-broken"))
        .thenReturn(SlackChannel.builder().id("C_BROKEN").name("team-broken").build());
    when(slackService.createPublicChannel(TOKEN, "team-null-members"))
        .thenReturn(SlackChannel.builder().id("C_NULL").name("team-null-members").build());

    when(courseStaffRepository.findByCourseId(1L))
        .thenReturn(
            List.of(
                CourseStaff.builder().email("TA@ucsb.edu").build(),
                CourseStaff.builder().email("notinslack@ucsb.edu").build(),
                CourseStaff.builder().email(null).build()));

    when(slackService.listChannelMembers(TOKEN, "C_A")).thenReturn(List.of("U_BOT", "U_ALICE"));
    when(slackService.listChannelMembers(TOKEN, "C_B"))
        .thenReturn(
            List.of(
                "U_BOT",
                "U_PROF",
                "U_TA",
                "U_CAROL",
                "U_ALICE",
                "U_STRANGER",
                "U_EXTERNAL",
                "U_NOEMAIL"));
    when(slackService.listChannelMembers(TOKEN, "C_BROKEN")).thenReturn(List.of("U_BOT"));
    when(slackService.listChannelMembers(TOKEN, "C_NULL")).thenReturn(List.of("U_BOT"));

    job().accept(ctx);

    assertEquals(
        log(
            "Creating Team Channels (6 team(s), channel names start with team-)",
            "Created channel #team-the-a-team for team The A Team",
            "Channel #team-team-b- for team Team B! already exists",
            "Channel #team-archived for team archived already exists, but is archived; unarchive it in Slack, then run this job again. Skipping it.",
            "Error setting up channel #team-bad-name for team bad/name: invalid_name_specials. Skipping it.",
            "Created channel #team-broken for team Broken",
            "Created channel #team-null-members for team Null Members",
            "Adding Team Members to Channels",
            "Added instructor Prof@UCSB.edu to #team-the-a-team",
            "Added Bob Student (bob@ucsb.edu) to #team-the-a-team",
            "Added instructor Prof@UCSB.edu to #team-broken",
            "Added instructor Prof@UCSB.edu to #team-null-members",
            "3 team member(s) could not be added, because they do not have an active account in the Slack workspace; see the Slack tab. Run this job again once they have joined.",
            "Removing Channel Members Who Are Not On The Team",
            "Removed Alice Student (alice@ucsb.edu) from #team-team-b-",
            "Removed Some Stranger (stranger@example.org) from #team-team-b-",
            "Removed No Email (null) from #team-team-b-",
            "Done. Channels created: 3, already existed: 1. Members added: 4, already present: 3, removed: 3."),
        jobStarted.getLog());

    verify(slackService).joinChannel(TOKEN, "C_B");
    // the instructor and Carol were already in #team-team-b-, so nobody is added to it
    verify(slackService, never())
        .inviteToChannel(
            org.mockito.ArgumentMatchers.eq(TOKEN), org.mockito.ArgumentMatchers.eq("C_B"), any());
    verify(slackService).inviteToChannel(TOKEN, "C_A", List.of("U_PROF", "U_BOB"));
    verify(slackService).inviteToChannel(TOKEN, "C_BROKEN", List.of("U_PROF"));
    verify(slackService).inviteToChannel(TOKEN, "C_NULL", List.of("U_PROF"));
    verify(slackService).removeFromChannel(TOKEN, "C_B", "U_ALICE");
    verify(slackService).removeFromChannel(TOKEN, "C_B", "U_STRANGER");
    verify(slackService).removeFromChannel(TOKEN, "C_B", "U_NOEMAIL");
    verify(slackService, never())
        .removeFromChannel(any(), any(), org.mockito.ArgumentMatchers.eq("U_TA"));
    verify(slackService, never())
        .removeFromChannel(any(), any(), org.mockito.ArgumentMatchers.eq("U_PROF"));
    verify(slackService, never())
        .removeFromChannel(any(), any(), org.mockito.ArgumentMatchers.eq("U_BOT"));
    verify(slackService, never())
        .removeFromChannel(any(), any(), org.mockito.ArgumentMatchers.eq("U_EXTERNAL"));
    verify(slackService, never()).listChannelMembers(TOKEN, "C_ARCH");
  }

  @Test
  public void stops_before_touching_slack_when_two_teams_get_the_same_channel_name() {
    courseHasToken();
    when(teamRepository.findByCourseIdOrderByNameAsc(1L))
        .thenReturn(
            List.of(team("Team 1"), team("team-1"), team("TEAM_1"), team("Team.1"), team("Other")));

    IllegalStateException e = assertThrows(IllegalStateException.class, () -> job().accept(ctx));

    assertEquals(
        "Two or more teams would get the same Slack channel name: #team-team-1 (teams Team 1, team-1, Team.1). Rename the teams so that their channel names differ, then run this job again. Nothing was changed in Slack.",
        e.getMessage());
    assertEquals(null, jobStarted.getLog());
    verifyNoMoreInteractions(slackService);
  }

  @Test
  public void adds_one_at_a_time_when_adding_together_fails_and_carries_on_after_errors()
      throws Exception {
    Course noInstructor = Course.builder().id(1L).slackBotToken("enc:v1:ciphertext").build();
    when(courseRepository.findById(1L)).thenReturn(Optional.of(noInstructor));
    when(tokenSecurityService.decrypt("enc:v1:ciphertext")).thenReturn(TOKEN);
    when(slackService.listUsers(TOKEN))
        .thenReturn(
            List.of(
                slackUser("U_ALICE", "Alice Student", "alice@ucsb.edu"),
                slackUser("U_BOB", "Bob Student", "bob@ucsb.edu"),
                slackUser("U_X", "Some Stranger", "stranger@example.org")));
    RosterStudent alice = student("Alice", "alice@ucsb.edu");
    RosterStudent bob = student("Bob", "bob@ucsb.edu");
    when(teamRepository.findByCourseIdOrderByNameAsc(1L))
        .thenReturn(List.of(team("cannot-join"), team("cannot-list"), team("t", alice, bob)));
    when(slackService.listPublicChannels(TOKEN))
        .thenReturn(
            List.of(
                SlackChannel.builder().id("C1").name("team-cannot-join").build(),
                SlackChannel.builder().id("C2").name("team-cannot-list").build(),
                SlackChannel.builder().id("C3").name("team-t").build()));
    Mockito.doThrow(new SlackApiException("missing_scope"))
        .when(slackService)
        .joinChannel(TOKEN, "C1");
    when(slackService.listChannelMembers(TOKEN, "C2"))
        .thenThrow(new SlackApiException("channel_not_found"));
    when(slackService.listChannelMembers(TOKEN, "C3")).thenReturn(List.of("U_X"));
    when(courseStaffRepository.findByCourseId(1L)).thenReturn(List.of());
    Mockito.doThrow(new SlackApiException("user_is_restricted"))
        .when(slackService)
        .inviteToChannel(TOKEN, "C3", List.of("U_ALICE", "U_BOB"));
    Mockito.doThrow(new SlackApiException("user_is_restricted"))
        .when(slackService)
        .inviteToChannel(TOKEN, "C3", List.of("U_ALICE"));
    Mockito.doThrow(new SlackApiException("restricted_action"))
        .when(slackService)
        .removeFromChannel(TOKEN, "C3", "U_X");

    job().accept(ctx);

    assertEquals(
        log(
            "Creating Team Channels (3 team(s), channel names start with team-)",
            "Error setting up channel #team-cannot-join for team cannot-join: missing_scope. Skipping it.",
            "Channel #team-cannot-list for team cannot-list already exists",
            "Channel #team-t for team t already exists",
            "Adding Team Members to Channels",
            "Error listing members of #team-cannot-list: channel_not_found",
            "Could not add 2 member(s) to #team-t in one step (user_is_restricted); adding them one at a time.",
            "Error adding Alice Student (alice@ucsb.edu) to #team-t: user_is_restricted",
            "Added Bob Student (bob@ucsb.edu) to #team-t",
            "Removing Channel Members Who Are Not On The Team",
            "Error removing Some Stranger (stranger@example.org) from #team-t: restricted_action",
            "Done. Channels created: 0, already existed: 2. Members added: 1, already present: 0, removed: 0."),
        jobStarted.getLog());
    verify(slackService).inviteToChannel(TOKEN, "C3", List.of("U_BOB"));
  }

  @Test
  public void instructor_not_in_slack_is_reported() throws Exception {
    courseHasToken();
    when(slackService.listUsers(TOKEN))
        .thenReturn(List.of(slackUser("U_ALICE", "Alice Student", "alice@ucsb.edu")));
    when(teamRepository.findByCourseIdOrderByNameAsc(1L)).thenReturn(List.of());
    when(slackService.listPublicChannels(TOKEN)).thenReturn(List.of());
    when(courseStaffRepository.findByCourseId(1L)).thenReturn(List.of());

    job().accept(ctx);

    assertEquals(
        log(
            "Creating Team Channels (0 team(s), channel names start with team-)",
            "The instructor (Prof@UCSB.edu) does not have an active account in the Slack workspace, so cannot be added to the channels.",
            "Adding Team Members to Channels",
            "Removing Channel Members Who Are Not On The Team",
            "Done. Channels created: 0, already existed: 0. Members added: 0, already present: 0, removed: 0."),
        jobStarted.getLog());
  }

  @Test
  public void fails_without_a_token() {
    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
    when(tokenSecurityService.decrypt("enc:v1:ciphertext")).thenReturn(null);
    assertThrows(IllegalStateException.class, () -> job().accept(ctx));
    when(tokenSecurityService.decrypt("enc:v1:ciphertext")).thenReturn("");
    assertThrows(IllegalStateException.class, () -> job().accept(ctx));
    verifyNoMoreInteractions(slackService);
  }

  @Test
  public void fails_before_changing_anything_if_slack_provides_no_emails() {
    courseHasToken();
    when(teamRepository.findByCourseIdOrderByNameAsc(1L)).thenReturn(List.of(team("t")));
    SlackUser deactivated = slackUser("U_OLD", "Old", "old@ucsb.edu");
    deactivated.setDeleted(true);
    when(slackService.listUsers(TOKEN))
        .thenReturn(List.of(slackUser("U_A", "A", null), deactivated));

    IllegalStateException e = assertThrows(IllegalStateException.class, () -> job().accept(ctx));
    assertEquals(
        "Slack did not provide the email of any user. Add the users:read.email scope to the Slack app, reinstall it to the workspace, and save the new token on the Settings tab.",
        e.getMessage());
    verify(slackService).listUsers(TOKEN);
    verifyNoMoreInteractions(slackService);
  }
}
