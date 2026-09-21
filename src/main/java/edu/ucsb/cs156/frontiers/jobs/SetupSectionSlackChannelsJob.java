package edu.ucsb.cs156.frontiers.jobs;

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
import edu.ucsb.cs156.frontiers.utilities.CanonicalFormConverter;
import edu.ucsb.cs156.jobs.services.JobContext;
import edu.ucsb.cs156.jobs.services.JobContextConsumer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.Builder;

/**
 * Makes the public Slack channels of a course's sections match the roster.
 *
 * <ol>
 *   <li>For each row of the sections table that has a Slack channel name, creates a public channel
 *       with that name, unless it already exists. (Several sections may share a channel.)
 *   <li>Adds each roster student (other than dropped students) to the channel of their section,
 *       unless they are already in it. Students are matched to Slack users by email; students
 *       without an active Slack account cannot be added, and are only counted.
 *   <li>Removes from each of those channels every person who is neither a roster student of one of
 *       the channel's sections, nor a member of the course staff, nor the instructor. Bots
 *       (including the bot this job acts as) and members that are not users of the workspace are
 *       never removed.
 * </ol>
 *
 * A problem with one channel or one person is logged, and the job carries on with the rest.
 */
@Builder
public class SetupSectionSlackChannelsJob implements JobContextConsumer {

  Course course;
  CourseRepository courseRepository;
  SectionRepository sectionRepository;
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

  @Override
  public void accept(JobContext ctx) throws Exception {
    Course currentCourse = courseRepository.findById(course.getId()).orElseThrow();
    String token = tokenSecurityService.decrypt(currentCourse.getSlackBotToken());
    if (token == null || token.isEmpty()) {
      throw new IllegalStateException(
          "No Slack token has been set for this course; enter one on the Settings tab.");
    }

    List<SlackUser> slackUsers = slackService.listUsers(token);
    Map<String, SlackUser> slackUsersById = new HashMap<>();
    Map<String, String> activeSlackIdByEmail = new HashMap<>();
    for (SlackUser user : slackUsers) {
      slackUsersById.put(user.getId(), user);
      if (user.isActivePerson() && user.email() != null) {
        activeSlackIdByEmail.put(canonical(user.email()), user.getId());
      }
    }
    if (activeSlackIdByEmail.isEmpty()) {
      // Without emails nobody can be matched, and everybody would be removed from the channels.
      throw new IllegalStateException(
          "Slack did not provide the email of any user. Add the users:read.email scope to the Slack app, reinstall it to the workspace, and save the new token on the Settings tab.");
    }

    ctx.log("Creating Section Channels");
    Map<String, Set<String>> sectionsByChannelName = sectionsByChannelName(currentCourse.getId());
    Map<String, List<String>> channelNamesBySection = channelNamesBySection(sectionsByChannelName);
    Map<String, String> channelIdByName =
        createChannels(ctx, token, sectionsByChannelName.keySet());

    Set<String> staffSlackIds = staffSlackIds(currentCourse, activeSlackIdByEmail);

    // Members of each channel before this job changes anything; null if they could not be listed
    Map<String, List<String>> membersByChannelName = new HashMap<>();
    // Slack ids of the students that belong in each channel
    Map<String, Set<String>> studentSlackIdsByChannelName = new HashMap<>();

    ctx.log("Adding Students to Channel");
    List<RosterStudent> students = studentsNotDropped(ctx, currentCourse.getId());
    int studentsNotInSlack = 0;
    for (RosterStudent student : students) {
      List<String> matchingChannels =
          channelNamesBySection.getOrDefault(student.getSection(), List.of());
      if (matchingChannels.isEmpty()) {
        ctx.log(
            "Student %s is in untranslated roster section %s"
                .formatted(describe(student), describeSection(student.getSection())));
        ctx.log(
            "Could not add %s to a section Slack channel because no configured channel matches untranslated roster section %s."
                .formatted(describe(student), describeSection(student.getSection())));
      } else {
        ctx.log(
            "Student %s is in untranslated roster section %s and maps to %s"
                .formatted(
                    describe(student),
                    describeSection(student.getSection()),
                    describeChannelNames(matchingChannels)));
      }
    }
    for (Map.Entry<String, String> channel : channelIdByName.entrySet()) {
      String channelName = channel.getKey();
      String channelId = channel.getValue();
      List<String> members;
      try {
        members = slackService.listChannelMembers(token, channelId);
      } catch (SlackApiException e) {
        ctx.log("Error listing members of #%s: %s".formatted(channelName, e.getMessage()));
        continue;
      }
      membersByChannelName.put(channelName, members);

      Set<String> studentSlackIds = new HashSet<>();
      List<RosterStudent> toAdd = new ArrayList<>();
      for (RosterStudent student : students) {
        if (sectionsByChannelName.get(channelName).contains(student.getSection())) {
          String slackId =
              student.getEmail() == null
                  ? null
                  : activeSlackIdByEmail.get(canonical(student.getEmail()));
          if (slackId == null) {
            studentsNotInSlack++;
          } else {
            studentSlackIds.add(slackId);
            if (!members.contains(slackId)) {
              toAdd.add(student);
            }
          }
        }
      }
      studentSlackIdsByChannelName.put(channelName, studentSlackIds);
      addStudents(ctx, token, channelName, channelId, toAdd, activeSlackIdByEmail);
    }
    if (studentsNotInSlack > 0) {
      ctx.log(
          "%d student(s) in these sections could not be added, because they do not have an active account in the Slack workspace; see the Slack tab."
              .formatted(studentsNotInSlack));
    }

    ctx.log("Removing Channel Members Who Are Not In The Section");
    for (Map.Entry<String, String> channel : channelIdByName.entrySet()) {
      String channelName = channel.getKey();
      List<String> members = membersByChannelName.get(channelName);
      if (members == null) {
        continue;
      }
      for (String memberId : members) {
        SlackUser member = slackUsersById.get(memberId);
        boolean belongs =
            staffSlackIds.contains(memberId)
                || studentSlackIdsByChannelName.get(channelName).contains(memberId);
        // Bots (including this one) and members who are not users of the workspace are left alone
        if (!belongs && member != null && member.isPerson()) {
          try {
            slackService.removeFromChannel(token, channel.getValue(), memberId);
            ctx.log("Removed %s from #%s".formatted(describe(member), channelName));
          } catch (SlackApiException e) {
            ctx.log(
                "Error removing %s from #%s: %s"
                    .formatted(describe(member), channelName, e.getMessage()));
          }
        }
      }
    }
    ctx.log("Done");
  }

