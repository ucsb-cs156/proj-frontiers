import { toast } from "react-toastify";
import { useBackend, useBackendMutation } from "main/utils/useBackend";
import React, { useState } from "react";
import { Accordion, Col, Form, Row } from "react-bootstrap";
import RosterStudentCSVUploadForm from "main/components/RosterStudent/RosterStudentCSVUploadForm";
import RosterStudentForm from "main/components/RosterStudent/RosterStudentForm";
import RosterStudentTable from "main/components/RosterStudent/RosterStudentTable";

export default function EnrollmentTabComponent({
  courseId,
  testIdPrefix,
  currentUser,
}) {
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
      url: `/api/rosterstudents/upload/egrades`,
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

  const onSuccessRoster = () => {
    toast("Roster successfully updated.");
    // Clear the search filter to show the updated roster
    setSearchTerm("");
  };

  const rosterPostMutation = useBackendMutation(
    objectToAxiosParamsPost,
    { onSuccess: onSuccessRoster },
    [`/api/rosterstudents/course/${courseId}`],
  );

  const rosterCsvMutation = useBackendMutation(
    objectToAxiosParamsCSV,
    { onSuccess: onSuccessRoster },
    [`/api/rosterstudents/course/${courseId}`],
  );

  const handleCsvSubmit = (formData) => {
    rosterCsvMutation.mutate(formData);
  };

  const handlePostSubmit = (student) => {
    rosterPostMutation.mutate(student);
  };

  return (
    <div data-testid={`${testIdPrefix}-EnrollmentTabComponent`}>
      <Row className="py-3">
        <Accordion defaultActiveKey="upload">
          <Accordion.Item eventKey="upload">
            <Accordion.Header>Upload Roster</Accordion.Header>
            <Accordion.Body>
              <RosterStudentCSVUploadForm submitAction={handleCsvSubmit} />
            </Accordion.Body>
          </Accordion.Item>
          <Accordion.Item eventKey="individual">
            <Accordion.Header>Add Individual Student</Accordion.Header>
            <Accordion.Body>
              <RosterStudentForm
                submitAction={handlePostSubmit}
                cancelDisabled={true}
              />
            </Accordion.Body>
          </Accordion.Item>
        </Accordion>
      </Row>
      <Row className="mb-3">
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
