package edu.ucsb.cs156.frontiers.errors;

public class InvalidInstallationTypeException extends RuntimeException {
  /**
   * Constructor for the exception
   *
   * @param type The type of linked GitHub installation
   */
  public InvalidInstallationTypeException(String type) {
    super("Invalid installation type: " + type + ". Frontiers can only be linked to organizations");
  }
}
