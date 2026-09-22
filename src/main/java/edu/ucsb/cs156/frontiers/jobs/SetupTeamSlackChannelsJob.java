package edu.ucsb.cs156.frontiers.jobs;

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
import edu.ucsb.cs156.frontiers.utilities.CanonicalFormConverter;
import edu.ucsb.cs156.jobs.services.JobContext;
import edu.ucsb.cs156.jobs.services.JobContextConsumer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.Builder;

/**
 * Makes the public Slack channels of a course's teams match the teams.
 *
 * <ol>
 *   <li>For each team, works out a channel name: "team-" followed by the team name in the form
 *       Slack requires (see {@link #sanitizeChannelName}). If two teams end up with the same
 *       channel name, the job stops with an error naming them, before touching Slack.
 *   <li>Creates each channel, unless it already exists.
 *   <li>Adds each member of each team, and the instructor of the course, to the team's channel,
 *       unless they are already in it. People are matched to Slack users by email; those without an
 *       active Slack account cannot be added, and are only counted.
 *   <li>Removes from each channel every person who is neither a member of the team, nor the
 *       instructor, nor a member of the course staff. Bots (including the bot this job acts as) and
 *       members that are not users of the workspace are never removed.
 * </ol>
 *
 * A problem with one channel or one person is logged, and the job carries on with the rest. The
 * last line of the log is a summary of how many channels were created or already existed, and how
 * many members were added, already present, or removed.
 */
@Builder
public class SetupTeamSlackChannelsJob implements JobContextConsumer {

  public static final String CHANNEL_PREFIX = "team-";

  Course course;
  CourseRepository courseRepository;
  TeamRepository teamRepository;
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
   * Turns a team name into the form Slack requires of channel names: all lowercase, spaces replaced
   * by hyphens, and every character other than letters, numbers, hyphens and underscores replaced
   * by a hyphen.
   *
   * @param teamName the team name; may be null
   * @return the sanitized name (without the "team-" prefix)
   */
  public static String sanitizeChannelName(String teamName) {
    if (teamName == null) {
      return "";
    }
    return teamName.toLowerCase(Locale.ROOT).replace(' ', '-').replaceAll("[^a-z0-9_-]", "-");
  }

  /**
   * @param teamName the team name
   * @return the name of the team's Slack channel
   */
  public static String channelNameFor(String teamName) {
    return CHANNEL_PREFIX + sanitizeChannelName(teamName);
  }

