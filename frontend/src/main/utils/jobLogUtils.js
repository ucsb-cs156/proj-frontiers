/**
 * How often, in seconds, an open job log asks the backend for the lines that
 * have been added since it last looked. This is the only place the interval
 * is defined.
 */
export const JOB_LOG_POLL_INTERVAL_SECONDS = 3;

/** The endpoint that answers one poll: a job's status and its new log lines. */
export const JOB_LOG_TAIL_URL = "/api/jobs/course/logs/tail";

// The statuses of a job that is over and will not write any more log lines
const FINISHED_JOB_STATUSES = ["complete", "error", "cancelled", "interrupted"];

/** Whether a job with this status is over, so that its log has stopped growing. */
export function isJobFinished(status) {
  return FINISHED_JOB_STATUSES.includes(status);
}

/** The path of the page that shows a whole job log, for a job of a course. */
export function jobLogPagePath(courseId, jobId) {
  return `/instructor/courses/${courseId}/jobs/${jobId}/logs`;
}
