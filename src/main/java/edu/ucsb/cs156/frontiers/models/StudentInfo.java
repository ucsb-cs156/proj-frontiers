package edu.ucsb.cs156.frontiers.models;

import java.util.List;

/**
 * What an autograder needs to know about one roster student: how to recognise them on GitHub, which
 * team they are on, and who else is on that team. This is the response of <code>
 * GET /api/courses/studentInfo</code>.
 *
 * <p>The <code>firstName</code> fields hold the shortest name that still identifies the student
 * among all the (non-dropped) students of the course, as computed by {@link
 * edu.ucsb.cs156.frontiers.services.TeamCsvService#nameAndTeams}: e.g. "Chris" if there is only one
 * Chris, "Chris L" if there are several. That is the name a teammate is likely to use for them. The
 * <code>legalFirstName</code> fields hold the roster's first name field as is.
 *
 * @param email the student's email as stored on the roster
 * @param firstName the shortest unique name for the student
 * @param legalFirstName the roster's first name field, unchanged
 * @param lastName the roster's last name field, unchanged
 * @param githubLogin the student's GitHub login, or null if they have not linked GitHub yet
 * @param team the name of the student's team (the first one, if they are on several), or null if
 *     they are not on a team
 * @param teams the names of all the student's teams, possibly empty
 * @param teamMembers everyone on <code>team</code>, including the student, or empty if the student
 *     has no team
 */
public record StudentInfo(
    String email,
    String firstName,
    String legalFirstName,
    String lastName,
    String githubLogin,
    String team,
    List<String> teams,
    List<Member> teamMembers) {

  /**
   * One member of the student's team.
   *
   * @param email the member's email as stored on the roster
   * @param firstName the shortest unique name for the member
   * @param legalFirstName the roster's first name field, unchanged
   * @param githubLogin the member's GitHub login, or null if they have not linked GitHub yet
   */
  public record Member(String email, String firstName, String legalFirstName, String githubLogin) {}
}
