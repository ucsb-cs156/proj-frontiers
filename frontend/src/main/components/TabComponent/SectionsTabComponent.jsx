import React, { useState } from "react";
import { Button, Col, Row } from "react-bootstrap";
import Modal from "react-bootstrap/Modal";
import { toast } from "react-toastify";

import { useBackend, useBackendMutation } from "main/utils/useBackend";
import SectionsForm from "main/components/Sections/SectionsForm";
import SectionsTable from "main/components/Sections/SectionsTable";
import { onSectionMutationError } from "main/utils/sectionsUtils";

export default function SectionsTabComponent({ courseId, testIdPrefix }) {
  const [showCreateModal, setShowCreateModal] = useState(false);

  const sectionsQueryKey = `/api/courses/${courseId}/sections`;

  const { data: sections } = useBackend(
    [sectionsQueryKey],
    // Stryker disable next-line StringLiteral : GET and empty string are equivalent
    { method: "GET", url: sectionsQueryKey },
    [],
    true,
  );

  const objectToAxiosParamsPost = (section) => ({
    url: sectionsQueryKey,
    method: "POST",
    params: {
      section: section.section,
      label: section.label,
    },
  });

  const onCreateSuccess = () => {
    toast("Section successfully created.");
    setShowCreateModal(false);
  };

  const createMutation = useBackendMutation(
    objectToAxiosParamsPost,
    { onSuccess: onCreateSuccess, onError: onSectionMutationError },
    [sectionsQueryKey],
  );

  const handleCreateSubmit = (section) => {
    createMutation.mutate(section);
  };

  return (
    <div data-testid={`${testIdPrefix}-sections-tab-component`}>
      <Modal
        show={showCreateModal}
        onHide={() => setShowCreateModal(false)}
        centered={true}
        data-testid={`${testIdPrefix}-create-section-modal`}
      >
        <Modal.Header closeButton>
          <Modal.Title>Create Section</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <SectionsForm submitAction={handleCreateSubmit} />
        </Modal.Body>
      </Modal>
      <Row sm={4} className="p-2 g-3">
        <Col>
          <Button
            onClick={() => setShowCreateModal(true)}
            data-testid={`${testIdPrefix}-create-section-button`}
            className="w-100"
          >
            Create Section
          </Button>
        </Col>
      </Row>
      <Row>
        <SectionsTable
          sections={sections}
          courseId={courseId}
          testIdPrefix={`${testIdPrefix}-sections-table`}
        />
      </Row>
    </div>
  );
}
