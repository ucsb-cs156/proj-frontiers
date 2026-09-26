import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";
import {
  COURSE_TABS,
  DEFAULT_COURSE_TAB,
  chooseCourseTab,
  courseTabStorageKey,
  getStoredCourseTab,
  storeCourseTab,
} from "main/utils/courseTabUtils";

describe("courseTabUtils tests", () => {
  beforeEach(() => {
    window.localStorage.clear();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  test("the tabs and the default tab", () => {
    expect(COURSE_TABS).toEqual([
      "students",
      "staff",
      "teams",
      "sections",
      "assignments",
      "new-assignments",
      "jobs",
      "downloads",
      "dokku",
      "slack",
      "settings",
    ]);
    expect(DEFAULT_COURSE_TAB).toBe("assignments");
  });

  test("the tab is stored per course", () => {
    expect(courseTabStorageKey(7)).toBe("frontiers.courseTab.7");
    expect(getStoredCourseTab(7)).toBeNull();

    storeCourseTab(7, "jobs");
    storeCourseTab(8, "staff");

    expect(window.localStorage.getItem("frontiers.courseTab.7")).toBe("jobs");
    expect(getStoredCourseTab(7)).toBe("jobs");
    expect(getStoredCourseTab(8)).toBe("staff");
    expect(getStoredCourseTab(9)).toBeNull();
  });

  test("local storage being unavailable is not an error", () => {
    vi.spyOn(Storage.prototype, "getItem").mockImplementation(() => {
      throw new Error("blocked");
    });
    vi.spyOn(Storage.prototype, "setItem").mockImplementation(() => {
      throw new Error("blocked");
    });

    expect(getStoredCourseTab(7)).toBeNull();
    expect(() => storeCourseTab(7, "jobs")).not.toThrow();
  });

  describe("chooseCourseTab", () => {
    const visibleTabs = ["students", "assignments", "jobs"];

    test("a requested tab that is visible is chosen, whatever is stored", () => {
      expect(
        chooseCourseTab({
          requestedTab: "jobs",
          storedTab: "students",
          visibleTabs,
        }),
      ).toBe("jobs");
    });

    test("a requested tab that is not visible gives the default tab, not the stored one", () => {
      expect(
        chooseCourseTab({
          requestedTab: "slack",
          storedTab: "students",
          visibleTabs,
        }),
      ).toBe("assignments");
      expect(
        chooseCourseTab({
          requestedTab: "bogus",
          storedTab: "students",
          visibleTabs,
        }),
      ).toBe("assignments");
      expect(
        chooseCourseTab({
          requestedTab: "",
          storedTab: "students",
          visibleTabs,
        }),
      ).toBe("assignments");
    });

    test("with no requested tab, the stored tab is chosen if it is visible", () => {
      expect(
        chooseCourseTab({
          requestedTab: null,
          storedTab: "students",
          visibleTabs,
        }),
      ).toBe("students");
    });

    test("with no requested tab, and no visible stored tab, the default tab is chosen", () => {
      expect(
        chooseCourseTab({ requestedTab: null, storedTab: null, visibleTabs }),
      ).toBe("assignments");
      expect(
        chooseCourseTab({
          requestedTab: null,
          storedTab: "slack",
          visibleTabs,
        }),
      ).toBe("assignments");
    });
  });
});
