package edu.ucsb.cs156.frontiers.models;

/**
 * One entry in the name-to-team CSV: the shortest name that still identifies the student, and the
 * name of the student's team (blank if they are not on a team).
 *
 * @param name the abbreviated display name
 * @param team the team name, or "" if the student has no team
 */
public record NameAndTeam(String name, String team) {}
