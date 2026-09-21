package edu.ucsb.cs156.frontiers.controllers;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.CourseOption;
import edu.ucsb.cs156.frontiers.entities.CourseStaff;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.enums.CourseOptions;
import edu.ucsb.cs156.frontiers.enums.RosterStatus;
import edu.ucsb.cs156.frontiers.errors.EntityNotFoundException;
import edu.ucsb.cs156.frontiers.errors.SlackApiException;
import edu.ucsb.cs156.frontiers.jobs.SetupSectionSlackChannelsJob;
import edu.ucsb.cs156.frontiers.models.SlackAuthTestResponse;
import edu.ucsb.cs156.frontiers.models.SlackUser;
import edu.ucsb.cs156.frontiers.repositories.CourseOptionRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseStaffRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.repositories.SectionRepository;
import edu.ucsb.cs156.frontiers.services.CanvasApiTokenSecurityService;
import edu.ucsb.cs156.frontiers.services.SlackService;
import edu.ucsb.cs156.frontiers.utilities.CanonicalFormConverter;
import edu.ucsb.cs156.jobs.entities.Job;
import edu.ucsb.cs156.jobs.services.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints for the Slack integration of a course.
 *
 * <p>The Slack bot token is a secret: it is encrypted at rest (using the same infrastructure as
 * Canvas API tokens), never logged, and only ever returned to the frontend in masked form.
 */
@RestController
@RequestMapping("/api/courses/slack")
@Tag(name = "Slack")
public class SlackController extends ApiController {

  private static final int VISIBLE_TOKEN_CHARS = 4;

  /** Error code used when a valid token could not be encrypted, and so was not stored. */
  public static final String ENCRYPTION_FAILED = "encryption_failed";

  /** Where a workspace can be opened when its own URL is not known. */
  public static final String SLACK_CLIENT_URL = "https://app.slack.com/client/";

  private final CourseRepository courseRepository;
  private final RosterStudentRepository rosterStudentRepository;
  private final CourseStaffRepository courseStaffRepository;
  private final SectionRepository sectionRepository;
  private final CourseOptionRepository courseOptionRepository;
  private final SlackService slackService;
  private final CanvasApiTokenSecurityService tokenSecurityService;
  private final JobService jobService;

  public SlackController(
      CourseRepository courseRepository,
      RosterStudentRepository rosterStudentRepository,
      CourseStaffRepository courseStaffRepository,
      SectionRepository sectionRepository,
      CourseOptionRepository courseOptionRepository,
      SlackService slackService,
      CanvasApiTokenSecurityService tokenSecurityService,
      JobService jobService) {
    this.courseRepository = courseRepository;
    this.rosterStudentRepository = rosterStudentRepository;
    this.courseStaffRepository = courseStaffRepository;
    this.sectionRepository = sectionRepository;
    this.courseOptionRepository = courseOptionRepository;
    this.slackService = slackService;
    this.tokenSecurityService = tokenSecurityService;
    this.jobService = jobService;
  }

  /** A person with an active account in the Slack workspace, and their role in the course. */
  public static record SlackUserView(
      String slackUserId,
      String name,
      String realName,
      String displayName,
      String email,
      String courseRole) {}

  /** A roster student or staff member who does not have an active account in the workspace. */
  public static record SlackMissingMemberView(
      String courseRole, String firstName, String lastName, String email, String slackStatus) {}

  public static final String ROLE_INSTRUCTOR = "INSTRUCTOR";
  public static final String ROLE_STAFF = "STAFF";
  public static final String ROLE_STUDENT = "STUDENT";
  public static final String ROLE_NONE = "NONE";

  /** Invited to the workspace, but has not yet signed in. */
  public static final String STATUS_INVITED = "INVITED";

  /** Has an account in the workspace, but it has been deactivated. */
  public static final String STATUS_DEACTIVATED = "DEACTIVATED";

  /** Slack has no record of this email address in the workspace. */
  public static final String STATUS_NOT_IN_SLACK = "NOT_IN_SLACK";

  private static final String STATUS_ACTIVE = "ACTIVE";

