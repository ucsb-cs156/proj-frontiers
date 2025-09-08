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
  const [postModal, setPostModal] = useState(false);
  const [csvModal, setCsvModal] = useState(false);
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
      onSuccess: () => onSuccessTeams(setPostModal),
      onError: (error) => {
        toast.error(
          `Error adding team: ${JSON.stringify(error.response.data, null, 2)}`,
        );
      },
    },
    [`/api/teams/all?courseId=${courseId}`],
  );

  const teamCsvMutation = useBackendMutation(
    objectToAxiosParamsCSV,
    {
      onSuccess: () => onSuccessTeams(setCsvModal),
      onError: (error) => {
        toast.error(
          `Error uploading CSV: ${JSON.stringify(error.response.data, null, 2)}`,
        );
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

  const downloadCsv = () => {
    window.open(`/api/csv/teams?courseId=${courseId}`, "_blank");
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
        show={postModal}
        onHide={() => setPostModal(false)}
        centered={true}
        data-testid={`${testIdPrefix}-post-modal`}
      >
        <ModalHeader closeButton>Add Individual Team</ModalHeader>
        <ModalBody>
          <TeamsForm submitAction={handlePostSubmit} cancelDisabled={true} />
        </ModalBody>
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
            onClick={() => setPostModal(true)}
            data-testid={`${testIdPrefix}-post-button`}
            className="w-100"
          >
            Add Individual Team
          </Button>
        </Col>
        <Col>
          <Button onClick={downloadCsv} className="w-100">
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
