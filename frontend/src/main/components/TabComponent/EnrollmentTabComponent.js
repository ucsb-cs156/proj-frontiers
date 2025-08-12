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
import RosterStudentCSVUploadForm from "main/components/RosterStudent/RosterStudentCSVUploadForm";
import RosterStudentForm from "main/components/RosterStudent/RosterStudentForm";
import RosterStudentTable from "main/components/RosterStudent/RosterStudentTable";
import Modal from "react-bootstrap/Modal";

export default function EnrollmentTabComponent({
  courseId,
  testIdPrefix,
  currentUser,
}) {
  const [postModal, showPostModal] = useState(false);
  const [csvModal, showCsvModal] = useState(false);
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
    { onSuccess: () => onSuccessRoster(showPostModal) },
    [`/api/rosterstudents/course/${courseId}`],
  );

  const rosterCsvMutation = useBackendMutation(
    objectToAxiosParamsCSV,
    { onSuccess: () => onSuccessRoster(showCsvModal) },
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
        show={csvModal}
        onHide={() => showCsvModal(false)}
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
        onHide={() => showPostModal(false)}
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
      <Row sm={3} className="p-2">
        <Col>
          <Button
            onClick={() => showCsvModal(true)}
            data-testid={`${testIdPrefix}-csv-button`}
            className="w-100"
          >
            Upload CSV Roster
          </Button>
        </Col>
        <Col>
          <Button
            onClick={() => showPostModal(true)}
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
      <Row>
        <RosterStudentTable
          students={rosterStudents.filter((student) => {
            const searchTermLower = searchTerm.toLowerCase();
            const fullName = `${student.firstName} ${student.lastName}`;
            if (student.studentId.toLowerCase().includes(searchTermLower)) {
              return true;
            } else if (student.email.toLowerCase().includes(searchTermLower)) {
              return true;
            } else if (
              student.githubLogin?.toLowerCase().includes(searchTermLower)
            ) {
              return true;
            } else if (fullName.toLowerCase().includes(searchTermLower)) {
              return true;
            }
            return false;
          })}
          currentUser={currentUser}
          courseId={courseId}
          testIdPrefix={`${testIdPrefix}-RosterStudentTable`}
        />
      </Row>
    </div>
  );
}
