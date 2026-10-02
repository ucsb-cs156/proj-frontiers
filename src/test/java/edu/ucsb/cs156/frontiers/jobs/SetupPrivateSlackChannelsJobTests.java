package edu.ucsb.cs156.frontiers.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.CourseStaff;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.enums.RosterStatus;
import edu.ucsb.cs156.frontiers.errors.SlackApiException;
import edu.ucsb.cs156.frontiers.models.SlackAuthTestResponse;
import edu.ucsb.cs156.frontiers.models.SlackChannel;
import edu.ucsb.cs156.frontiers.models.SlackUser;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseStaffRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
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
public class SetupPrivateSlackChannelsJobTests {

  @Mock private CourseRepository courseRepository;
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
          .instructorEmail("Prof@UCSB.edu")
          .slackBotToken("enc:v1:ciphertext")
          .build();

  private SetupPrivateSlackChannelsJob job() {
    return SetupPrivateSlackChannelsJob.builder()
        .course(Course.builder().id(1L).build())
        .courseRepository(courseRepository)
        .rosterStudentRepository(rosterStudentRepository)
        .courseStaffRepository(courseStaffRepository)
        .slackService(slackService)
        .tokenSecurityService(tokenSecurityService)
        .build();
  }

  private static SlackUser slackUser(String id, String firstName, String lastName, String email) {
    return SlackUser.builder()
        .id(id)
        .profile(
            SlackUser.Profile.builder()
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .build())
        .build();
  }

  private static RosterStudent student(String firstName, String lastName, String email) {
    return RosterStudent.builder().firstName(firstName).lastName(lastName).email(email).build();
  }

  private static SlackChannel channel(String id, String name) {
    return SlackChannel.builder().id(id).name(name).creator("U_BOT").build();
  }

  private void courseHasToken(Course theCourse) {
    when(courseRepository.findById(1L)).thenReturn(Optional.of(theCourse));
    when(tokenSecurityService.decrypt("enc:v1:ciphertext")).thenReturn(TOKEN);
  }

  private void rosterIs(RosterStudent... students) {
    when(rosterStudentRepository
            .findByCourseIdAndRosterStatusInOrderByFirstNameAscLastNameAscIgnoreCase(
                1L, List.of(RosterStatus.ROSTER, RosterStatus.MANUAL)))
        .thenReturn(List.of(students));
  }

  private void botIs(String userId) {
    when(slackService.authTest(TOKEN))
        .thenReturn(SlackAuthTestResponse.builder().ok(true).userId(userId).build());
  }

  private static String log(String... lines) {
    return String.join("\n", lines);
  }

  @Test
  public void scope_is_the_course() {
    SetupPrivateSlackChannelsJob job =
        SetupPrivateSlackChannelsJob.builder().course(Course.builder().id(42L).build()).build();
    assertEquals("course", job.getScopeType());
    assertEquals(42L, job.getScopeId());
  }

  @Test
  public void sanitize_follows_the_rules() {
    assertEquals("", SetupPrivateSlackChannelsJob.sanitize(null));
    assertEquals("", SetupPrivateSlackChannelsJob.sanitize(""));
    assertEquals("gaucho", SetupPrivateSlackChannelsJob.sanitize("Gaucho"));
    assertEquals("jose-nunez", SetupPrivateSlackChannelsJob.sanitize("José Núñez"));
    assertEquals("o-brien", SetupPrivateSlackChannelsJob.sanitize("O'Brien"));
    assertEquals("de-la-cruz", SetupPrivateSlackChannelsJob.sanitize("De La Cruz"));
    assertEquals("smith-jones", SetupPrivateSlackChannelsJob.sanitize("Smith-Jones"));
    assertEquals("a_b-c3", SetupPrivateSlackChannelsJob.sanitize("a_b . c3"));
    assertEquals("kim", SetupPrivateSlackChannelsJob.sanitize(" .Kim, "));
    assertEquals("", SetupPrivateSlackChannelsJob.sanitize("---"));
  }

  @Test
  public void firstWord_is_the_first_name_up_to_the_first_space() {
    assertEquals("", SetupPrivateSlackChannelsJob.firstWord(null));
    assertEquals("", SetupPrivateSlackChannelsJob.firstWord(""));
    assertEquals("Mary", SetupPrivateSlackChannelsJob.firstWord("Mary"));
    assertEquals("Mary", SetupPrivateSlackChannelsJob.firstWord("Mary Ann"));
    assertEquals("Mary", SetupPrivateSlackChannelsJob.firstWord("  Mary \t Ann Beth "));
  }

