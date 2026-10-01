import { afterEach, describe, expect, test, vi } from "vitest";
import {
  API_KEYS_DOCS_URL,
  EXPIRY_WARNING_DAYS,
  apiKeysQueryKey,
  daysUntil,
  studentInfoCurlExample,
} from "main/utils/apiKeyUtils";

describe("apiKeyUtils tests", () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  test("the docs link and the warning threshold", () => {
    expect(API_KEYS_DOCS_URL).toBe(
      "https://github.com/ucsb-cs156/proj-frontiers/blob/main/docs/api-keys.md",
    );
    expect(EXPIRY_WARNING_DAYS).toBe(14);
  });

  test("apiKeysQueryKey is per course", () => {
    expect(apiKeysQueryKey(7)).toBe("/api/courses/key?courseId=7");
    expect(apiKeysQueryKey("12")).toBe("/api/courses/key?courseId=12");
  });

  test("studentInfoCurlExample fills in this deployment, the course and the key", () => {
    expect(studentInfoCurlExample(7, "u1Zc6N0T0dG9xWcY4h2JbQ")).toBe(
      `curl -s '${window.location.origin}/api/courses/studentInfo?courseId=7&email=cgaucho@ucsb.edu' \\\n` +
        "  -H 'X-API-KEY: u1Zc6N0T0dG9xWcY4h2JbQ'",
    );
    expect(window.location.origin).toMatch(/^http/);
  });

  test("daysUntil rounds up to whole days", () => {
    const now = new Date("2026-10-01T12:00:00Z");
    expect(daysUntil("2026-10-03T00:00:00Z", now)).toBe(2); // 36 hours
    expect(daysUntil("2026-10-01T18:00:00Z", now)).toBe(1); // later today
    expect(daysUntil("2026-10-03T12:00:00Z", now)).toBe(2); // exactly 2 days
    expect(daysUntil("2026-10-15T12:00:00Z", now)).toBe(14);
    expect(daysUntil("2026-09-30T11:00:00Z", now)).toBe(-1); // 25 hours ago
  });

  test("daysUntil defaults to the current time", () => {
    vi.useFakeTimers({ toFake: ["Date"] });
    vi.setSystemTime(new Date("2026-10-01T12:00:00Z"));
    expect(daysUntil("2026-10-04T12:00:00Z")).toBe(3);
    expect(daysUntil("2026-10-04T13:00:00Z")).toBe(4);
  });
});
