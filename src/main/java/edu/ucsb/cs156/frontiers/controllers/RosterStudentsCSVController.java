package edu.ucsb.cs156.frontiers.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.enums.InsertStatus;
import edu.ucsb.cs156.frontiers.enums.RosterStatus;
import edu.ucsb.cs156.frontiers.errors.EntityNotFoundException;
import edu.ucsb.cs156.frontiers.models.LoadResult;
import edu.ucsb.cs156.frontiers.models.UpsertResponse;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.services.UpdateUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "RosterStudents")
@RequestMapping("/api/rosterstudents")
@RestController
@Slf4j
public class RosterStudentsCSVController extends ApiController {

  @Autowired private RosterStudentRepository rosterStudentRepository;

  @Autowired private CourseRepository courseRepository;

  @Autowired private UpdateUserService updateUserService;

  public enum RosterSourceType {
    UCSB_EGRADES,
    CHICO_CANVAS,
    OREGON_STATE,
    UNKNOWN
  }

  public static final String UCSB_EGRADES_HEADERS =
      "Enrl Cd,Perm #,Grade,Final Units,Student Last,Student First Middle,Quarter,Course ID,Section,Meeting Time(s) / Location(s),Email,ClassLevel,Major1,Major2,Date/Time,Pronoun";
  public static final String CHICO_CANVAS_HEADERS =
      "Student Name,Student ID,Student SIS ID,Email,Section Name";
  public static final String OREGON_STATE_HEADERS =
      "Full name,Sortable name,Canvas user id,Overall course grade,Assignment on time percent,Last page view time,Last participation time,Last logged out,Email,SIS Id";

  public static RosterSourceType getRosterSourceType(String[] headers) {

    Map<RosterSourceType, String[]> sourceTypeToHeaders = new HashMap<>();

    sourceTypeToHeaders.put(RosterSourceType.UCSB_EGRADES, UCSB_EGRADES_HEADERS.split(","));
    sourceTypeToHeaders.put(RosterSourceType.CHICO_CANVAS, CHICO_CANVAS_HEADERS.split(","));
    sourceTypeToHeaders.put(RosterSourceType.OREGON_STATE, OREGON_STATE_HEADERS.split(","));

    for (Map.Entry<RosterSourceType, String[]> entry : sourceTypeToHeaders.entrySet()) {
      RosterSourceType type = entry.getKey();
      String[] expectedHeaders = entry.getValue();
      if (headers.length >= expectedHeaders.length) {
        boolean matches = true;
        for (int i = 0; i < expectedHeaders.length; i++) {
          if (!expectedHeaders[i].trim().equalsIgnoreCase(headers[i].trim())) {
            matches = false;
            break;
          }
        }
        if (matches) {
          return type;
        }
      }
    }
    // If no known type matches, return UNKNOWN
    return RosterSourceType.UNKNOWN;
  }