  @Test
  public void channelNameFor_follows_the_rules() {
    assertEquals(
        "private-chris-gaucho", SetupPrivateSlackChannelsJob.channelNameFor("Chris", "Gaucho", ""));
    assertEquals(
        "private-mary-de-la-cruz",
        SetupPrivateSlackChannelsJob.channelNameFor("Mary Ann", "De La Cruz", ""));
    assertEquals(
        "private-chris-gaucho-cgaucho",
        SetupPrivateSlackChannelsJob.channelNameFor("Chris", "Gaucho", "cgaucho"));
    assertEquals("private-chris", SetupPrivateSlackChannelsJob.channelNameFor("Chris", "", ""));
    assertEquals("private-gaucho", SetupPrivateSlackChannelsJob.channelNameFor(null, "Gaucho", ""));
    assertEquals("private", SetupPrivateSlackChannelsJob.channelNameFor(null, null, ""));

    // Slack allows at most 80 characters
    assertEquals(80, SetupPrivateSlackChannelsJob.MAX_CHANNEL_NAME_LENGTH);
    String eighty = "private-a-" + "b".repeat(70);
    assertEquals(80, eighty.length());
    assertEquals(eighty, SetupPrivateSlackChannelsJob.channelNameFor("A", "b".repeat(70), ""));
    assertEquals(eighty, SetupPrivateSlackChannelsJob.channelNameFor("A", "b".repeat(71), ""));
  }

