import React, { useState } from "react";
import { Button, Col, Row } from "react-bootstrap";
import Modal from "react-bootstrap/Modal";
import { toast } from "react-toastify";

import { useBackend, useBackendMutation } from "main/utils/useBackend";
import NewAssignmentsTable from "main/components/NewAssignments/NewAssignmentsTable";
import NewIndividualAssignmentForm from "main/components/NewAssignments/NewIndividualAssignmentForm";
import NewTeamAssignmentForm from "main/components/NewAssignments/NewTeamAssignmentForm";
import {
  JOBS_QUERY_KEY,
  NEW_ASSIGNMENTS_URL,
  assignmentsQueryKey,
  formDataToParams,
  jobStartedMessage,
  onAssignmentMutationError,
} from "main/utils/newAssignmentsUtils";

export default function NewAssignmentsTabComponent({ courseId, testIdPrefix }) {
  // which kind of assignment the create modal is for, or null when it is closed
  const [createType, setCreateType] = useState(null);

  const queryKey = assignmentsQueryKey(courseId);

  const { data: assignments } = useBackend(
    [queryKey],
    {
      // Stryker disable next-line StringLiteral : GET and empty string are equivalent
      method: "GET",
      url: NEW_ASSIGNMENTS_URL,
      params: { courseId },
    },
    [],
    true,
  );

  const objectToAxiosParamsPost = (formData) => ({
    url: `${NEW_ASSIGNMENTS_URL}/post`,
    method: "POST",
    params: {
      courseId: courseId,
      asnType: createType,
      ...formDataToParams(createType, formData),
    },
  });

  const onCreateSuccess = (data) => {
    toast(jobStartedMessage("created", data));
    setCreateType(null);
  };

  const createMutation = useBackendMutation(
    objectToAxiosParamsPost,
    { onSuccess: onCreateSuccess, onError: onAssignmentMutationError },
    [queryKey, JOBS_QUERY_KEY],
  );

  const handleCreateSubmit = (formData) => {
    createMutation.mutate(formData);
  };

  const creatingTeam = createType === "TEAM";
  const CreateForm = creatingTeam
    ? NewTeamAssignmentForm
    : NewIndividualAssignmentForm;

  return (
    <div data-testid={`${testIdPrefix}-new-assignments-tab-component`}>
      <Modal
        show={createType !== null}
        onHide={() => setCreateType(null)}
        centered={true}
        data-testid={`${testIdPrefix}-create-assignment-modal`}
      >
        <Modal.Header closeButton>
          <Modal.Title>
            Create {creatingTeam ? "Team" : "Individual"} Assignment
          </Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <p data-testid={`${testIdPrefix}-create-assignment-modal-note`}>
            Saving starts a job that creates the repositories.
          </p>
          <CreateForm submitAction={handleCreateSubmit} />
        </Modal.Body>
      </Modal>
      <Row sm={4} className="p-2 g-3">
        <Col>
          <Button
            onClick={() => setCreateType("INDIVIDUAL")}
            data-testid={`${testIdPrefix}-create-individual-assignment-button`}
            className="w-100"
          >
            Create Individual Assignment
          </Button>
        </Col>
        <Col>
          <Button
            onClick={() => setCreateType("TEAM")}
            data-testid={`${testIdPrefix}-create-team-assignment-button`}
            className="w-100"
          >
            Create Team Assignment
          </Button>
        </Col>
      </Row>
      <Row>
        <NewAssignmentsTable
          assignments={assignments}
          courseId={courseId}
          testIdPrefix={`${testIdPrefix}-new-assignments-table`}
        />
      </Row>
    </div>
  );
}
