import React, { useState } from "react";
import { Button, Card, Col, Row } from "react-bootstrap";
import Modal from "react-bootstrap/Modal";

import { useBackend, useBackendMutation } from "main/utils/useBackend";
import ApiKeysTable from "main/components/ApiKeys/ApiKeysTable";
import ApiKeyCreateForm from "main/components/ApiKeys/ApiKeyCreateForm";
import NewApiKeyModal from "main/components/ApiKeys/NewApiKeyModal";
import { API_KEYS_DOCS_URL, apiKeysQueryKey } from "main/utils/apiKeyUtils";

export default function ApiKeysTabComponent({ courseId, testIdPrefix }) {
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [issuedKey, setIssuedKey] = useState(null);

  const queryKey = apiKeysQueryKey(courseId);

  const { data: apiKeys } = useBackend(
    [queryKey],
    // Stryker disable next-line StringLiteral : GET and empty string are equivalent
    { method: "GET", url: "/api/courses/key", params: { courseId } },
    [],
    true,
  );

  // The label is optional; leaving the parameter out altogether when it is
  // blank keeps the request (and the backend's trimming rules) simple.
  const objectToAxiosParamsCreate = ({ label, choice }) => ({
    url: "/api/courses/key",
    method: "POST",
    params: label
      ? { courseId: courseId, choice: choice, label: label }
      : { courseId: courseId, choice: choice },
  });

  const onCreateSuccess = (data) => {
    setShowCreateModal(false);
    setIssuedKey(data);
  };

  const createMutation = useBackendMutation(
    objectToAxiosParamsCreate,
    { onSuccess: onCreateSuccess },
    [queryKey],
  );

  const handleCreateSubmit = (formData) => {
    createMutation.mutate(formData);
  };

  return (
    <div data-testid={`${testIdPrefix}-api-keys-tab-component`}>
      <Modal
        show={showCreateModal}
        onHide={() => setShowCreateModal(false)}
        centered={true}
        data-testid={`${testIdPrefix}-create-api-key-modal`}
      >
        <Modal.Header closeButton>
          <Modal.Title>Create API Key</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <ApiKeyCreateForm submitAction={handleCreateSubmit} />
        </Modal.Body>
      </Modal>

      <NewApiKeyModal
        issuedKey={issuedKey}
        courseId={courseId}
        onClose={() => setIssuedKey(null)}
      />

      <Card>
        <Card.Header as="h5">API Keys</Card.Header>
        <Card.Body>
          <Card.Text data-testid={`${testIdPrefix}-api-keys-text`}>
            API keys let a script, such as a Gradescope autograder or a GitHub
            Action, call a few Frontiers endpoints for this course without
            logging in. Each key works only for this course, expires after 90
            days or 6 months, and can be revoked at any time. A key is shown
            once, when it is created; Frontiers does not store it. Turning off
            the &quot;Enable Api Keys&quot; course option disables every key of
            the course. See{" "}
            <a
              href={API_KEYS_DOCS_URL}
              target="_blank"
              rel="noopener noreferrer"
              data-testid={`${testIdPrefix}-api-keys-docs-link`}
            >
              the API key documentation
            </a>{" "}
            for the endpoints that accept a key and examples.
          </Card.Text>
          <Row sm={4} className="g-3 mb-3">
            <Col>
              <Button
                onClick={() => setShowCreateModal(true)}
                data-testid={`${testIdPrefix}-create-api-key-button`}
                className="w-100"
              >
                Create API Key
              </Button>
            </Col>
          </Row>
          <Row>
            <ApiKeysTable
              apiKeys={apiKeys}
              courseId={courseId}
              testIdPrefix={`${testIdPrefix}-api-keys-table`}
            />
          </Row>
        </Card.Body>
      </Card>
    </div>
  );
}
