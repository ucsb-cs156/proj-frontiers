import { Container, Button } from "react-bootstrap";
import { useNavigate } from "react-router";
import OnboardingLayout from "main/layouts/OnboardingLayout/OnboardingLayout";
import CoursesTable from "main/components/Courses/CoursesTable";
import { useBackend, useBackendMutation } from "main/utils/useBackend";
import { toast } from "react-toastify";

export default function OnboardingCoursesPage() {
  const navigate = useNavigate();

  const {
    data: courses,
    error: _error,
    status: _status,
  } = useBackend(
    ["/api/courses/list"],
    { method: "GET", url: "/api/courses/list" },
    [],
  );

  const onJoinSuccess = (message) => {
    toast(message);
  };

  const onJoinFail = (result) => {
    toast(result.response.data ? result.response.data : result.message);
  };

  const cellToAxiosParamsStudent = (cell) => {
    return {
      url: `/api/rosterstudents/joinCourse`,
      method: "PUT",
      params: {
        rosterStudentId: cell.row.original.rosterStudentId,
      },
    };
  };

  const studentJoinMutation = useBackendMutation(
    cellToAxiosParamsStudent,
    { onSuccess: onJoinSuccess, onError: onJoinFail },
    [`/api/courses/list`],
  );

  const joinStudentCourseCallback = async (cell) => {
    studentJoinMutation.mutate(cell);
  };

  const isStudentJoining = (cell) => {
    return (
      studentJoinMutation.isPending &&
      studentJoinMutation.variables.row.index === cell.row.index
    );
  };

  const handleContinue = () => {
    navigate("/onboarding/complete");
  };

  return (
    <OnboardingLayout currentStep={4} totalSteps={5}>
      <Container className="text-center">
        <h1 className="mb-4">Great! You're on the roster for these courses:</h1>
        <p className="mb-4">
          If you are joining the course, please click "Join Course", then accept
          the GitHub invite.
        </p>
        {courses.length > 0 ? (
          <CoursesTable
            courses={courses}
            testId="OnboardingCoursesTable"
            joinCallback={joinStudentCourseCallback}
            isLoading={isStudentJoining}
          />
        ) : (
          <p>You are not enrolled in any student courses yet.</p>
        )}
        <div className="mt-4">
          <Button
            variant="primary"
            onClick={handleContinue}
            data-testid="OnboardingCourses-continueButton"
          >
            Continue
          </Button>
        </div>
      </Container>
    </OnboardingLayout>
  );
}
