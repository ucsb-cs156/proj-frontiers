package edu.ucsb.cs156.frontiers.testconfig;

import org.springframework.boot.test.context.TestComponent;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.access.prepost.PreAuthorize;

@TestComponent("CourseSecurity")
public class TestCourseSecurity {
  @PreAuthorize(
      "(hasRole('ROLE_INSTRUCTOR') && hasAuthority('COURSE_PERMISSIONS'))|| hasRole('ROLE_ADMIN')")
  public Boolean hasManagePermissions(
      MethodSecurityExpressionOperations operations, Long courseId) {
    return true;
  }
}
