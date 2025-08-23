package edu.ucsb.cs156.frontiers.security;

import edu.ucsb.cs156.frontiers.config.SecurityConfig;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.errors.EntityNotFoundException;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.prepost.PreAuthorize;

@TestComponent
public class DummyCourseSecurity {

  @Autowired CourseRepository courseRepository;

  @Autowired RosterStudentRepository rosterStudentRepository;

  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  public Course loadCourse(Long courseId) {
    /*
    This method simply exists to add the preauthorization annotation so that the method can be tested directly.
     */
    return courseRepository
        .findById(courseId)
        .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));
  }

  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  public boolean nullTest(Long courseId) {
    if (courseRepository.findById(courseId).isEmpty()) {
      return true;
    } else {
      return false;
    }
  }

  @PreAuthorize("@CourseSecurity.hasInstructorPermissions(#root, #courseId)")
  public Course loadCourseInstructor(Long courseId) {
    /*
    This method simply exists to add the preauthorization annotation so that the method can be tested directly.
     */
    return courseRepository
        .findById(courseId)
        .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));
  }

  @PreAuthorize("@CourseSecurity.hasInstructorPermissions(#root, #courseId)")
  public boolean nullTestInstructor(Long courseId) {
    if (courseRepository.findById(courseId).isEmpty()) {
      return true;
    } else {
      return false;
    }
  }

  @PreAuthorize("@CourseSecurity.hasRosterStudentManagementPermissions(#root, #id)")
  public RosterStudent loadRosterStudent(Long id) {
    return rosterStudentRepository
        .findById(id)
        .orElseThrow(() -> new EntityNotFoundException(RosterStudent.class, id));
  }

  @Bean
  public static RoleHierarchy loadedRoleHierarchy() {
    return SecurityConfig.roleHierarchy();
  }
}
