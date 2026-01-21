import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import { ProgressBar, Row, Button, Col } from "react-bootstrap";
import CoursesTable from "main/components/Courses/CoursesTable";
import { useBackend, useBackendMutation } from "main/utils/useBackend";
import { toast } from "react-toastify";
import { Link } from "react-router";

export default function OnboardingSelectCoursesPage() {
  const { data: courses } = useBackend(
    // Stryker disable next-line all : don't test internal caching of React Query
    ["/api/courses/list"],
    // Stryker disable next-line StringLiteral : The default value for an empty ("") method is GET. Therefore, there is no way to kill a mutation that transforms "GET" to ""
    { method: "GET", url: "/api/courses/list" },
    // Stryker disable next-line all : don't test default value of empty list
    [],
  );

  const cellToAxiosParamsStudent = (cell) => {
    return {
      url: `/api/rosterstudents/joinCourse`,
      method: "PUT",
      params: {
        rosterStudentId: cell.row.original.rosterStudentId,
      },
    };
  };

  const onJoinSuccess = (message) => {
    toast(message);
  };

  const onJoinFail = (result) => {
    toast(result.response.data ? result.response.data : result.message);
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

  return (
    <BasicLayout>
      <Row>
        <h1>Joining Courses</h1>
        <p>
          Next, you&#39;re listed on the roster of the courses below. Follow the
          prompts to join:
        </p>
        <CoursesTable
          courses={courses}
          testId={"CoursesTable"}
          joinCallback={joinStudentCourseCallback}
          isLoading={isStudentJoining}
        />
      </Row>
      <Row className="mt-auto pb-3 justify-content-center">
        <Col xs="auto" className="text-center">
          <Button as={Link} to={"/onboarding/success"}>
            Continue
          </Button>
        </Col>
      </Row>
      <Row className="pb-3">
        <ProgressBar now={60} />
      </Row>
    </BasicLayout>
  );
}
