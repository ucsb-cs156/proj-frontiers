import { describe, expect, test } from "vitest";
import {
  JOB_LOG_POLL_INTERVAL_SECONDS,
  JOB_LOG_TAIL_URL,
  isJobFinished,
  jobLogPagePath,
} from "main/utils/jobLogUtils";

describe("jobLogUtils tests", () => {
  test("the job log is polled every 3 seconds", () => {
    expect(JOB_LOG_POLL_INTERVAL_SECONDS).toBe(3);
  });

  test("the tail endpoint", () => {
    expect(JOB_LOG_TAIL_URL).toBe("/api/jobs/course/logs/tail");
  });

  test("a job is finished once it is complete, in error, cancelled or interrupted", () => {
    ["complete", "error", "cancelled", "interrupted"].forEach((status) => {
      expect(isJobFinished(status)).toBe(true);
    });
  });

  test("a job that is queued, running or being cancelled is not finished", () => {
    ["queued", "running", "cancelling"].forEach((status) => {
      expect(isJobFinished(status)).toBe(false);
    });
  });

  test("anything else is not finished either", () => {
    expect(isJobFinished(null)).toBe(false);
    expect(isJobFinished(undefined)).toBe(false);
    expect(isJobFinished("")).toBe(false);
    expect(isJobFinished("Complete")).toBe(false);
  });

  test("jobLogPagePath", () => {
    expect(jobLogPagePath(7, 12)).toBe("/instructor/courses/7/jobs/12/logs");
    expect(jobLogPagePath("3", "40")).toBe(
      "/instructor/courses/3/jobs/40/logs",
    );
  });
});
