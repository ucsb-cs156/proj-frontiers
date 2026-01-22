import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import { ProgressBar, Row, Button, Col } from "react-bootstrap";
import { Link } from "react-router";
import { StudentCoursesTable } from "main/components/Courses/StudentCoursesTable";

export default function OnboardingSelectCoursesPage() {
  return (
    <BasicLayout>
      <Row>
        <h1>Joining Courses</h1>
        <p>
          Next, you&#39;re listed on the roster of the courses below. Follow the
          prompts to join:
        </p>
        <StudentCoursesTable />
      </Row>
      <Row className="mt-auto pb-3 justify-content-center">
        <Col xs="auto" className="text-center">
          <Button as={Link} to={"/onboarding/success"}>
            Continue
          </Button>
        </Col>
      </Row>
      <Row className="pb-3">
        <ProgressBar now={75} />
      </Row>
    </BasicLayout>
  );
}
