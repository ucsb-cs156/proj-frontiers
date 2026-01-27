package edu.ucsb.cs156.frontiers.validators;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucsb.cs156.frontiers.entities.Course;
import org.junit.jupiter.api.Test;

public class LinkedCourseValidatorTests {
  @Test
  void testValidCourse() {
    Course course = Course.builder().canvasApiToken("banana").canvasCourseId("apple").build();
    LinkedCourseValidator validator = new LinkedCourseValidator();
    assertTrue(validator.isValid(course, null));
  }

  @Test
  void api_but_no_id() {
    Course course = Course.builder().canvasApiToken("banana").build();
    LinkedCourseValidator validator = new LinkedCourseValidator();
    assertFalse(validator.isValid(course, null));
  }

  @Test
  void test_null_course() {
    LinkedCourseValidator validator = new LinkedCourseValidator();
    assertFalse(validator.isValid(null, null));
  }

  @Test
  void test_empty_course() {
    Course course = Course.builder().build();

    LinkedCourseValidator validator = new LinkedCourseValidator();
    assertFalse(validator.isValid(course, null));
  }
}