  @Test
  public void creates_and_renames_channels_and_adds_the_student_and_the_staff() throws Exception {
    courseHasToken(course);

    SlackUser bot = slackUser("U_BOT", "Frontiers", "Bot", "bot@ucsb.edu");
    bot.setBot(true);
    SlackUser invited = slackUser("U_FRANK", "Frank", "Student", "frank@ucsb.edu");
    invited.setInvitedUser(true);
    when(slackService.listUsers(TOKEN))
        .thenReturn(
            List.of(
                slackUser("U_PROF", "Prof", "Essor", "prof@ucsb.edu"),
                slackUser("U_TA", "Tee", "Ay", "ta@ucsb.edu"),
                // names come from Slack when it has them...
                slackUser("U_ALICE", "Alice Marie", "Anderson", "alice@ucsb.edu"),
                // ...one by one: Slack has no last name here, so that comes from the roster
                slackUser("U_BOB", "Bobby", " ", "bob@umail.ucsb.edu"),
                slackUser("U_CAROL", null, null, "carol@ucsb.edu"),
                slackUser("U_DAVE", "Dave", "Student", "dave@ucsb.edu"),
                slackUser("U_ERIN", "Erin", "Student", "erin@ucsb.edu"),
                slackUser("U_STRANGER", "Some", "Stranger", "stranger@example.org"),
                slackUser("U_NOEMAIL", "No", "Email", null),
                bot,
                invited));

    when(courseStaffRepository.findByCourseId(1L))
        .thenReturn(
            List.of(
                CourseStaff.builder().email("TA@ucsb.edu").build(),
                // the instructor may be listed as staff too; they are still added only once
                CourseStaff.builder().email("prof@ucsb.edu").build(),
                CourseStaff.builder().email("notinslack@ucsb.edu").build(),
                CourseStaff.builder().email(null).build()));

    rosterIs(
        student("Alice", "Student", "alice@ucsb.edu"),
        student("Bob", "Student", "bob@ucsb.edu"),
        // the same person twice on the roster gets one channel
        student("Robert", "Student", "bob@umail.ucsb.edu"),
        student("Carol Ann", "Cruz", "carol@ucsb.edu"),
        student("Dave", "Student", "dave@ucsb.edu"),
        student("Erin", "Student", "erin@ucsb.edu"),
        student("Frank", "Student", "frank@ucsb.edu"),
        student("NoEmail", "Student", null),
        student("Prof", "Essor", "prof@ucsb.edu"),
        student("Tee", "Ay", "ta@ucsb.edu"));

    botIs("U_BOT");
    SlackChannel archived = channel("C_ERIN", "private-erin-student");
    archived.setArchived(true);
    when(slackService.listPrivateChannels(TOKEN))
        .thenReturn(
            List.of(
                channel("C_DAVE", "private-dave-student"),
                // found by its members, whatever its name
                channel("C_BOB", "private-robert-old"),
                archived,
                // not created by the bot, so not even looked at
                SlackChannel.builder().id("C_OTHER").name("private-alice-anderson").build(),
                SlackChannel.builder().id("C_THEIRS").name("secret").creator("U_PROF").build(),
                // no student, and more than one student: nobody's channel
                channel("C_STAFF", "staff-only"),
                channel("C_TWO", "study-group")));
    when(slackService.listChannelMembers(TOKEN, "C_DAVE"))
        .thenReturn(List.of("U_BOT", "U_DAVE", "U_PROF"));
    when(slackService.listChannelMembers(TOKEN, "C_BOB"))
        .thenReturn(List.of("U_BOT", "U_PROF", "U_TA", "U_BOB", "U_STRANGER", "U_EXTERNAL"));
    when(slackService.listChannelMembers(TOKEN, "C_ERIN")).thenReturn(List.of("U_ERIN"));
    when(slackService.listChannelMembers(TOKEN, "C_STAFF"))
        .thenReturn(List.of("U_BOT", "U_PROF", "U_TA"));
    when(slackService.listChannelMembers(TOKEN, "C_TWO"))
        .thenReturn(List.of("U_BOT", "U_ALICE", "U_CAROL"));
    when(slackService.createPrivateChannel(TOKEN, "private-alice-anderson"))
        .thenReturn(channel("C_ALICE", "private-alice-anderson"));
    when(slackService.createPrivateChannel(TOKEN, "private-carol-cruz"))
        .thenReturn(channel("C_CAROL", "private-carol-cruz"));

    job().accept(ctx);

    assertEquals(
        log(
            "Creating Private Channels (5 student(s), channel names start with private-)",
            "Prof Essor (prof@ucsb.edu) is also the instructor or a staff member of the course, so does not get a private channel.",
            "Tee Ay (ta@ucsb.edu) is also the instructor or a staff member of the course, so does not get a private channel.",
            "Created private channel #private-alice-anderson for Alice Student (alice@ucsb.edu)",
            "Added Alice Student (alice@ucsb.edu) to #private-alice-anderson",
            "Added instructor Prof@UCSB.edu to #private-alice-anderson",
            "Added staff member TA@ucsb.edu to #private-alice-anderson",
            "Renamed private channel #private-robert-old to #private-bobby-student for Bob Student (bob@ucsb.edu)",
            "Created private channel #private-carol-cruz for Carol Ann Cruz (carol@ucsb.edu)",
            "Added Carol Ann Cruz (carol@ucsb.edu) to #private-carol-cruz",
            "Added instructor Prof@UCSB.edu to #private-carol-cruz",
            "Added staff member TA@ucsb.edu to #private-carol-cruz",
            "Private channel #private-dave-student for Dave Student (dave@ucsb.edu) already exists",
            "Added staff member TA@ucsb.edu to #private-dave-student",
            "Private channel #private-erin-student for Erin Student (erin@ucsb.edu) already exists, but is archived; unarchive it in Slack, then run this job again. Skipping it.",
            "2 student(s) did not get a private channel, because they do not have an active account in the Slack workspace; see the Slack tab. Run this job again once they have joined.",
            "2 staff member(s) (counting the instructor) could not be added to the channels, because they do not have an active account in the Slack workspace; see the Slack tab. Run this job again once they have joined.",
            "Done. Channels created: 2, already existed: 2 (of which renamed: 1). Members added: 7, already present: 5."),
        jobStarted.getLog());

    verify(slackService).renameChannel(TOKEN, "C_BOB", "private-bobby-student");
    verify(slackService, Mockito.times(1)).renameChannel(any(), any(), any());
    verify(slackService, Mockito.times(2)).createPrivateChannel(any(), any());
    verify(slackService).inviteToChannel(TOKEN, "C_ALICE", List.of("U_ALICE", "U_PROF", "U_TA"));
    verify(slackService).inviteToChannel(TOKEN, "C_CAROL", List.of("U_CAROL", "U_PROF", "U_TA"));
    verify(slackService).inviteToChannel(TOKEN, "C_DAVE", List.of("U_TA"));
    // everybody who belongs in Bob's channel is in it already; nobody is ever removed from it
    verify(slackService, never()).inviteToChannel(eq(TOKEN), eq("C_BOB"), any());
    verify(slackService, never()).inviteToChannel(eq(TOKEN), eq("C_ERIN"), any());
    verify(slackService, never()).removeFromChannel(any(), any(), any());
    verify(slackService, never()).listChannelMembers(TOKEN, "C_OTHER");
    verify(slackService, never()).listChannelMembers(TOKEN, "C_THEIRS");
    verify(slackService, never()).listPublicChannels(any());
  }

