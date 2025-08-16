package edu.ucsb.cs156.frontiers.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * This annotation interacts with the mock CourseSecurity annotation. It adds authorities that tell
 * the CourseSecurity annotation used in testing that the user has permission to manage the course
 * being accessed. It should be used on methods that have the
 * <i>@PreAuthorize("@CourseSecurity.hasManagePermissions")</i> or
 * <i>@PreAuthorize("@CourseSecurity.hasInstructorPermissions")</i> annotations. This should not be
 * combined with <i>@WithMockUser</i>, because this annotation implies
 * <i>@WithMockUser("ROLE_INSTRUCTOR")</i>
 */
@Retention(RetentionPolicy.RUNTIME)
@WithMockUser(authorities = {"COURSE_PERMISSIONS", "ROLE_INSTRUCTOR"})
public @interface WithInstructorCoursePermissions {}
