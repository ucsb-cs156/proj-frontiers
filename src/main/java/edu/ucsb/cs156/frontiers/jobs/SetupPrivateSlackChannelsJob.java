package edu.ucsb.cs156.frontiers.jobs;

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
import edu.ucsb.cs156.frontiers.utilities.CanonicalFormConverter;
import edu.ucsb.cs156.jobs.services.JobContext;
import edu.ucsb.cs156.jobs.services.JobContextConsumer;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import lombok.Builder;

/**
 * Gives each student of a course a private Slack channel shared with the staff of the course.
 *
 * <ol>
 *   <li>For each roster student (status ROSTER or MANUAL) with an active Slack account, works out a
 *       channel name: "private-first-last" (see {@link #channelNameFor}). The first and last name
 *       come from the student's Slack profile when it has them, and otherwise from the roster; only
 *       the first word of the first name is used. Students who would get the same name also get the
 *       part of their email before the @ at the end.
 *   <li>Finds the channel each student already has. A channel is a student's if it is a private
 *       channel that this bot created, and that student is the only one of the students among its
 *       members. Channels are never recognized by name, so a channel is found again even after the
 *       name of the student has changed.
 *   <li>Creates the private channel of each student who does not have one, and renames the channel
 *       of each student whose channel does not have the name worked out in the first step.
 *   <li>Adds the student, the instructor and the staff of the course to the channel, unless they
 *       are already in it. People are matched to Slack users by email; those without an active
 *       Slack account cannot be added, and are only counted.
 *   <li>Removes from each student's channel every person who is neither the student, nor the
 *       instructor, nor a member of the course staff: for example somebody who has been deleted
 *       from the staff of the course. Bots (including the bot this job acts as) and members that
 *       are not users of the workspace are never removed. If Slack refuses because of a workspace
 *       setting (restricted_action), the log says how to change the setting and no further removals
 *       are attempted in that run.
 * </ol>
 *
 * A problem with one channel or one person is logged, and the job carries on with the rest; but if
 * the members of an existing channel cannot be listed, the job stops before changing anything,
 * since it could not tell whose channel that is and might create a duplicate. The last line of the
 * log is a summary.
 */
@Builder
public class SetupPrivateSlackChannelsJob implements JobContextConsumer {

  public static final String CHANNEL_PREFIX = "private";

  /** Slack does not allow longer channel names. */
  public static final int MAX_CHANNEL_NAME_LENGTH = 80;

  /** Slack's error code when a channel (public or private, archived or not) has the name. */
  public static final String NAME_TAKEN = "name_taken";

  /** What to tell the instructor when creating a channel fails with {@link #NAME_TAKEN}. */
  public static final String NAME_TAKEN_ADVICE =
      "Slack already has a channel named #%s, which this job does not recognize as the private channel of this student: it may be a public channel, a private channel that this bot did not create, or a private channel that does not have exactly one student of the course among its members. Rename that channel in Slack, or fix its members, then run this job again.";

  /**
   * What to tell the instructor when removing a member fails with {@link
   * SlackService#RESTRICTED_ACTION}.
   */
  public static final String REMOVAL_RESTRICTED_ADVICE =
      "Slack does not allow this bot to remove members from private channels, so no more members will be removed in this run. A Workspace Owner can change this in Slack under Workspace settings, Roles & permissions (on older workspaces: Permissions, Channel Management): set \"People who can remove members from private channels\" to \"Everyone, except guests\". Then run this job again.";

  /** Only these roster students are considered: in particular, not dropped students. */
  public static final List<RosterStatus> ENROLLED_STATUSES =
      List.of(RosterStatus.ROSTER, RosterStatus.MANUAL);

  Course course;
  CourseRepository courseRepository;
  RosterStudentRepository rosterStudentRepository;
  CourseStaffRepository courseStaffRepository;
  SlackService slackService;
  CanvasApiTokenSecurityService tokenSecurityService;