  @Test
  public void adds_the_instructor_even_when_they_are_not_listed_as_staff() throws Exception {
    courseHasToken(course);
    when(slackService.listUsers(TOKEN))
        .thenReturn(
            List.of(
                slackUser("U_PROF", "Prof", "Essor", "prof@ucsb.edu"),
                slackUser("U_ALICE", "Alice", "Student", "alice@ucsb.edu"),
                slackUser("U_BOB", "Bob", "Student", "bob@ucsb.edu")));
    // the Staff tab is empty: the instructor comes from the course itself
    when(courseStaffRepository.findByCourseId(1L)).thenReturn(List.of());
    rosterIs(
        student("Alice", "Student", "alice@ucsb.edu"), student("Bob", "Student", "bob@ucsb.edu"));
    botIs("U_BOT");
    when(slackService.listPrivateChannels(TOKEN))
        .thenReturn(List.of(channel("C_BOB", "private-bob-student")));
    when(slackService.listChannelMembers(TOKEN, "C_BOB")).thenReturn(List.of("U_BOT", "U_BOB"));
    when(slackService.createPrivateChannel(TOKEN, "private-alice-student"))
        .thenReturn(channel("C_ALICE", "private-alice-student"));

    job().accept(ctx);

    assertEquals(
        log(
            "Creating Private Channels (2 student(s), channel names start with private-)",
            "Created private channel #private-alice-student for Alice Student (alice@ucsb.edu)",
            "Added Alice Student (alice@ucsb.edu) to #private-alice-student",
            "Added instructor Prof@UCSB.edu to #private-alice-student",
            "Private channel #private-bob-student for Bob Student (bob@ucsb.edu) already exists",
            "Added instructor Prof@UCSB.edu to #private-bob-student",
            "Done. Channels created: 1, already existed: 1 (of which renamed: 0). Members added: 3, already present: 1."),
        jobStarted.getLog());
    verify(slackService).inviteToChannel(TOKEN, "C_ALICE", List.of("U_ALICE", "U_PROF"));
    verify(slackService).inviteToChannel(TOKEN, "C_BOB", List.of("U_PROF"));
  }

  @Test
  public void students_with_the_same_name_are_told_apart_by_email() throws Exception {
    Course noInstructor = Course.builder().id(1L).slackBotToken("enc:v1:ciphertext").build();
    courseHasToken(noInstructor);
    when(slackService.listUsers(TOKEN))
        .thenReturn(
            List.of(
                slackUser("U_SAM1", "Sam", "Lee", "sam.lee@ucsb.edu"),
                slackUser("U_SAM2", "Sam", "Lee", "slee2@ucsb.edu"),
                slackUser("U_PAT", "Pat", "Lee", "pat@ucsb.edu")));
    when(courseStaffRepository.findByCourseId(1L)).thenReturn(List.of());
    rosterIs(
        student("Samuel", "Lee", "Sam.Lee@ucsb.edu"),
        student("Samantha", "Lee", "slee2@ucsb.edu"),
        student("Pat", "Lee", "pat@ucsb.edu"));
    botIs("U_BOT");
    // Sam's channel from before there was a second Sam Lee gets the longer name
    when(slackService.listPrivateChannels(TOKEN))
        .thenReturn(List.of(channel("C_SAM1", "private-sam-lee")));
    when(slackService.listChannelMembers(TOKEN, "C_SAM1")).thenReturn(List.of("U_BOT", "U_SAM1"));
    when(slackService.createPrivateChannel(TOKEN, "private-sam-lee-slee2"))
        .thenReturn(channel("C_SAM2", "private-sam-lee-slee2"));
    when(slackService.createPrivateChannel(TOKEN, "private-pat-lee"))
        .thenReturn(channel("C_PAT", "private-pat-lee"));

    job().accept(ctx);

    assertEquals(
        log(
            "Creating Private Channels (3 student(s), channel names start with private-)",
            "Renamed private channel #private-sam-lee to #private-sam-lee-sam-lee for Samuel Lee (Sam.Lee@ucsb.edu)",
            "Created private channel #private-sam-lee-slee2 for Samantha Lee (slee2@ucsb.edu)",
            "Added Samantha Lee (slee2@ucsb.edu) to #private-sam-lee-slee2",
            "Created private channel #private-pat-lee for Pat Lee (pat@ucsb.edu)",
            "Added Pat Lee (pat@ucsb.edu) to #private-pat-lee",
            "Done. Channels created: 2, already existed: 1 (of which renamed: 1). Members added: 2, already present: 1."),
        jobStarted.getLog());
    verify(slackService).renameChannel(TOKEN, "C_SAM1", "private-sam-lee-sam-lee");
  }

