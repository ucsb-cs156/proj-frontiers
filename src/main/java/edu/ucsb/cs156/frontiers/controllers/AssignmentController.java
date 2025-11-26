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

@Tag(name = "Assignments")
@RequestMapping("/api/assignments")
@RestController
@Slf4j
public class AssignmentController extends ApiController {

  @Autowired private AssignmentRepository assignmentRepository;

  @Autowired private CourseRepository courseRepository;

  /** Create a new assignment */
  @Operation(summary = "Create a new assignment")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @PostMapping("")
  public Assignment postAssignment(
      @Parameter(name = "courseId") @RequestParam Long courseId,
      @Parameter(name = "name") @RequestParam String name,
      @Parameter(name = "asn_type") @RequestParam String asn_type,
      @Parameter(name = "visibility") @RequestParam String visibility,
      @Parameter(name = "permission") @RequestParam String permission) {

    // Find the course or throw 404
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));

    // Build the Assignment object
    Assignment assignment =
        Assignment.builder()
            .course(course)
            .name(name)
            .asn_type(asn_type)
            .visibility(visibility)
            .permission(permission)
            .build();

    // Save and return
    Assignment savedAssignment = assignmentRepository.save(assignment);
    return savedAssignment;
  }

  /** Edit an assignment */
  @Operation(summary = "Edit an assignment")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @PutMapping("/{id}")
  public Assignment putAssignment(
      @Parameter(name = "id") @PathVariable Long id,
      @Parameter(name = "courseId") @RequestParam Long courseId,
      @Parameter(name = "asn_type") @RequestParam String asn_type,
      @Parameter(name = "visibility") @RequestParam String visibility,
      @Parameter(name = "permission") @RequestParam String permission) {

    // Validate that the course exists
    courseRepository
        .findById(courseId)
        .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));

    // Find the Assignment object
    Assignment assignment =
        assignmentRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException(Assignment.class, id));

    // Update Assignment object
    assignment.setAsn_type(asn_type);
    assignment.setVisibility(visibility);
    assignment.setPermission(permission);

    // Save and return
    Assignment savedAssignment = assignmentRepository.save(assignment);
    return savedAssignment;
  }

  /** Delete an an assignment */
  @Operation(summary = "Delete an assignment")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @DeleteMapping("/{id}")
  public Object deleteAssignment(
      @Parameter(name = "id") @PathVariable Long id,
      @Parameter(name = "courseId") @RequestParam Long courseId) {

    // Validate that the course exists
    courseRepository
        .findById(courseId)
        .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));

    // Find the Assignment object
    Assignment assignment =
        assignmentRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException(Assignment.class, id));

    // Delete the assignment
    assignmentRepository.delete(assignment);

    return genericMessage(String.format("Assignment with id %s deleted", id));
  }
}