  @Override
  public String getScopeType() {
    return "course";
  }

  @Override
  public Long getScopeId() {
    return course.getId();
  }

  /**
   * Turns part of a name into the form Slack requires of channel names: accents are dropped, the
   * rest is lowercased, and each run of characters other than letters, numbers and underscores is
   * replaced by one hyphen (none at the start or the end).
   *
   * @param namePart for example a last name; may be null
   * @return the sanitized name part; empty if nothing is left
   */
  public static String sanitize(String namePart) {
    if (namePart == null) {
      return "";
    }
    return Normalizer.normalize(namePart, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9_]+", "-")
        .replaceAll("^-|-$", "");
  }

  /**
   * @param firstName a first name, possibly followed by middle names; may be null
   * @return the first name up to the first space
   */
  public static String firstWord(String firstName) {
    if (firstName == null) {
      return "";
    }
    return firstName.strip().split("\\s+")[0];
  }

  /**
   * @param firstName the first name; only the part up to the first space is used
   * @param lastName the last name
   * @param suffix added at the end, to tell apart students with the same name; may be empty
   * @return the name of the student's private Slack channel, for example private-chris-gaucho
   */
  public static String channelNameFor(String firstName, String lastName, String suffix) {
    String name =
        String.join(
            "-",
            Stream.of(CHANNEL_PREFIX, sanitize(firstWord(firstName)), sanitize(lastName), suffix)
                .filter(part -> !part.isEmpty())
                .toList());
    return name.substring(0, Math.min(name.length(), MAX_CHANNEL_NAME_LENGTH));
  }

  /** A student who has an active Slack account, and so gets a channel. */
  private static class StudentChannel {
    RosterStudent student;
    String firstName;
    String lastName;
    String channelName;
    // the channel the student already has, if any, and its members
    SlackChannel existing;
    List<String> members = List.of();
  }

  /** The counts reported at the end of the log. */
  private static class Summary {
    int channelsCreated;
    int channelsExisting;
    int channelsRenamed;
    int membersAdded;
    int membersAlreadyPresent;
    int membersRemoved;
    // set once Slack has refused a removal because of a workspace setting
    boolean removalsRestricted;
  }