  @Test
  public void carries_on_after_errors_and_adds_one_at_a_time_when_adding_together_fails()
      throws Exception {
    // the instructor does not have a Slack account
    courseHasToken(course);
    when(slackService.listUsers(TOKEN))
        .thenReturn(
            List.of(
                slackUser("U_TA", "Tee", "Ay", "ta@ucsb.edu"),
                slackUser("U_ALICE", "Alice", "Student", "alice@ucsb.edu"),
                slackUser("U_BOB", "Bob", "Student", "bob@ucsb.edu"),
                slackUser("U_CAROL", "Carol", "Student", "carol@ucsb.edu"),
                slackUser("U_DAVE", "Dave", "Student", "dave@ucsb.edu")));
    when(courseStaffRepository.findByCourseId(1L))
        .thenReturn(List.of(CourseStaff.builder().email("ta@ucsb.edu").build()));
    rosterIs(
        student("Alice", "Student", "alice@ucsb.edu"),
        student("Bob", "Student", "bob@ucsb.edu"),
        student("Carol", "Student", "carol@ucsb.edu"),
        student("Dave", "Student", "dave@ucsb.edu"));
    botIs("U_BOT");
    when(slackService.listPrivateChannels(TOKEN))
        .thenReturn(List.of(channel("C_CAROL", "private-carol-old")));
    when(slackService.listChannelMembers(TOKEN, "C_CAROL")).thenReturn(List.of("U_CAROL"));
    when(slackService.createPrivateChannel(TOKEN, "private-alice-student"))
        .thenThrow(new SlackApiException("name_taken"));
    when(slackService.createPrivateChannel(TOKEN, "private-bob-student"))
        .thenThrow(new SlackApiException("missing_scope"));
    Mockito.doThrow(new SlackApiException("not_authorized"))
        .when(slackService)
        .renameChannel(TOKEN, "C_CAROL", "private-carol-student");
    when(slackService.createPrivateChannel(TOKEN, "private-dave-student"))
        .thenReturn(channel("C_DAVE", "private-dave-student"));
    // (lenient, because inviteToChannel is also called with other arguments, which succeed)
    Mockito.lenient()
        .doThrow(new SlackApiException("user_is_restricted"))
        .when(slackService)
        .inviteToChannel(TOKEN, "C_DAVE", List.of("U_DAVE", "U_TA"));
    Mockito.lenient()
        .doThrow(new SlackApiException("user_is_restricted"))
        .when(slackService)
        .inviteToChannel(TOKEN, "C_DAVE", List.of("U_DAVE"));

    job().accept(ctx);

    assertEquals(
        log(
            "Creating Private Channels (4 student(s), channel names start with private-)",
            "Error creating private channel #private-alice-student for Alice Student (alice@ucsb.edu): name_taken. Skipping it.",
            "Slack already has a channel named #private-alice-student, which this job does not recognize as the private channel of this student: it may be a public channel, a private channel that this bot did not create, or a private channel that does not have exactly one student of the course among its members. Rename that channel in Slack, or fix its members, then run this job again.",
            "Error creating private channel #private-bob-student for Bob Student (bob@ucsb.edu): missing_scope. Skipping it.",
            "Error renaming private channel #private-carol-old to #private-carol-student for Carol Student (carol@ucsb.edu): not_authorized. It keeps its name.",
            "Added staff member ta@ucsb.edu to #private-carol-old",
            "Created private channel #private-dave-student for Dave Student (dave@ucsb.edu)",
            "Could not add 2 member(s) to #private-dave-student in one step (user_is_restricted); adding them one at a time.",
            "Error adding Dave Student (dave@ucsb.edu) to #private-dave-student: user_is_restricted",
            "Added staff member ta@ucsb.edu to #private-dave-student",
            "1 staff member(s) (counting the instructor) could not be added to the channels, because they do not have an active account in the Slack workspace; see the Slack tab. Run this job again once they have joined.",
            "Done. Channels created: 1, already existed: 1 (of which renamed: 0). Members added: 2, already present: 1."),
        jobStarted.getLog());
    verify(slackService).inviteToChannel(TOKEN, "C_CAROL", List.of("U_TA"));
    verify(slackService).inviteToChannel(TOKEN, "C_DAVE", List.of("U_TA"));
  }

