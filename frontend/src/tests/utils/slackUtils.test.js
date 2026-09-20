import {
  courseRoleLabel,
  slackInfoQueryKey,
  slackStatusLabel,
} from "main/utils/slackUtils";

describe("slackUtils tests", () => {
  test("courseRoleLabel", () => {
    expect(courseRoleLabel("INSTRUCTOR")).toBe("Instructor");
    expect(courseRoleLabel("STAFF")).toBe("Staff");
    expect(courseRoleLabel("STUDENT")).toBe("Student");
    expect(courseRoleLabel("NONE")).toBe("None");
    expect(courseRoleLabel("SOMETHING_NEW")).toBe("SOMETHING_NEW");
  });

  test("slackStatusLabel", () => {
    expect(slackStatusLabel("INVITED")).toBe("Invited (has not signed in yet)");
    expect(slackStatusLabel("DEACTIVATED")).toBe("Account deactivated");
    expect(slackStatusLabel("NOT_IN_SLACK")).toBe("Not in Slack");
    expect(slackStatusLabel("SOMETHING_NEW")).toBe("SOMETHING_NEW");
  });

  test("slackInfoQueryKey", () => {
    expect(slackInfoQueryKey(7)).toBe("/api/courses/slack/info?courseId=7");
  });
});