  /**
   * Returns the Slack workspace (team) associated with the course, along with a masked version of
   * the Slack bot token.
   *
   * @param courseId the id of the course
   * @return a map with courseId, slackBotToken (masked), slackTeamId, slackTeamName and
   *     slackTeamUrl; values are empty strings when no token has been stored
   */
  @Operation(summary = "Get Slack workspace info and Slack bot token (masked) for a course")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @GetMapping("/info")
  public Map<String, String> getSlackInfo(
      @Parameter(name = "courseId") @RequestParam Long courseId) {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));
    return slackInfo(course);
  }

  /**
   * Validates a Slack bot token by calling the Slack <code>auth.test</code> method. If the token is
   * valid, it is encrypted and stored, along with the id and name of the workspace (team) it
   * belongs to. If it is invalid, nothing is stored, and a 400 response describes the problem.
   *
   * <p>The token should be sent in the (form encoded) body of the request rather than in the query
   * string, so that it does not end up in access logs.
   *
   * @param courseId the id of the course
   * @param slackBotToken the Slack bot token (<code>xoxb-...</code>)
   * @return on success, a map with ok=true, courseId, slackBotToken (masked), slackTeamId and
   *     slackTeamName; on failure, a map with ok=false, error (Slack error code) and message. If
   *     the token is valid but cannot be encrypted (e.g. the encryption key is not configured), it
   *     is not stored, and a 500 response with error {@link #ENCRYPTION_FAILED} is returned.
   */
  @Operation(summary = "Validate (via Slack auth.test) and store the Slack bot token for a course")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @PostMapping("/token")
  public ResponseEntity<Map<String, Object>> updateSlackToken(
      @Parameter(name = "courseId") @RequestParam Long courseId,
      @Parameter(name = "slackBotToken") @RequestParam String slackBotToken) {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));

    String token = slackBotToken.strip();
    SlackAuthTestResponse authTest = slackService.authTest(token);

    if (!authTest.getOk()) {
      Map<String, Object> body = new LinkedHashMap<>();
      body.put("ok", false);
      body.put("error", String.valueOf(authTest.getError()));
      body.put("message", errorMessage(authTest.getError()));
      return ResponseEntity.badRequest().body(body);
    }

    String encryptedToken;
    try {
      encryptedToken = tokenSecurityService.encrypt(token);
    } catch (IllegalStateException e) {
      // Typically a missing or malformed encryption key. The exception message describes the key
      // problem and never contains the token.
      Map<String, Object> body = new LinkedHashMap<>();
      body.put("ok", false);
      body.put("error", ENCRYPTION_FAILED);
      body.put("message", encryptionFailedMessage(e.getMessage()));
      return ResponseEntity.internalServerError().body(body);
    }

    course.setSlackBotToken(encryptedToken);
    course.setSlackTeamId(authTest.getTeamId());
    course.setSlackTeamName(authTest.getTeam());
    course.setSlackTeamUrl(authTest.getUrl());
    Course savedCourse = courseRepository.save(course);

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("ok", true);
    body.putAll(slackInfo(savedCourse));
    return ResponseEntity.ok(body);
  }

  private Map<String, String> slackInfo(Course course) {
    Map<String, String> info = new LinkedHashMap<>();
    info.put("courseId", course.getId().toString());
    info.put("slackBotToken", maskToken(tokenSecurityService.decrypt(course.getSlackBotToken())));
    info.put("slackTeamId", course.getSlackTeamId() != null ? course.getSlackTeamId() : "");
    info.put("slackTeamName", course.getSlackTeamName() != null ? course.getSlackTeamName() : "");
    info.put("slackTeamUrl", slackTeamUrl(course));
    return info;
  }

  /**
   * The URL of the course's Slack workspace, e.g. <code>https://ucsb-cs156-f26.slack.com/</code>.
   * Tokens stored before the URL was being recorded fall back to a URL based on the team id.
   *
   * @param course the course
   * @return the URL, or an empty string if the course is not connected to a workspace
   */
  public static String slackTeamUrl(Course course) {
    if (course.getSlackTeamUrl() != null && !course.getSlackTeamUrl().isEmpty()) {
      return course.getSlackTeamUrl();
    }
    if (course.getSlackTeamId() != null && !course.getSlackTeamId().isEmpty()) {
      return SLACK_CLIENT_URL + course.getSlackTeamId();
    }
    return "";
  }

  /**
   * Lists the people with active accounts in the course's Slack workspace (no bots, no deactivated
   * accounts, no invitations that have not been accepted yet). Each is matched by email against the
   * course to determine whether they are the instructor, a staff member, a roster student (only
   * those with status ROSTER or MANUAL; not dropped students), or none of these.
   *
   * @param courseId the id of the course
   * @return the active users of the workspace
   */
  @Operation(summary = "List active users of the course's Slack workspace, with their course role")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @GetMapping("/users")
  public List<SlackUserView> getSlackUsers(
      @Parameter(name = "courseId") @RequestParam Long courseId) {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));
    List<SlackUser> slackUsers = listSlackUsers(course);

    Map<String, String> roleByEmail = new HashMap<>();
    for (RosterStudent student : enrolledStudents(courseId)) {
      if (student.getEmail() != null) {
        roleByEmail.put(canonical(student.getEmail()), ROLE_STUDENT);
      }
    }
    for (CourseStaff staff : courseStaffRepository.findByCourseId(courseId)) {
      if (staff.getEmail() != null) {
        roleByEmail.put(canonical(staff.getEmail()), ROLE_STAFF);
      }
    }
    if (course.getInstructorEmail() != null) {
      roleByEmail.put(canonical(course.getInstructorEmail()), ROLE_INSTRUCTOR);
    }

    List<SlackUserView> result = new ArrayList<>();
    for (SlackUser user : slackUsers) {
      if (user.isActivePerson()) {
        String role =
            user.email() == null
                ? ROLE_NONE
                : roleByEmail.getOrDefault(canonical(user.email()), ROLE_NONE);
        result.add(
            new SlackUserView(
                user.getId(),
                user.getName(),
                user.getRealName(),
                user.displayName(),
                user.email(),
                role));
      }
    }
    return result;
  }

  /**
   * Lists the roster students (only those with status ROSTER or MANUAL; not dropped students) and
   * the staff of the course that do not have an active account in the course's Slack workspace,
   * matching by email. For each, slackStatus indicates whether they have been invited but have not
   * signed in yet, have a deactivated account, or are unknown to the workspace.
   *
   * @param courseId the id of the course
   * @return staff first, then students
   */
  @Operation(summary = "List roster students and staff that are not active in the Slack workspace")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @GetMapping("/missing")
  public List<SlackMissingMemberView> getMissingMembers(
      @Parameter(name = "courseId") @RequestParam Long courseId) {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));
    List<SlackUser> slackUsers = listSlackUsers(course);

    // An active account wins over an invitation, which wins over a deactivated account
    Map<String, String> statusByEmail = new HashMap<>();
    for (String status : List.of(STATUS_DEACTIVATED, STATUS_INVITED, STATUS_ACTIVE)) {
      for (SlackUser user : slackUsers) {
        if (user.isPerson() && user.email() != null && status.equals(slackStatus(user))) {
          statusByEmail.put(canonical(user.email()), status);
        }
      }
    }

    List<SlackMissingMemberView> result = new ArrayList<>();
    for (CourseStaff staff : courseStaffRepository.findByCourseId(courseId)) {
      String status = missingStatus(statusByEmail, staff.getEmail());
      if (status != null) {
        result.add(
            new SlackMissingMemberView(
                ROLE_STAFF, staff.getFirstName(), staff.getLastName(), staff.getEmail(), status));
      }
    }
    for (RosterStudent student : enrolledStudents(courseId)) {
      String status = missingStatus(statusByEmail, student.getEmail());
      if (status != null) {
        result.add(
            new SlackMissingMemberView(
                ROLE_STUDENT,
                student.getFirstName(),
                student.getLastName(),
                student.getEmail(),
                status));
      }
    }
    return result;
  }

  /**
   * Launches a job that creates the public Slack channels named in the sections table of the
   * course, adds the roster students of each section to its channel, and removes from those
   * channels anybody who is neither a student of the section, nor staff, nor the instructor. See
   * {@link SetupSectionSlackChannelsJob}.
   *
   * @param courseId the id of the course
   * @return the job that was launched; its log can be seen on the Jobs tab of the course
   */
  @Operation(summary = "Launch job that sets up the Slack channels of the sections of a course")
  @PreAuthorize("@CourseSecurity.hasInstructorPermissions(#root, #courseId)")
  @PostMapping("/sectionChannels")
  public Job setupSectionChannels(@Parameter(name = "courseId") @RequestParam Long courseId) {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));
    for (CourseOptions option :
        List.of(CourseOptions.SLACK_INTEGRATION, CourseOptions.TRANSLATE_SECTIONS)) {
      boolean enabled =
          courseOptionRepository
              .findByCourseIdAndOption(courseId, option.name())
              .map(CourseOption::getEnabled)
              .orElse(false);
      if (!enabled) {
        throw new IllegalArgumentException(
            "The course option %s must be enabled to set up section Slack channels."
                .formatted(option.name()));
      }
    }
    String token = tokenSecurityService.decrypt(course.getSlackBotToken());
    if (token == null || token.isEmpty()) {
      throw new IllegalArgumentException(NO_TOKEN_MESSAGE);
    }

    SetupSectionSlackChannelsJob job =
        SetupSectionSlackChannelsJob.builder()
            .course(course)
            .courseRepository(courseRepository)
            .sectionRepository(sectionRepository)
            .rosterStudentRepository(rosterStudentRepository)
            .courseStaffRepository(courseStaffRepository)
            .slackService(slackService)
            .tokenSecurityService(tokenSecurityService)
            .build();
    return jobService.runAsJob(job);
  }

  public static final String NO_TOKEN_MESSAGE =
      "No Slack token has been set for this course; enter one on the Settings tab.";

  /** Only these roster students are considered: in particular, not dropped students. */
  public static final List<RosterStatus> ENROLLED_STATUSES =
      List.of(RosterStatus.ROSTER, RosterStatus.MANUAL);

  private List<RosterStudent> enrolledStudents(Long courseId) {
    return rosterStudentRepository
        .findByCourseIdAndRosterStatusInOrderByFirstNameAscLastNameAscIgnoreCase(
            courseId, ENROLLED_STATUSES);
  }

  private static String slackStatus(SlackUser user) {
    if (user.isActivePerson()) {
      return STATUS_ACTIVE;
    }
    return user.getDeleted() ? STATUS_DEACTIVATED : STATUS_INVITED;
  }

  /**
   * @return null if the email belongs to an active Slack user; otherwise the status to report
   */
  private static String missingStatus(Map<String, String> statusByEmail, String email) {
    if (email == null) {
      return STATUS_NOT_IN_SLACK;
    }
    String status = statusByEmail.getOrDefault(canonical(email), STATUS_NOT_IN_SLACK);
    return status.equals(STATUS_ACTIVE) ? null : status;
  }

  private static String canonical(String email) {
    return CanonicalFormConverter.convertToValidEmail(email.strip());
  }

  private List<SlackUser> listSlackUsers(Course course) {
    String token = tokenSecurityService.decrypt(course.getSlackBotToken());
    if (token == null || token.isEmpty()) {
      throw new IllegalArgumentException(NO_TOKEN_MESSAGE);
    }
    return slackService.listUsers(token);
  }

  /**
   * A failed call to Slack maps to a 502/Bad Gateway, with the Slack error code and a message.
   *
   * @param e the exception; its message is the Slack error code
   * @return a map with ok=false, error (Slack error code) and message
   */
  @ExceptionHandler({SlackApiException.class})
  public ResponseEntity<Map<String, Object>> handleSlackApiException(SlackApiException e) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("ok", false);
    body.put("error", e.getMessage());
    body.put("message", describeError(e.getMessage()));
    return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
  }

  /**
   * Masks a token so that only the first four and last four characters are visible. Tokens too
   * short to mask meaningfully (eight characters or fewer) are masked entirely; Slack does not
   * issue tokens that short.
   *
   * @param token the plaintext token
   * @return the masked token, or an empty string if there is no token
   */
  public static String maskToken(String token) {
    if (token == null || token.isEmpty()) {
      return "";
    }
    if (token.length() <= 2 * VISIBLE_TOKEN_CHARS) {
      return "*".repeat(token.length());
    }
    return token.substring(0, VISIBLE_TOKEN_CHARS)
        + "*".repeat(token.length() - 2 * VISIBLE_TOKEN_CHARS)
        + token.substring(token.length() - VISIBLE_TOKEN_CHARS);
  }

  /**
   * Message shown when a valid token could not be encrypted.
   *
   * @param details message from the encryption service, describing the problem with the key
   * @return a human readable message
   */
  public static String encryptionFailedMessage(String details) {
    return "Slack accepted this token, but the server could not encrypt it, so the token was not saved. Ask an administrator to check the TOKEN_ENCRYPTION_KEY environment variable (see docs/slack.md). Details: %s"
        .formatted(details);
  }

  /**
   * Translates a Slack error code into a message suitable for showing to an instructor who has just
   * tried to save a token.
   *
   * @param error error code from Slack, e.g. <code>invalid_auth</code>
   * @return a human readable message
   */
  public static String errorMessage(String error) {
    return describeError(error) + " The token was not saved.";
  }

  /**
   * Translates a Slack error code into a message suitable for showing to an instructor.
   *
   * @param error error code from Slack, e.g. <code>invalid_auth</code>
   * @return a human readable message
   */
  public static String describeError(String error) {
    return switch (String.valueOf(error)) {
      case "invalid_auth", "not_authed" ->
          "Slack rejected this token as invalid. Copy the Bot User OAuth Token (xoxb-...) from your Slack app's OAuth & Permissions page and try again.";
      case "token_revoked", "token_expired", "account_inactive" ->
          "This Slack token has been revoked or is no longer active. Reinstall the Slack app to the workspace to get a new token.";
      case "missing_scope" ->
          "This Slack token is missing a required scope. Add the scope under OAuth & Permissions, reinstall the Slack app to the workspace, and enter the new token.";
      case "ratelimited" ->
          "Slack is limiting requests from this app. Please try again in a minute.";
      case SlackService.SLACK_UNREACHABLE -> "Could not reach Slack. Please try again later.";
      default -> "Slack reported an error (%s).".formatted(error);
    };
  }
}