  /**
   * Upload Roster students for Course in any supported format. It is important to keep the code in
   * this method consistent with the code for adding a single roster student
   *
   * @param courseId
   * @param file
   * @return
   * @throws JsonProcessingException
   * @throws IOException
   * @throws CsvException
   */
  @Operation(summary = "Upload Roster students for Course in any supported Format")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @PostMapping(
      value = "/upload/csv",
      consumes = {"multipart/form-data"})
  public ResponseEntity<LoadResult> uploadRosterStudentsCSV(
      @Parameter(name = "courseId") @RequestParam Long courseId,
      @Parameter(name = "file") @RequestParam("file") MultipartFile file)
      throws JsonProcessingException, IOException, CsvException {

    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId.toString()));

    int counts[] = {0, 0};
    List<RosterStudent> rejectedStudents = new ArrayList<>();
    List<RosterStudent> saveableStudents = new ArrayList<>();

    try (InputStream inputStream = new BufferedInputStream(file.getInputStream());
        InputStreamReader reader = new InputStreamReader(inputStream);
        CSVReader csvReader = new CSVReader(reader); ) {

      String[] headers = csvReader.readNext();
      RosterSourceType sourceType = getRosterSourceType(headers);
      if (sourceType == RosterSourceType.UNKNOWN) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown Roster Source Type");
      }
      if (sourceType == RosterSourceType.UCSB_EGRADES) {
        csvReader.skip(1);
      }
      List<String[]> myEntries = csvReader.readAll();
      for (String[] row : myEntries) {
        RosterStudent rosterStudent = fromCSVRow(row, sourceType);
        UpsertResponse upsertResponse =
            RosterStudentsController.upsertStudent(rosterStudent, course, RosterStatus.ROSTER);
        if (upsertResponse.getInsertStatus() == InsertStatus.REJECTED) {
          rejectedStudents.add(upsertResponse.rosterStudent());
        } else {
          saveableStudents.add(upsertResponse.rosterStudent());
          InsertStatus s = upsertResponse.getInsertStatus();
          counts[s.ordinal()]++;
        }
      }
    }
    LoadResult loadResult =
        new LoadResult(
            counts[InsertStatus.INSERTED.ordinal()],
            counts[InsertStatus.UPDATED.ordinal()],
            rejectedStudents);
    if (rejectedStudents.isEmpty()) {
      saveableStudents = rosterStudentRepository.saveAll(saveableStudents);
      updateUserService.attachUsersToRosterStudents(saveableStudents);
      return ResponseEntity.ok(loadResult);
    } else {
      return ResponseEntity.status(HttpStatus.CONFLICT).body(loadResult);
    }
  }

  public static RosterStudent fromCSVRow(String[] row, RosterSourceType sourceType) {
    if (sourceType == RosterSourceType.UCSB_EGRADES) {
      return fromUCSBEgradesCSVRow(row);
    } else if (sourceType == RosterSourceType.CHICO_CANVAS) {
      return fromChicoCanvasCSVRow(row);
    } else if (sourceType == RosterSourceType.OREGON_STATE) {
      return fromOregonStateCSVRow(row);
    } else {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV format not recognized");
    }
  }

  public static void checkRowLength(String[] row, int expectedLength, RosterSourceType sourceType) {
    if (row.length < expectedLength) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          String.format(
              "%s CSV row does not have enough columns. Length = %d Row content = [%s]",
              sourceType.toString(), row.length, Arrays.toString(row)));
    }
  }

  public static RosterStudent fromUCSBEgradesCSVRow(String[] row) {
    checkRowLength(row, 11, RosterSourceType.UCSB_EGRADES);
    return RosterStudent.builder()
        .firstName(row[5])
        .lastName(row[4])
        .studentId(row[1])
        .email(row[10])
        .section(row[0])
        .build();
  }

  public static RosterStudent fromChicoCanvasCSVRow(String[] row) {
    checkRowLength(row, 4, RosterSourceType.CHICO_CANVAS);
    return RosterStudent.builder()
        .firstName(getFirstName(row[0]))
        .lastName(getLastName(row[0]))
        .studentId(row[2])
        .email(row[3])
        .section("")
        .build();
  }

  public static RosterStudent fromOregonStateCSVRow(String[] row) {

    checkRowLength(row, 10, RosterSourceType.OREGON_STATE);
    String sortableName = row[1];
    String sortableNameParts[] = sortableName.split(",");
    String lastName = sortableNameParts[0].trim();
    String firstName = sortableNameParts.length > 1 ? sortableNameParts[1].trim() : "";
    return RosterStudent.builder()
        .firstName(firstName)
        .lastName(lastName)
        .studentId(row[9])
        .email(row[8])
        .section("")
        .build();
  }

  /**
   * Get everything except up to and not including the last space in the full name. If the string
   * contains no spaces, return an empty string.
   *
   * @param fullName
   * @return
   */
  public static String getFirstName(String fullName) {
    int lastSpaceIndex = fullName.lastIndexOf(" ");
    if (lastSpaceIndex == -1) {
      return ""; // No spaces found, return empty string
    }
    return fullName.substring(0, lastSpaceIndex).trim(); // Return everything before the last space
  }

  /**
   * Get everything after the last space in the full name. If the string contains no spaces, return
   * the entire input string as the result.
   *
   * @param fullName
   * @return best estimate of last name
   */
  public static String getLastName(String fullName) {
    int lastSpaceIndex = fullName.lastIndexOf(" ");
    if (lastSpaceIndex == -1) {
      return fullName; // No spaces found, return the entire string
    }
    return fullName.substring(lastSpaceIndex + 1).trim(); // Return everything after the last space
  }
}
