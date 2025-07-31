import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import { Link } from "react-router-dom";
import { Button } from "react-bootstrap";

export default function NotFoundPage() {
  return (
    <BasicLayout>
      <h1>Page Not Found</h1>
      <p>Let's get you back on track.</p>
      <Button as={Link} to="/">
        Click to Return Home
      </Button>
    </BasicLayout>
  );
}
