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

/**
 * CourseSecurity provides methods to check permissions for managing courses and roster students. It
 * uses the CurrentUserService to get the current user and RoleHierarchy to check roles.
 *
 * <p>The methods defined here are used as annotations (e.g. <code>
 *   @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #id)")</code>) in the
 * CourseController and RosterStudentController to enforce security checks.
 *
 * <p>Note that for a method with a courseId, you <em>still</em> need to verify in each method,
 * whether the course exists or not. These annotations will <em>only</em> check whether or not the
 * particular user has access to a particular course, for example.
 *
 * <p>When testing, use <code>@WithInstructorCoursePermissions</code> or <code>
 * @WithStaffCoursePermissions</code> to mock a user with the appropriate roles.
 */
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

  /**
   * Use this when you want to check whether the user is either a staff member, instructor or admin
   * for the course.
   *
   * @param operations
   * @param courseId
   * @return true if the user has manage permissions for the course, false otherwise.
   */
  @PreAuthorize("hasRole('ROLE_USER')")
  public Boolean hasManagePermissions(
      MethodSecurityExpressionOperations operations, Long courseId) {
    Optional<Course> course = courseRepository.findById(courseId);
    if (course.isEmpty()) {
      return true;
    }
    return baseHasManagePermissions(operations, course.get());
  }

  /**
   * Use this for operations that only an instructor can do, but not a staff member, such as adding
   * or deleting a course staff member.
   *
   * @param operations
   * @param courseId
   * @return true if the user has instructor permissions for the course, false otherwise.
   */
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

  /**
   * This method checks if the current user has management permissions for the course associated
   * with the given rosterStudent. This allows us to create endpoints that just take a roster
   * student id, not a course id, and still check permissions. This one works for both staff and
   * instructor permissions.
   *
   * @param operations
   * @param rosterStudentId
   * @return
   */
  @PreAuthorize("hasRole('ROLE_USER')")
  public Boolean hasRosterStudentManagementPermissions(
      MethodSecurityExpressionOperations operations, Long rosterStudentId) {
    return rosterStudentRepository
        .findById(rosterStudentId)
        .map(rosterStudent -> baseHasManagePermissions(operations, rosterStudent.getCourse()))
        .orElse(true);
  }

  /**
   * This is a helper method that checks if the current user has management permissions for the
   * given course.
   *
   * @param operations
   * @param course
   * @return
   */
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
