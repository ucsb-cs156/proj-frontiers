import { useEffect, useState } from "react";
import axios from "axios";
import {
  JOB_LOG_POLL_INTERVAL_SECONDS,
  JOB_LOG_TAIL_URL,
  isJobFinished,
} from "main/utils/jobLogUtils";

/**
 * Follows the log of a job of a course, like `tail -f`: asks the backend for
 * the log every JOB_LOG_POLL_INTERVAL_SECONDS seconds, each time only for the
 * lines added since the last one received, and stops asking once the job is
 * over, when the component using it goes away, or when a request fails.
 *
 * @param {number|string} courseId the course the job belongs to
 * @param {number|string} jobId the job whose log to follow
 * @returns {{lines: string[], status: string|null, error: Error|null}} the log
 *   lines so far, oldest first; the latest status of the job, or null before the
 *   first answer; and the error of the request that made it stop, if there was one
 */
export function useJobLogTail(courseId, jobId) {
  const [lines, setLines] = useState([]);
  const [status, setStatus] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    let stopped = false;
    let timer;
    // the id of the last log line received; the backend sends lines after it
    let afterId = 0;

    setLines([]);
    setStatus(null);
    setError(null);

    const poll = async () => {
      try {
        const response = await axios.get(JOB_LOG_TAIL_URL, {
          params: { courseId, jobId, afterId },
        });
        if (stopped) {
          return;
        }
        const { status: newStatus, lines: newLines } = response.data;
        if (newLines.length > 0) {
          afterId = newLines[newLines.length - 1].id;
          setLines((previous) => [
            ...previous,
            ...newLines.map((line) => line.message),
          ]);
        }
        setStatus(newStatus);
        if (!isJobFinished(newStatus)) {
          timer = setTimeout(poll, JOB_LOG_POLL_INTERVAL_SECONDS * 1000);
        }
      } catch (e) {
        if (!stopped) {
          setError(e);
        }
      }
    };

    poll();

    return () => {
      stopped = true;
      clearTimeout(timer);
    };
  }, [courseId, jobId]);

  return { lines, status, error };
}
