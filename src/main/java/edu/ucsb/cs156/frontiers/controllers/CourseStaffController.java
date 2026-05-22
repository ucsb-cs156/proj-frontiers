package edu.ucsb.cs156.frontiers.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import edu.ucsb.cs156.frontiers.entities.*;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.errors.EntityNotFoundException;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseStaffRepository;
import edu.ucsb.cs156.frontiers.services.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "CourseStaff")
@RequestMapping("/api/coursestaff")
@RestController
@Slf4j
public class CourseStaffController extends ApiController {

  @Autowired private OrganizationMemberService organizationMemberService;

  @Autowired private CourseStaffRepository courseStaffRepository;

  @Autowired private CourseRepository courseRepository;

  @Autowired private UpdateUserService updateUserService;

  @Autowired private CurrentUserService currentUserService;

  public static final String STAFF_CSV_HEADERS = "firstName,lastName,email";

  private CourseStaff buildCourseStaffForCourse(
      Course course, String firstName, String lastName, String email) {
    CourseStaff courseStaff =
        CourseStaff.builder()
            .firstName(firstName)
            .lastName(lastName)
            .email(email.strip())
            .course(course)
            .build();

    if (course.getInstallationId() != null) {
      courseStaff.setOrgStatus(OrgStatus.JOINCOURSE);
    } else {
      courseStaff.setOrgStatus(OrgStatus.PENDING);
    }

    return courseStaff;
  }

  public static boolean hasStaffCSVHeaders(String[] headers) {
    String[] expectedHeaders = STAFF_CSV_HEADERS.split(",");

    if (headers == null || headers.length < expectedHeaders.length) {
      return false;
    }

    for (int i = 0; i < expectedHeaders.length; i++) {
      if (!expectedHeaders[i].trim().equalsIgnoreCase(headers[i].trim())) {
        return false;
      }
    }

    return true;
  }

