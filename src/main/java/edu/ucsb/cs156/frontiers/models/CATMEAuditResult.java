package edu.ucsb.cs156.frontiers.models;

import java.util.List;

/**
 * The result of auditing a course's roster against an uploaded CATME CSV file.
 *
 * @param studentsToUpdate students who are in CATME, but whose name and/or section do not match
 *     Frontiers
 * @param studentsToDrop students who are in the uploaded CATME CSV file, but are not currently
 *     enrolled (MANUAL or ROSTER status) in Frontiers
 */
public record CATMEAuditResult(
    List<CATMEStudentUpdate> studentsToUpdate, List<CATMEStudentDrop> studentsToDrop) {}
