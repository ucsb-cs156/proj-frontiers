package edu.ucsb.cs156.frontiers.config;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.models.CurrentUser;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.services.CurrentUserService;
import java.util.Collection;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Slf4j
@Component("CourseSecurity")
public class CourseSecurity {
  private final CurrentUserService currentUserService;
  private final RoleHierarchy roleHierarchy;
  private final CourseRepository courseRepository;
  private final RosterStudentRepository rosterStudentRepository;

  public CourseSecurity(
      CurrentUserService currentUserService,
      RoleHierarchy roleHierarchy,
      CourseRepository courseRepository,
      RosterStudentRepository rosterStudentRepository) {
    this.currentUserService = currentUserService;
    this.roleHierarchy = roleHierarchy;
    this.courseRepository = courseRepository;
    this.rosterStudentRepository = rosterStudentRepository;
  }

  @PreAuthorize("hasRole('ROLE_USER')")
  public Boolean hasManagePermissions(
      MethodSecurityExpressionOperations operations, Long courseId) {
    Optional<Course> course = courseRepository.findById(courseId);
    if (course.isEmpty()) {
      return true;
    }
    return baseHasManagePermissions(operations, course.get());
  }

  @PreAuthorize("hasRole('ROLE_INSTRUCTOR')")
  public Boolean hasInstructorPermissions(
      MethodSecurityExpressionOperations operations, Long courseId) {
    CurrentUser currentUser = currentUserService.getCurrentUser();
    Collection<? extends GrantedAuthority> authorities =
        roleHierarchy.getReachableGrantedAuthorities(currentUser.getRoles());
    if (authorities.stream().anyMatch(role -> role.getAuthority().equals("ROLE_ADMIN"))) {
      return true;
    } else {
      Optional<Course> course = courseRepository.findById(courseId);
      if (course.isEmpty()) {
        return true;
      }
      return currentUser.getUser().getEmail().equals(course.get().getInstructorEmail());
    }
  }

  @PreAuthorize("hasRole('ROLE_USER')")
  public Boolean hasRosterStudentManagementPermissions(
      MethodSecurityExpressionOperations operations, Long rosterStudentId) {
    return rosterStudentRepository
        .findById(rosterStudentId)
        .map(rosterStudent -> baseHasManagePermissions(operations, rosterStudent.getCourse()))
        .orElse(true);
  }

  public Boolean baseHasManagePermissions(
      MethodSecurityExpressionOperations operations, Course course) {
    CurrentUser currentUser = currentUserService.getCurrentUser();
    Collection<? extends GrantedAuthority> authorities =
        roleHierarchy.getReachableGrantedAuthorities(currentUser.getRoles());
    if (authorities.stream().anyMatch(role -> role.getAuthority().equals("ROLE_ADMIN"))) {
      return true;
    } else {
      if (course.getCourseStaff().stream()
          .anyMatch(staff -> staff.getEmail().equals(currentUser.getUser().getEmail()))) {
        return true;
      }
      return currentUser.getUser().getEmail().equals(course.getInstructorEmail());
    }
  }
}
