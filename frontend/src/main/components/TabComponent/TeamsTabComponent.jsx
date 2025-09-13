import { toast } from "react-toastify";
import { useBackend, useBackendMutation } from "main/utils/useBackend";
import React, { useState } from "react";
import {
  Button,
  Col,
  Form,
  ModalBody,
  ModalHeader,
  Row,
} from "react-bootstrap";
import TeamsCSVUploadForm from "main/components/Teams/TeamsCSVUploadForm";
import TeamsForm from "main/components/Teams/TeamsForm";
import Modal from "react-bootstrap/Modal";
import TeamsTable from "main/components/Teams/TeamsTable";

export default function TeamsTabComponent({
  courseId,
  testIdPrefix,
  currentUser,
}) {
  const [postTeamModal, setPostTeamModal] = useState(false);
  const [csvModal, setCsvModal] = useState(false);
  const [errorPostTeamModal, setErrorPostTeamModal] = useState(false);
  const [errorPostCSVTeamModal, setErrorPostCSVTeamModal] = useState(false);
  const [partialSuccessPostCSVTeamModal, setPartialSuccessPostCSVTeamModal] =
    useState(false);
  const [successPostCSVTeamModal, setSuccessPostCSVTeamModal] = useState(false);

  const { data: teams } = useBackend(
    [`/api/teams/all?courseId=${courseId}`],
    // Stryker disable next-line StringLiteral : GET and empty string are equivalent
    { method: "GET", url: `/api/teams/all?courseId=${courseId}` },
    [],
    true,
  );
  const [searchTeams, setSearchTeams] = useState("");

  const objectToAxiosParamsCSV = (formData) => {
    const file = new FormData();
    file.append("file", formData.upload[0]);
    return {
      url: `/api/teams/upload/csv`,
      data: file,
      params: {
        courseId: courseId,
      },
      method: "POST",
    };
  };

  const objectToAxiosParamsPost = (team) => ({
    url: `/api/teams/post`,
    method: "POST",
    params: {
      courseId: courseId,
      name: team.name,
    },
  });

  const onSuccessTeams = (modalFn) => {
    toast("Team successfully added.");
    setSearchTeams("");
    modalFn(false);
  };

  const teamPostMutation = useBackendMutation(
    objectToAxiosParamsPost,
    {
      onSuccess: () => onSuccessTeams(setPostTeamModal),
      onError: (error) => {
        setPostTeamModal(false);
        if (error.response.status === 409) {
          setErrorPostTeamModal({
            message: `Team name already exists. Please choose a different name.`,
          });
        } else {
          setErrorPostTeamModal({
            message: `${JSON.stringify(error.response.status)} error occurred while attempting to create team.`,
          });
        }
      },
    },
    [`/api/teams/all?courseId=${courseId}`],
  );

  const teamCsvMutation = useBackendMutation(
    objectToAxiosParamsCSV,
    {
      onSuccess: (data) => {
        setCsvModal(false);
        setSearchTeams("");
        setSuccessPostCSVTeamModal({
          message: `New members: ${data.created}, Existing members: ${data.existing}`,
        });
      },
      onError: (error) => {
        setCsvModal(false);
        setSearchTeams("");
        if (error.response.status === 400) {
          setErrorPostCSVTeamModal({
            message: `Upload failed (Error 400). Please ensure your CSV follows one of the formats documented in the 'Help' section.`,
          });
        } else if (error.response.status === 409) {
          setPartialSuccessPostCSVTeamModal({
            message: `CSV Import Complete with Rejected Members (Error 409). Rejected Students: ${JSON.stringify(error.response.data.rejected, null, 2)}, Existing Students: ${JSON.stringify(error.response.data.existing, null, 2)}, New Students: ${JSON.stringify(error.response.data.created, null, 2)}`,
          });
        } else {
          setErrorPostCSVTeamModal({
            message: `${JSON.stringify(error.response.status, null, 2)} error occurred while processing the CSV file.`,
          });
        }
      },
    },
    [`/api/teams/all?courseId=${courseId}`],
  );

  const handleCsvSubmit = (formData) => {
    teamCsvMutation.mutate(formData);
  };

  const handlePostSubmit = (team) => {
    teamPostMutation.mutate(team);
  };

  return (
    <div data-testid={`${testIdPrefix}-teams-tab-component`}>
      <Modal
        show={csvModal}
        onHide={() => setCsvModal(false)}
        centered={true}
        data-testid={`${testIdPrefix}-csv-modal`}
      >
        <ModalHeader closeButton>Upload Teams by CSV</ModalHeader>
        <ModalBody>
          <TeamsCSVUploadForm submitAction={handleCsvSubmit} />
        </ModalBody>
      </Modal>
      <Modal
        show={postTeamModal}
        onHide={() => setPostTeamModal(false)}
        centered={true}
        data-testid={`${testIdPrefix}-post-modal`}
      >
        <ModalHeader closeButton>Add Individual Team</ModalHeader>
        <ModalBody>
          <TeamsForm submitAction={handlePostSubmit} cancelDisabled={true} />
        </ModalBody>
      </Modal>
      <Modal
        show={errorPostTeamModal}
        onHide={() => setErrorPostTeamModal(false)}
        centered={true}
        data-testid={`${testIdPrefix}-error-post-team-modal`}
      >
        <ModalHeader closeButton>
          <h4 className="text-danger"> Error Creating Team </h4>
        </ModalHeader>
        <ModalBody>{errorPostTeamModal.message}</ModalBody>
      </Modal>
      <Modal
        show={errorPostCSVTeamModal}
        onHide={() => setErrorPostCSVTeamModal(false)}
        centered={true}
        data-testid={`${testIdPrefix}-error-post-csv-team-modal`}
      >
        <ModalHeader closeButton>
          <h4 className="text-danger"> CSV Import Unsuccessful </h4>
        </ModalHeader>
        <ModalBody>{errorPostCSVTeamModal.message}</ModalBody>
      </Modal>
      <Modal
        show={successPostCSVTeamModal}
        onHide={() => setSuccessPostCSVTeamModal(false)}
        centered={true}
        data-testid={`${testIdPrefix}-success-post-csv-team-modal`}
      >
        <ModalHeader closeButton>
          <h4 className="text-success"> CSV Import Successful </h4>
        </ModalHeader>
        <ModalBody>{successPostCSVTeamModal.message}</ModalBody>
      </Modal>
      <Modal
        show={partialSuccessPostCSVTeamModal}
        onHide={() => setPartialSuccessPostCSVTeamModal(false)}
        centered={true}
        data-testid={`${testIdPrefix}-partial-success-post-csv-team-modal`}
      >
        <ModalHeader closeButton>
          <h4 className="text-danger"> CSV Import Completed with Errors </h4>
        </ModalHeader>
        <ModalBody>{partialSuccessPostCSVTeamModal.message}</ModalBody>
      </Modal>
      <Row sm={3} className="p-2">
        <Col>
          <Button
            onClick={() => setCsvModal(true)}
            data-testid={`${testIdPrefix}-csv-button`}
            className="w-100"
          >
            Upload Teams by CSV
          </Button>
        </Col>
        <Col>
          <Button
            onClick={() => setPostTeamModal(true)}
            data-testid={`${testIdPrefix}-post-button`}
            className="w-100"
          >
            Add Individual Team
          </Button>
        </Col>
        <Col>
          <Button
            className="w-100"
            data-testid={`${testIdPrefix}-download-button`}
            disabled
          >
            Download Team CSV
          </Button>
        </Col>
      </Row>
      <Row className="mb-1 py-2">
        <Form>
          <Form.Group as={Row} controlId="searchFilter">
            <Form.Label column sm={2}>
              Search Teams:
            </Form.Label>
            <Col sm={10}>
              <Form.Control
                type="text"
                placeholder="Search by Team Name."
                value={searchTeams}
                onChange={(e) => setSearchTeams(e.target.value)}
                data-testid={`${testIdPrefix}-search`}
              />
            </Col>
          </Form.Group>
        </Form>
      </Row>
      <Row>
        <TeamsTable
          teams={(teams || []).filter((team) => {
            const searchTermLower = searchTeams.toLowerCase();
            if (team.name.toLowerCase().includes(searchTermLower)) {
              return true;
            }
            return false;
          })}
          currentUser={currentUser}
          courseId={courseId}
          testIdPrefix={`${testIdPrefix}-teams-table`}
        />
      </Row>
    </div>
  );
}