  /**
   * The roster students of the course, other than dropped students: the same students that the main
   * table of the Students tab shows. In particular a student whose roster status is not set (the
   * column is nullable) counts, as on that tab; asking the database only for the statuses ROSTER
   * and MANUAL would silently leave such students out.
   *
   * <p>Logs how many students there are with each status, so that the log shows why students were,
   * or were not, considered.
   */
  private List<RosterStudent> studentsNotDropped(JobContext ctx, Long courseId) throws Exception {
    List<RosterStudent> result = new ArrayList<>();
    int total = 0;
    int withoutStatus = 0;
    Map<RosterStatus, Integer> countByStatus = new EnumMap<>(RosterStatus.class);
    for (RosterStudent student :
        rosterStudentRepository.findByCourseIdOrderByFirstNameAscLastNameAscIgnoreCase(courseId)) {
      total++;
      if (student.getRosterStatus() == null) {
        withoutStatus++;
      } else {
        countByStatus.merge(student.getRosterStatus(), 1, Integer::sum);
      }
      if (student.getRosterStatus() != RosterStatus.DROPPED) {
        result.add(student);
      }
    }
    ctx.log(
        "This course has %d roster student(s): %d ROSTER, %d MANUAL, %d DROPPED, %d with no roster status. All but the DROPPED students are considered: %d student(s)."
            .formatted(
                total,
                countByStatus.getOrDefault(RosterStatus.ROSTER, 0),
                countByStatus.getOrDefault(RosterStatus.MANUAL, 0),
                countByStatus.getOrDefault(RosterStatus.DROPPED, 0),
                withoutStatus,
                result.size()));
    return result;
  }

  /**
   * @return for each (normalized) channel name, in order of section, the sections that use it
   */
  private Map<String, Set<String>> sectionsByChannelName(Long courseId) {
    Map<String, Set<String>> result = new LinkedHashMap<>();
    for (Section section : sectionRepository.findByCourseIdOrderBySectionAsc(courseId)) {
      String channelName = normalizeChannelName(section.getSlackChannelName());
      if (!channelName.isEmpty()) {
        result.computeIfAbsent(channelName, name -> new HashSet<>()).add(section.getSection());
      }
    }
    return result;
  }

  private Map<String, List<String>> channelNamesBySection(
      Map<String, Set<String>> sectionsByChannelName) {
    Map<String, List<String>> result = new HashMap<>();
    for (Map.Entry<String, Set<String>> entry : sectionsByChannelName.entrySet()) {
      for (String section : entry.getValue()) {
        result.computeIfAbsent(section, unused -> new ArrayList<>()).add(entry.getKey());
      }
    }
    return result;
  }

