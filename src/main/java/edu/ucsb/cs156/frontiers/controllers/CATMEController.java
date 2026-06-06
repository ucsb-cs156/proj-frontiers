package edu.ucsb.cs156.frontiers.controllers;

import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "CATME")
@RequestMapping("/api/catme")
@RestController
public class CATMEController extends ApiController {

  @Autowired private RosterStudentRepository rosterStudentRepository;

  @Operation(summary = "Convert CATME roster names into course emails")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @PostMapping(value = "/emails", consumes = "text/plain", produces = "text/plain")
  public String getCourseEmailsFromCatme(
      @Parameter(name = "courseId") @RequestParam Long courseId,
      @Parameter(name = "payload") @RequestBody(required = false) String payload) {

    String normalizedPayload = payload == null ? "" : payload;
    if (normalizedPayload.isBlank()) {
      return "";
    }

    Map<String, String> emailByName =
        StreamSupport.stream(rosterStudentRepository.findByCourseId(courseId).spliterator(), false)
            .filter(
                student ->
                    student.getFirstName() != null
                        && student.getLastName() != null
                        && student.getEmail() != null)
            .collect(
                Collectors.toMap(
                    student -> toLookupKey(student.getLastName(), student.getFirstName()),
                    RosterStudent::getEmail,
                    (first, second) -> first));

    // \\R handles all line endings and -1 preserves trailing blank lines.
    return Arrays.stream(normalizedPayload.split("\\R", -1))
        .map(line -> formatEmailResultLine(extractName(line), emailByName))
        .collect(Collectors.joining("\n"));
  }

  private static String extractName(String line) {
    String trimmed = normalizeSpaces(line);
    if (trimmed.isBlank()) {
      return "";
    }

    // CATME lines look like: "LAST, FIRST MIDDLE <score> <timestamp>".
    int dateStart = findDateTokenStart(trimmed);
    if (dateStart == -1) {
      return trimmed;
    }

    String beforeDate = trimmed.substring(0, dateStart).trim();
    int lastSpace = beforeDate.lastIndexOf(' ');
    if (lastSpace == -1) {
      return beforeDate;
    }

    String possibleScore = beforeDate.substring(lastSpace + 1);
    if (possibleScore.chars().allMatch(Character::isDigit)) {
      return beforeDate.substring(0, lastSpace).trim();
    }
    return beforeDate;
  }

  private static String formatEmailResultLine(String name, Map<String, String> emailByName) {
    if (name.isBlank()) {
      return "";
    }
    String email = emailByName.get(toLookupKey(name));
    if (email != null) {
      return email;
    }
    return "# NO EMAIL FOUND FOR " + name;
  }

  private static String toLookupKey(String lastName, String firstName) {
    return toLookupKey(lastName + ", " + firstName);
  }

  private static String toLookupKey(String name) {
    return normalizeSpaces(name).toUpperCase().replaceFirst("\\s*,\\s*", ",");
  }

  private static String normalizeSpaces(String value) {
    return value.replaceAll("\\s+", " ").trim();
  }

  private static int findDateTokenStart(String value) {
    for (int i = 0; i + 10 <= value.length(); i++) {
      if (isDateToken(value, i)) {
        return i;
      }
    }
    return -1;
  }

  private static boolean isDateToken(String value, int start) {
    if (start > 0 && value.charAt(start - 1) != ' ') {
      return false;
    }

    return Character.isDigit(value.charAt(start))
        && Character.isDigit(value.charAt(start + 1))
        && Character.isDigit(value.charAt(start + 2))
        && Character.isDigit(value.charAt(start + 3))
        && value.charAt(start + 4) == '-'
        && Character.isDigit(value.charAt(start + 5))
        && Character.isDigit(value.charAt(start + 6))
        && value.charAt(start + 7) == '-'
        && Character.isDigit(value.charAt(start + 8))
        && Character.isDigit(value.charAt(start + 9));
  }
}
