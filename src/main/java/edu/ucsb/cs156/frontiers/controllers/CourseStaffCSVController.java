package edu.ucsb.cs156.frontiers.controllers;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.CourseStaff;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.errors.EntityNotFoundException;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseStaffRepository;
import edu.ucsb.cs156.frontiers.services.UpdateUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "CourseStaff")
@RequestMapping("/api/coursestaff")
@RestController("CourseStaffCSVController")
@Slf4j
public class CourseStaffCSVController extends ApiController {

  @Autowired private CourseStaffRepository courseStaffRepository;

  @Autowired private CourseRepository courseRepository;

  @Autowired private UpdateUserService updateUserService;

  public static final String STAFF_CSV_HEADERS = "firstName,lastName,email";

  @Operation(summary = "Upload Course Staff from CSV")
  @PreAuthorize("@CourseSecurity.hasInstructorPermissions(#root, #courseId)")
  @PostMapping(
      value = "/upload/csv",
      consumes = {"multipart/form-data"})
  public Map<String, Integer> uploadStaffCSV(
      @Parameter(name = "courseId") @RequestParam Long courseId,
      @Parameter(name = "file") @RequestParam("file") MultipartFile file)
      throws IOException, CsvException {

    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId.toString()));

    int count = 0;

    try (InputStream inputStream = new BufferedInputStream(file.getInputStream());
        InputStreamReader reader = new InputStreamReader(inputStream);
        CSVReader csvReader = new CSVReader(reader)) {

      String[] headers = csvReader.readNext();
      if (headers == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV file is empty");
      }

      validateHeaders(headers);

      String[] row;
      while ((row = csvReader.readNext()) != null) {
        if (row.length < 3) {
          throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST,
              String.format(
                  "CSV row does not have enough columns. Expected at least 3, got %d", row.length));
        }

        CourseStaff courseStaff =
            CourseStaff.builder()
                .firstName(row[0].trim())
                .lastName(row[1].trim())
                .email(row[2].trim())
                .course(course)
                .build();

        if (course.getInstallationId() != null) {
          courseStaff.setOrgStatus(OrgStatus.JOINCOURSE);
        } else {
          courseStaff.setOrgStatus(OrgStatus.PENDING);
        }

        CourseStaff savedCourseStaff = courseStaffRepository.save(courseStaff);
        updateUserService.attachUserToCourseStaff(savedCourseStaff);
        count++;
      }
    }

    return Map.of("count", count);
  }

  static void validateHeaders(String[] headers) {
    String[] expectedHeaders = STAFF_CSV_HEADERS.split(",");
    if (headers.length < expectedHeaders.length) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          String.format(
              "CSV headers do not match expected format. Expected: %s", STAFF_CSV_HEADERS));
    }
    for (int i = 0; i < expectedHeaders.length; i++) {
      if (!expectedHeaders[i].trim().equalsIgnoreCase(headers[i].trim())) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            String.format(
                "CSV headers do not match expected format. Expected: %s", STAFF_CSV_HEADERS));
      }
    }
  }
}
