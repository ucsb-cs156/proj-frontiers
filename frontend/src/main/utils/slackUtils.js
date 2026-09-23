const courseRoleLabels = {
  INSTRUCTOR: "Instructor",
  STAFF: "Staff",
  STUDENT: "Student",
  NONE: "None",
};

const slackStatusLabels = {
  INVITED: "Invited (has not signed in yet)",
  DEACTIVATED: "Account deactivated",
  NOT_IN_SLACK: "Not in Slack",
};

// Unknown values are shown as is, so that a new backend value is never hidden
export function courseRoleLabel(courseRole) {
  return courseRoleLabels[courseRole] ?? courseRole;
}

export function slackStatusLabel(slackStatus) {
  return slackStatusLabels[slackStatus] ?? slackStatus;
}

export function slackInfoQueryKey(courseId) {
  return `/api/courses/slack/info?courseId=${courseId}`;
}

// Used by the backend in place of the workspace's own URL (e.g.
// https://ucsb-cs156-f26.slack.com/) for tokens that were saved before the
// workspace URL was being recorded; see SlackController.SLACK_CLIENT_URL
const slackClientUrlPrefix = "https://app.slack.com/";

// The admin page of a workspace is at its URL followed by /admin. Returns null
// when the workspace's own URL is not known, since there is then no admin URL.
export function slackAdminUrl(slackTeamUrl) {
  if (!slackTeamUrl || slackTeamUrl.startsWith(slackClientUrlPrefix)) {
    return null;
  }
  const withoutTrailingSlash = slackTeamUrl.endsWith("/")
    ? slackTeamUrl.slice(0, -1)
    : slackTeamUrl;
  return `${withoutTrailingSlash}/admin`;
}
