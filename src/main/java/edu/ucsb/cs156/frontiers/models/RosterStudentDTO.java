package edu.ucsb.cs156.frontiers.models;

import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.enums.RosterStatus;

/**
 * This is a DTO class that represents a student in the roster. It is used to transfer data between
 * the server and the client.
 */
public record RosterStudentDTO(
    Long id,
    Long courseId,
    String studentId,
    String firstName,
    String lastName,
    String email,
    long userId,
    Integer userGithubId,
    String userGithubLogin,
    RosterStatus rosterStatus,
    OrgStatus orgStatus) {

  public RosterStudentDTO(RosterStudent rosterStudent) {
    this(
        rosterStudent.getId(),
        rosterStudent.getCourse().getId(),
        rosterStudent.getStudentId(),
        rosterStudent.getFirstName(),
        rosterStudent.getLastName(),
        rosterStudent.getEmail(),
        rosterStudent.getUser() != null ? rosterStudent.getUser().getId() : 0,
        rosterStudent.getGithubId(),
        rosterStudent.getGithubLogin(),
        rosterStudent.getRosterStatus(),
        rosterStudent.getOrgStatus());
  }
}
