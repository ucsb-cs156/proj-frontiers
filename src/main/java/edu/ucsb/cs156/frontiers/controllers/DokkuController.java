package edu.ucsb.cs156.frontiers.controllers;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.CourseStaff;
import edu.ucsb.cs156.frontiers.entities.DokkuAccountTranslation;
import edu.ucsb.cs156.frontiers.entities.Team;
import edu.ucsb.cs156.frontiers.entities.TeamMember;
import edu.ucsb.cs156.frontiers.errors.EntityNotFoundException;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseStaffRepository;
import edu.ucsb.cs156.frontiers.repositories.DokkuAccountTranslationRepository;
import edu.ucsb.cs156.frontiers.repositories.TeamRepository;
import edu.ucsb.cs156.frontiers.utilities.CanonicalFormConverter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * Endpoints that produce files for setting up access to the UCSB Dokku servers (dokku-00 through
 * dokku-18).
 */
@Tag(name = "Dokku")
@RequestMapping("/api/dokku")
@RestController
public class DokkuController extends ApiController {

  /** The lowest numbered dokku instance, i.e. dokku-00. */
  public static final int LOWEST_DOKKU_NUMBER = 0;

  /** The highest numbered dokku instance, i.e. dokku-18. */
  public static final int HIGHEST_DOKKU_NUMBER = 18;

  /**
   * Matches team names of the form {@code xxx-yy}: anything, a literal hyphen, then exactly two
   * digits at the end. The two digits are captured as group 1 and are the dokku number.
   */
  static final Pattern TEAM_NAME_PATTERN = Pattern.compile("^.*-(\\d{2})$");

  /**
   * Every dokku instance name, keyed by its two digit suffix and ordered from {@link
   * #LOWEST_DOKKU_NUMBER} to {@link #HIGHEST_DOKKU_NUMBER}, e.g. {@code "07" -> "dokku-07"}. This
   * is the single place the range of dokku numbers is expanded.
   */
  static final Map<String, String> DOKKU_NAME_BY_SUFFIX = dokkuNamesBySuffix();

  private static Map<String, String> dokkuNamesBySuffix() {
    Map<String, String> names = new LinkedHashMap<>();
    for (int n = LOWEST_DOKKU_NUMBER; n <= HIGHEST_DOKKU_NUMBER; n++) {
      String suffix = String.format("%02d", n);
      names.put(suffix, "dokku-" + suffix);
    }
    return names;
  }

  @Autowired private CourseRepository courseRepository;

  @Autowired private CourseStaffRepository courseStaffRepository;

  @Autowired private TeamRepository teamRepository;

  @Autowired private DokkuAccountTranslationRepository dokkuAccountTranslationRepository;

  /**
   * Returns every dokku instance name in order, {@code dokku-00} through {@code dokku-18}.
   *
   * @return the dokku instance names
   */
  public static Collection<String> allDokkuNames() {
    return DOKKU_NAME_BY_SUFFIX.values();
  }

  /**
   * Extracts the dokku username from an email address: everything before the first {@code @}. An
   * email with no {@code @} is returned unchanged.
   *
   * @param email the email address
   * @return the username
   */
  public static String usernameFromEmail(String email) {
    int at = email.indexOf('@');
    return at < 0 ? email : email.substring(0, at);
  }

  /**
   * Returns the dokku username for an email: the translated username if there is a {@link
   * DokkuAccountTranslation} for the email (compared in canonical form), otherwise the part of the
   * email before the {@code @}.
   *
   * @param email the email address
   * @param translations map from canonical email to dokku username
   * @return the dokku username
   */
  public static String usernameFor(String email, Map<String, String> translations) {
    String translated = translations.get(CanonicalFormConverter.convertToValidEmail(email));
    return translated != null ? translated : usernameFromEmail(email);
  }

  /**
   * Returns the dokku instance named by a team, if the team name has the form {@code xxx-yy} and
   * {@code yy} is between {@link #LOWEST_DOKKU_NUMBER} and {@link #HIGHEST_DOKKU_NUMBER} inclusive.
   *
   * @param teamName the team name, e.g. {@code "s26-07"}
   * @return the dokku instance name, e.g. {@code "dokku-07"}, or empty if the team does not
   *     correspond to a dokku instance
   */
  public static Optional<String> dokkuNameForTeam(String teamName) {
    if (teamName == null) {
      return Optional.empty();
    }
    Matcher matcher = TEAM_NAME_PATTERN.matcher(teamName);
    if (!matcher.matches()) {
      return Optional.empty();
    }
    return Optional.ofNullable(DOKKU_NAME_BY_SUFFIX.get(matcher.group(1)));
  }

