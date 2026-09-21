package edu.ucsb.cs156.frontiers.controllers;

import com.opencsv.CSVParser;
import edu.ucsb.cs156.frontiers.entities.CourseOption;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.Section;
import edu.ucsb.cs156.frontiers.enums.CourseOptions;
import edu.ucsb.cs156.frontiers.enums.RosterStatus;
import edu.ucsb.cs156.frontiers.models.CATMEAuditResult;
import edu.ucsb.cs156.frontiers.models.CATMEStudentDrop;
import edu.ucsb.cs156.frontiers.models.CATMEStudentUpdate;
import edu.ucsb.cs156.frontiers.repositories.CourseOptionRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.repositories.SectionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "CATME")
@RequestMapping("/api/catme")
@RestController
public class CATMEController extends ApiController {

  @Autowired private RosterStudentRepository rosterStudentRepository;

  @Autowired private CourseOptionRepository courseOptionRepository;

  @Autowired private SectionRepository sectionRepository;

  public static final String CATME_AUDIT_HEADER_LINE_1 = "Activity,Class,Term,Format,Instr,School";
  public static final String CATME_AUDIT_HEADER_LINE_4 =
      "\"Name\",\"Student ID\",\"Email\",\"Section\",\"Team Name\",";
  public static final String CATME_AUDIT_FORMAT_ERROR_MESSAGE =
      "The uploaded file was not in a recognized CATME TeamMaker CSV format.";

  /**
   * Audit the roster for a course against a CATME TeamMaker CSV export, producing a list of
   * students whose name and/or section should be updated in CATME, and a list of students who
   * should be dropped from CATME because they are no longer enrolled (MANUAL or ROSTER status) in
   * Frontiers.
   *
   * @param courseId the id of the course
   * @param file the uploaded CATME TeamMaker CSV file
   * @return a {@link CATMEAuditResult}, or a 422 error if the file is not in the expected format
   * @throws IOException if the file cannot be read
   */
  @Operation(summary = "Audit a course's roster against an uploaded CATME TeamMaker CSV file")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @PostMapping(
      value = "/audit",
      consumes = {"multipart/form-data"})
  public ResponseEntity<Object> auditCatmeCSV(
      @Parameter(name = "courseId") @RequestParam Long courseId,
      @Parameter(name = "file") @RequestParam("file") MultipartFile file)
      throws IOException {

    List<String> lines;
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
      lines = reader.lines().collect(Collectors.toList());
    }

    List<CATMEStudentRow> csvRows = parseCatmeAuditCSV(lines);
    if (csvRows == null) {
      return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
          .body(genericMessage(CATME_AUDIT_FORMAT_ERROR_MESSAGE));
    }

    List<RosterStudent> enrolledStudents =
        StreamSupport.stream(rosterStudentRepository.findByCourseId(courseId).spliterator(), false)
            .filter(
                student ->
                    student.getStudentId() != null
                        && (student.getRosterStatus() == RosterStatus.MANUAL
                            || student.getRosterStatus() == RosterStatus.ROSTER))
            .toList();

    Map<String, CATMEStudentRow> csvRowsByStudentId =
        csvRows.stream()
            .collect(
                Collectors.toMap(CATMEStudentRow::studentId, row -> row, (first, second) -> first));

    Map<String, String> sectionToLabel = new HashMap<>();
    Map<String, String> labelToSection = new HashMap<>();
    boolean translateSections =
        courseOptionRepository
            .findByCourseIdAndOption(courseId, CourseOptions.TRANSLATE_SECTIONS.name())
            .map(CourseOption::getEnabled)
            .orElse(false);
    if (translateSections) {
      for (Section section : sectionRepository.findByCourseId(courseId)) {
        sectionToLabel.put(section.getSection(), section.getLabel());
        labelToSection.put(section.getLabel(), section.getSection());
      }
    }