  @Override
  public void accept(JobContext ctx) throws Exception {
    Course currentCourse = courseRepository.findById(course.getId()).orElseThrow();
    String token = tokenSecurityService.decrypt(currentCourse.getSlackBotToken());
    if (token == null || token.isEmpty()) {
      throw new IllegalStateException(
          "No Slack token has been set for this course; enter one on the Settings tab.");
    }

    Map<String, SlackUser> slackUsersById = new HashMap<>();
    Map<String, SlackUser> activeSlackUserByEmail = new HashMap<>();
    for (SlackUser user : slackService.listUsers(token)) {
      slackUsersById.put(user.getId(), user);
      if (user.isActivePerson() && user.email() != null) {
        activeSlackUserByEmail.put(canonical(user.email()), user);
      }
    }
    if (activeSlackUserByEmail.isEmpty()) {
      // Without emails nobody can be matched to a Slack user.
      throw new IllegalStateException(
          "Slack did not provide the email of any user. Add the users:read.email scope to the Slack app, reinstall it to the workspace, and save the new token on the Settings tab.");
    }

    // The instructor and the staff, who belong in every channel: Slack id, and a label for the log
    Map<String, String> staff = new LinkedHashMap<>();
    int staffNotInSlack = 0;
    if (currentCourse.getInstructorEmail() != null) {
      SlackUser instructor =
          activeSlackUserByEmail.get(canonical(currentCourse.getInstructorEmail()));
      if (instructor == null) {
        staffNotInSlack++;
      } else {
        staff.put(instructor.getId(), "instructor " + currentCourse.getInstructorEmail());
      }
    }
    for (CourseStaff staffMember : courseStaffRepository.findByCourseId(currentCourse.getId())) {
      SlackUser slackUser =
          staffMember.getEmail() == null
              ? null
              : activeSlackUserByEmail.get(canonical(staffMember.getEmail()));
      if (slackUser == null) {
        staffNotInSlack++;
      } else {
        staff.putIfAbsent(slackUser.getId(), "staff member " + staffMember.getEmail());
      }
    }

    // The students who get a channel, by Slack id
    Map<String, StudentChannel> students = new LinkedHashMap<>();
    int studentsNotInSlack = 0;
    List<String> studentsOnStaff = new ArrayList<>();
    for (RosterStudent student :
        rosterStudentRepository
            .findByCourseIdAndRosterStatusInOrderByFirstNameAscLastNameAscIgnoreCase(
                currentCourse.getId(), ENROLLED_STATUSES)) {
      SlackUser slackUser =
          student.getEmail() == null
              ? null
              : activeSlackUserByEmail.get(canonical(student.getEmail()));
      if (slackUser == null) {
        studentsNotInSlack++;
      } else if (staff.containsKey(slackUser.getId())) {
        // They are in every channel, so no channel could be recognized as theirs
        studentsOnStaff.add(describe(student));
      } else {
        StudentChannel studentChannel = new StudentChannel();
        studentChannel.student = student;
        studentChannel.firstName = firstNonBlank(slackUser.firstName(), student.getFirstName());
        studentChannel.lastName = firstNonBlank(slackUser.lastName(), student.getLastName());
        students.putIfAbsent(slackUser.getId(), studentChannel);
      }
    }
    chooseChannelNames(students);

    // Fails, before anything is changed, if the members of a channel cannot be listed
    findExistingChannels(token, students);

    ctx.log(
        "Creating Private Channels (%d student(s), channel names start with %s-)"
            .formatted(students.size(), CHANNEL_PREFIX));
    for (String student : studentsOnStaff) {
      ctx.log(
          "%s is also the instructor or a staff member of the course, so does not get a private channel."
              .formatted(student));
    }
    Summary summary = new Summary();
    for (Map.Entry<String, StudentChannel> entry : students.entrySet()) {
      StudentChannel studentChannel = entry.getValue();
      String channelId = setUpChannel(ctx, token, studentChannel, summary);
      if (channelId == null) {
        continue;
      }

      // who belongs, with a label for the log; the student first
      Map<String, String> belonging = new LinkedHashMap<>();
      belonging.put(entry.getKey(), describe(studentChannel.student));
      belonging.putAll(staff);
      List<Map.Entry<String, String>> toAdd = new ArrayList<>();
      for (Map.Entry<String, String> person : belonging.entrySet()) {
        if (studentChannel.members.contains(person.getKey())) {
          summary.membersAlreadyPresent++;
        } else {
          toAdd.add(person);
        }
      }
      addMembers(ctx, token, studentChannel.channelName, channelId, toAdd, summary);
      removeOthers(
          ctx, token, studentChannel, channelId, belonging.keySet(), slackUsersById, summary);
    }

    if (studentsNotInSlack > 0) {
      ctx.log(
          "%d student(s) did not get a private channel, because they do not have an active account in the Slack workspace; see the Slack tab. Run this job again once they have joined."
              .formatted(studentsNotInSlack));
    }
    if (staffNotInSlack > 0) {
      ctx.log(
          "%d staff member(s) (counting the instructor) could not be added to the channels, because they do not have an active account in the Slack workspace; see the Slack tab. Run this job again once they have joined."
              .formatted(staffNotInSlack));
    }
    ctx.log(
        "Done. Channels created: %d, already existed: %d (of which renamed: %d). Members added: %d, already present: %d, removed: %d."
            .formatted(
                summary.channelsCreated,
                summary.channelsExisting,
                summary.channelsRenamed,
                summary.membersAdded,
                summary.membersAlreadyPresent,
                summary.membersRemoved));
  }

