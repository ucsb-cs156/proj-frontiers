package edu.ucsb.cs156.frontiers.models;

/**
 * One row of a CATME TeamMaker CSV export that assigns a student to a team.
 *
 * @param studentId the student's id
 * @param name the student's name, as it appears in the file
 * @param teamName the team name from the file
 */
public record CATMETeamAssignment(String studentId, String name, String teamName) {}