  @Test
  public void a_student_with_several_channels_keeps_the_first_one_that_is_not_archived()
      throws Exception {
    Course noInstructor = Course.builder().id(1L).slackBotToken("enc:v1:ciphertext").build();
    courseHasToken(noInstructor);
    when(slackService.listUsers(TOKEN))
        .thenReturn(
            List.of(
                slackUser("U_ALICE", "Alice", "Student", "alice@ucsb.edu"),
                slackUser("U_BOB", "Bob", "Student", "bob@ucsb.edu"),
                slackUser("U_CAROL", "Carol", "Student", "carol@ucsb.edu")));
    when(courseStaffRepository.findByCourseId(1L)).thenReturn(List.of());
    rosterIs(
        student("Alice", "Student", "alice@ucsb.edu"),
        student("Bob", "Student", "bob@ucsb.edu"),
        student("Carol", "Student", "carol@ucsb.edu"));
    botIs("U_BOT");
    SlackChannel aliceArchived = channel("C_A1", "private-alice-archived");
    aliceArchived.setArchived(true);
    SlackChannel carolArchived = channel("C_C2", "private-carol-archived");
    carolArchived.setArchived(true);
    when(slackService.listPrivateChannels(TOKEN))
        .thenReturn(
            List.of(
                aliceArchived,
                channel("C_A2", "private-alice-student"),
                channel("C_B1", "private-bob-student"),
                channel("C_B2", "private-bob-second"),
                channel("C_C1", "private-carol-student"),
                carolArchived));
    when(slackService.listChannelMembers(TOKEN, "C_A1")).thenReturn(List.of("U_ALICE"));
    when(slackService.listChannelMembers(TOKEN, "C_A2")).thenReturn(List.of("U_BOT", "U_ALICE"));
    when(slackService.listChannelMembers(TOKEN, "C_B1")).thenReturn(List.of("U_BOB"));
    when(slackService.listChannelMembers(TOKEN, "C_B2")).thenReturn(List.of("U_BOB"));
    when(slackService.listChannelMembers(TOKEN, "C_C1")).thenReturn(List.of("U_CAROL"));
    when(slackService.listChannelMembers(TOKEN, "C_C2")).thenReturn(List.of("U_CAROL"));

    job().accept(ctx);

    assertEquals(
        log(
            "Creating Private Channels (3 student(s), channel names start with private-)",
            "Private channel #private-alice-student for Alice Student (alice@ucsb.edu) already exists",
            "Private channel #private-bob-student for Bob Student (bob@ucsb.edu) already exists",
            "Private channel #private-carol-student for Carol Student (carol@ucsb.edu) already exists",
            "Done. Channels created: 0, already existed: 3 (of which renamed: 0). Members added: 0, already present: 3."),
        jobStarted.getLog());
    verify(slackService, never()).createPrivateChannel(any(), any());
    verify(slackService, never()).renameChannel(any(), any(), any());
    verify(slackService, never()).inviteToChannel(any(), any(), any());
  }

