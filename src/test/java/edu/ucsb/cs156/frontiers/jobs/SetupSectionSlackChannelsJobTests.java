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
import edu.ucsb.cs156.frontiers.entities.Section;
import edu.ucsb.cs156.frontiers.enums.RosterStatus;
import edu.ucsb.cs156.frontiers.errors.SlackApiException;
import edu.ucsb.cs156.frontiers.models.SlackChannel;
import edu.ucsb.cs156.frontiers.models.SlackUser;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseStaffRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.repositories.SectionRepository;
import edu.ucsb.cs156.frontiers.services.CanvasApiTokenSecurityService;
import edu.ucsb.cs156.frontiers.services.SlackService;
import edu.ucsb.cs156.jobs.entities.Job;
import edu.ucsb.cs156.jobs.services.JobContext;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SetupSectionSlackChannelsJobTests {

  @Mock private CourseRepository courseRepository;
  @Mock private SectionRepository sectionRepository;
  @Mock private RosterStudentRepository rosterStudentRepository;
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
          .instructorEmail("prof@ucsb.edu")
          .slackBotToken("enc:v1:ciphertext")
          .build();

  private SetupSectionSlackChannelsJob job() {
    return SetupSectionSlackChannelsJob.builder()
        // only the id of the course given to the job is used; the rest is read again
        .course(Course.builder().id(1L).build())
        .courseRepository(courseRepository)
        .sectionRepository(sectionRepository)
        .rosterStudentRepository(rosterStudentRepository)
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

  private static Section section(String section, String slackChannelName) {
    return Section.builder()
        .section(section)
        .label("label " + section)
        .slackChannelName(slackChannelName)
        .build();
  }

  private static RosterStudent student(String firstName, String email, String section) {
    return RosterStudent.builder()
        .firstName(firstName)
        .lastName("Student")
        .email(email)
        .section(section)
        .rosterStatus(RosterStatus.ROSTER)
        .build();
  }

  private void courseHasToken() {
    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
    when(tokenSecurityService.decrypt("enc:v1:ciphertext")).thenReturn(TOKEN);
  }

  private void students(RosterStudent... students) {
    when(rosterStudentRepository.findByCourseIdOrderByFirstNameAscLastNameAscIgnoreCase(1L))
        .thenReturn(List.of(students));
  }

  private static String log(String... lines) {
    return String.join("\n", lines);
  }

  @Test
  public void scope_is_the_course() {
    SetupSectionSlackChannelsJob job =
        SetupSectionSlackChannelsJob.builder().course(Course.builder().id(42L).build()).build();
    assertEquals("course", job.getScopeType());
    assertEquals(42L, job.getScopeId());
  }

  @Test
  public void normalizeChannelName_strips_hash_and_lowercases() {
    assertEquals("", SetupSectionSlackChannelsJob.normalizeChannelName(null));
    assertEquals("", SetupSectionSlackChannelsJob.normalizeChannelName("   "));
    assertEquals("", SetupSectionSlackChannelsJob.normalizeChannelName(" # "));
    assertEquals("sec-0100", SetupSectionSlackChannelsJob.normalizeChannelName("sec-0100"));
    assertEquals("sec-0100", SetupSectionSlackChannelsJob.normalizeChannelName("  # Sec-0100 "));
    assertEquals("a#b", SetupSectionSlackChannelsJob.normalizeChannelName("A#B"));
  }

  @Test
  public void creates_channels_adds_students_and_removes_others() throws Exception {
    courseHasToken();

    SlackUser bot = slackUser("U_BOT", "Frontiers", null);
    bot.setBot(true);
    SlackUser invited = slackUser("U_EVE", "Eve Student", "eve@ucsb.edu");
    invited.setInvitedUser(true);
    SlackUser deactivated = slackUser("U_FRANK", "Frank Student", "frank@ucsb.edu");
    deactivated.setDeleted(true);
    // same person: an invitation, listed before a deactivated account, counts as invited
    SlackUser reinvited = slackUser("U_GRACE", "Grace Student", "Grace@ucsb.edu");
    reinvited.setInvitedUser(true);
    SlackUser reinvitedOldAccount = slackUser("U_GRACE_OLD", "Grace Student", "grace@ucsb.edu");
    reinvitedOldAccount.setDeleted(true);
    // a deactivated account whose invitation was never accepted counts as deactivated
    SlackUser deletedInvitation = slackUser("U_HEIDI", "Heidi Student", "heidi@ucsb.edu");
    deletedInvitation.setDeleted(true);
    deletedInvitation.setInvitedUser(true);
    // same person: a deactivated account, listed before another deactivated account
    SlackUser twiceDeactivated1 = slackUser("U_JUDY1", "Judy Student", "judy@ucsb.edu");
    twiceDeactivated1.setDeleted(true);
    SlackUser twiceDeactivated2 = slackUser("U_JUDY2", "Judy Student", "judy@ucsb.edu");
    twiceDeactivated2.setDeleted(true);
    when(slackService.listUsers(TOKEN))
        .thenReturn(
            List.of(
                slackUser("U_PROF", "Prof Essor", "Prof@UCSB.edu"),
                slackUser("U_TA", "Tee Ay", "ta@ucsb.edu"),
                slackUser("U_ALICE", "Alice Student", "alice@ucsb.edu"),
                slackUser("U_BOB", "Bob Student", "bob@umail.ucsb.edu"),
                slackUser("U_CAROL", "Carol Student", "carol@ucsb.edu"),
                slackUser("U_DAVE", "Dave Student", "dave@ucsb.edu"),
                slackUser("U_DROPPED", "Dropped Student", "dropped@ucsb.edu"),
                slackUser("U_STRANGER", "Some Stranger", "stranger@example.org"),
                slackUser("U_NOEMAIL", "No Email", null),
                bot,
                invited,
                deactivated,
                reinvited,
                reinvitedOldAccount,
                deletedInvitation,
                twiceDeactivated1,
                twiceDeactivated2));

    when(sectionRepository.findByCourseIdOrderBySectionAsc(1L))
        .thenReturn(
            List.of(
                section("0100", " # Sec-0100 "),
                section("0150", "sec-0100"),
                section("0200", "sec-0200"),
                section("0400", null),
                section("0500", "   "),
                section("0600", "old-archived"),
                section("0700", "bad name")));
    when(slackService.listPublicChannels(TOKEN))
        .thenReturn(
            List.of(
                SlackChannel.builder().id("C100").name("sec-0100").build(),
                SlackChannel.builder().id("C_GENERAL").name("general").build(),
                SlackChannel.builder().id("C_ARCH").name("old-archived").archived(true).build()));
    when(slackService.createPublicChannel(TOKEN, "sec-0200"))
        .thenReturn(SlackChannel.builder().id("C200").name("sec-0200").build());
    when(slackService.createPublicChannel(TOKEN, "bad name"))
        .thenThrow(new SlackApiException("invalid_name_specials"));

    students(
        student("Alice", "alice@ucsb.edu", "0100"),
        student("Bob", "bob@ucsb.edu", "0100"),
        student("Carol", "carol@ucsb.edu", "0200"),
        student("Dave", "dave@ucsb.edu", "0150"),
        student("Eve", "eve@ucsb.edu", "0100"),
        student("Frank", "frank@ucsb.edu", "0100"),
        student("Grace", "grace@ucsb.edu", "0100"),
        student("Heidi", "heidi@ucsb.edu", "0200"),
        student("Judy", "judy@ucsb.edu", "0100"),
        student("NoEmail", null, "0100"),
        student("Other", "other@ucsb.edu", "0300"),
        student("NoSection", "nosection@ucsb.edu", null),
        RosterStudent.builder()
            .firstName("Dropped")
            .lastName("Student")
            .email("dropped2@ucsb.edu")
            .section("0100")
            .rosterStatus(RosterStatus.DROPPED)
            .build());
    when(courseStaffRepository.findByCourseId(1L))
        .thenReturn(
            List.of(
                CourseStaff.builder().email("TA@ucsb.edu").build(),
                CourseStaff.builder().email("notinslack@ucsb.edu").build(),
                CourseStaff.builder().email(null).build()));

    when(slackService.listChannelMembers(TOKEN, "C100"))
        .thenReturn(
            List.of(
                "U_BOT",
                "U_PROF",
                "U_TA",
                "U_ALICE",
                "U_CAROL",
                "U_STRANGER",
                "U_EXTERNAL",
                "U_DROPPED",
                "U_NOEMAIL"));
    when(slackService.listChannelMembers(TOKEN, "C200")).thenReturn(List.of("U_BOT"));

    job().accept(ctx);

    assertEquals(
        log(
            "Creating Section Channels",
            "Channel #sec-0100 already exists",
            "Created channel #sec-0200",
            "Channel #old-archived already exists, but is archived; unarchive it in Slack, then run this job again. Skipping it.",
            "Error setting up channel #bad name: invalid_name_specials. Skipping it.",
            "Adding Students to Channel",
            "Student Alice Student (alice@ucsb.edu) is in untranslated roster section 0100 and maps to #sec-0100",
            "Student Bob Student (bob@ucsb.edu) is in untranslated roster section 0100 and maps to #sec-0100",
            "Student Carol Student (carol@ucsb.edu) is in untranslated roster section 0200 and maps to #sec-0200",
            "Student Dave Student (dave@ucsb.edu) is in untranslated roster section 0150 and maps to #sec-0100",
            "Student Eve Student (eve@ucsb.edu) is in untranslated roster section 0100 and maps to #sec-0100",
            "Student Frank Student (frank@ucsb.edu) is in untranslated roster section 0100 and maps to #sec-0100",
            "Student Grace Student (grace@ucsb.edu) is in untranslated roster section 0100 and maps to #sec-0100",
            "Student Heidi Student (heidi@ucsb.edu) is in untranslated roster section 0200 and maps to #sec-0200",
            "Student Judy Student (judy@ucsb.edu) is in untranslated roster section 0100 and maps to #sec-0100",
            "Student NoEmail Student (null) is in untranslated roster section 0100 and maps to #sec-0100",
            "Student Other Student (other@ucsb.edu) is in untranslated roster section 0300",
            "Could not add Other Student (other@ucsb.edu) to a section Slack channel because no configured channel matches untranslated roster section 0300.",
            "Student NoSection Student (nosection@ucsb.edu) is in untranslated roster section (blank)",
            "Could not add NoSection Student (nosection@ucsb.edu) to a section Slack channel because no configured channel matches untranslated roster section (blank).",
            "Added Bob Student (bob@ucsb.edu) to #sec-0100",
            "Added Dave Student (dave@ucsb.edu) to #sec-0100",
            "Added Carol Student (carol@ucsb.edu) to #sec-0200",
            "6 student(s) in these sections could not be added: 2 invited to the Slack workspace but not accepted yet, 3 with a deactivated Slack account, 1 not in the Slack workspace. See the Slack tab. Run this job again once they have joined.",
            "Removing Channel Members Who Are Not In The Section",
            "Removed Carol Student (carol@ucsb.edu) from #sec-0100",
            "Removed Some Stranger (stranger@example.org) from #sec-0100",
            "Removed Dropped Student (dropped@ucsb.edu) from #sec-0100",
            "Removed No Email (null) from #sec-0100",
            "Done"),
        jobStarted.getLog());

    verify(slackService).listUsers(TOKEN);
    verify(slackService).listPublicChannels(TOKEN);
    verify(slackService).joinChannel(TOKEN, "C100");
    verify(slackService).createPublicChannel(TOKEN, "sec-0200");
    verify(slackService).createPublicChannel(TOKEN, "bad name");
    verify(slackService).listChannelMembers(TOKEN, "C100");
    verify(slackService).listChannelMembers(TOKEN, "C200");
    verify(slackService).inviteToChannel(TOKEN, "C100", List.of("U_BOB", "U_DAVE"));
    verify(slackService).inviteToChannel(TOKEN, "C200", List.of("U_CAROL"));
    verify(slackService).removeFromChannel(TOKEN, "C100", "U_CAROL");
    verify(slackService).removeFromChannel(TOKEN, "C100", "U_STRANGER");
    verify(slackService).removeFromChannel(TOKEN, "C100", "U_DROPPED");
    verify(slackService).removeFromChannel(TOKEN, "C100", "U_NOEMAIL");
    // in particular: the archived channel is not joined, nobody else is removed (not the bot, the
    // instructor, the staff, the students of the section, or the member Slack does not list as a
    // user), and nobody is removed from the new channel
    verifyNoMoreInteractions(slackService);
  }

  @Test
  public void adds_students_one_at_a_time_if_adding_them_together_fails() throws Exception {
    courseHasToken();
    when(slackService.listUsers(TOKEN))
        .thenReturn(
            List.of(
                slackUser("U_ALICE", "Alice Student", "alice@ucsb.edu"),
                slackUser("U_BOB", "Bob Student", "bob@ucsb.edu")));
    when(sectionRepository.findByCourseIdOrderBySectionAsc(1L))
        .thenReturn(List.of(section("0100", "sec-0100")));
    when(slackService.listPublicChannels(TOKEN)).thenReturn(List.of());
    when(slackService.createPublicChannel(TOKEN, "sec-0100"))
        .thenReturn(SlackChannel.builder().id("C100").name("sec-0100").build());
    students(student("Alice", "alice@ucsb.edu", "0100"), student("Bob", "bob@ucsb.edu", "0100"));
    when(courseStaffRepository.findByCourseId(1L)).thenReturn(List.of());
    when(slackService.listChannelMembers(TOKEN, "C100")).thenReturn(List.of());

    Mockito.doThrow(new SlackApiException("user_is_restricted"))
        .when(slackService)
        .inviteToChannel(TOKEN, "C100", List.of("U_ALICE", "U_BOB"));
    Mockito.doThrow(new SlackApiException("user_is_restricted"))
        .when(slackService)
        .inviteToChannel(TOKEN, "C100", List.of("U_ALICE"));

    job().accept(ctx);

    assertEquals(
        log(
            "Creating Section Channels",
            "Created channel #sec-0100",
            "Adding Students to Channel",
            "Student Alice Student (alice@ucsb.edu) is in untranslated roster section 0100 and maps to #sec-0100",
            "Student Bob Student (bob@ucsb.edu) is in untranslated roster section 0100 and maps to #sec-0100",
            "Could not add 2 student(s) to #sec-0100 in one step (user_is_restricted); adding them one at a time.",
            "Error adding Alice Student (alice@ucsb.edu) to #sec-0100: user_is_restricted",
            "Added Bob Student (bob@ucsb.edu) to #sec-0100",
            "Removing Channel Members Who Are Not In The Section",
            "Done"),
        jobStarted.getLog());
    verify(slackService).inviteToChannel(TOKEN, "C100", List.of("U_BOB"));
    verify(slackService, never()).removeFromChannel(any(), any(), any());
  }

  @Test
  public void carries_on_when_a_channel_cannot_be_joined_listed_or_a_member_removed()
      throws Exception {
    Course courseWithoutInstructor =
        Course.builder().id(1L).slackBotToken("enc:v1:ciphertext").build();
    when(courseRepository.findById(1L)).thenReturn(Optional.of(courseWithoutInstructor));
    when(tokenSecurityService.decrypt("enc:v1:ciphertext")).thenReturn(TOKEN);

    when(slackService.listUsers(TOKEN))
        .thenReturn(
            List.of(
                slackUser("U_ALICE", "Alice Student", "alice@ucsb.edu"),
                slackUser("U_X", "Some Stranger", "stranger@example.org"),
                slackUser("U_Y", "Other Stranger", "other@example.org")));
    when(sectionRepository.findByCourseIdOrderBySectionAsc(1L))
        .thenReturn(
            List.of(
                section("0100", "cannot-join"),
                section("0200", "cannot-list"),
                section("0300", "sec-0300")));
    when(slackService.listPublicChannels(TOKEN))
        .thenReturn(
            List.of(
                SlackChannel.builder().id("C1").name("cannot-join").build(),
                SlackChannel.builder().id("C2").name("cannot-list").build(),
                SlackChannel.builder().id("C3").name("sec-0300").build()));
    Mockito.doThrow(new SlackApiException("missing_scope"))
        .when(slackService)
        .joinChannel(TOKEN, "C1");
    when(slackService.listChannelMembers(TOKEN, "C2"))
        .thenThrow(new SlackApiException("channel_not_found"));
    when(slackService.listChannelMembers(TOKEN, "C3")).thenReturn(List.of("U_ALICE", "U_X", "U_Y"));
    Mockito.doThrow(new SlackApiException("user_not_found"))
        .when(slackService)
        .removeFromChannel(TOKEN, "C3", "U_X");

    students(student("Alice", "alice@ucsb.edu", "0300"));
    when(courseStaffRepository.findByCourseId(1L)).thenReturn(List.of());

    job().accept(ctx);

    assertEquals(
        log(
            "Creating Section Channels",
            "Error setting up channel #cannot-join: missing_scope. Skipping it.",
            "Channel #cannot-list already exists",
            "Channel #sec-0300 already exists",
            "Adding Students to Channel",
            "Student Alice Student (alice@ucsb.edu) is in untranslated roster section 0300 and maps to #sec-0300",
            "Error listing members of #cannot-list: channel_not_found",
            "Removing Channel Members Who Are Not In The Section",
            "Error removing Some Stranger (stranger@example.org) from #sec-0300: user_not_found",
            "Removed Other Stranger (other@example.org) from #sec-0300",
            "Done"),
        jobStarted.getLog());
    verify(slackService, never()).listChannelMembers(TOKEN, "C1");
    verify(slackService, never()).inviteToChannel(any(), any(), any());
    verify(slackService, never()).removeFromChannel(TOKEN, "C3", "U_ALICE");
  }

  @Test
  public void fails_without_a_token() {
    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
    when(tokenSecurityService.decrypt("enc:v1:ciphertext")).thenReturn(null);

    IllegalStateException e = assertThrows(IllegalStateException.class, () -> job().accept(ctx));
    assertEquals(
        "No Slack token has been set for this course; enter one on the Settings tab.",
        e.getMessage());
    verifyNoMoreInteractions(slackService);
  }

  @Test
  public void fails_with_an_empty_token() {
    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
    when(tokenSecurityService.decrypt("enc:v1:ciphertext")).thenReturn("");

    assertThrows(IllegalStateException.class, () -> job().accept(ctx));
    verifyNoMoreInteractions(slackService);
  }

  @Test
  public void fails_before_changing_anything_if_slack_provides_no_emails() {
    courseHasToken();
    SlackUser deactivated = slackUser("U_OLD", "Old Account", "old@ucsb.edu");
    deactivated.setDeleted(true);
    when(slackService.listUsers(TOKEN))
        .thenReturn(List.of(slackUser("U_ALICE", "Alice Student", null), deactivated));

    IllegalStateException e = assertThrows(IllegalStateException.class, () -> job().accept(ctx));
    assertEquals(
        "Slack did not provide the email of any user. Add the users:read.email scope to the Slack app, reinstall it to the workspace, and save the new token on the Settings tab.",
        e.getMessage());
    verify(slackService).listUsers(TOKEN);
    verifyNoMoreInteractions(slackService);
    assertEquals(null, jobStarted.getLog());
  }

  /**
   * Runs the job for a course with one section channel and one student (zed@ucsb.edu) who is not
   * active in Slack.
   *
   * @param otherSlackUsers the Slack users other than the (active) instructor
   * @return the line of the log about students who could not be added
   */
  private String couldNotBeAddedLine(SlackUser... otherSlackUsers) throws Exception {
    courseHasToken();
    List<SlackUser> slackUsers = new java.util.ArrayList<>();
    slackUsers.add(slackUser("U_PROF", "Prof Essor", "prof@ucsb.edu"));
    slackUsers.addAll(List.of(otherSlackUsers));
    when(slackService.listUsers(TOKEN)).thenReturn(slackUsers);
    when(sectionRepository.findByCourseIdOrderBySectionAsc(1L))
        .thenReturn(List.of(section("0100", "sec-0100")));
    when(slackService.listPublicChannels(TOKEN)).thenReturn(List.of());
    when(slackService.createPublicChannel(TOKEN, "sec-0100"))
        .thenReturn(SlackChannel.builder().id("C100").name("sec-0100").build());
    students(student("Zed", "zed@ucsb.edu", "0100"));
    when(courseStaffRepository.findByCourseId(1L)).thenReturn(List.of());
    when(slackService.listChannelMembers(TOKEN, "C100")).thenReturn(List.of());

    job().accept(ctx);

    verify(slackService, never()).inviteToChannel(any(), any(), any());
    List<String> lines =
        jobStarted.getLog().lines().filter(line -> line.contains("could not be added")).toList();
    assertEquals(1, lines.size());
    return lines.get(0);
  }

  @Test
  public void says_when_the_only_reason_is_an_invitation_not_accepted_yet() throws Exception {
    SlackUser invited = slackUser("U_ZED", "Zed Student", "zed@ucsb.edu");
    invited.setInvitedUser(true);
    assertEquals(
        "1 student(s) in these sections could not be added: 1 invited to the Slack workspace but not accepted yet. See the Slack tab. Run this job again once they have joined.",
        couldNotBeAddedLine(invited));
  }

  @Test
  public void says_when_the_only_reason_is_a_deactivated_account() throws Exception {
    SlackUser deactivated = slackUser("U_ZED", "Zed Student", "zed@ucsb.edu");
    deactivated.setDeleted(true);
    assertEquals(
        "1 student(s) in these sections could not be added: 1 with a deactivated Slack account. See the Slack tab. Run this job again once they have joined.",
        couldNotBeAddedLine(deactivated));
  }

  @Test
  public void says_when_the_only_reason_is_not_being_in_the_workspace() throws Exception {
    // a bot is not a person, so its invitation does not make the student "invited"
    SlackUser botWithTheStudentsEmail = slackUser("U_ZEDBOT", "Zed Bot", "zed@ucsb.edu");
    botWithTheStudentsEmail.setBot(true);
    botWithTheStudentsEmail.setInvitedUser(true);
    // nor does an invitation without an email
    SlackUser invitedWithoutEmail = slackUser("U_NOEMAIL", "No Email", null);
    invitedWithoutEmail.setInvitedUser(true);
    assertEquals(
        "1 student(s) in these sections could not be added: 1 not in the Slack workspace. See the Slack tab. Run this job again once they have joined.",
        couldNotBeAddedLine(botWithTheStudentsEmail, invitedWithoutEmail));
  }

  @Test
  public void stops_removing_and_explains_when_the_workspace_forbids_removals() throws Exception {
    Course courseWithoutInstructor =
        Course.builder().id(1L).slackBotToken("enc:v1:ciphertext").build();
    when(courseRepository.findById(1L)).thenReturn(Optional.of(courseWithoutInstructor));
    when(tokenSecurityService.decrypt("enc:v1:ciphertext")).thenReturn(TOKEN);
    when(slackService.listUsers(TOKEN))
        .thenReturn(
            List.of(
                slackUser("U_ALICE", "Alice Student", "alice@ucsb.edu"),
                slackUser("U_X", "Some Stranger", "stranger@example.org"),
                slackUser("U_Y", "Other Stranger", "other@example.org"),
                slackUser("U_Z", "Third Stranger", "third@example.org")));
    when(sectionRepository.findByCourseIdOrderBySectionAsc(1L))
        .thenReturn(List.of(section("0100", "sec-0100"), section("0300", "sec-0300")));
    when(slackService.listPublicChannels(TOKEN))
        .thenReturn(
            List.of(
                SlackChannel.builder().id("C1").name("sec-0100").build(),
                SlackChannel.builder().id("C3").name("sec-0300").build()));
    when(slackService.listChannelMembers(TOKEN, "C1")).thenReturn(List.of("U_X", "U_Y"));
    when(slackService.listChannelMembers(TOKEN, "C3")).thenReturn(List.of("U_ALICE", "U_Z"));
    Mockito.doThrow(new SlackApiException("restricted_action"))
        .when(slackService)
        .removeFromChannel(TOKEN, "C1", "U_X");
    students(student("Alice", "alice@ucsb.edu", "0300"));
    when(courseStaffRepository.findByCourseId(1L)).thenReturn(List.of());

    job().accept(ctx);

    assertEquals(
        log(
            "Creating Section Channels",
            "Channel #sec-0100 already exists",
            "Channel #sec-0300 already exists",
            "Adding Students to Channel",
            "Student Alice Student (alice@ucsb.edu) is in untranslated roster section 0300 and maps to #sec-0300",
            "Removing Channel Members Who Are Not In The Section",
            "Error removing Some Stranger (stranger@example.org) from #sec-0100: restricted_action",
            SlackService.REMOVAL_RESTRICTED_ADVICE,
            "Done"),
        jobStarted.getLog());
    // Once Slack has refused, nobody else is tried: not in the same channel, nor in later ones
    verify(slackService).removeFromChannel(TOKEN, "C1", "U_X");
    verify(slackService, never()).removeFromChannel(TOKEN, "C1", "U_Y");
    verify(slackService, never()).removeFromChannel(TOKEN, "C3", "U_Z");
    verify(slackService, never()).removeFromChannel(TOKEN, "C3", "U_ALICE");
  }
}
