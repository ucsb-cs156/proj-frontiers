package edu.ucsb.cs156.frontiers.errors;

/**
 * A custom RuntimeException that is thrown when a user is trying to access a course that they are
 * not authorized to access. Admins should never encounter this, but Instructors may encounter it
 * if/when they try to access a course where they are not the instructor of record.
 */
public class CourseNotAuthorized extends RuntimeException {
  /**
   * Constructor for the exception
   *
   * @param id the id that was being searched for
   */
  public CourseNotAuthorized(Object id) {
    super(String.format("User not authorized to access course with id %s", id.toString()));
  }
}
