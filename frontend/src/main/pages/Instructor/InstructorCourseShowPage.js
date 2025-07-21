import React, { useEffect, useState } from "react";
import { useBackend } from "main/utils/useBackend";

import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import InstructorCoursesTable from "main/components/Courses/InstructorCoursesTable";
import { useCurrentUser } from "main/utils/currentUser";
import { useNavigate, useParams } from "react-router-dom";

import RosterStudentTable from "main/components/RosterStudent/RosterStudentTable";
import Modal from "react-bootstrap/Modal";
import { Button } from "react-bootstrap";

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
      <div className="pt-2">
        <h1>Course</h1>
        <InstructorCoursesTable
          courses={course ? [course] : []}
          currentUser={currentUser}
          testId={testId}
        />
        <h2>Roster Students</h2>
        <RosterStudentTable
          // Stryker disable next-line ArrayDeclaration : checking for ["Stryker was here"] is tough
          students={rosterStudents || []}
          currentUser={currentUser}
          courseId={course ? course.id : ""}
          testIdPrefix={`${testId}-RosterStudentTable`}
        />
      </div>
    </BasicLayout>
  );
}
