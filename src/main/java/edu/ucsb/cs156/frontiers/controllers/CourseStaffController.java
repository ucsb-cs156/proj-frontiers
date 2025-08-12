package edu.ucsb.cs156.frontiers.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import edu.ucsb.cs156.frontiers.entities.*;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.errors.EntityNotFoundException;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseStaffRepository;
import edu.ucsb.cs156.frontiers.services.*;
import edu.ucsb.cs156.frontiers.services.jobs.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "CourseStaff")
@RequestMapping("/api/coursestaff")
@RestController
@Slf4j
public class CourseStaffController extends ApiController {

  @Autowired private JobService jobService;
  @Autowired private OrganizationMemberService organizationMemberService;

  @Autowired private CourseStaffRepository courseStaffRepository;

  @Autowired private CourseRepository courseRepository;

  @Autowired private UpdateUserService updateUserService;

  @Autowired private CurrentUserService currentUserService;

  /**
   * This method creates a new CourseStaff.
   *
   * @return the created CourseStaff
   */
  @Operation(summary = "Create a new course staff")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
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

    CourseStaff courseStaff =
        CourseStaff.builder()
            .firstName(firstName)
            .lastName(lastName)
            .email(email)
            .course(course)
            .build();

    if (course.getInstallationId() != null) {
      courseStaff.setOrgStatus(OrgStatus.JOINCOURSE);
    } else {
      courseStaff.setOrgStatus(OrgStatus.PENDING);
    }

    CourseStaff savedCourseStaff = courseStaffRepository.save(courseStaff);

    updateUserService.attachUserToCourseStaff(savedCourseStaff);

    return savedCourseStaff;
  }

  /**
   * This method returns a list of course staff for a given course.
   *
   * @return a list of all courses.
   */
  @Operation(summary = "List all course staff for a course")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
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
}