  @Test
  public void stops_before_changing_anything_if_the_members_of_a_channel_cannot_be_listed() {
    courseHasToken(course);
    when(slackService.listUsers(TOKEN))
        .thenReturn(List.of(slackUser("U_ALICE", "Alice", "Student", "alice@ucsb.edu")));
    when(courseStaffRepository.findByCourseId(1L)).thenReturn(List.of());
    rosterIs(student("Alice", "Student", "alice@ucsb.edu"));
    botIs("U_BOT");
    when(slackService.listPrivateChannels(TOKEN))
        .thenReturn(List.of(channel("C_X", "private-somebody")));
    when(slackService.listChannelMembers(TOKEN, "C_X"))
        .thenThrow(new SlackApiException("missing_scope"));

    IllegalStateException e = assertThrows(IllegalStateException.class, () -> job().accept(ctx));

    assertEquals(
        "Could not list the members of the private channel #private-somebody (missing_scope), so cannot tell which student it belongs to. Nothing was changed in Slack.",
        e.getMessage());
    assertEquals(null, jobStarted.getLog());
    verify(slackService, never()).createPrivateChannel(any(), any());
    verify(slackService, never()).renameChannel(any(), any(), any());
    verify(slackService, never()).inviteToChannel(any(), any(), any());
  }

  @Test
  public void fails_if_slack_does_not_say_who_the_bot_is() {
    courseHasToken(course);
    when(slackService.listUsers(TOKEN))
        .thenReturn(List.of(slackUser("U_ALICE", "Alice", "Student", "alice@ucsb.edu")));
    when(courseStaffRepository.findByCourseId(1L)).thenReturn(List.of());
    rosterIs(student("Alice", "Student", "alice@ucsb.edu"));
    when(slackService.authTest(TOKEN))
        .thenReturn(SlackAuthTestResponse.builder().ok(false).error("token_revoked").build());

    SlackApiException e = assertThrows(SlackApiException.class, () -> job().accept(ctx));

    assertEquals("token_revoked", e.getMessage());
    verify(slackService, never()).listPrivateChannels(any());
    verify(slackService, never()).createPrivateChannel(any(), any());
  }

  @Test
  public void recognizes_no_channel_if_slack_gives_no_user_id_for_the_bot() throws Exception {
    Course noInstructor = Course.builder().id(1L).slackBotToken("enc:v1:ciphertext").build();
    courseHasToken(noInstructor);
    when(slackService.listUsers(TOKEN))
        .thenReturn(List.of(slackUser("U_ALICE", "Alice", "Student", "alice@ucsb.edu")));
    when(courseStaffRepository.findByCourseId(1L)).thenReturn(List.of());
    rosterIs(student("Alice", "Student", "alice@ucsb.edu"));
    botIs(null);
    when(slackService.listPrivateChannels(TOKEN))
        .thenReturn(List.of(SlackChannel.builder().id("C_X").name("private-x").build()));
    when(slackService.createPrivateChannel(TOKEN, "private-alice-student"))
        .thenReturn(channel("C_ALICE", "private-alice-student"));

    job().accept(ctx);

    assertEquals(
        log(
            "Creating Private Channels (1 student(s), channel names start with private-)",
            "Created private channel #private-alice-student for Alice Student (alice@ucsb.edu)",
            "Added Alice Student (alice@ucsb.edu) to #private-alice-student",
            "Done. Channels created: 1, already existed: 0 (of which renamed: 0). Members added: 1, already present: 0."),
        jobStarted.getLog());
    verify(slackService, never()).listChannelMembers(any(), any());
  }

  @Test
  public void fails_without_a_token() {
    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
    when(tokenSecurityService.decrypt("enc:v1:ciphertext")).thenReturn(null);
    assertThrows(IllegalStateException.class, () -> job().accept(ctx));
    when(tokenSecurityService.decrypt("enc:v1:ciphertext")).thenReturn("");
    IllegalStateException e = assertThrows(IllegalStateException.class, () -> job().accept(ctx));
    assertEquals(
        "No Slack token has been set for this course; enter one on the Settings tab.",
        e.getMessage());
    verifyNoMoreInteractions(slackService);
  }

  @Test
  public void fails_before_changing_anything_if_slack_provides_no_emails() {
    courseHasToken(course);
    SlackUser deactivated = slackUser("U_OLD", "Old", "Student", "old@ucsb.edu");
    deactivated.setDeleted(true);
    when(slackService.listUsers(TOKEN))
        .thenReturn(List.of(slackUser("U_A", "A", "Student", null), deactivated));

    IllegalStateException e = assertThrows(IllegalStateException.class, () -> job().accept(ctx));
    assertEquals(
        "Slack did not provide the email of any user. Add the users:read.email scope to the Slack app, reinstall it to the workspace, and save the new token on the Settings tab.",
        e.getMessage());
    verify(slackService).listUsers(TOKEN);
    verifyNoMoreInteractions(slackService);
  }
}
