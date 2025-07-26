import React, { useEffect, useState } from "react";
import { useBackend, useBackendMutation } from "main/utils/useBackend";

import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import InstructorCoursesTable from "main/components/Courses/InstructorCoursesTable";
import { useCurrentUser } from "main/utils/currentUser";
import { useNavigate, useParams } from "react-router-dom";

import RosterStudentTable from "main/components/RosterStudent/RosterStudentTable";
import Modal from "react-bootstrap/Modal";
import { Accordion, Button, Card, Row, Tab, Tabs } from "react-bootstrap";
import RosterStudentCSVUploadForm from "main/components/RosterStudent/RosterStudentCSVUploadForm";
import { toast } from "react-toastify";
import RosterStudentForm from "main/components/RosterStudent/RosterStudentForm";
import IndividualAssignmentForm from "main/components/Assignments/IndividualAssignmentForm";

export default function InstructorCourseShowPage() {
  const currentUser = useCurrentUser();
  const courseId = useParams().id;
  const [showErrorModal, setShowErrorModal] = useState(false);

  const {
    data: course,
    error: _errorCourse,
    status: _statusCourse,
    failureCount: courseBackendFailureCount,
  } = useBackend(
    [`/api/courses/${courseId}`],
    // Stryker disable next-line StringLiteral : GET and empty string are equivalent
    { method: "GET", url: `/api/courses/${courseId}` },
    null,
    true,
  );

  const getCourseFailed = courseBackendFailureCount > 0;

  const {
    data: rosterStudents,
    error: _errorRosterStudents,
    status: _statusRosterStudents,
  } = useBackend(
    [`/api/rosterstudents/course/${courseId}`],
    // Stryker disable next-line StringLiteral : GET and empty string are equivalent
    { method: "GET", url: `/api/rosterstudents/course/${courseId}` },
    null,
    true,
  );

  const navigate = useNavigate();
  useEffect(() => {
    if (getCourseFailed) {
      setShowErrorModal(true);
      const timer = setTimeout(() => {
        navigate("/instructor/courses", { replace: true });
      }, 3000);
      return () => {
        clearTimeout(timer);
      };
    }
  }, [getCourseFailed, navigate]);

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

  const objectToAxiosParamsIndividualAssignment = (assignment) => ({
    url: `/api/repos/createRepos`,
    method: "POST",
    params: {
      courseId: courseId,
      repoPrefix: assignment.repoPrefix,
      isPrivate: assignment.assignmentPrivacy,
    },
  });

  const onSuccessRoster = () => {
    toast("Roster successfully updated.");
  };

  const onSuccessAssignment = () => {
    toast("Repository creation successfully started.");
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

  const indvidiualAssignmentMutation = useBackendMutation(
    objectToAxiosParamsIndividualAssignment,
    { onSuccess: onSuccessAssignment },
  );

  const handleCsvSubmit = (formData) => {
    rosterCsvMutation.mutate(formData);
  };

  const handlePostSubmit = (student) => {
    rosterPostMutation.mutate(student);
  };

  const postIndividualAssignment = (assignment) => {
    indvidiualAssignmentMutation.mutate(assignment);
  };

  const testId = "InstructorCourseShowPage";
  return (
    <BasicLayout>
      <Modal show={showErrorModal}>
        <Modal.Header>
          <Modal.Title>Course Not Found</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          Course not found. You will be returned to the course list in 3
          seconds.
        </Modal.Body>
        <Modal.Footer>
          <Button onClick={() => setShowErrorModal(false)} variant={"primary"}>
            Close
          </Button>
        </Modal.Footer>
      </Modal>
      <h1 data-testid={`${testId}-title`}>
        Course: {course ? `${course.courseName} (${course.id})` : "Loading..."}
      </h1>
      <Tabs defaultActiveKey={"default"}>
        <Tab eventKey={"default"} title={"Management"} className="pt-2">
          <InstructorCoursesTable
            courses={course ? [course] : []}
            currentUser={currentUser}
            testId={testId}
          />
          <Row md={2}>
            <Card>
              <Card.Header>Individual Assignment</Card.Header>
              <Card.Body>
                <IndividualAssignmentForm
                  submitAction={postIndividualAssignment}
                />
              </Card.Body>
            </Card>
          </Row>
        </Tab>
        <Tab eventKey={"enrollment"} title={"Enrollment"} className="pt-2">
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
          <Row>
            <RosterStudentTable
              // Stryker disable next-line ArrayDeclaration : checking for ["Stryker was here"] is tough
              students={rosterStudents || []}
              currentUser={currentUser}
              courseId={course ? course.id : ""}
              testIdPrefix={`${testId}-RosterStudentTable`}
            />
          </Row>
        </Tab>
      </Tabs>
    </BasicLayout>
  );
}
