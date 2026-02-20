package edu.ucsb.cs156.frontiers.validators;

import edu.ucsb.cs156.frontiers.entities.Course;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * This is a validator, which enforces a property. These are used together with @Validated to in a
 * spring managed bean (for example, a controller or service).
 *
 * <p>With this validator, you would have to put lots of code in your controller to check for linked
 * courses. Instead, we can just put @HasLinkedCanvasCourse on the method parameters that require
 * it, and if the contraint is not satisfied, a ConstraintViolationException will be thrown.
 */
public class LinkedCourseValidator implements ConstraintValidator<HasLinkedCanvasCourse, Course> {

  @Override
  public boolean isValid(Course course, ConstraintValidatorContext constraintValidatorContext) {
    return course != null
        && course.getCanvasApiToken() != null
        && course.getCanvasCourseId() != null;
  }
}
