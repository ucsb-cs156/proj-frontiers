package edu.ucsb.cs156.frontiers.errors;

public class NoLinkedOrganizationException extends RuntimeException {
  /**
   * Constructor for the exception
   *
   * @param courseName Name of the Course that does not have a linked GitHub Organization
   */
  public NoLinkedOrganizationException(String courseName) {
    super(
        "No linked GitHub Organization to "
            + courseName
            + ". Please link a GitHub Organization first.");
  }
}
