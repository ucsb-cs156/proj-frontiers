package edu.ucsb.cs156.frontiers.models;

import edu.ucsb.cs156.jobs.entities.JobLog;
import java.util.List;

/**
 * One poll of a job's log, for showing a log as it is written: the job's status, and the log lines
 * written since the last line the client already has.
 *
 * @param status the status of the job, e.g. "queued", "running" or "complete"
 * @param lines the new log lines, oldest first; empty if there are none
 */
public record JobLogTail(String status, List<JobLog> lines) {}
