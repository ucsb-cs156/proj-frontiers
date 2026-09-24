import React, { useState } from "react";
import { Accordion, Button, Form } from "react-bootstrap";
import { toast } from "react-toastify";
import { useBackend } from "main/utils/useBackend";
import {
  EMAIL_FORMATS,
  courseEmailsQueryKey,
} from "main/utils/courseEmailsUtils";

/**
 * A card, open by default, that shows the email addresses of the people of
 * the given type (STUDENTS, STAFF or ALL) in a course, in a read only text
 * area with a button to copy them.
 */
export default function CourseEmailsCard({
  courseId,
  type,
  testIdPrefix = "CourseEmailsCard",
}) {
  const [format, setFormat] = useState(EMAIL_FORMATS[0].value);

  const { data: emails } = useBackend(
    [courseEmailsQueryKey(courseId), type, format],
    {
      // Stryker disable next-line StringLiteral : GET and empty string are equivalent
      method: "GET",
      url: "/api/courses/emails",
      params: { courseId, type, format },
    },
    "",
    true,
  );

  const copyEmails = async () => {
    try {
      await navigator.clipboard.writeText(emails);
      toast("Emails copied to the clipboard.");
    } catch {
      toast("Unable to copy the emails to the clipboard.");
    }
  };

  return (
    <div className="card mt-4" data-testid={`${testIdPrefix}-card`}>
      <Accordion flush defaultActiveKey="emails">
        <Accordion.Item eventKey="emails">
          <Accordion.Header>Emails</Accordion.Header>
          <Accordion.Body>
            <Form.Group className="mb-3" controlId={`${testIdPrefix}-format`}>
              <Form.Label>Format</Form.Label>
              <Form.Select
                value={format}
                onChange={(e) => setFormat(e.target.value)}
                data-testid={`${testIdPrefix}-format`}
              >
                {EMAIL_FORMATS.map((f) => (
                  <option key={f.value} value={f.value}>
                    {f.label}
                  </option>
                ))}
              </Form.Select>
            </Form.Group>
            <Form.Control
              as="textarea"
              rows={8}
              readOnly
              value={emails}
              aria-label="Emails"
              data-testid={`${testIdPrefix}-emails`}
              style={{
                fontFamily: "monospace",
                backgroundColor: "black",
                color: "white",
              }}
            />
            <Button
              className="mt-3"
              onClick={copyEmails}
              disabled={emails === ""}
              data-testid={`${testIdPrefix}-copy`}
            >
              Copy
            </Button>
          </Accordion.Body>
        </Accordion.Item>
      </Accordion>
    </div>
  );
}
