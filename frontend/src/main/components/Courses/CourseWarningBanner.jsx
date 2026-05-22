import { Alert } from "react-bootstrap";
import { useBackend } from "main/utils/useBackend";

export function CourseWarningBanner({ courseId, orgName }) {
  const { data: warnings } = useBackend(
    [`/api/courses/warnings/${courseId}`],
    {
      method: "GET",
      url: `/api/courses/warnings/${courseId}`,
    },
    undefined,
    true,
    {
      placeholderData: { showOrganizationAgeWarning: false },
      staleTime: "static",
    },
  );

  const permission = warnings?.defaultBasePermission;
  const showPermissionWarning =
    permission && permission !== "none" && permission !== "null";

  return (
    <>
      {warnings?.showOrganizationAgeWarning && (
        <Alert variant="warning">
          Warning: This GitHub Organization is less than 30 days old. You will
          experience difficulties enrolling more than 50 students in a day.
        </Alert>
      )}
      {showPermissionWarning && (
        <Alert variant="warning">
          Warning: The default base permission for this organization is "
          {permission.charAt(0).toUpperCase() + permission.slice(1)}". Students
          may be able to view one another's private repos.{" "}
          {orgName && (
            <Alert.Link
              href={`https://github.com/organizations/${orgName}/settings/member_privileges`}
              target="_blank"
              rel="noopener noreferrer"
            >
              Change this in GitHub settings.
            </Alert.Link>
          )}
        </Alert>
      )}
    </>
  );
}
