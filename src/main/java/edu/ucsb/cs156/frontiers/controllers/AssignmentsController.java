package edu.ucsb.cs156.frontiers.controllers;

import edu.ucsb.cs156.frontiers.entities.Assignment;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.enums.AssignmentType;
import edu.ucsb.cs156.frontiers.errors.EntityNotFoundException;
import edu.ucsb.cs156.frontiers.repositories.AssignmentRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Assignments")
@RequestMapping("/api/assignments")
@RestController
@Slf4j
public class AssignmentsController extends ApiController {
  @Autowired private AssignmentRepository assignmentRepository;

  @Autowired private CourseRepository courseRepository;

  /**
   * This method creates a new Assignment.
   *
   * @param courseId the ID of the course the assignment is associated with
   * @param name the name of the assignment
   * @param asnType the assignment type (individual vs team)
   * @param visibility the visbility of the assignment (public vs private)
   * @param permission the permissions for the assignment (read, write, maintain, admin)
   * @return the created Assignment
   */
  @Operation(summary = "Create a new assignment")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @PostMapping("/post")
  public Assignment postAssignment(
      @Parameter(name = "courseId") @RequestParam Long courseId,
      @Parameter(name = "name") @RequestParam String name,
      @Parameter(name = "asnType") @RequestParam AssignmentType asnType,
      @Parameter(name = "visibility") @RequestParam
          edu.ucsb.cs156.frontiers.enums.Visibility visibility,
      @Parameter(name = "permission") @RequestParam
          edu.ucsb.cs156.frontiers.enums.Permission permission)
      throws EntityNotFoundException {

    // Get Course or else throw an error

    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));

    Assignment assignment =
        Assignment.builder()
            .course(course)
            .name(name)
            .asnType(asnType)
            .visibility(visibility)
            .permission(permission)
            .build();

    Assignment savedAssignment = assignmentRepository.save(assignment);

    return savedAssignment;
  }

  /**
   * This method updates an Assignment.
   *
   * @param assignmentId the ID of the course the assignment is associated with
   * @param name the name of the assignment
   * @param asnType the assignment type (individual vs team)
   * @param visibility the visbility of the assignment (public vs private)
   * @param permission the permissions for the assignment (read, write, maintain, admin)
   * @return the created Assignment
   */
  @Operation(summary = "Update an assignment")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @PutMapping("/put")
  public Assignment updateAssignment(
      @Parameter(name = "assignmentId") @RequestParam Long assignmentId,
      @Parameter(name = "name") @RequestParam String name,
      @Parameter(name = "asnType") @RequestParam AssignmentType asnType,
      @Parameter(name = "visibility") @RequestParam
          edu.ucsb.cs156.frontiers.enums.Visibility visibility,
      @Parameter(name = "permission") @RequestParam
          edu.ucsb.cs156.frontiers.enums.Permission permission)
      throws EntityNotFoundException {

    Assignment assignment =
        assignmentRepository
            .findById(assignmentId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, assignmentId));

    assignment.setAsnType(asnType);
    assignment.setVisibility(visibility);
    assignment.setPermission(permission);
    return assignmentRepository.save(assignment);
  }
}
