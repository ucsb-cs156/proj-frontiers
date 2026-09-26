package edu.ucsb.cs156.frontiers.models;

import edu.ucsb.cs156.frontiers.entities.Assignment;
import edu.ucsb.cs156.jobs.entities.Job;

/**
 * The result of creating or editing an assignment: the saved assignment, and the job that was
 * started to create its repositories.
 *
 * @param assignment the saved assignment
 * @param job the job started to create the repositories
 */
public record AssignmentWithJob(Assignment assignment, Job job) {}
