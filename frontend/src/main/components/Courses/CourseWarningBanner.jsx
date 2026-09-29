import { useState } from "react";
import { Alert, Button } from "react-bootstrap";
import { toast } from "react-toastify";
import { useBackend, useBackendMutation } from "main/utils/useBackend";

/** Where verified educators can upgrade a GitHub organization at no cost. */
export const GITHUB_EDUCATION_TEACHER_URL =
  "https://education.github.com/globalcampus/teacher";

export function CourseWarningBanner({
  courseId,
  orgName,
  hideBasePermissionWarning = false,
  hideFreePlanWarning = false,
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
        showFreePlanWarning: false,
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

  // Dismissing the free plan warning is remembered on the course (so it stays
  // dismissed on reload); this local flag hides it immediately, without waiting
  // for the course to be refetched.
  const [freePlanWarningDismissed, setFreePlanWarningDismissed] =
    useState(false);

  const objectToAxiosParamsHideFreePlanWarning = () => ({
    url: `/api/courses/warnings/hideFreePlanWarning/${courseId}`,
    method: "POST",
  });

  const onSuccessHideFreePlanWarning = () => {
    setFreePlanWarningDismissed(true);
    toast("Free plan warning dismissed for this course");
  };

  const hideFreePlanWarningMutation = useBackendMutation(
    objectToAxiosParamsHideFreePlanWarning,
    { onSuccess: onSuccessHideFreePlanWarning },
    [`/api/courses/${courseId}`],
  );

  const showFreePlanWarning =
    warnings?.showFreePlanWarning &&
    !hideFreePlanWarning &&
    !freePlanWarningDismissed;

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
      {showFreePlanWarning && (
        <Alert variant="warning" data-testid="CourseWarningBanner-freePlan">
          Warning: this GitHub organization is on the Free plan. Some features
          do not work on the Free plan; in particular, the &quot;Require Signed
          Commits&quot; option for assignments cannot be applied to private
          repositories. Verified educators can upgrade the organization at no
          cost through{" "}
          <a
            href={GITHUB_EDUCATION_TEACHER_URL}
            target="_blank"
            rel="noopener noreferrer"
            data-testid="CourseWarningBanner-freePlan-link"
          >
            GitHub Education for Teachers
          </a>
          .
          <Button
            size="sm"
            variant="outline-secondary"
            className="ms-2"
            onClick={() => hideFreePlanWarningMutation.mutate()}
            data-testid="CourseWarningBanner-freePlan-dismiss"
          >
            Dismiss
          </Button>
        </Alert>
      )}
    </>
  );
}
