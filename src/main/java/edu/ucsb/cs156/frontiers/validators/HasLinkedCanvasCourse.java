package edu.ucsb.cs156.frontiers.validators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = LinkedCourseValidator.class)
public @interface HasLinkedCanvasCourse {
  String message() default "This course does not have a linked Canvas course.";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