  @Override
  public void accept(JobContext ctx) throws Exception {
    Course currentCourse = courseRepository.findById(course.getId()).orElseThrow();
    String token = tokenSecurityService.decrypt(currentCourse.getSlackBotToken());
    if (token == null || token.isEmpty()) {
      throw new IllegalStateException(
          "No Slack token has been set for this course; enter one on the Settings tab.");
    }

    // Work out the channel names first, and stop if any two teams collide, before calling Slack
    Map<String, Team> teamByChannelName = new LinkedHashMap<>();
    Map<String, List<String>> teamNamesByChannelName = new LinkedHashMap<>();
    for (Team team : teamRepository.findByCourseIdOrderByNameAsc(currentCourse.getId())) {
      String channelName = channelNameFor(team.getName());
      teamByChannelName.putIfAbsent(channelName, team);
      teamNamesByChannelName
          .computeIfAbsent(channelName, name -> new ArrayList<>())
          .add(team.getName());
    }
    List<String> collisions = new ArrayList<>();
    for (Map.Entry<String, List<String>> entry : teamNamesByChannelName.entrySet()) {
      if (entry.getValue().size() > 1) {
        collisions.add(
            "#%s (teams %s)".formatted(entry.getKey(), String.join(", ", entry.getValue())));
      }
    }
    if (!collisions.isEmpty()) {
      throw new IllegalStateException(
          "Two or more teams would get the same Slack channel name: %s. Rename the teams so that their channel names differ, then run this job again. Nothing was changed in Slack."
              .formatted(String.join("; ", collisions)));
    }
    ctx.log(
        "Creating Team Channels (%d team(s), channel names start with %s)"
            .formatted(teamByChannelName.size(), CHANNEL_PREFIX));

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

    Summary summary = new Summary();
    Map<String, String> channelIdByName = createChannels(ctx, token, teamByChannelName, summary);

    String instructorSlackId =
        currentCourse.getInstructorEmail() == null
            ? null
            : activeSlackIdByEmail.get(canonical(currentCourse.getInstructorEmail()));
    if (currentCourse.getInstructorEmail() != null && instructorSlackId == null) {
      ctx.log(
          "The instructor (%s) does not have an active account in the Slack workspace, so cannot be added to the channels."
              .formatted(currentCourse.getInstructorEmail()));
    }
    Set<String> staffSlackIds = new HashSet<>();
    for (CourseStaff staff : courseStaffRepository.findByCourseId(currentCourse.getId())) {
      if (staff.getEmail() != null) {
        staffSlackIds.add(activeSlackIdByEmail.get(canonical(staff.getEmail())));
      }
    }

    ctx.log("Adding Team Members to Channels");
    Map<String, List<String>> membersByChannelName = new HashMap<>();
    Map<String, Set<String>> belongingIdsByChannelName = new HashMap<>();
    int membersNotInSlack = 0;
    for (Map.Entry<String, String> channel : channelIdByName.entrySet()) {
      String channelName = channel.getKey();
      String channelId = channel.getValue();
      Team team = teamByChannelName.get(channelName);
      List<String> members;
      try {
        members = slackService.listChannelMembers(token, channelId);
      } catch (SlackApiException e) {
        ctx.log("Error listing members of #%s: %s".formatted(channelName, e.getMessage()));
        continue;
      }
      membersByChannelName.put(channelName, members);

      // who belongs, with a label for the log; the instructor first
      Map<String, String> belonging = new LinkedHashMap<>();
      if (instructorSlackId != null) {
        belonging.put(instructorSlackId, "instructor " + currentCourse.getInstructorEmail());
      }
      for (TeamMember member :
          team.getTeamMembers() == null ? List.<TeamMember>of() : team.getTeamMembers()) {
        RosterStudent student = member.getRosterStudent();
        String slackId =
            student == null || student.getEmail() == null
                ? null
                : activeSlackIdByEmail.get(canonical(student.getEmail()));
        if (slackId == null) {
          membersNotInSlack++;
        } else {
          belonging.putIfAbsent(slackId, describe(student));
        }
      }
      belongingIdsByChannelName.put(channelName, belonging.keySet());

      List<Map.Entry<String, String>> toAdd = new ArrayList<>();
      for (Map.Entry<String, String> person : belonging.entrySet()) {
        if (members.contains(person.getKey())) {
          summary.membersAlreadyPresent++;
        } else {
          toAdd.add(person);
        }
      }
      addMembers(ctx, token, channelName, channelId, toAdd, summary);
    }
    if (membersNotInSlack > 0) {
      ctx.log(
          "%d team member(s) could not be added, because they do not have an active account in the Slack workspace; see the Slack tab. Run this job again once they have joined."
              .formatted(membersNotInSlack));
    }

    ctx.log("Removing Channel Members Who Are Not On The Team");
    channels:
    for (Map.Entry<String, String> channel : channelIdByName.entrySet()) {
      String channelName = channel.getKey();
      List<String> members = membersByChannelName.get(channelName);
      if (members == null) {
        continue;
      }
      for (String memberId : members) {
        SlackUser member = slackUsersById.get(memberId);
        boolean belongs =
            belongingIdsByChannelName.get(channelName).contains(memberId)
                || staffSlackIds.contains(memberId);
        // Bots (including this one) and members who are not users of the workspace are left alone
        if (!belongs && member != null && member.isPerson()) {
          try {
            slackService.removeFromChannel(token, channel.getValue(), memberId);
            summary.membersRemoved++;
            ctx.log("Removed %s from #%s".formatted(describe(member), channelName));
          } catch (SlackApiException e) {
            ctx.log(
                "Error removing %s from #%s: %s"
                    .formatted(describe(member), channelName, e.getMessage()));
            if (SlackService.RESTRICTED_ACTION.equals(e.getMessage())) {
              // A workspace setting forbids it, so every other removal would fail the same way
              ctx.log(SlackService.REMOVAL_RESTRICTED_ADVICE);
              break channels;
            }
          }
        }
      }
    }
    ctx.log(
        "Done. Channels created: %d, already existed: %d. Members added: %d, already present: %d, removed: %d."
            .formatted(
                summary.channelsCreated,
                summary.channelsExisting,
                summary.membersAdded,
                summary.membersAlreadyPresent,
                summary.membersRemoved));
  }

  /** The counts reported at the end of the log. */
  private static class Summary {
    int channelsCreated;
    int channelsExisting;
    int membersAdded;
    int membersAlreadyPresent;
    int membersRemoved;
  }

  /**
   * Creates the channels that do not exist yet, and makes the bot a member of the ones that do.
   *
   * @return the id of each channel that is ready to be used, by name
   */
  private Map<String, String> createChannels(
      JobContext ctx, String token, Map<String, Team> teamByChannelName, Summary summary) {
    Map<String, SlackChannel> existingByName = new HashMap<>();
    for (SlackChannel existing : slackService.listPublicChannels(token)) {
      existingByName.put(existing.getName(), existing);
    }

    Map<String, String> channelIdByName = new LinkedHashMap<>();
    for (Map.Entry<String, Team> entry : teamByChannelName.entrySet()) {
      String channelName = entry.getKey();
      String teamName = entry.getValue().getName();
      SlackChannel existing = existingByName.get(channelName);
      try {
        if (existing == null) {
          SlackChannel created = slackService.createPublicChannel(token, channelName);
          channelIdByName.put(channelName, created.getId());
          summary.channelsCreated++;
          ctx.log("Created channel #%s for team %s".formatted(channelName, teamName));
        } else if (existing.getArchived()) {
          ctx.log(
              "Channel #%s for team %s already exists, but is archived; unarchive it in Slack, then run this job again. Skipping it."
                  .formatted(channelName, teamName));
        } else {
          slackService.joinChannel(token, existing.getId());
          channelIdByName.put(channelName, existing.getId());
          summary.channelsExisting++;
          ctx.log("Channel #%s for team %s already exists".formatted(channelName, teamName));
        }
      } catch (SlackApiException e) {
        ctx.log(
            "Error setting up channel #%s for team %s: %s. Skipping it."
                .formatted(channelName, teamName, e.getMessage()));
      }
    }
    return channelIdByName;
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
