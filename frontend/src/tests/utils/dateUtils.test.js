import { formatTime } from "main/utils/dateUtils";

describe("dateUtils tests", () => {
  describe("formatTime", () => {
    test("formats a valid timestamp correctly", () => {
      // Create a specific date for testing
      const timestamp = "2023-01-01T12:30:45.000Z";

      // Mock Date.prototype.toLocaleString to return a predictable value
      const originalToLocaleString = Date.prototype.toLocaleString;
      Date.prototype.toLocaleString = jest.fn(() => "1/1/2023, 12:30:45 PM");

      const result = formatTime(timestamp);

      expect(result).toBe("1/1/2023, 12:30:45 PM");

      // Restore the original method
      Date.prototype.toLocaleString = originalToLocaleString;
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