  public static CourseStaff fromStaffCSVRow(String[] row) {
    if (row.length < 3) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          String.format(
              "Staff CSV row does not have enough columns. Length = %d Row content = [%s]",
              row.length, Arrays.toString(row)));
    }

    return CourseStaff.builder()
        .firstName(row[0].strip())
        .lastName(row[1].strip())
        .email(row[2].strip())
        .build();
  }

  /**
   * This method creates a new CourseStaff.
   *
   * @return the created CourseStaff
   */
  @Operation(summary = "Add a staff member to a course")
  @PreAuthorize("@CourseSecurity.hasInstructorPermissions(#root, #courseId)")
  @PostMapping("/post")
  public CourseStaff postCourseStaff(
      @Parameter(name = "firstName") @RequestParam String firstName,
      @Parameter(name = "lastName") @RequestParam String lastName,
      @Parameter(name = "email") @RequestParam String email,
      @Parameter(name = "courseId") @RequestParam Long courseId)
      throws EntityNotFoundException {

    // Get Course or else throw an error

    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));

    CourseStaff courseStaff = buildCourseStaffForCourse(course, firstName, lastName, email);

    CourseStaff savedCourseStaff = courseStaffRepository.save(courseStaff);

    updateUserService.attachUserToCourseStaff(savedCourseStaff);

    return savedCourseStaff;
  }

  @Operation(summary = "Upload course staff from CSV")
  @PreAuthorize("@CourseSecurity.hasInstructorPermissions(#root, #courseId)")
  @PostMapping(
      value = "/upload/csv",
      consumes = {"multipart/form-data"})
  public ResponseEntity<List<CourseStaff>> uploadCourseStaffCSV(
      @Parameter(name = "courseId") @RequestParam Long courseId,
      @Parameter(name = "file") @RequestParam("file") MultipartFile file)
      throws IOException, CsvException {

    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));

    List<CourseStaff> courseStaff;
    try (InputStream inputStream = new BufferedInputStream(file.getInputStream());
        InputStreamReader reader = new InputStreamReader(inputStream);
        CSVReader csvReader = new CSVReader(reader); ) {

      String[] headers = csvReader.readNext();
      if (!hasStaffCSVHeaders(headers)) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Unknown Course Staff CSV format");
      }

      courseStaff =
          csvReader.readAll().stream()
              .map(CourseStaffController::fromStaffCSVRow)
              .map(
                  staff ->
                      buildCourseStaffForCourse(
                          course, staff.getFirstName(), staff.getLastName(), staff.getEmail()))
              .toList();
    }

    List<CourseStaff> savedCourseStaff = courseStaffRepository.saveAll(courseStaff);
    savedCourseStaff.forEach(updateUserService::attachUserToCourseStaff);

    return ResponseEntity.ok(savedCourseStaff);
  }

  /**
   * This method returns a list of course staff for a given course.
   *
   * @return a list of all courses.
   */
  @Operation(summary = "List all course staff members for a course")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @GetMapping("/course")
  public Iterable<CourseStaff> courseStaffForCourse(
      @Parameter(name = "courseId") @RequestParam Long courseId) throws EntityNotFoundException {
    courseRepository
        .findById(courseId)
        .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));
    Iterable<CourseStaff> courseStaffs = courseStaffRepository.findByCourseId(courseId);
    return courseStaffs;
  }

  @Operation(
      summary =
          "Allow staff member to join a course by generating an invitation to the linked Github Org")
  @PreAuthorize("hasRole('ROLE_USER')")
  @PutMapping("/joinCourse")
  public ResponseEntity<String> joinCourseOnGitHub(
      @Parameter(name = "courseStaffId", description = "Staff Member joining a course on GitHub")
          @RequestParam
          Long courseStaffId)
      throws NoSuchAlgorithmException, InvalidKeySpecException, JsonProcessingException {

    User currentUser = currentUserService.getUser();
    CourseStaff courseStaff =
        courseStaffRepository
            .findById(courseStaffId)
            .orElseThrow(() -> new EntityNotFoundException(CourseStaff.class, courseStaffId));

    if (courseStaff.getUser() == null || currentUser.getId() != courseStaff.getUser().getId()) {
      throw new IllegalArgumentException(
          String.format(
              "This operation is restricted to the user associated with staff member with id %d",
              courseStaff.getId()));
    }

    if (courseStaff.getGithubId() != null
        && courseStaff.getGithubLogin() != null
        && (courseStaff.getOrgStatus() == OrgStatus.MEMBER
            || courseStaff.getOrgStatus() == OrgStatus.OWNER)) {
      return ResponseEntity.badRequest()
          .body("You have already linked a Github account to this course.");
    }

    if (courseStaff.getCourse().getOrgName() == null
        || courseStaff.getCourse().getInstallationId() == null) {
      return ResponseEntity.badRequest()
          .body("Course has not been set up. Please ask your instructor for help.");
    }
    courseStaff.setGithubId(currentUser.getGithubId());
    courseStaff.setGithubLogin(currentUser.getGithubLogin());
    OrgStatus status = organizationMemberService.inviteOrganizationOwner(courseStaff);
    courseStaff.setOrgStatus(status);
    courseStaffRepository.save(courseStaff);
    if (status == OrgStatus.INVITED) {
      return ResponseEntity.accepted().body("Successfully invited staff member to Organization");
    } else if (status == OrgStatus.MEMBER || status == OrgStatus.OWNER) {
      return ResponseEntity.accepted()
          .body("Already in organization - set status to %s".formatted(status.toString()));
    } else {
      return ResponseEntity.internalServerError()
          .body("Could not invite staff member to Organization");
    }
  }

  @Operation(summary = "Update a staff member")
  @PreAuthorize("@CourseSecurity.hasInstructorPermissions(#root, #courseId)")
  @PutMapping("")
  public CourseStaff updateStaffMember(
      @Parameter(name = "courseId") @RequestParam Long courseId,
      @Parameter(name = "id") @RequestParam Long id,
      @Parameter(name = "firstName") @RequestParam String firstName,
      @Parameter(name = "lastName") @RequestParam String lastName)
      throws EntityNotFoundException {

    CourseStaff staffMember =
        courseStaffRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException(CourseStaff.class, id));

    staffMember.setFirstName(firstName.trim());
    staffMember.setLastName(lastName.trim());
    return courseStaffRepository.save(staffMember);
  }

  @Operation(summary = "Delete a staff member")
  @PreAuthorize("@CourseSecurity.hasInstructorPermissions(#root, #courseId)")
  @DeleteMapping("/delete")
  @Transactional
  public ResponseEntity<String> deleteStaffMember(
      @Parameter(name = "id") @RequestParam Long id,
      @Parameter(name = "courseId") @RequestParam Long courseId,
      @Parameter(
              name = "removeFromOrg",
              description = "Whether to remove course staff from GitHub organization")
          @RequestParam(defaultValue = "false")
          boolean removeFromOrg)
      throws EntityNotFoundException {
    CourseStaff staffMember =
        courseStaffRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException(CourseStaff.class, id));
    Course course = staffMember.getCourse();

    boolean orgRemovalAttempted = false;
    boolean orgRemovalSuccessful = false;
    String orgRemovalErrorMessage = null;

    // Try to remove the course staff from the organization if they have a GitHub login
    if (staffMember.getGithubLogin() != null
        && course.getOrgName() != null
        && course.getInstallationId() != null
        && removeFromOrg) {
      orgRemovalAttempted = true;
      try {
        organizationMemberService.removeOrganizationMember(staffMember);
        orgRemovalSuccessful = true;
      } catch (Exception e) {
        log.error("Error removing course staff from organization: {}", e.getMessage());
        orgRemovalErrorMessage = e.getMessage();
        // Continue with deletion even if organization removal fails
      }
    }

    course.getCourseStaff().remove(staffMember);
    staffMember.setCourse(null);
    courseStaffRepository.delete(staffMember);

    if (!orgRemovalAttempted) {
      return ResponseEntity.ok(
          "Successfully deleted staff member and removed them from the staff roster.");
    } else if (orgRemovalSuccessful) {
      return ResponseEntity.ok(
          "Successfully deleted staff member and removed them from the staff roster and organization.");
    } else {
      return ResponseEntity.ok(
          "Successfully deleted staff member but there was an error removing them from the course organization: "
              + orgRemovalErrorMessage);
    }
  }
}
