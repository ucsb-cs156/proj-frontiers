import { Button } from "react-bootstrap";

export default function SingleButtonJobForm({ callback, text }) {
  return (
    <Button onClick={callback} data-testid="singlebutton-job-submit">
      {text}
    </Button>
  );
}