  @Operation(
      summary = "Download dokku_users_list.csv for a course",
      description =
          "Returns a CSV file with no header line and one `username,dokku-nn` line per grant of"
              + " access. Every staff member of the course is granted access to every dokku"
              + " instance from dokku-"
              + LOWEST_DOKKU_NUMBER
              + " through dokku-"
              + HIGHEST_DOKKU_NUMBER
              + ". Each member of a team whose name ends in `-nn` (nn between "
              + LOWEST_DOKKU_NUMBER
              + " and "
              + HIGHEST_DOKKU_NUMBER
              + ") is granted access to dokku-nn; other teams are ignored. Usernames are the part"
              + " of the email before the @, unless the email has a dokku account translation, in"
              + " which case the translated username is used.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "CSV file",
            content =
                @Content(
                    mediaType = "text/csv",
                    schema = @Schema(type = "string", format = "binary"))),
        @ApiResponse(responseCode = "404", description = "Course not found"),
        @ApiResponse(responseCode = "500", description = "Internal Server Error")
      })
  @GetMapping(value = "/dokku_users_list", produces = "text/csv")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  public ResponseEntity<StreamingResponseBody> dokkuUsersList(
      @Parameter(name = "courseId", description = "course id", example = "1") @RequestParam
          Long courseId)
      throws EntityNotFoundException {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));

    StreamingResponseBody stream =
        (outputStream) -> {
          try (Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
              CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
            writeHeaderLines(writer, course.getDokkuUsersListHeader());
            Map<String, String> translations = translationsByEmail();
            writeStaffLines(
                csvPrinter, courseStaffRepository.findByCourseId(courseId), translations);
            writeTeamLines(
                csvPrinter, teamRepository.findByCourseIdOrderByNameAsc(courseId), translations);
          }
        };

    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=dokku_users_list.csv")
        .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
        .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
        .body(stream);
  }

  @Operation(
      summary = "Get the extra lines placed at the start of dokku_users_list.csv for a course",
      description =
          "Returns the course's dokkuUsersListHeader as plain text, or an empty body if none is"
              + " set.")
  @GetMapping(value = "/users_list_header", produces = "text/plain")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  public String getUsersListHeader(
      @Parameter(name = "courseId", description = "course id", example = "1") @RequestParam
          Long courseId)
      throws EntityNotFoundException {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));
    return course.getDokkuUsersListHeader() == null ? "" : course.getDokkuUsersListHeader();
  }

  @Operation(
      summary = "Set the extra lines placed at the start of dokku_users_list.csv for a course",
      description =
          "The request body (plain text, line breaks preserved) replaces the course's"
              + " dokkuUsersListHeader. A blank body clears it. Returns the saved text.")
  @PutMapping(value = "/users_list_header", consumes = "text/plain", produces = "text/plain")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  public String setUsersListHeader(
      @Parameter(name = "courseId", description = "course id", example = "1") @RequestParam
          Long courseId,
      @Parameter(name = "header") @RequestBody(required = false) String header)
      throws EntityNotFoundException {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));
    String normalized = header == null || header.isBlank() ? null : header;
    course.setDokkuUsersListHeader(normalized);
    courseRepository.save(course);
    return normalized == null ? "" : normalized;
  }

  /**
   * Writes the course's dokkuUsersListHeader before any generated lines: one line per non-blank
   * line of the header, stripped, with line endings normalized to the CSV record separator. The
   * lines are written verbatim (not as quoted CSV fields), since each is already a
   * username,dokku-nn entry. Nothing is written when the header is null.
   */
  private static void writeHeaderLines(Writer writer, String header) throws IOException {
    if (header == null) {
      return;
    }
    for (String line : header.split("\\R")) {
      String trimmed = line.strip();
      if (!trimmed.isEmpty()) {
        writer.write(trimmed);
        writer.write(CSVFormat.DEFAULT.getRecordSeparator());
      }
    }
  }

  /** Loads every {@link DokkuAccountTranslation} into a map from canonical email to username. */
  private Map<String, String> translationsByEmail() {
    Map<String, String> translations = new HashMap<>();
    for (DokkuAccountTranslation translation : dokkuAccountTranslationRepository.findAll()) {
      translations.put(
          CanonicalFormConverter.convertToValidEmail(translation.getEmail()),
          translation.getUsername());
    }
    return translations;
  }

  /**
   * Writes one line per staff member per dokku instance: each staff member gets access to every
   * instance from {@link #LOWEST_DOKKU_NUMBER} through {@link #HIGHEST_DOKKU_NUMBER}.
   */
  private static void writeStaffLines(
      CSVPrinter csvPrinter, Iterable<CourseStaff> staff, Map<String, String> translations)
      throws IOException {
    for (CourseStaff member : staff) {
      String username = usernameFor(member.getEmail(), translations);
      for (String dokku : allDokkuNames()) {
        csvPrinter.printRecord(username, dokku);
      }
    }
  }

  /**
   * Writes one line per member of each team whose name maps to a dokku instance (see {@link
   * #dokkuNameForTeam(String)}); teams that do not map to a dokku instance are skipped.
   */
  private static void writeTeamLines(
      CSVPrinter csvPrinter, Iterable<Team> teams, Map<String, String> translations)
      throws IOException {
    for (Team team : teams) {
      Optional<String> dokku = dokkuNameForTeam(team.getName());
      if (dokku.isEmpty()) {
        continue;
      }
      List<TeamMember> members = team.getTeamMembers();
      if (members == null) {
        continue;
      }
      for (TeamMember member : members) {
        csvPrinter.printRecord(
            usernameFor(member.getRosterStudent().getEmail(), translations), dokku.get());
      }
    }
  }
}
