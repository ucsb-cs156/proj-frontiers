import { describe, expect, test } from "vitest";
import {
  EMAIL_FORMATS,
  courseEmailsQueryKey,
} from "main/utils/courseEmailsUtils";

describe("courseEmailsUtils tests", () => {
  test("courseEmailsQueryKey includes the course id", () => {
    expect(courseEmailsQueryKey(7)).toBe("/api/courses/emails?courseId=7");
    expect(courseEmailsQueryKey("12")).toBe("/api/courses/emails?courseId=12");
  });

  test("EMAIL_FORMATS lists the endpoint's formats, One per line first", () => {
    expect(EMAIL_FORMATS).toEqual([
      { value: "ONE_PER_LINE", label: "One per line" },
      { value: "COMMA_SEPARATED", label: "Comma separated" },
    ]);
  });
});
