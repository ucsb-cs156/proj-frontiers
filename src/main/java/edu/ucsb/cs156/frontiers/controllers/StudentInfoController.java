package edu.ucsb.cs156.frontiers.controllers;

import edu.ucsb.cs156.frontiers.config.AllowApiKeyAccess;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.enums.RosterStatus;
import edu.ucsb.cs156.frontiers.errors.StudentNotOnRosterException;
import edu.ucsb.cs156.frontiers.models.NameAndTeam;
import edu.ucsb.cs156.frontiers.models.StudentInfo;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.services.TeamCsvService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.StreamSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Looks up one roster student by email and returns what an autograder needs to check their
 * submission: their GitHub login, their team, and the names of their teammates. The endpoint
 * accepts a course-scoped API key (see docs/api-keys.md), so that a Gradescope autograder or a
 * GitHub Action can call it without a logged-in user.
 */
@Tag(name = "Student Info")
@RestController
@RequestMapping("/api/courses/studentInfo")
@Slf4j
public class StudentInfoController extends ApiController {

  private static final String UMAIL_SUFFIX = "@umail.ucsb.edu";
  private static final String UCSB_SUFFIX = "@ucsb.edu";

  private final RosterStudentRepository rosterStudentRepository;
  private final TeamCsvService teamCsvService;

  public StudentInfoController(
      RosterStudentRepository rosterStudentRepository, TeamCsvService teamCsvService) {
    this.rosterStudentRepository = rosterStudentRepository;
    this.teamCsvService = teamCsvService;
  }

  /**
   * Puts an email into the form used for comparison: surrounding whitespace removed, lower case,
   * and the legacy <code>@umail.ucsb.edu</code> domain replaced by <code>@ucsb.edu</code> (both
   * reach the same mailbox, and Gradescope may report either one).
   *
   * @param email the email as supplied
   * @return the normalized email
   */
  public static String normalizeEmail(String email) {
    String normalized = email.strip().toLowerCase();
    if (normalized.endsWith(UMAIL_SUFFIX)) {
      normalized =
          normalized.substring(0, normalized.length() - UMAIL_SUFFIX.length()) + UCSB_SUFFIX;
    }
    return normalized;
  }

  private static String lower(String s) {
    return s == null ? "" : s.toLowerCase();
  }

  @Operation(
      summary = "Get a student's GitHub login, team and teammates (for autograders)",
      description =
          """
          Looks up a non-dropped roster student of the course by email (case-insensitive;
          @umail.ucsb.edu is treated as @ucsb.edu) and returns their GitHub login, their team,
          and everyone on that team. The firstName fields are the shortest names that are
          unique within the course (e.g. "Chris L"), as on the team CSVs; legalFirstName is the
          roster's first name field. Accepts a course-scoped API key in the X-API-KEY header.
          """)
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @AllowApiKeyAccess
  @GetMapping("")
  public StudentInfo getStudentInfo(
      @Parameter(name = "courseId") @RequestParam Long courseId,
      @Parameter(name = "email") @RequestParam String email) {

    String wantedEmail = normalizeEmail(email);

    // Dropped students are not part of the current roster: they cannot be looked up, and they are
    // not listed as anyone's teammate. The rest are sorted by first name (first word only, as in
    // the short names), last name and email, so that the teammates come back in a stable,
    // human-friendly order.
    List<RosterStudent> students =
        StreamSupport.stream(rosterStudentRepository.findByCourseId(courseId).spliterator(), false)
            .filter(student -> student.getRosterStatus() != RosterStatus.DROPPED)
            .sorted(
                Comparator.comparing(
                        (RosterStudent student) ->
                            teamCsvService.firstWord(student.getFirstName()).toLowerCase())
                    .thenComparing(student -> lower(student.getLastName()))
                    .thenComparing(student -> lower(student.getEmail())))
            .toList();

    // Unique short names must be worked out over the whole roster, not just the team, so that they
    // match the names students see on the team CSVs.
    List<NameAndTeam> namesAndTeams = teamCsvService.nameAndTeams(students);

    int index = -1;
    for (int i = 0; i < students.size(); i++) {
      String studentEmail = students.get(i).getEmail();
      if (studentEmail != null && normalizeEmail(studentEmail).equals(wantedEmail)) {
        index = i;
        break;
      }
    }
    if (index < 0) {
      throw new StudentNotOnRosterException(wantedEmail, courseId);
    }

    RosterStudent student = students.get(index);
    String team = namesAndTeams.get(index).team();

    List<StudentInfo.Member> teamMembers = new ArrayList<>();
    if (!team.isEmpty()) {
      for (int i = 0; i < students.size(); i++) {
        RosterStudent candidate = students.get(i);
        if (candidate.getTeams().contains(team)) {
          teamMembers.add(
              new StudentInfo.Member(
                  candidate.getEmail(),
                  namesAndTeams.get(i).name(),
                  candidate.getFirstName(),
                  candidate.getGithubLogin()));
        }
      }
    }

    return new StudentInfo(
        student.getEmail(),
        namesAndTeams.get(index).name(),
        student.getFirstName(),
        student.getLastName(),
        student.getGithubLogin(),
        team.isEmpty() ? null : team,
        student.getTeams(),
        teamMembers);
  }
}
