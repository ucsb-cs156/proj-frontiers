import React, { useState } from "react";
import { Button, Card, Col, Row } from "react-bootstrap";
import Modal from "react-bootstrap/Modal";
import { toast } from "react-toastify";

import { useBackend, useBackendMutation } from "main/utils/useBackend";
import DokkuAccountTranslationsForm from "main/components/Dokku/DokkuAccountTranslationsForm";
import DokkuAccountTranslationsTable from "main/components/Dokku/DokkuAccountTranslationsTable";
import {
  dokkuTranslationsQueryKey,
  dokkuUsersListUrl,
  onDokkuTranslationMutationError,
} from "main/utils/dokkuUtils";

export default function DokkuTabComponent({ courseId, testIdPrefix }) {
  const [showCreateModal, setShowCreateModal] = useState(false);

  const queryKey = dokkuTranslationsQueryKey(courseId);

  const { data: translations } = useBackend(
    [queryKey],
    // Stryker disable next-line StringLiteral : GET and empty string are equivalent
    { method: "GET", url: "/api/dokku/translations", params: { courseId } },
    [],
    true,
  );

  const downloadCsv = () => {
    window.open(dokkuUsersListUrl(courseId), "_blank");
  };

  const objectToAxiosParamsPost = (translation) => ({
    url: "/api/dokku/translations",
    method: "POST",
    params: {
      courseId: courseId,
      email: translation.email,
      username: translation.username,
    },
  });

  const onCreateSuccess = () => {
    toast("Dokku account translation successfully created.");
    setShowCreateModal(false);
  };

  const createMutation = useBackendMutation(
    objectToAxiosParamsPost,
    { onSuccess: onCreateSuccess, onError: onDokkuTranslationMutationError },
    [queryKey],
  );

  const handleCreateSubmit = (translation) => {
    createMutation.mutate(translation);
  };

  return (
    <div data-testid={`${testIdPrefix}-dokku-tab-component`}>
      <Modal
        show={showCreateModal}
        onHide={() => setShowCreateModal(false)}
        centered={true}
        data-testid={`${testIdPrefix}-create-dokku-translation-modal`}
      >
        <Modal.Header closeButton>
          <Modal.Title>Create Dokku Account Translation</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <DokkuAccountTranslationsForm submitAction={handleCreateSubmit} />
        </Modal.Body>
      </Modal>

      <Card className="mb-3">
        <Card.Header as="h5">Dokku Users List</Card.Header>
        <Card.Body>
          <Card.Text data-testid={`${testIdPrefix}-dokku-download-text`}>
            Download <code>dokku_users_list.csv</code>, the file used to set up
            access to the dokku servers. It has one{" "}
            <code>username,dokku-nn</code> line per grant of access: every staff
            member of this course gets access to every dokku instance, and each
            member of a team whose name ends in <code>-nn</code> gets access to{" "}
            <code>dokku-nn</code>. The username is the part of the email before
            the <code>@</code>, unless the email has a translation in the table
            below.
          </Card.Text>
          <Row sm={4} className="g-3">
            <Col>
              <Button
                onClick={downloadCsv}
                data-testid={`${testIdPrefix}-dokku-download-button`}
                className="w-100"
              >
                Download dokku_users_list.csv
              </Button>
            </Col>
          </Row>
        </Card.Body>
      </Card>

      <Card>
        <Card.Header as="h5">Dokku Account Translations</Card.Header>
        <Card.Body>
          <Card.Text data-testid={`${testIdPrefix}-dokku-translations-text`}>
            Use this table for the case when a person&apos;s dokku username does
            not match the part of their email that comes before the{" "}
            <code>@</code> sign. Each row maps an email address to the dokku
            username to use for it instead. The table is shared by all courses.
          </Card.Text>
          <Row sm={4} className="g-3 mb-3">
            <Col>
              <Button
                onClick={() => setShowCreateModal(true)}
                data-testid={`${testIdPrefix}-create-dokku-translation-button`}
                className="w-100"
              >
                Create Translation
              </Button>
            </Col>
          </Row>
          <Row>
            <DokkuAccountTranslationsTable
              translations={translations}
              courseId={courseId}
              testIdPrefix={`${testIdPrefix}-dokku-translations-table`}
            />
          </Row>
        </Card.Body>
      </Card>
    </div>
  );
}
