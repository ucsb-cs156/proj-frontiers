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
  @PreAuthorize("@CourseSecurity.hasStaffPermissions(#root, #courseId)")
  @PostMapping("")
  public Assignment postAssignment(
      @Parameter(name = "courseId") @RequestParam Long courseId,
      @Parameter(name = "name") @RequestParam String name,
      @Parameter(name = "asn_type") @RequestParam String asn_type,
      @Parameter(name = "visibility") @RequestParam String visibility,
      @Parameter(name = "permission") @RequestParam String permission) {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));

    Assignment assignment =
        Assignment.builder()
            .course(course)
            .name(name)
            .asn_type(asn_type)
            .visibility(visibility)
            .permission(permission)
            .build();

    Assignment savedAssignment = assignmentRepository.save(assignment);
    return savedAssignment;
  }
}
