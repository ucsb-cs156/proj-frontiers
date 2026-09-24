import React, { useState } from "react";
import { Button, Card, Col, Row } from "react-bootstrap";
import Modal from "react-bootstrap/Modal";
import { toast } from "react-toastify";

import { useBackend, useBackendMutation } from "main/utils/useBackend";
import DokkuAccountTranslationsForm from "main/components/Dokku/DokkuAccountTranslationsForm";
import DokkuAccountTranslationsTable from "main/components/Dokku/DokkuAccountTranslationsTable";
import DokkuUsersListHeaderForm from "main/components/Dokku/DokkuUsersListHeaderForm";
import {
  dokkuTranslationsQueryKey,
  dokkuUsersListHeaderQueryKey,
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

  // The header is plain text with line breaks, so it travels in the request
  // body rather than as a query parameter.
  const headerQueryKey = dokkuUsersListHeaderQueryKey(courseId);
  const { data: usersListHeader } = useBackend(
    [headerQueryKey],
    // Stryker disable next-line StringLiteral : GET and empty string are equivalent
    {
      method: "GET",
      url: "/api/dokku/users_list_header",
      params: { courseId },
    },
    "",
    true,
  );

  const objectToAxiosParamsHeader = (formData) => ({
    url: "/api/dokku/users_list_header",
    method: "PUT",
    params: { courseId: courseId },
    headers: { "Content-Type": "text/plain" },
    data: formData.dokkuUsersListHeader,
  });

  const onHeaderSaved = () => {
    toast("Dokku users list header saved.");
  };

  const headerMutation = useBackendMutation(
    objectToAxiosParamsHeader,
    { onSuccess: onHeaderSaved },
    [headerQueryKey],
  );

  const handleHeaderSubmit = (formData) => {
    headerMutation.mutate(formData);
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

      <Card className="mt-3">
        <Card.Header as="h5">Dokku Users List Header</Card.Header>
        <Card.Body>
          <Card.Text data-testid={`${testIdPrefix}-dokku-header-text`}>
            Lines entered here are placed, exactly as written, at the very start
            of dokku_users_list.csv, before the lines generated for staff and
            teams. Use it for people who need dokku access but are not part of
            this course, such as ECI staff or people working on projects outside
            the course enrollment. One username,dokku-nn entry per line.
          </Card.Text>
          <DokkuUsersListHeaderForm
            initialContents={usersListHeader}
            submitAction={handleHeaderSubmit}
          />
        </Card.Body>
      </Card>
    </div>
  );
}
