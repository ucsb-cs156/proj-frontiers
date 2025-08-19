import { formatTime } from "main/utils/dateUtils";

describe("dateUtils tests", () => {
  describe("formatTime", () => {
    test("formats a valid timestamp correctly", () => {
      // Create a specific date for testing
      const timestamp = "2023-01-01T12:30:45.000Z";

      const result = formatTime(timestamp);

      expect(result).toBe(new Date(timestamp).toLocaleString());
    });

    test("returns empty string for null timestamp", () => {
      expect(formatTime(null)).toBe("");
    });

    test("returns empty string for undefined timestamp", () => {
      expect(formatTime(undefined)).toBe("");
    });

    test("returns empty string for empty string timestamp", () => {
      expect(formatTime("")).toBe("");
    });
  });
});
