import React, { useEffect, useState } from "react";
import { useBackend } from "main/utils/useBackend";

import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import InstructorCoursesTable from "main/components/Courses/InstructorCoursesTable";
import { useCurrentUser } from "main/utils/currentUser";
import { useNavigate, useParams } from "react-router";

import Modal from "react-bootstrap/Modal";
import { Button, Tab, Tabs } from "react-bootstrap";
import AssignmentTabComponent from "main/components/TabComponent/AssignmentTabComponent";
import EnrollmentTabComponent from "main/components/TabComponent/EnrollmentTabComponent";
import StaffTabComponent from "main/components/TabComponent/StaffTabComponent";

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

  const navigate = useNavigate();
  useEffect(() => {
    if (getCourseFailed) {
      setShowErrorModal(true);
      const timer = setTimeout(() => {
        navigate("/", { replace: true });
      }, 3000);
      // Stryker disable next-line BlockStatement
      return () => {
        clearTimeout(timer);
      };
    }
  }, [getCourseFailed, navigate]);

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
      <Tabs defaultActiveKey={"enrollment"} className="instructor-course-tabs">
        <Tab eventKey={"enrollment"} title={"Enrollment"} className="pt-2">
          <EnrollmentTabComponent
            courseId={courseId}
            testIdPrefix={testId}
            currentUser={currentUser}
          />
        </Tab>
        <Tab eventKey={"staff"} title={"Staff"} className="pt-2">
          <StaffTabComponent
            courseId={courseId}
            testIdPrefix={testId}
            currentUser={currentUser}
          />
        </Tab>
        <Tab eventKey={"assignments"} title={"Assignments"} className="pt-2">
          <AssignmentTabComponent courseId={courseId} />
        </Tab>
        <Tab eventKey={"settings"} title={"Settings"} className="pt-2">
          <InstructorCoursesTable
            courses={course ? [course] : []}
            currentUser={currentUser}
            testId={testId}
          />
        </Tab>
      </Tabs>
    </BasicLayout>
  );
}
