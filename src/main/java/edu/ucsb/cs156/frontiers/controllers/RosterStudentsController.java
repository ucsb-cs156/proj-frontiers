package edu.ucsb.cs156.frontiers.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.Job;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.enums.RosterStatus;
import edu.ucsb.cs156.frontiers.errors.EntityNotFoundException;
import edu.ucsb.cs156.frontiers.errors.NoLinkedOrganizationException;
import edu.ucsb.cs156.frontiers.jobs.UpdateOrgMembershipJob;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.services.CurrentUserService;
import edu.ucsb.cs156.frontiers.services.OrganizationMemberService;
import edu.ucsb.cs156.frontiers.services.UpdateUserService;
import edu.ucsb.cs156.frontiers.services.jobs.JobService;
import edu.ucsb.cs156.frontiers.utilities.CanonicalFormConverter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "RosterStudents")
@RequestMapping("/api/rosterstudents")
@RestController
@Slf4j
public class RosterStudentsController extends ApiController {

  @Autowired private JobService jobService;
  @Autowired private OrganizationMemberService organizationMemberService;

  public enum InsertStatus {
    INSERTED,
    UPDATED
  };

  @Autowired private RosterStudentRepository rosterStudentRepository;

  @Autowired private CourseRepository courseRepository;

  @Autowired private UpdateUserService updateUserService;

  @Autowired private CurrentUserService currentUserService;

  public enum RosterSourceType {
    UCSB_EGRADES,
    CHICO_CANVAS,
    UNKNOWN
  }

  public static final String UCSB_EGRADES_HEADERS =
      "Enrl Cd,Perm #,Grade,Final Units,Student Last,Student First Middle,Quarter,Course ID,Section,Meeting Time(s) / Location(s),Email,ClassLevel,Major1,Major2,Date/Time,Pronoun";
  public static final String CHICO_CANVAS_HEADERS =
      "Student Name,Student ID,Student SIS ID,Email,Section Name";

