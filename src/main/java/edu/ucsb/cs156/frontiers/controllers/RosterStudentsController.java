package edu.ucsb.cs156.frontiers.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.Job;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.enums.InsertStatus;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.enums.RosterStatus;
import edu.ucsb.cs156.frontiers.errors.EntityNotFoundException;
import edu.ucsb.cs156.frontiers.errors.NoLinkedOrganizationException;
import edu.ucsb.cs156.frontiers.jobs.UpdateOrgMembershipJob;
import edu.ucsb.cs156.frontiers.models.RosterStudentDTO;
import edu.ucsb.cs156.frontiers.models.UpsertResponse;
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
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
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
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "RosterStudents")
@RequestMapping("/api/rosterstudents")
@RestController
@Slf4j
public class RosterStudentsController extends ApiController {

  @Autowired private JobService jobService;
  @Autowired private OrganizationMemberService organizationMemberService;

  @Autowired private RosterStudentRepository rosterStudentRepository;

  @Autowired private CourseRepository courseRepository;

  @Autowired private UpdateUserService updateUserService;

  @Autowired private CurrentUserService currentUserService;

  /**
   * This method creates a new RosterStudent. It is important to keep the code in this method
   * consistent with the code for adding multiple roster students from a CSV
   *
   * @return the created RosterStudent
   */
  @Operation(summary = "Create a new roster student")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @PostMapping("/post")
  public ResponseEntity<UpsertResponse> postRosterStudent(
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
    if (upsertResponse.getInsertStatus() == InsertStatus.REJECTED) {
      return ResponseEntity.status(HttpStatus.CONFLICT).body(upsertResponse);
    } else {
      rosterStudent = rosterStudentRepository.save(upsertResponse.rosterStudent());
      updateUserService.attachUserToRosterStudent(rosterStudent);
      return ResponseEntity.ok(upsertResponse);
    }
  }

  /**
   * This method returns a list of roster students for a given course.
   *
   * @return a list of all courses.
   */
  @Operation(summary = "List all roster students for a course")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @GetMapping("/course/{courseId}")
  public Iterable<RosterStudentDTO> rosterStudentForCourse(
      @Parameter(name = "courseId") @PathVariable Long courseId) throws EntityNotFoundException {
    courseRepository
        .findById(courseId)
        .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));
    Iterable<RosterStudent> rosterStudents = rosterStudentRepository.findByCourseId(courseId);
    Iterable<RosterStudentDTO> rosterStudentDTOs =
        () ->
            java.util.stream.StreamSupport.stream(rosterStudents.spliterator(), false)
                .map(RosterStudentDTO::new)
                .sorted(
                    java.util.Comparator.comparing(RosterStudentDTO::lastName)
                        .thenComparing(RosterStudentDTO::firstName))
                .iterator();
    return rosterStudentDTOs;
  }

  public static UpsertResponse upsertStudent(
      RosterStudent student, Course course, RosterStatus rosterStatus) {
    String convertedEmail = CanonicalFormConverter.convertToValidEmail(student.getEmail());
    Optional<RosterStudent> existingStudent =
        course.getRosterStudents().stream()
            .filter(
                filteringStudent -> student.getStudentId().equals(filteringStudent.getStudentId()))
            .findFirst();
    Optional<RosterStudent> existingStudentByEmail =
        course.getRosterStudents().stream()
            .filter(filteringStudent -> convertedEmail.equals(filteringStudent.getEmail()))
            .findFirst();
    if (existingStudent.isPresent() && existingStudentByEmail.isPresent()) {
      if (existingStudent.get().getId().equals(existingStudentByEmail.get().getId())) {
        RosterStudent existingStudentObj = existingStudent.get();
        existingStudentObj.setRosterStatus(rosterStatus);
        existingStudentObj.setFirstName(student.getFirstName());
        existingStudentObj.setLastName(student.getLastName());
        existingStudentObj.setSection(student.getSection());
        return new UpsertResponse(InsertStatus.UPDATED, existingStudentObj);
      } else {
        return new UpsertResponse(InsertStatus.REJECTED, student);
      }
    } else if (existingStudent.isPresent() || existingStudentByEmail.isPresent()) {
      RosterStudent existingStudentObj =
          existingStudent.isPresent() ? existingStudent.get() : existingStudentByEmail.get();
      existingStudentObj.setRosterStatus(rosterStatus);
      existingStudentObj.setFirstName(student.getFirstName());
      existingStudentObj.setLastName(student.getLastName());
      existingStudentObj.setSection(student.getSection());
      existingStudentObj.setEmail(convertedEmail);
      existingStudentObj.setStudentId(student.getStudentId());
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
      return new UpsertResponse(InsertStatus.INSERTED, student);
    }
  }

  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
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

    if (rosterStudent.getRosterStatus() == RosterStatus.DROPPED) {
      throw new AccessDeniedException(
          "You have dropped this course. Please contact your instructor.");
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
  @PreAuthorize("@CourseSecurity.hasRosterStudentManagementPermissions(#root, #id)")
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

  @Operation(
      summary = "Restore a roster student",
      description = "Makes a student who previously dropped the course able to join and interact")
  @PreAuthorize("@CourseSecurity.hasRosterStudentManagementPermissions(#root, #id)")
  @PutMapping("/restore")
  public RosterStudent restoreRosterStudent(@Parameter(name = "id") @RequestParam Long id)
      throws EntityNotFoundException {
    RosterStudent rosterStudent =
        rosterStudentRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException(RosterStudent.class, id));
    rosterStudent.setRosterStatus(RosterStatus.MANUAL);
    return rosterStudentRepository.save(rosterStudent);
  }

  @Operation(summary = "Delete a roster student")
  @PreAuthorize("@CourseSecurity.hasRosterStudentManagementPermissions(#root, #id)")
  @DeleteMapping("/delete")
  @Transactional
  public ResponseEntity<String> deleteRosterStudent(
      @Parameter(name = "id") @RequestParam Long id,
      @Parameter(
              name = "removeFromOrg",
              description = "Whether to remove student from GitHub organization")
          @RequestParam(defaultValue = "true")
          boolean removeFromOrg)
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
    // and removeFromOrg parameter is true
    if (removeFromOrg
        && rosterStudent.getGithubLogin() != null
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

    if (!rosterStudent.getTeamMembers().isEmpty()) {
      rosterStudent
          .getTeamMembers()
          .forEach(
              teamMember -> {
                teamMember.getTeam().getTeamMembers().remove(teamMember);
                teamMember.setTeam(null);
              });
    }

    rosterStudent.getCourse().getRosterStudents().remove(rosterStudent);
    rosterStudent.setCourse(null);
    rosterStudentRepository.delete(rosterStudent);

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