  /**
   * Slack channel names are lowercase, and are written with a leading # that is not part of the
   * name.
   *
   * @param name the name as entered for the section; may be null
   * @return the name to use with the Slack API, or an empty string if there is none
   */
  public static String normalizeChannelName(String name) {
    if (name == null) {
      return "";
    }
    String stripped = name.strip();
    if (stripped.startsWith("#")) {
      stripped = stripped.substring(1).strip();
    }
    return stripped.toLowerCase(Locale.ROOT);
  }

  /**
   * Creates the channels that do not exist yet, and makes the bot a member of the ones that do.
   *
   * @return the id of each channel that is ready to be used, by name
   */
  private Map<String, String> createChannels(
      JobContext ctx, String token, Set<String> channelNames) {
    Map<String, SlackChannel> existingByName = new HashMap<>();
    for (SlackChannel existing : slackService.listPublicChannels(token)) {
      existingByName.put(existing.getName(), existing);
    }

    Map<String, String> channelIdByName = new LinkedHashMap<>();
    for (String channelName : channelNames) {
      SlackChannel existing = existingByName.get(channelName);
      try {
        if (existing == null) {
          SlackChannel created = slackService.createPublicChannel(token, channelName);
          channelIdByName.put(channelName, created.getId());
          ctx.log("Created channel #%s".formatted(channelName));
        } else if (existing.getArchived()) {
          ctx.log(
              "Channel #%s already exists, but is archived; unarchive it in Slack, then run this job again. Skipping it."
                  .formatted(channelName));
        } else {
          slackService.joinChannel(token, existing.getId());
          channelIdByName.put(channelName, existing.getId());
          ctx.log("Channel #%s already exists".formatted(channelName));
        }
      } catch (SlackApiException e) {
        ctx.log(
            "Error setting up channel #%s: %s. Skipping it."
                .formatted(channelName, e.getMessage()));
      }
    }
    return channelIdByName;
  }

  /**
   * Adds the students with one call to Slack. Slack adds all or none, so if that fails, they are
   * added one at a time, so that one problem student does not keep the others out.
   */
  private void addStudents(
      JobContext ctx,
      String token,
      String channelName,
      String channelId,
      List<RosterStudent> toAdd,
      Map<String, String> activeSlackIdByEmail) {
    if (toAdd.isEmpty()) {
      return;
    }
    List<String> slackIds = new ArrayList<>();
    for (RosterStudent student : toAdd) {
      slackIds.add(activeSlackIdByEmail.get(canonical(student.getEmail())));
    }
    try {
      slackService.inviteToChannel(token, channelId, slackIds);
      for (RosterStudent student : toAdd) {
        ctx.log("Added %s to #%s".formatted(describe(student), channelName));
      }
      return;
    } catch (SlackApiException e) {
      ctx.log(
          "Could not add %d student(s) to #%s in one step (%s); adding them one at a time."
              .formatted(toAdd.size(), channelName, e.getMessage()));
    }
    for (int i = 0; i < toAdd.size(); i++) {
      try {
        slackService.inviteToChannel(token, channelId, List.of(slackIds.get(i)));
        ctx.log("Added %s to #%s".formatted(describe(toAdd.get(i)), channelName));
      } catch (SlackApiException e) {
        ctx.log(
            "Error adding %s to #%s: %s"
                .formatted(describe(toAdd.get(i)), channelName, e.getMessage()));
      }
    }
  }

  /** Slack ids of the staff of the course and of its instructor, who belong in every channel. */
  private Set<String> staffSlackIds(Course currentCourse, Map<String, String> slackIdByEmail) {
    Set<String> result = new HashSet<>();
    for (CourseStaff staff : courseStaffRepository.findByCourseId(currentCourse.getId())) {
      if (staff.getEmail() != null) {
        // null (staff member not in Slack) is harmless in this set: no channel member has id null
        result.add(slackIdByEmail.get(canonical(staff.getEmail())));
      }
    }
    if (currentCourse.getInstructorEmail() != null) {
      result.add(slackIdByEmail.get(canonical(currentCourse.getInstructorEmail())));
    }
    return result;
  }

  private static String describe(RosterStudent student) {
    return "%s %s (%s)"
        .formatted(student.getFirstName(), student.getLastName(), student.getEmail());
  }

  private static String describe(SlackUser user) {
    return "%s (%s)".formatted(user.getRealName(), user.email());
  }

  private static String describeSection(String section) {
    return section == null ? "(blank)" : section;
  }

  private static String describeChannelNames(List<String> channelNames) {
    return "#" + String.join(", #", channelNames);
  }

  private static String canonical(String email) {
    return CanonicalFormConverter.convertToValidEmail(email.strip());
  }
}