  /**
   * Works out the name of each student's channel. Students who would get the same name are told
   * apart by the part of their email before the @.
   */
  private static void chooseChannelNames(Map<String, StudentChannel> students) {
    Map<String, Integer> timesUsed = new HashMap<>();
    for (StudentChannel studentChannel : students.values()) {
      studentChannel.channelName =
          channelNameFor(studentChannel.firstName, studentChannel.lastName, "");
      timesUsed.merge(studentChannel.channelName, 1, Integer::sum);
    }
    for (StudentChannel studentChannel : students.values()) {
      if (timesUsed.get(studentChannel.channelName) > 1) {
        studentChannel.channelName =
            channelNameFor(
                studentChannel.firstName,
                studentChannel.lastName,
                sanitize(studentChannel.student.getEmail().split("@")[0]));
      }
    }
  }

  /**
   * Finds the channel each student already has: a private channel that this bot created, in which
   * that student is the only one of the students.
   *
   * @throws IllegalStateException if the members of such a channel cannot be listed
   */
  private void findExistingChannels(String token, Map<String, StudentChannel> students) {
    SlackAuthTestResponse authTest = slackService.authTest(token);
    if (!authTest.getOk()) {
      throw new SlackApiException(String.valueOf(authTest.getError()));
    }
    for (SlackChannel channel : slackService.listPrivateChannels(token)) {
      // Only channels made by this job are considered, so that no other private channel that the
      // bot happens to have been added to is ever renamed, or opened up to the whole staff
      if (authTest.getUserId() == null || !authTest.getUserId().equals(channel.getCreator())) {
        continue;
      }
      List<String> members;
      try {
        members = slackService.listChannelMembers(token, channel.getId());
      } catch (SlackApiException e) {
        throw new IllegalStateException(
            "Could not list the members of the private channel #%s (%s), so cannot tell which student it belongs to. Nothing was changed in Slack."
                .formatted(channel.getName(), e.getMessage()));
      }
      List<String> studentMembers = members.stream().filter(students::containsKey).toList();
      if (studentMembers.size() == 1) {
        StudentChannel studentChannel = students.get(studentMembers.get(0));
        // If a student somehow has several, the one that is in use rather than archived wins
        if (studentChannel.existing == null || studentChannel.existing.getArchived()) {
          studentChannel.existing = channel;
          studentChannel.members = members;
        }
      }
    }
  }

  /**
   * Creates the student's channel, or renames the one they have if its name is not the right one.
   *
   * @return the id of the channel, or null if it is not ready to be used
   */
  private String setUpChannel(
      JobContext ctx, String token, StudentChannel studentChannel, Summary summary) {
    String channelName = studentChannel.channelName;
    String student = describe(studentChannel.student);
    SlackChannel existing = studentChannel.existing;
    if (existing == null) {
      try {
        SlackChannel created = slackService.createPrivateChannel(token, channelName);
        summary.channelsCreated++;
        ctx.log("Created private channel #%s for %s".formatted(channelName, student));
        return created.getId();
      } catch (SlackApiException e) {
        ctx.log(
            "Error creating private channel #%s for %s: %s. Skipping it."
                .formatted(channelName, student, e.getMessage()));
        if (NAME_TAKEN.equals(e.getMessage())) {
          ctx.log(NAME_TAKEN_ADVICE.formatted(channelName));
        }
        return null;
      }
    }
    if (existing.getArchived()) {
      ctx.log(
          "Private channel #%s for %s already exists, but is archived; unarchive it in Slack, then run this job again. Skipping it."
              .formatted(existing.getName(), student));
      return null;
    }
    summary.channelsExisting++;
    if (channelName.equals(existing.getName())) {
      ctx.log("Private channel #%s for %s already exists".formatted(channelName, student));
      return existing.getId();
    }
    try {
      slackService.renameChannel(token, existing.getId(), channelName);
      summary.channelsRenamed++;
      ctx.log(
          "Renamed private channel #%s to #%s for %s"
              .formatted(existing.getName(), channelName, student));
    } catch (SlackApiException e) {
      ctx.log(
          "Error renaming private channel #%s to #%s for %s: %s. It keeps its name."
              .formatted(existing.getName(), channelName, student, e.getMessage()));
      studentChannel.channelName = existing.getName();
    }
    return existing.getId();
  }

