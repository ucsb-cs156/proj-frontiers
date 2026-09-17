import { describe, expect, test } from "vitest";
import {
  courseOptionsQueryKey,
  titleCaseFromOption,
} from "main/utils/courseOptionsUtils";

describe("courseOptionsUtils tests", () => {
  test("courseOptionsQueryKey builds the shared react-query key for a course", () => {
    expect(courseOptionsQueryKey(7)).toBe("/api/course/options/?courseId=7");
    expect(courseOptionsQueryKey("42")).toBe(
      "/api/course/options/?courseId=42",
    );
  });

  test("titleCaseFromOption converts enum names to title case", () => {
    expect(titleCaseFromOption("TRANSLATE_SECTIONS")).toBe(
      "Translate Sections",
    );
    expect(titleCaseFromOption("ENABLE_API_KEYS")).toBe("Enable Api Keys");
  });
});
