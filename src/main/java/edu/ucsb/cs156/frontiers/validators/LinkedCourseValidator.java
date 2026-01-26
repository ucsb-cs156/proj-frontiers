package edu.ucsb.cs156.frontiers.validators;

import edu.ucsb.cs156.frontiers.entities.Course;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class LinkedCourseValidator implements ConstraintValidator<HasLinkedCanvasCourse, Course> {

  @Override
  public boolean isValid(Course course, ConstraintValidatorContext constraintValidatorContext) {
    return course != null
        && course.getCanvasApiToken() != null
        && course.getCanvasCourseId() != null;
  }
}
