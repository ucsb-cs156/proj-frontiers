package edu.ucsb.cs156.frontiers.models;

/**
 * Represents a student that appears in the uploaded CATME CSV file, but is not currently an
 * enrolled (MANUAL or ROSTER status) roster student in Frontiers, and therefore should be dropped
 * from CATME.
 *
 * @param studentId the student's id
 * @param name the student's name, as it appears in the CATME CSV file
 * @param email the student's email, as it appears in the CATME CSV file
 * @param section the student's section, as it appears in the CATME CSV file
 */
public record CATMEStudentDrop(String studentId, String name, String email, String section) {}
