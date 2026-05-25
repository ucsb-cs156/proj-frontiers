import { Alert } from "react-bootstrap";
import { useBackend } from "main/utils/useBackend";

export function CourseWarningBanner({
  courseId,
  orgName,
  hideBasePermissionWarning = false,
}) {
  const { data: warnings } = useBackend(
    [`/api/courses/warnings/${courseId}`],
    {
      method: "GET",
      url: `/api/courses/warnings/${courseId}`,
    },
    undefined,
    true,
    {
      placeholderData: {
        showOrganizationAgeWarning: false,
        showDefaultBasePermissions: false,
      },
      staleTime: "static",
    },
  );

  const showDefaultBasePermissionWarning =
    warnings?.showDefaultBasePermissions &&
    orgName &&
    !hideBasePermissionWarning;

  const memberPrivilegesUrl = orgName
    ? `https://github.com/organizations/${orgName}/settings/member_privileges`
    : null;

  return (
    <>
      {warnings?.showOrganizationAgeWarning && (
        <Alert variant="warning">
          Warning: This GitHub Organization is less than 30 days old. You will
          experience difficulties enrolling more than 50 students in a day.
        </Alert>
      )}
      {showDefaultBasePermissionWarning && (
        <Alert
          variant="warning"
          data-testid="CourseWarningBanner-defaultBasePermission"
        >
          Warning: the organization setting for Default Base Permission is not
          the recommended value of None. This means that students in the
          organization may be able to access other students&apos; private repos.{" "}
          <a
            href={memberPrivilegesUrl}
            target="_blank"
            rel="noopener noreferrer"
            data-testid="CourseWarningBanner-defaultBasePermission-link"
          >
            You can change that setting here
          </a>
          .
        </Alert>
      )}
    </>
  );
}
