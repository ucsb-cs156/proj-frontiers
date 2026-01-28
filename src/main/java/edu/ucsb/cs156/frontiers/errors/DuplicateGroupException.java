package edu.ucsb.cs156.frontiers.errors;

public class DuplicateGroupException extends RuntimeException {
  public DuplicateGroupException() {
    super("Frontiers cannot support two Canvas groups with the same name.");
  }
}
