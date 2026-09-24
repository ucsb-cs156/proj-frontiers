package edu.ucsb.cs156.frontiers.controllers;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.CourseStaff;
import edu.ucsb.cs156.frontiers.entities.Team;
import edu.ucsb.cs156.frontiers.entities.TeamMember;
import edu.ucsb.cs156.frontiers.errors.EntityNotFoundException;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseStaffRepository;
import edu.ucsb.cs156.frontiers.repositories.TeamRepository;
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
              + " of the email before the @.",
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
    courseRepository
        .findById(courseId)
        .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));

    StreamingResponseBody stream =
        (outputStream) -> {
          try (Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
              CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
            writeStaffLines(csvPrinter, courseStaffRepository.findByCourseId(courseId));
            writeTeamLines(csvPrinter, teamRepository.findByCourseIdOrderByNameAsc(courseId));
          }
        };

    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=dokku_users_list.csv")
        .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
        .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
        .body(stream);
  }

  /**
   * Writes one line per staff member per dokku instance: each staff member gets access to every
   * instance from {@link #LOWEST_DOKKU_NUMBER} through {@link #HIGHEST_DOKKU_NUMBER}.
   */
  private static void writeStaffLines(CSVPrinter csvPrinter, Iterable<CourseStaff> staff)
      throws IOException {
    for (CourseStaff member : staff) {
      String username = usernameFromEmail(member.getEmail());
      for (String dokku : allDokkuNames()) {
        csvPrinter.printRecord(username, dokku);
      }
    }
  }

  /**
   * Writes one line per member of each team whose name maps to a dokku instance (see {@link
   * #dokkuNameForTeam(String)}); teams that do not map to a dokku instance are skipped.
   */
  private static void writeTeamLines(CSVPrinter csvPrinter, Iterable<Team> teams)
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
            usernameFromEmail(member.getRosterStudent().getEmail()), dokku.get());
      }
    }
  }
}
