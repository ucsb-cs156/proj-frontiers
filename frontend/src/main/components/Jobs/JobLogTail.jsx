import React, { useEffect, useRef } from "react";
import { Badge } from "react-bootstrap";
import { useJobLogTail } from "main/utils/useJobLogTail";
import {
  JOB_LOG_POLL_INTERVAL_SECONDS,
  isJobFinished,
} from "main/utils/jobLogUtils";

/**
 * Shows the log of a job of a course as it is written, like `tail -f`: new
 * lines are added at the bottom every few seconds, and the view scrolls to
 * follow them, until the job is over. See useJobLogTail.
 */
export default function JobLogTail({
  courseId,
  jobId,
  testIdPrefix = "JobLogTail",
}) {
  const { lines, status, error } = useJobLogTail(courseId, jobId);
  const logRef = useRef(null);

  // follow the end of the log as it grows
  useEffect(() => {
    logRef.current.scrollTop = logRef.current.scrollHeight;
  }, [lines]);

  let statusText;
  if (error) {
    statusText = "The log could not be loaded.";
  } else if (status === null) {
    statusText = "Loading the log...";
  } else if (isJobFinished(status)) {
    statusText = "The job is over, so the log will not change any more.";
  } else {
    statusText = `Checking for new lines every ${JOB_LOG_POLL_INTERVAL_SECONDS} seconds.`;
  }

  return (
    <div data-testid={testIdPrefix}>
      <p data-testid={`${testIdPrefix}-status`}>
        Job {jobId}
        {status !== null && (
          <>
            {" "}
            <Badge bg="secondary" data-testid={`${testIdPrefix}-status-badge`}>
              {status}
            </Badge>
          </>
        )}
        {" — "}
        <span data-testid={`${testIdPrefix}-status-text`}>{statusText}</span>
      </p>
      <pre
        ref={logRef}
        data-testid={`${testIdPrefix}-log`}
        style={{
          backgroundColor: "black",
          color: "white",
          fontFamily: "monospace",
          maxHeight: "60vh",
          overflowY: "auto",
          whiteSpace: "pre-wrap",
          padding: "0.75rem",
        }}
      >
        {lines.length === 0 ? "No log lines yet." : lines.join("\n")}
      </pre>
    </div>
  );
}
