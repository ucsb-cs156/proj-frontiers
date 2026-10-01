import React from "react";
import { Alert, Button } from "react-bootstrap";
import Modal from "react-bootstrap/Modal";
import { toast } from "react-toastify";

import { formatTime } from "main/utils/dateUtils";
import {
  API_KEYS_DOCS_URL,
  studentInfoCurlExample,
} from "main/utils/apiKeyUtils";

/**
 * Shows a key that was just created. This is the only time the key is ever
 * visible, so the modal says so and offers a copy button.
 *
 * @param issuedKey the response of POST /api/courses/key, or null to hide
 */
export default function NewApiKeyModal({
  issuedKey,
  courseId,
  onClose,
  testIdPrefix = "NewApiKeyModal",
}) {
  if (!issuedKey) {
    return null;
  }

  const copyKey = async () => {
    await navigator.clipboard.writeText(issuedKey.key);
    toast("API key copied to clipboard.");
  };

  return (
    <Modal
      show={true}
      onHide={onClose}
      centered={true}
      size="lg"
      data-testid={`${testIdPrefix}-modal`}
    >
      <Modal.Header closeButton>
        <Modal.Title>Your new API key</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <Alert variant="warning" data-testid={`${testIdPrefix}-warning`}>
          Copy this key now. Frontiers does not store it, so it cannot be shown
          again. If you lose it, revoke it and create a new one.
        </Alert>
        <p>
          Key: <code data-testid={`${testIdPrefix}-key`}>{issuedKey.key}</code>
          <Button
            size="sm"
            variant="outline-secondary"
            className="ms-2"
            onClick={copyKey}
            data-testid={`${testIdPrefix}-copy`}
          >
            Copy
          </Button>
        </p>
        <p data-testid={`${testIdPrefix}-details`}>
          {issuedKey.label ? `Label: ${issuedKey.label}. ` : ""}
          Expires: {formatTime(issuedKey.expiresAt)}.
        </p>
        <p data-testid={`${testIdPrefix}-example-intro`}>
          Example: look up a student the way a Gradescope autograder would (see{" "}
          <a
            href={API_KEYS_DOCS_URL}
            target="_blank"
            rel="noopener noreferrer"
            data-testid={`${testIdPrefix}-docs-link`}
          >
            the API key documentation
          </a>{" "}
          for the other endpoints):
        </p>
        <pre
          className="bg-light p-2 rounded"
          data-testid={`${testIdPrefix}-example`}
        >
          {studentInfoCurlExample(courseId, issuedKey.key)}
        </pre>
      </Modal.Body>
      <Modal.Footer>
        <Button onClick={onClose} data-testid={`${testIdPrefix}-close`}>
          I have copied the key
        </Button>
      </Modal.Footer>
    </Modal>
  );
}