  public static RosterSourceType getRosterSourceType(String[] headers) {

    Map<RosterSourceType, String[]> sourceTypeToHeaders = new HashMap<>();

    sourceTypeToHeaders.put(RosterSourceType.UCSB_EGRADES, UCSB_EGRADES_HEADERS.split(","));
    sourceTypeToHeaders.put(RosterSourceType.CHICO_CANVAS, CHICO_CANVAS_HEADERS.split(","));

    for (Map.Entry<RosterSourceType, String[]> entry : sourceTypeToHeaders.entrySet()) {
      RosterSourceType type = entry.getKey();
      String[] expectedHeaders = entry.getValue();
      if (headers.length >= expectedHeaders.length) {
        boolean matches = true;
        for (int i = 0; i < expectedHeaders.length; i++) {
          if (!expectedHeaders[i].equalsIgnoreCase(headers[i])) {
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

  public static record UpsertResponse(InsertStatus insertStatus, RosterStudent rosterStudent) {}

  /**
   * This method creates a new RosterStudent. It is important to keep the code in this method
   * consistent with the code for adding multiple roster students from a CSV
   *
   * @return the created RosterStudent
   */
  @Operation(summary = "Create a new roster student")
  @PreAuthorize("hasRole('ROLE_INSTRUCTOR')")
  @PostMapping("/post")
  public UpsertResponse postRosterStudent(
      @Parameter(name = "studentId") @RequestParam String studentId,
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

    RosterStudent rosterStudent =
        RosterStudent.builder()
            .studentId(studentId)
            .firstName(firstName)
            .lastName(lastName)
            .email(email)
            .build();

    UpsertResponse upsertResponse = upsertStudent(rosterStudent, course, RosterStatus.MANUAL);
    return upsertResponse;
  }

  /**
   * This method returns a list of roster students for a given course.
   *
   * @return a list of all courses.
   */
  @Operation(summary = "List all roster students for a course")
  @PreAuthorize("hasRole('ROLE_INSTRUCTOR')")
  @GetMapping("/course/{courseId}")
  public Iterable<RosterStudent> rosterStudentForCourse(
      @Parameter(name = "courseId") @PathVariable Long courseId) throws EntityNotFoundException {
    courseRepository
        .findById(courseId)
        .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));
    Iterable<RosterStudent> rosterStudents = rosterStudentRepository.findByCourseId(courseId);
    return rosterStudents;
  }

  /**
   * Upload Roster students for Course in UCSB Egrades Format It is important to keep the code in
   * this method consistent with the code for adding a single roster student
   *
   * @param courseId
   * @param file
   * @return
   * @throws JsonProcessingException
   * @throws IOException
   * @throws CsvException
   */
  @Operation(summary = "Upload Roster students for Course in UCSB Egrades Format")
  @PreAuthorize("hasRole('ROLE_INSTRUCTOR')")
  @PostMapping(
      value = "/upload/csv",
      consumes = {"multipart/form-data"})
  public Map<String, String> uploadRosterStudentsCSV(
      @Parameter(name = "courseId") @RequestParam Long courseId,
      @Parameter(name = "file") @RequestParam("file") MultipartFile file)
      throws JsonProcessingException, IOException, CsvException {

    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId.toString()));

    int counts[] = {0, 0};

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
        UpsertResponse upsertResponse = upsertStudent(rosterStudent, course, RosterStatus.ROSTER);
        InsertStatus s = upsertResponse.insertStatus;
        counts[s.ordinal()]++;
      }
    }
    return Map.of(
        "filename", file.getOriginalFilename(),
        "message",
            String.format(
                "Inserted %d new students, Updated %d students",
                counts[InsertStatus.INSERTED.ordinal()], counts[InsertStatus.UPDATED.ordinal()]));
  }

  public static RosterStudent fromCSVRow(String[] row, RosterSourceType sourceType) {
    if (sourceType == RosterSourceType.UCSB_EGRADES) {
      return fromUCSBEgradesCSVRow(row);
    } else if (sourceType == RosterSourceType.CHICO_CANVAS) {
      return fromChicoCanvasCSVRow(row);
    } else {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV format not recognized");
    }
  }

  public static RosterStudent fromUCSBEgradesCSVRow(String[] row) {
    return RosterStudent.builder()
        .firstName(row[5])
        .lastName(row[4])
        .studentId(row[1])
        .email(row[10])
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

  public static RosterStudent fromChicoCanvasCSVRow(String[] row) {
    return RosterStudent.builder()
        .firstName(getFirstName(row[0]))
        .lastName(getLastName(row[0]))
        .studentId(row[2])
        .email(row[3])
        .build();
  }

  public UpsertResponse upsertStudent(
      RosterStudent student, Course course, RosterStatus rosterStatus) {
    String convertedEmail = CanonicalFormConverter.convertToValidEmail(student.getEmail());
    Optional<RosterStudent> existingStudent =
        rosterStudentRepository.findByCourseIdAndStudentId(course.getId(), student.getStudentId());
    Optional<RosterStudent> existingStudentByEmail =
        rosterStudentRepository.findByCourseIdAndEmail(course.getId(), convertedEmail);
    if (existingStudent.isPresent() || existingStudentByEmail.isPresent()) {
      RosterStudent existingStudentObj =
          existingStudent.isPresent() ? existingStudent.get() : existingStudentByEmail.get();
      existingStudentObj.setRosterStatus(rosterStatus);
      existingStudentObj.setFirstName(student.getFirstName());
      existingStudentObj.setLastName(student.getLastName());
      existingStudentObj.setEmail(convertedEmail);
      existingStudentObj.setStudentId(student.getStudentId());
      existingStudentObj = rosterStudentRepository.save(existingStudentObj);
      updateUserService.attachUserToRosterStudent(existingStudentObj);
      return new UpsertResponse(InsertStatus.UPDATED, existingStudentObj);
    } else {
      student.setCourse(course);
      student.setEmail(convertedEmail);
      student.setRosterStatus(rosterStatus);
      // if an installationID exists, orgStatus should be set to JOINCOURSE. if it doesn't exist
      // (null), set orgStatus to PENDING.
      if (course.getInstallationId() != null) {
        student.setOrgStatus(OrgStatus.JOINCOURSE);
      } else {
        student.setOrgStatus(OrgStatus.PENDING);
      }
      student = rosterStudentRepository.save(student);
      updateUserService.attachUserToRosterStudent(student);
      return new UpsertResponse(InsertStatus.INSERTED, student);
    }
  }

  @PreAuthorize("hasRole('ROLE_INSTRUCTOR')")
  @PostMapping("/updateCourseMembership")
  public Job updateCourseMembership(
      @Parameter(name = "courseId", description = "Course ID") @RequestParam Long courseId)
      throws NoSuchAlgorithmException, InvalidKeySpecException, JsonProcessingException {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));
    if (course.getInstallationId() == null || course.getOrgName() == null) {
      throw new NoLinkedOrganizationException(course.getCourseName());
    } else {
      UpdateOrgMembershipJob job =
          UpdateOrgMembershipJob.builder()
              .rosterStudentRepository(rosterStudentRepository)
              .organizationMemberService(organizationMemberService)
              .course(course)
              .build();

      return jobService.runAsJob(job);
    }
  }

  @Operation(
      summary =
          "Allow roster student to join a course by generating an invitation to the linked Github Org")
  @PreAuthorize("hasRole('ROLE_USER')")
  @PutMapping("/joinCourse")
  public ResponseEntity<String> joinCourseOnGitHub(
      @Parameter(
              name = "rosterStudentId",
              description = "Roster Student joining a course on GitHub")
          @RequestParam
          Long rosterStudentId)
      throws NoSuchAlgorithmException, InvalidKeySpecException, JsonProcessingException {

    User currentUser = currentUserService.getUser();
    RosterStudent rosterStudent =
        rosterStudentRepository
            .findById(rosterStudentId)
            .orElseThrow(() -> new EntityNotFoundException(RosterStudent.class, rosterStudentId));

    if (rosterStudent.getUser() == null || currentUser.getId() != rosterStudent.getUser().getId()) {
      throw new AccessDeniedException("User not authorized join the course as this roster student");
    }

    if (rosterStudent.getGithubId() != null
        && rosterStudent.getGithubLogin() != null
        && (rosterStudent.getOrgStatus() == OrgStatus.MEMBER
            || rosterStudent.getOrgStatus() == OrgStatus.OWNER)) {
      return ResponseEntity.badRequest()
          .body("This user has already linked a Github account to this course.");
    }

    if (rosterStudent.getCourse().getOrgName() == null
        || rosterStudent.getCourse().getInstallationId() == null) {
      return ResponseEntity.badRequest()
          .body("Course has not been set up. Please ask your instructor for help.");
    }
    rosterStudent.setGithubId(currentUser.getGithubId());
    rosterStudent.setGithubLogin(currentUser.getGithubLogin());
    OrgStatus status = organizationMemberService.inviteOrganizationMember(rosterStudent);
    rosterStudent.setOrgStatus(status);
    rosterStudentRepository.save(rosterStudent);
    if (status == OrgStatus.INVITED) {
      return ResponseEntity.accepted().body("Successfully invited student to Organization");
    } else if (status == OrgStatus.MEMBER || status == OrgStatus.OWNER) {
      return ResponseEntity.accepted()
          .body("Already in organization - set status to %s".formatted(status.toString()));
    } else {
      return ResponseEntity.internalServerError().body("Could not invite student to Organization");
    }
  }

  @Operation(summary = "Get Associated Roster Students with a User")
  @PreAuthorize("hasRole('ROLE_USER')")
  @GetMapping("/associatedRosterStudents")
  public Iterable<RosterStudent> getAssociatedRosterStudents() {
    User currentUser = currentUserService.getUser();
    Iterable<RosterStudent> rosterStudents = rosterStudentRepository.findAllByUser((currentUser));
    return rosterStudents;
  }

  @Operation(summary = "Update a roster student")
  @PreAuthorize("hasRole('ROLE_INSTRUCTOR')")
  @PutMapping("/update")
  public RosterStudent updateRosterStudent(
      @Parameter(name = "id") @RequestParam Long id,
      @Parameter(name = "firstName") @RequestParam(required = false) String firstName,
      @Parameter(name = "lastName") @RequestParam(required = false) String lastName,
      @Parameter(name = "studentId") @RequestParam(required = false) String studentId)
      throws EntityNotFoundException {

    if (firstName == null
        || lastName == null
        || studentId == null
        || firstName.trim().isEmpty()
        || lastName.trim().isEmpty()
        || studentId.trim().isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Required fields cannot be empty");
    }

    RosterStudent rosterStudent =
        rosterStudentRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException(RosterStudent.class, id));

    if (!rosterStudent.getStudentId().trim().equals(studentId.trim())) {
      Optional<RosterStudent> existingStudent =
          rosterStudentRepository.findByCourseIdAndStudentId(
              rosterStudent.getCourse().getId(), studentId.trim());
      if (existingStudent.isPresent()) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Student ID already exists in this course");
      }
    }

    rosterStudent.setFirstName(firstName.trim());
    rosterStudent.setLastName(lastName.trim());
    rosterStudent.setStudentId(studentId.trim());

    return rosterStudentRepository.save(rosterStudent);
  }

  @Operation(summary = "Delete a roster student")
  @PreAuthorize("hasRole('ROLE_INSTRUCTOR')")
  @DeleteMapping("/delete")
  @Transactional
  public ResponseEntity<String> deleteRosterStudent(@Parameter(name = "id") @RequestParam Long id)
      throws EntityNotFoundException {
    RosterStudent rosterStudent =
        rosterStudentRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException(RosterStudent.class, id));
    Course course = rosterStudent.getCourse();

    boolean orgRemovalAttempted = false;
    boolean orgRemovalSuccessful = false;
    String orgRemovalErrorMessage = null;

    // Try to remove the student from the organization if they have a GitHub login
    if (rosterStudent.getGithubLogin() != null
        && course.getOrgName() != null
        && course.getInstallationId() != null) {
      orgRemovalAttempted = true;
      try {
        organizationMemberService.removeOrganizationMember(rosterStudent);
        orgRemovalSuccessful = true;
      } catch (Exception e) {
        log.error("Error removing student from organization: {}", e.getMessage());
        orgRemovalErrorMessage = e.getMessage();
        // Continue with deletion even if organization removal fails
      }
    }

    course.getRosterStudents().remove(rosterStudent);
    rosterStudentRepository.delete(rosterStudent);
    courseRepository.save(course);

    if (!orgRemovalAttempted) {
      return ResponseEntity.ok(
          "Successfully deleted roster student and removed him/her from the course list");
    } else if (orgRemovalSuccessful) {
      return ResponseEntity.ok(
          "Successfully deleted roster student and removed him/her from the course list and organization");
    } else {
      return ResponseEntity.ok(
          "Successfully deleted roster student but there was an error removing them from the course organization: "
              + orgRemovalErrorMessage);
    }
  }
}
