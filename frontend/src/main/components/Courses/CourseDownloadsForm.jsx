import React from "react";
import { Button, Form } from "react-bootstrap";

export default function CourseDownloadsForm({ downloadAction, testIdPrefix }) {
  const handleSubmit = (event) => {
    event.preventDefault();
    downloadAction();
  };

  return (
    <Form onSubmit={handleSubmit}>
      <Form.Group>
        <h3 data-testid={`${testIdPrefix}-downloads-header`}>
          Course Downloads
        </h3>
        <Form.Text className="text-muted d-block mb-3">
          Export utility options available for this course instance.
        </Form.Text>
      </Form.Group>
      {}
      <Button
        type="submit"
        variant="primary"
        data-testid={`${testIdPrefix}-btn-download-students-csv`}
      >
        Download Students CSV
      </Button>
    </Form>
  );
}
