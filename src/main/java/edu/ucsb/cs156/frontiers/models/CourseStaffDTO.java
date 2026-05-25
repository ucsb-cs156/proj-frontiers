package edu.ucsb.cs156.frontiers.models;

import edu.ucsb.cs156.frontiers.entities.CourseStaff;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;

/**
 * This is a DTO class that represents a staff member in a course. It is used to transfer data
 * between the server and the client.
 */
public record CourseStaffDTO(
    Long id,
    Long courseId,
    String firstName,
    String lastName,
    String email,
    String role,
    long userId,
    Integer githubId,
    String githubLogin,
    OrgStatus orgStatus) {

  public CourseStaffDTO(CourseStaff courseStaff) {
    this(
        courseStaff.getId(),
        courseStaff.getCourse().getId(),
        courseStaff.getFirstName(),
        courseStaff.getLastName(),
        courseStaff.getEmail(),
        courseStaff.getRole(),
        courseStaff.getUser() != null ? courseStaff.getUser().getId() : 0,
        courseStaff.getGithubId(),
        courseStaff.getGithubLogin(),
        courseStaff.getOrgStatus());
  }
}
