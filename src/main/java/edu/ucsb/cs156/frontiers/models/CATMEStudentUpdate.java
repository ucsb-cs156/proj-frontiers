package edu.ucsb.cs156.frontiers.models;

/**
 * Represents a single field that differs between the roster and the uploaded CATME CSV file for
 * a given student, so that the field can be updated in CATME.
 *
 * @param studentId the student's id
 * @param name the student's name (as recorded in Frontiers)
 * @param field the name of the field that differs (e.g. "Name" or "Section")
 * @param oldValue the value currently in CATME
 * @param newValue the value that should be in CATME (i.e. the value in Frontiers)
 */
public record CATMEStudentUpdate(
    String studentId, String name, String field, String oldValue, String newValue) {}
