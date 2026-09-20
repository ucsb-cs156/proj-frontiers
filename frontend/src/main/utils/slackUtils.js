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
