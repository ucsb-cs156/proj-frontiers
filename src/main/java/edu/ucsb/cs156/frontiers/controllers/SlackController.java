package edu.ucsb.cs156.frontiers.controllers;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.errors.EntityNotFoundException;
import edu.ucsb.cs156.frontiers.models.SlackAuthTestResponse;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.services.CanvasApiTokenSecurityService;
import edu.ucsb.cs156.frontiers.services.SlackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

  private final CourseRepository courseRepository;
  private final SlackService slackService;
  private final CanvasApiTokenSecurityService tokenSecurityService;

  public SlackController(
      CourseRepository courseRepository,
      SlackService slackService,
      CanvasApiTokenSecurityService tokenSecurityService) {
    this.courseRepository = courseRepository;
    this.slackService = slackService;
    this.tokenSecurityService = tokenSecurityService;
  }

  /**
   * Returns the Slack workspace (team) associated with the course, along with a masked version of
   * the Slack bot token.
   *
   * @param courseId the id of the course
   * @return a map with courseId, slackBotToken (masked), slackTeamId and slackTeamName; values are
   *     empty strings when no token has been stored
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
    return info;
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
   * Translates a Slack error code into a message suitable for showing to an instructor.
   *
   * @param error error code from Slack, e.g. <code>invalid_auth</code>
   * @return a human readable message
   */
  public static String errorMessage(String error) {
    return switch (String.valueOf(error)) {
      case "invalid_auth", "not_authed" ->
          "Slack rejected this token as invalid. Copy the Bot User OAuth Token (xoxb-...) from your Slack app's OAuth & Permissions page and try again. The token was not saved.";
      case "token_revoked", "token_expired", "account_inactive" ->
          "This Slack token has been revoked or is no longer active. Reinstall the Slack app to the workspace to get a new token. The token was not saved.";
      case "missing_scope" ->
          "This Slack token is missing a required scope. Add the scope under OAuth & Permissions, reinstall the Slack app to the workspace, and enter the new token. The token was not saved.";
      case SlackService.SLACK_UNREACHABLE ->
          "Could not reach Slack to verify the token. Please try again later. The token was not saved.";
      default ->
          "Slack could not verify this token (%s). The token was not saved.".formatted(error);
    };
  }
}
