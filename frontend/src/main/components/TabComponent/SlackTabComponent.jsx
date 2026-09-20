import React from "react";
import { Alert } from "react-bootstrap";
import { useBackend } from "main/utils/useBackend";
import SlackUsersTable from "main/components/Slack/SlackUsersTable";
import SlackMissingMembersTable from "main/components/Slack/SlackMissingMembersTable";
import { slackAdminUrl } from "main/utils/slackUtils";

// Each of these queries makes the backend call the Slack API, so they are not retried on
// failure, and not repeated every time the browser window regains focus.
const slackQueryOptions = {
  retry: false,
  // Stryker disable next-line BooleanLiteral : window focus refetching is not observable in tests
  refetchOnWindowFocus: false,
};

function SlackQueryError({ error, testId }) {
  return (
    <Alert variant="danger" data-testid={testId}>
      {error.response?.data?.message ??
        `Error getting information from Slack: ${error}`}
    </Alert>
  );
}

export default function SlackTabComponent({
  courseId,
  testIdPrefix,
  slackTeamName,
  slackTeamUrl,
}) {
  const adminUrl = slackAdminUrl(slackTeamUrl);
  const usersUrl = `/api/courses/slack/users?courseId=${courseId}`;
  const missingUrl = `/api/courses/slack/missing?courseId=${courseId}`;

  const { data: users, error: usersError } = useBackend(
    [usersUrl],
    // Stryker disable next-line StringLiteral : GET and empty string are equivalent
    { method: "GET", url: usersUrl },
    [],
    true,
    slackQueryOptions,
  );

  const { data: missing, error: missingError } = useBackend(
    [missingUrl],
    // Stryker disable next-line StringLiteral : GET and empty string are equivalent
    { method: "GET", url: missingUrl },
    [],
    true,
    slackQueryOptions,
  );

  return (
    <div data-testid={`${testIdPrefix}-slack-tab-component`}>
      <p className="fs-5">
        Slack workspace:{" "}
        <a
          href={slackTeamUrl}
          target="_blank"
          rel="noopener noreferrer"
          data-testid={`${testIdPrefix}-slack-workspace-link`}
        >
          {slackTeamName}
        </a>
        {adminUrl && (
          <a
            href={adminUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="ms-3"
            data-testid={`${testIdPrefix}-slack-admin-link`}
          >
            Admin
          </a>
        )}
      </p>

      <h5 data-testid={`${testIdPrefix}-slack-users-heading`}>
        Active Slack users ({users.length})
      </h5>
      <p className="text-muted">
        Course Role shows whether the email of the Slack user matches the
        instructor, a staff member, or a student on the roster of this course.
        Click a column header to sort. If the Email column is empty, the Slack
        app is missing the <code>users:read.email</code> scope.
      </p>
      {usersError ? (
        <SlackQueryError
          error={usersError}
          testId={`${testIdPrefix}-slack-users-error`}
        />
      ) : (
        <SlackUsersTable
          users={users}
          testIdPrefix={`${testIdPrefix}-slack-users-table`}
        />
      )}

      <h5
        className="mt-4"
        data-testid={`${testIdPrefix}-slack-missing-heading`}
      >
        Roster students and staff not active in Slack ({missing.length})
      </h5>
      <p className="text-muted">
        Students (other than dropped students) and staff of this course whose
        email does not match an active Slack user.
      </p>
      {missingError ? (
        <SlackQueryError
          error={missingError}
          testId={`${testIdPrefix}-slack-missing-error`}
        />
      ) : (
        <SlackMissingMembersTable
          members={missing}
          testIdPrefix={`${testIdPrefix}-slack-missing-table`}
        />
      )}
    </div>
  );
}
