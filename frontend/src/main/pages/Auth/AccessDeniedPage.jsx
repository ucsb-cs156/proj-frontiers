import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import { Link } from "react-router";
import { Button } from "react-bootstrap";

export default function AccessDeniedPage() {
  return (
    <BasicLayout>
      <h2>You do not have access to this page.</h2>
      <Button as={Link} to="/">
        Return
      </Button>
    </BasicLayout>
  );
}
