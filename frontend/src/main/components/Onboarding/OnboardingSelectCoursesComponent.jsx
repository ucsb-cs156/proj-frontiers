import { Row, Button, Col } from "react-bootstrap";
import { Link } from "react-router";
import { StudentCoursesTable } from "main/components/Courses/StudentCoursesTable";

export default function OnboardingSelectCoursesComponent() {
  return (
    <>
      <Row>
        <h1>Joining Courses</h1>
        <p>
          Next, you&#39;re listed on the roster of the courses below. Follow the
          prompts to join:
        </p>
        <StudentCoursesTable testid={"OnboardingCoursesTable"} />
      </Row>
      <Row className="pt-5 justify-content-center pb-3">
        <Col xs="auto" className="text-center">
          <Button as={Link} to="/onboarding/success">
            Continue
          </Button>
        </Col>
      </Row>
    </>
  );
}
