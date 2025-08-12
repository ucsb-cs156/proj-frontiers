import { Button } from "react-bootstrap";

export default function SingleButtonJobForm({ callback, text, testid }) {
  return (
    <Button onClick={callback} data-testid={`${testid}-job-submit`}>
      {text}
    </Button>
  );
}
