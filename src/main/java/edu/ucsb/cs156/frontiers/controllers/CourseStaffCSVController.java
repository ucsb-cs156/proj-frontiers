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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "CourseStaff")
@RequestMapping("/api/coursestaff")
@RestController
@Slf4j
public class CourseStaffCSVController extends ApiController {

  @Autowired private CourseStaffRepository courseStaffRepository;

  @Autowired private CourseRepository courseRepository;

  @Autowired private UpdateUserService updateUserService;

  /**
   * Upload staff for a Course from a CSV file. Each row should contain firstName, lastName, and
   * email.
   *
   * @param courseId the ID of the course
   * @param file the CSV file to upload
   * @return
   * @throws IOException
   * @throws CsvException
   */
  @Operation(summary = "Upload Staff CSV for a course")
  @PreAuthorize("@CourseSecurity.hasInstructorPermissions(#root, #courseId)")
  @PostMapping(
      value = "/upload/csv",
      consumes = {"multipart/form-data"})
  public ResponseEntity<Object> uploadStaffCSV(
      @Parameter(name = "courseId") @RequestParam Long courseId,
      @Parameter(name = "file") @RequestParam("file") MultipartFile file)
      throws IOException, CsvException {

    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId.toString()));

    int inserted = 0;

    try (InputStream inputStream = new BufferedInputStream(file.getInputStream());
        InputStreamReader reader = new InputStreamReader(inputStream);
        CSVReader csvReader = new CSVReader(reader); ) {

      csvReader.readNext(); // skip header row

      for (String[] row : csvReader.readAll()) {
        String email = row[2].trim();

        if (courseStaffRepository.findByEmailAndCourse(email, course).isPresent()) {
          continue;
        }

        CourseStaff staff =
            CourseStaff.builder()
                .firstName(row[0].trim())
                .lastName(row[1].trim())
                .email(row[2].trim())
                .course(course)
                .build();

        if (course.getInstallationId() != null) {
          staff.setOrgStatus(OrgStatus.JOINCOURSE);
        } else {
          staff.setOrgStatus(OrgStatus.PENDING);
        }

        CourseStaff saved = courseStaffRepository.save(staff);
        updateUserService.attachUserToCourseStaff(saved);
        inserted++;
      }
    }

    return ResponseEntity.ok(Map.of("inserted", inserted));
  }
}
