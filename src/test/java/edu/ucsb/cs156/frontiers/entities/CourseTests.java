package edu.ucsb.cs156.frontiers.entities;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class CourseTests {

  @Test
  public void hideBasePermissionWarning_field_can_be_set_with_builder_and_setter() {
    Course course = Course.builder().hideBasePermissionWarning(true).build();

    assertTrue(course.getHideBasePermissionWarning());

    course.setHideBasePermissionWarning(false);

    assertFalse(course.getHideBasePermissionWarning());
  }
}
