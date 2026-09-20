import {
  courseRoleLabel,
  slackAdminUrl,
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

  test("slackAdminUrl adds /admin to the workspace URL", () => {
    expect(slackAdminUrl("https://ucsb-cs156-f26.slack.com/")).toBe(
      "https://ucsb-cs156-f26.slack.com/admin",
    );
    expect(slackAdminUrl("https://ucsb-cs156-f26.slack.com")).toBe(
      "https://ucsb-cs156-f26.slack.com/admin",
    );
  });

  test("slackAdminUrl is null when the workspace's own URL is not known", () => {
    expect(slackAdminUrl("https://app.slack.com/client/T12345678")).toBeNull();
    expect(slackAdminUrl("")).toBeNull();
    expect(slackAdminUrl(undefined)).toBeNull();
  });

  test("slackAdminUrl only treats a URL that starts with the Slack client URL as unknown", () => {
    expect(
      slackAdminUrl("https://example.slack.com/?next=https://app.slack.com/"),
    ).toBe("https://example.slack.com/?next=https://app.slack.com/admin");
  });
});
