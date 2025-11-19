import { toast } from "react-toastify";
import { useBackend, useBackendMutation } from "main/utils/useBackend";
import { Link } from "react-router";
import React, { useState } from "react";
import {
  Button,
  Col,
  Form,
  ModalBody,
  ModalHeader,
  Row,
} from "react-bootstrap";
import RosterStudentCSVUploadForm from "main/components/RosterStudent/RosterStudentCSVUploadForm";
import RosterStudentForm from "main/components/RosterStudent/RosterStudentForm";
import RosterStudentTable from "main/components/RosterStudent/RosterStudentTable";
import Modal from "react-bootstrap/Modal";
import DroppedStudentsTable from "main/components/RosterStudent/DroppedStudentsTable";
import OverlayTrigger from "react-bootstrap/OverlayTrigger";
import Tooltip from "react-bootstrap/Tooltip";


export default function EnrollmentTabComponent({
  courseId,
  testIdPrefix,
  currentUser,
}) {
  const [postModal, setPostModal] = useState(false);
  const [csvModal, setCsvModal] = useState(false);
  const [csvErrorModal, setCsvErrorModal] = useState(false);
  const [csvErrorModalData, setCsvErrorModalData] = useState(null);

  const { data: rosterStudents } = useBackend(
    [`/api/rosterstudents/course/${courseId}`],
    // Stryker disable next-line StringLiteral : GET and empty string are equivalent
    { method: "GET", url: `/api/rosterstudents/course/${courseId}` },
    [],
    true,
  );
  const [searchTerm, setSearchTerm] = useState("");

  const objectToAxiosParamsCSV = (formData) => {
    const file = new FormData();
    file.append("file", formData.upload[0]);
    return {
      url: `/api/rosterstudents/upload/csv`,
      data: file,
      params: {
        courseId: courseId,
      },
      method: "POST",
    };
  };

  const objectToAxiosParamsPost = (student) => ({
    url: `/api/rosterstudents/post`,
    method: "POST",
    params: {
      courseId: courseId,
      firstName: student.firstName,
      lastName: student.lastName,
      studentId: student.studentId,
      email: student.email,
    },
  });

  const onSuccessRoster = (modalFn) => {
    toast("Roster successfully updated.");
    // Clear the search filter to show the updated roster
    setSearchTerm("");
    modalFn(false);
  };

  const rosterPostMutation = useBackendMutation(
    objectToAxiosParamsPost,
    {
      onSuccess: () => onSuccessRoster(setPostModal),
      onError: (error) => {
        toast.error(
          `Error adding student: ${JSON.stringify(error.response.data, null, 2)}`,
        );
      },
    },
    [`/api/rosterstudents/course/${courseId}`],
  );

  const rosterCsvMutation = useBackendMutation(
    objectToAxiosParamsCSV,
    {
      onSuccess: () => onSuccessRoster(setCsvModal),
      onError: (error) => {
        if (error.response.status !== 409) {
          toast.error(
            `Error uploading CSV: ${JSON.stringify(error.response.data, null, 2)}`,
          );
        } else {
          setCsvErrorModal(true);
          setCsvErrorModalData(error.response.data.rejected);
          setCsvModal(false);
        }
      },
    },
    [`/api/rosterstudents/course/${courseId}`],
  );

  const handleCsvSubmit = (formData) => {
    rosterCsvMutation.mutate(formData);
  };

  const handlePostSubmit = (student) => {
    rosterPostMutation.mutate(student);
  };

  const downloadCsv = () => {
    window.open(`/api/csv/rosterstudents?courseId=${courseId}`, "_blank");
  };

  return (
    <div data-testid={`${testIdPrefix}-EnrollmentTabComponent`}>
      <Modal
        show={csvErrorModal}
        onHide={() => setCsvErrorModal(false)}
        centered={true}
        data-testid={`${testIdPrefix}-csv-error-modal`}
        size="lg"
      >
        <ModalHeader closeButton>Upload CSV Roster</ModalHeader>
        <ModalBody>
          <p>
            The following students couldn't be uploaded to the roster as their
            emails and student IDs match two separate students:
          </p>
          <RosterStudentTable
            students={csvErrorModalData}
            testIdPrefix={`${testIdPrefix}-RosterStudentTable-csv-error`}
          />
        </ModalBody>
      </Modal>
      <Modal
        show={csvModal}
        onHide={() => setCsvModal(false)}
        centered={true}
        data-testid={`${testIdPrefix}-csv-modal`}
      >
        <ModalHeader closeButton>Upload CSV Roster</ModalHeader>
        <ModalBody>
          <RosterStudentCSVUploadForm submitAction={handleCsvSubmit} />
        </ModalBody>
      </Modal>
      <Modal
        show={postModal}
        onHide={() => setPostModal(false)}
        centered={true}
        data-testid={`${testIdPrefix}-post-modal`}
      >
        <ModalHeader closeButton>Add Individual Student</ModalHeader>
        <ModalBody>
          <RosterStudentForm
            submitAction={handlePostSubmit}
            cancelDisabled={true}
          />
        </ModalBody>
      </Modal>
      <Row sm={3} className="p-2">
        <Col>
          <div className="d-flex align-items-center gap-2">
            <Button
              onClick={() => setCsvModal(true)}
              data-testid={`${testIdPrefix}-csv-button`}
              className="flex-grow-1"
            >
              Upload CSV Roster
            </Button>

            <OverlayTrigger
              placement="right"
              overlay={
                <Tooltip id={`${testIdPrefix}-csv-help-tooltip`}>
                  CSV Upload format Help
                </Tooltip>
              }
            >
              <span className="d-inline-block" style={{ position: "relative" }}>
                <Link
                  to="/help/csv"
                  target="_blank"
                  rel="noopener noreferrer"
                  aria-label="CSV upload help"
                  data-testid={`${testIdPrefix}-csv-info-link`}
                  className="d-flex align-items-center text-decoration-none text-primary"
                  style={{ cursor: "pointer", fontSize: "1.4rem" }}
                >
                  ℹ️
                </Link>
              </span>
            </OverlayTrigger>
          </div>
        </Col>        
        <Col>
          <Button
            onClick={() => setPostModal(true)}
            data-testid={`${testIdPrefix}-post-button`}
            className="w-100"
          >
            Add Individual Student
          </Button>
        </Col>
        <Col>
          <Button onClick={downloadCsv} className="w-100">
            Download Student CSV
          </Button>
        </Col>
      </Row>
      <Row className="mb-1">
        <Form>
          <Form.Group as={Row} controlId="searchFilter">
            <Form.Label column sm={2}>
              Search Students:
            </Form.Label>
            <Col sm={10}>
              <Form.Control
                type="text"
                placeholder="Search by name, email, student ID, or Github Login"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                data-testid={`${testIdPrefix}-search`}
              />
            </Col>
          </Form.Group>
        </Form>
      </Row>
      <Row>
        <RosterStudentTable
          students={rosterStudents
            .filter((student) => {
              const searchTermLower = searchTerm.toLowerCase();
              const fullName = `${student.firstName} ${student.lastName}`;
              if (student.studentId.toLowerCase().includes(searchTermLower)) {
                return true;
              } else if (
                student.email.toLowerCase().includes(searchTermLower)
              ) {
                return true;
              } else if (
                student.githubLogin?.toLowerCase().includes(searchTermLower)
              ) {
                return true;
              } else if (fullName.toLowerCase().includes(searchTermLower)) {
                return true;
              }
              return false;
            })
            .filter((student) => student.rosterStatus !== "DROPPED")}
          currentUser={currentUser}
          courseId={courseId}
          testIdPrefix={`${testIdPrefix}-RosterStudentTable`}
        />
      </Row>
      <Row>
        <h2>Dropped Students</h2>
        <DroppedStudentsTable
          students={rosterStudents.filter(
            (student) => student.rosterStatus === "DROPPED",
          )}
          courseId={courseId}
        />
      </Row>
    </div>
  );
}
