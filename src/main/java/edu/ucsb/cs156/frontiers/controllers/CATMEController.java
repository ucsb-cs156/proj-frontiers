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

    if (payload == null) {
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

    return Arrays.stream(payload.split("\\R", -1))
        .map(line -> formatEmailResultLine(extractName(line), emailByName))
        .collect(Collectors.joining("\n"));
  }

  private static String extractName(String line) {
    String trimmed = normalizeSpaces(line);
    if (trimmed.isBlank()) {
      return "";
    }
    return normalizeSpaces(trimmed.replaceFirst("\\s+\\d+\\s+\\d{4}-\\d{2}-\\d{2}.*$", ""));
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
    String[] nameParts = name.split(",", 2);
    if (nameParts.length < 2) {
      return normalizeSpaces(name).toUpperCase();
    }

    return normalizeSpaces(nameParts[0]).toUpperCase()
        + ","
        + normalizeSpaces(nameParts[1]).toUpperCase();
  }

  private static String normalizeSpaces(String value) {
    return value.replaceAll("\\s+", " ").trim();
  }
}
