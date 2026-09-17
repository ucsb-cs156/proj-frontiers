package edu.ucsb.cs156.frontiers.controllers;

import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.CourseOption;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.Section;
import edu.ucsb.cs156.frontiers.enums.CourseOptions;
import edu.ucsb.cs156.frontiers.enums.RosterStatus;
import edu.ucsb.cs156.frontiers.errors.EntityNotFoundException;
import edu.ucsb.cs156.frontiers.models.RosterStudentDTO;
import edu.ucsb.cs156.frontiers.repositories.CourseOptionRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.repositories.SectionRepository;
import edu.ucsb.cs156.frontiers.services.RosterStudentDTOService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
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

@Tag(name = "CSV Downloads")
@RequestMapping("/api/csv")
@RestController
@Slf4j
public class CSVDownloadsController extends ApiController {

  @Autowired private CourseRepository courseRepository;

  @Autowired private RosterStudentDTOService rosterStudentDTOService;

  @Autowired private RosterStudentRepository rosterStudentRepository;

  @Autowired private CourseOptionRepository courseOptionRepository;

  @Autowired private SectionRepository sectionRepository;

  @Operation(
      summary = "Download CSV File of Roster Students",
      description = "Returns a CSV file as a response",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "CSV file",
            content =
                @Content(
                    mediaType = "text/csv",
                    schema = @Schema(type = "string", format = "binary"))),
        @ApiResponse(responseCode = "500", description = "Internal Server Error")
      })
  @GetMapping(value = "/rosterstudents", produces = "text/csv")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  public ResponseEntity<StreamingResponseBody> csvForQuarter(
      @Parameter(name = "courseId", description = "course id", example = "1") @RequestParam
          Long courseId)
      throws EntityNotFoundException, Exception, IOException {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));
    StreamingResponseBody stream =
        (outputStream) -> {
          List<RosterStudentDTO> list = rosterStudentDTOService.getRosterStudentDTOs(courseId);
          try (Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
            try {
              StatefulBeanToCsv<RosterStudentDTO> beanToCsvWriter =
                  rosterStudentDTOService.getStatefulBeanToCSV(writer);
              beanToCsvWriter.write(list);
            } catch (CsvDataTypeMismatchException | CsvRequiredFieldEmptyException e) {
              log.error("Error writing CSV file", e);
              throw new IOException("Error writing CSV file: " + e.getMessage());
            }
          }
        };

    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            String.format("attachment;filename=%s_roster.csv", course.getCourseName()))
        .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
        .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
        .body(stream);
  }

  @Operation(
      summary = "Download CATME CSV File of Roster Students",
      description = "Returns a CSV file as a response",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "CSV file",
            content =
                @Content(
                    mediaType = "text/csv",
                    schema = @Schema(type = "string", format = "binary"))),
        @ApiResponse(responseCode = "500", description = "Internal Server Error")
      })
  @GetMapping(value = "/catme", produces = "text/csv")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  public ResponseEntity<StreamingResponseBody> catmeCsv(
      @Parameter(name = "courseId", description = "course id", example = "1") @RequestParam
          Long courseId)
      throws EntityNotFoundException {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));
    StreamingResponseBody stream =
        (outputStream) -> {
          List<RosterStudent> rosterStudents =
              rosterStudentRepository
                  .findByCourseIdAndRosterStatusInOrderByFirstNameAscLastNameAscIgnoreCase(
                      courseId, List.of(RosterStatus.ROSTER, RosterStatus.MANUAL));
          Map<String, String> sectionTranslations = getSectionTranslations(courseId);
          try (Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
              CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
            csvPrinter.printRecord("first", "last", "email", "id", "section", "team");
            for (RosterStudent student : rosterStudents) {
              List<String> teams = student.getTeams();
              String team = teams != null && !teams.isEmpty() ? teams.get(0) : "";
              csvPrinter.printRecord(
                  student.getFirstName(),
                  student.getLastName(),
                  student.getEmail(),
                  student.getStudentId(),
                  translateSection(student.getSection(), sectionTranslations),
                  team);
            }
          }
        };

    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            String.format("attachment;filename=%s_catme.csv", course.getCourseName()))
        .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
        .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
        .body(stream);
  }

  /**
   * Returns the map from raw section value to label for the course, if the TRANSLATE_SECTIONS
   * course option is enabled; otherwise an empty map (raw values are used as-is).
   *
   * @param courseId the id of the course
   * @return map of raw section value to translated label
   */
  public Map<String, String> getSectionTranslations(Long courseId) {
    boolean translateSections =
        courseOptionRepository
            .findByCourseIdAndOption(courseId, CourseOptions.TRANSLATE_SECTIONS.name())
            .map(CourseOption::getEnabled)
            .orElse(false);
    Map<String, String> translations = new HashMap<>();
    if (translateSections) {
      for (Section section : sectionRepository.findByCourseId(courseId)) {
        translations.put(section.getSection(), section.getLabel());
      }
    }
    return translations;
  }

  /**
   * Translates a raw roster student section value using the given map. A null section is treated as
   * blank; a section with no translation is returned unchanged.
   *
   * @param rawSection the section value from the roster student row (may be null)
   * @param translations map of raw section value to label
   * @return the value to put in the CATME section column
   */
  public static String translateSection(String rawSection, Map<String, String> translations) {
    String section = rawSection == null ? "" : rawSection;
    return translations.getOrDefault(section, section);
  }
}
