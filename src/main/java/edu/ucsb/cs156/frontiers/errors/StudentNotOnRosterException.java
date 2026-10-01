package edu.ucsb.cs156.frontiers.errors;

/**
 * Thrown when a lookup by email finds no (non-dropped) roster student in the course. Like its
 * parent {@link EntityNotFoundException}, it maps to a 404.
 */
public class StudentNotOnRosterException extends EntityNotFoundException {
  /**
   * Constructor for the exception
   *
   * @param email the (normalized) email that was searched for
   * @param courseId the id of the course that was searched
   */
  public StudentNotOnRosterException(String email, Long courseId) {
    super("No roster student with email %s in course %d".formatted(email, courseId));
  }
}
