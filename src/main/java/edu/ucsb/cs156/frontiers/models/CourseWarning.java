package edu.ucsb.cs156.frontiers.models;

public record CourseWarning(
    boolean showOrganizationAgeWarning, boolean showDefaultBasePermissions) {
  public CourseWarning(boolean showOrganizationAgeWarning) {
    this(showOrganizationAgeWarning, false);
  }
}