  /**
   * Adds the people with one call to Slack. Slack adds all or none, so if that fails, they are
   * added one at a time, so that one problem person does not keep the others out.
   *
   * @param toAdd Slack id and description of each person to add
   */
  private void addMembers(
      JobContext ctx,
      String token,
      String channelName,
      String channelId,
      List<Map.Entry<String, String>> toAdd,
      Summary summary) {
    if (toAdd.isEmpty()) {
      return;
    }
    List<String> slackIds = toAdd.stream().map(Map.Entry::getKey).toList();
    try {
      slackService.inviteToChannel(token, channelId, slackIds);
      for (Map.Entry<String, String> person : toAdd) {
        summary.membersAdded++;
        ctx.log("Added %s to #%s".formatted(person.getValue(), channelName));
      }
      return;
    } catch (SlackApiException e) {
      ctx.log(
          "Could not add %d member(s) to #%s in one step (%s); adding them one at a time."
              .formatted(toAdd.size(), channelName, e.getMessage()));
    }
    for (Map.Entry<String, String> person : toAdd) {
      try {
        slackService.inviteToChannel(token, channelId, List.of(person.getKey()));
        summary.membersAdded++;
        ctx.log("Added %s to #%s".formatted(person.getValue(), channelName));
      } catch (SlackApiException e) {
        ctx.log(
            "Error adding %s to #%s: %s".formatted(person.getValue(), channelName, e.getMessage()));
      }
    }
  }

  /**
   * Removes from the channel everybody who does not belong in it, such as somebody who is no longer
   * on the staff of the course. Bots (including this one) and members who are not users of the
   * workspace are left alone.
   *
   * @param belonging Slack ids of the student, the instructor and the staff
   */
  private void removeOthers(
      JobContext ctx,
      String token,
      StudentChannel studentChannel,
      String channelId,
      Set<String> belonging,
      Map<String, SlackUser> slackUsersById,
      Summary summary) {
    String channelName = studentChannel.channelName;
    for (String memberId : studentChannel.members) {
      SlackUser member = slackUsersById.get(memberId);
      if (summary.removalsRestricted
          || belonging.contains(memberId)
          || member == null
          || !member.isPerson()) {
        continue;
      }
      try {
        slackService.removeFromChannel(token, channelId, memberId);
        summary.membersRemoved++;
        ctx.log("Removed %s from #%s".formatted(describe(member), channelName));
      } catch (SlackApiException e) {
        ctx.log(
            "Error removing %s from #%s: %s"
                .formatted(describe(member), channelName, e.getMessage()));
        if (SlackService.RESTRICTED_ACTION.equals(e.getMessage())) {
          // A workspace setting forbids it, so every other removal would fail the same way
          ctx.log(REMOVAL_RESTRICTED_ADVICE);
          summary.removalsRestricted = true;
        }
      }
    }
  }

  private static String firstNonBlank(String preferred, String fallback) {
    return preferred == null || preferred.isBlank() ? fallback : preferred;
  }

  private static String describe(RosterStudent student) {
    return "%s %s (%s)"
        .formatted(student.getFirstName(), student.getLastName(), student.getEmail());
  }

  private static String describe(SlackUser user) {
    return "%s (%s)".formatted(user.getRealName(), user.email());
  }

  private static String canonical(String email) {
    return CanonicalFormConverter.convertToValidEmail(email.strip());
  }
}
