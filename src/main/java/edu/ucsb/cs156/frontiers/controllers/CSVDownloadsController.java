package edu.ucsb.cs156.frontiers.controllers;

import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.enums.RosterStatus;
import edu.ucsb.cs156.frontiers.errors.EntityNotFoundException;
import edu.ucsb.cs156.frontiers.models.NameAndTeam;
import edu.ucsb.cs156.frontiers.models.RosterStudentDTO;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.services.RosterStudentDTOService;
import edu.ucsb.cs156.frontiers.services.SectionTranslationService;
import edu.ucsb.cs156.frontiers.services.TeamCsvService;
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

  @Autowired private SectionTranslationService sectionTranslationService;

  @Autowired private TeamCsvService teamCsvService;

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
          Map<String, String> sectionTranslations =
              sectionTranslationService.getSectionTranslations(courseId);
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
                  SectionTranslationService.translateSection(
                      student.getSection(), sectionTranslations),
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

  @Operation(
      summary = "Download compact Name to Team CSV File",
      description =
          "Returns a CSV file with the roster laid out in several side-by-side Name,Team column"
              + " pairs. Names are sorted by first name and abbreviated as far as possible while"
              + " remaining unique (first name; first name plus last initial; first name plus last"
              + " name). Exact duplicate names are marked with an asterisk.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "CSV file",
            content =
                @Content(
                    mediaType = "text/csv",
                    schema = @Schema(type = "string", format = "binary"))),
        @ApiResponse(responseCode = "400", description = "columns is less than 1"),
        @ApiResponse(responseCode = "404", description = "Course not found"),
        @ApiResponse(responseCode = "500", description = "Internal Server Error")
      })
  @GetMapping(value = "/name2team", produces = "text/csv")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  public ResponseEntity<StreamingResponseBody> name2TeamCsv(
      @Parameter(name = "courseId", description = "course id", example = "1") @RequestParam
          Long courseId,
      @Parameter(
              name = "columns",
              description = "number of Name,Team column pairs (default 4)",
              example = "4")
          @RequestParam(defaultValue = "4")
          int columns)
      throws EntityNotFoundException {
    return teamCsvResponse(courseId, columns, "name2team", teamCsvService::writeName2TeamCsv);
  }

  @Operation(
      summary = "Download compact Team Table CSV File",
      description =
          "Returns a CSV file with the teams laid out in several side-by-side Team,Name column"
              + " pairs, one row per student, teams sorted by name and balanced across the columns"
              + " so that no team is split. Names are sorted by first name and abbreviated as far"
              + " as possible while remaining unique (first name; first name plus last initial;"
              + " first name plus last name). Exact duplicate names are marked with an asterisk."
              + " Students with no team are listed last with a blank team.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "CSV file",
            content =
                @Content(
                    mediaType = "text/csv",
                    schema = @Schema(type = "string", format = "binary"))),
        @ApiResponse(responseCode = "400", description = "columns is less than 1"),
        @ApiResponse(responseCode = "404", description = "Course not found"),
        @ApiResponse(responseCode = "500", description = "Internal Server Error")
      })
  @GetMapping(value = "/teamtable", produces = "text/csv")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  public ResponseEntity<StreamingResponseBody> teamTableCsv(
      @Parameter(name = "courseId", description = "course id", example = "1") @RequestParam
          Long courseId,
      @Parameter(
              name = "columns",
              description = "number of Team,Name column pairs (default 4)",
              example = "4")
          @RequestParam(defaultValue = "4")
          int columns)
      throws EntityNotFoundException {
    return teamCsvResponse(courseId, columns, "teamtable", teamCsvService::writeTeamTableCsv);
  }

  /** Writes one of the compact team CSV tables from a list of abbreviated names and teams. */
  @FunctionalInterface
  interface TeamCsvWriter {
    void write(Writer writer, List<NameAndTeam> entries, int columns) throws IOException;
  }

  /**
   * Shared response for the compact team CSV downloads: looks up the course, validates the column
   * count, loads the ROSTER and MANUAL students sorted by first name, abbreviates their names and
   * streams the table written by the given writer.
   *
   * @param courseId the course id
   * @param columns the number of column pairs, at least 1
   * @param suffix the file name suffix, e.g. "name2team" gives "&lt;courseName&gt;_name2team.csv"
   * @param csvWriter writes the table
   * @return the streaming CSV response
   * @throws EntityNotFoundException if the course does not exist
   * @throws IllegalArgumentException if columns is less than 1
   */
  private ResponseEntity<StreamingResponseBody> teamCsvResponse(
      Long courseId, int columns, String suffix, TeamCsvWriter csvWriter)
      throws EntityNotFoundException {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));
    if (columns < 1) {
      throw new IllegalArgumentException("columns must be at least 1");
    }
    StreamingResponseBody stream =
        (outputStream) -> {
          List<RosterStudent> rosterStudents =
              rosterStudentRepository
                  .findByCourseIdAndRosterStatusInOrderByFirstNameAscLastNameAscIgnoreCase(
                      courseId, List.of(RosterStatus.ROSTER, RosterStatus.MANUAL));
          List<NameAndTeam> entries = teamCsvService.nameAndTeams(rosterStudents);
          try (Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
            csvWriter.write(writer, entries, columns);
          }
        };

    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            String.format("attachment;filename=%s_%s.csv", course.getCourseName(), suffix))
        .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
        .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
        .body(stream);
  }
}
