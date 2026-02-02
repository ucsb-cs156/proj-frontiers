import { Alert } from "react-bootstrap";
import { useBackend } from "main/utils/useBackend";

export function CourseWarningBanner({ courseId }) {
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

  return (
    <>
      {warnings?.showOrganizationAgeWarning && (
        <Alert variant="warning">
          Warning: This GitHub Organization is less than 30 days old. You will
          experience difficulties enrolling more than 50 students in a day.
        </Alert>
      )}
    </>
  );
}