    List<CATMEStudentUpdate> studentsToUpdate = new ArrayList<>();
    for (RosterStudent student : enrolledStudents) {
      CATMEStudentRow row = csvRowsByStudentId.get(student.getStudentId());
      if (row == null) {
        continue;
      }
      String expectedName = formatStudentName(student.getLastName(), student.getFirstName());
      if (!expectedName.equals(row.name())) {
        studentsToUpdate.add(
            new CATMEStudentUpdate(
                student.getStudentId(), expectedName, "Name", row.name(), expectedName));
      }
      String rosterSection = student.getSection() == null ? "" : student.getSection();
      String catmeSection = row.section();
      String reverseTranslatedCatmeSection =
          labelToSection.getOrDefault(catmeSection, catmeSection);
      if (!rosterSection.equals(reverseTranslatedCatmeSection)) {
        String expectedCatmeSection = sectionToLabel.getOrDefault(rosterSection, rosterSection);
        studentsToUpdate.add(
            new CATMEStudentUpdate(
                student.getStudentId(),
                expectedName,
                "Section",
                catmeSection,
                expectedCatmeSection));
      }
    }

    Map<String, RosterStudent> enrolledStudentsByStudentId =
        enrolledStudents.stream()
            .collect(
                Collectors.toMap(
                    RosterStudent::getStudentId, student -> student, (first, second) -> first));

    List<CATMEStudentDrop> studentsToDrop = new ArrayList<>();
    for (CATMEStudentRow row : csvRows) {
      if (!enrolledStudentsByStudentId.containsKey(row.studentId())) {
        studentsToDrop.add(
            new CATMEStudentDrop(row.studentId(), row.name(), row.email(), row.section()));
      }
    }

    return ResponseEntity.ok(new CATMEAuditResult(studentsToUpdate, studentsToDrop));
  }

  /**
   * Represents a single student row parsed from a CATME TeamMaker CSV export.
   *
   * @param name the student's name
   * @param studentId the student's id
   * @param email the student's email
   * @param section the student's section
   */
  private record CATMEStudentRow(String name, String studentId, String email, String section) {}

  /**
   * Parses the lines of an uploaded CATME TeamMaker CSV file, validating the expected format.
   *
   * @param lines the lines of the file
   * @return the list of parsed student rows, or null if the file is not in the expected format
   */
  private static List<CATMEStudentRow> parseCatmeAuditCSV(List<String> lines) {
    if (lines.size() < 4) {
      return null;
    }
    if (!lines.get(0).trim().equals(CATME_AUDIT_HEADER_LINE_1)) {
      return null;
    }
    // line 1 (index 1) is intentionally ignored
    if (!lines.get(2).trim().isEmpty()) {
      return null;
    }
    if (!lines.get(3).trim().startsWith(CATME_AUDIT_HEADER_LINE_4)) {
      return null;
    }

    CSVParser csvParser = new CSVParser();
    List<CATMEStudentRow> rows = new ArrayList<>();
    for (int i = 4; i < lines.size(); i++) {
      String line = lines.get(i);
      if (line.trim().isEmpty()) {
        break;
      }
      try {
        String[] fields = csvParser.parseLine(line);
        if (fields.length < 4) {
          return null;
        }
        rows.add(new CATMEStudentRow(fields[0], fields[1], fields[2], fields[3]));
      } catch (IOException e) {
        return null;
      }
    }
    return rows;
  }

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
    return toLookupKey(formatStudentName(lastName, firstName));
  }

  private static String toLookupKey(String name) {
    return normalizeSpaces(name).toUpperCase().replaceFirst("\\s*,\\s*", ",");
  }

  private static String formatStudentName(String lastName, String firstName) {
    String normalizedLastName = normalizeSpaces(lastName == null ? "" : lastName);
    String normalizedFirstName = normalizeSpaces(firstName == null ? "" : firstName);
    if (normalizedLastName.isBlank()) {
      return normalizedFirstName;
    }
    if (normalizedFirstName.isBlank()) {
      return normalizedLastName;
    }
    return normalizedLastName + ", " + normalizedFirstName;
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
