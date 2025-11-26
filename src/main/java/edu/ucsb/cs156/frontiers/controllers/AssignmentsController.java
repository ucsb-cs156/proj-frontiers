package edu.ucsb.cs156.frontiers.controllers;

import edu.ucsb.cs156.frontiers.entities.Assignment;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.errors.EntityNotFoundException;
import edu.ucsb.cs156.frontiers.repositories.AssignmentRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Assignment")
@RequestMapping("/api/assignments")
@RestController
@Slf4j
public class AssignmentsController extends ApiController {

  @Autowired private AssignmentRepository assignmentRepository;

  @Autowired private CourseRepository courseRepository;

  /**
   * This method creates a new Assignment.
   *
   * @param courseId the id of the course
   * @param name the name of the assignment
   * @param asn_type the type of the assigment
   * @param visibility the visibility of the assignment
   * @param permission the permission on the assignment
   * @return the created assignment
   */
  @Operation(summary = "Create a new assignment")
  @PreAuthorize("hasRole('ROLE_ADMIN') || hasRole('ROLE_INSTRUCTOR')")
  @PostMapping("/post")
  public InstructorAssignmentView postAssignment(
      @Parameter(name = "courseId") @RequestParam Long courseId,
      @Parameter(name = "name") @RequestParam String name,
      @Parameter(name = "asn_type") @RequestParam String asn_type,
      @Parameter(name = "visibility") @RequestParam String visibility,
      @Parameter(name = "permission") @RequestParam String permission) {
    // get course by courseId
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));
    Assignment assignment =
        Assignment.builder()
            .course(course)
            .name(name)
            .asnType(asn_type)
            .visibility(visibility)
            .permission(permission)
            .build();
    Assignment savedAssignment = assignmentRepository.save(assignment);

    return new InstructorAssignmentView(savedAssignment);
  }

  /** Projection of Assignment entity with fields that are relevant for instructors and admins */
  public static record InstructorAssignmentView(
      Long id, Course course, String name, String asnType, String visibility, String permission) {

    // Creates view from Assignment entity
    public InstructorAssignmentView(Assignment a) {
      this(
          a.getId(),
          a.getCourse(),
          a.getName(),
          a.getAsnType(),
          a.getVisibility(),
          a.getPermission());
    }
  }

  /**
   * This method updates an existing assignment.
   *
   * @param id the id of the assignment
   * @param asn_type the new type of the assignment
   * @param visibility the new visibility of the assignment
   * @param permission the new permission on the assignment
   * @return the updated assignment
   */
  @Operation(summary = "Update an existing assignment")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #id)")
  @PutMapping("")
  public InstructorAssignmentView updateAssignment(
      @Parameter(name = "id") @RequestParam Long id,
      @Parameter(name = "asn_type") @RequestParam String asn_type,
      @Parameter(name = "visibility") @RequestParam String visibility,
      @Parameter(name = "permission") @RequestParam String permission) {

    Assignment assignment =
        assignmentRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException(Assignment.class, id));

    assignment.setAsnType(asn_type);
    assignment.setVisibility(visibility);
    assignment.setPermission(permission);

    Assignment savedAssignment = assignmentRepository.save(assignment);

    return new InstructorAssignmentView(savedAssignment);
  }
}
