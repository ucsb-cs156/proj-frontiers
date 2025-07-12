package edu.ucsb.cs156.frontiers.annotations;

import org.springframework.security.test.context.support.WithMockUser;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@WithMockUser(authorities = {"COURSE_PERMISSION","ROLE_ADMIN"})
public @interface WithInstructorCoursePermissions {
}
