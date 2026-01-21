import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import { ProgressBar, Row, Col, Button } from "react-bootstrap";
import { Link } from "react-router";

export default function OnboardingSuccessPage() {
  return (
    <BasicLayout>
      <Row>
        <h1>Complete!</h1>
        <p>Congratulations on completing the onboarding process!</p>
      </Row>
      <Row className="">
        <Col xs="auto" className="text-center">
          <Button as={Link} to="/">
            Click here to return home
          </Button>
        </Col>
      </Row>
      <Row className="mt-auto pb-3">
        <ProgressBar now={100} />
      </Row>
    </BasicLayout>
  );
}
