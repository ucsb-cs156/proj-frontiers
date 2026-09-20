import React, { useState } from "react";
import { Button, Col, Row } from "react-bootstrap";
import Modal from "react-bootstrap/Modal";
import { toast } from "react-toastify";

import { useBackend, useBackendMutation } from "main/utils/useBackend";
import SectionsForm from "main/components/Sections/SectionsForm";
import SectionsTable from "main/components/Sections/SectionsTable";
import { onSectionMutationError } from "main/utils/sectionsUtils";
import { useCourseOptions } from "main/utils/courseOptionsUtils";

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

  // The Slack Channel Name field is only shown when the SLACK_INTEGRATION
  // course option is enabled and the course has an active Slack token
  // (i.e. it has connected to a Slack workspace).
  const { data: optionsMap = {} } = useCourseOptions(courseId);
  const slackInfoQueryKey = `/api/courses/slack/info?courseId=${courseId}`;
  const { data: slackInfo = {} } = useBackend(
    [slackInfoQueryKey],
    // Stryker disable next-line StringLiteral : GET and empty string are equivalent
    { method: "GET", url: slackInfoQueryKey },
    {},
    true,
    // Stryker disable next-line all : only affects when the query runs, not the response shape
    { enabled: optionsMap.SLACK_INTEGRATION === true },
  );
  const showSlackChannel =
    optionsMap.SLACK_INTEGRATION === true && Boolean(slackInfo.slackTeamId);

  const objectToAxiosParamsPost = (section) => ({
    url: sectionsQueryKey,
    method: "POST",
    params: {
      section: section.section,
      label: section.label,
      ...(showSlackChannel && {
        slackChannelName: section.slackChannelName,
      }),
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
          <SectionsForm
            submitAction={handleCreateSubmit}
            showSlackChannel={showSlackChannel}
          />
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
          showSlackChannel={showSlackChannel}
        />
      </Row>
    </div>
  );
}
