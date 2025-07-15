package edu.ucsb.cs156.frontiers.annotations;

import org.springframework.security.test.context.support.WithMockUser;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This annotation interacts with the mock CourseSecurity annotation. It adds authorities that tells the CourseSecurity annotation
 * used in testing that the user has permission to manage the course being accessed. It should be used on methods that have the
 * <p>@PreAuthorize("@CourseSecurity.hasManagePermissions") annotation.</p>
 * It cannot be combined with <p>@WithMockUser</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@WithMockUser(authorities = {"COURSE_PERMISSION","ROLE_ADMIN"})
public @interface WithInstructorCoursePermissions {
}
