import React, { useEffect, useState } from "react";
import { useBackend } from "main/utils/useBackend";

import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import InstructorCoursesTable from "main/components/Courses/InstructorCoursesTable";
import { useCurrentUser } from "main/utils/currentUser";
import { useNavigate, useParams } from "react-router";

import Modal from "react-bootstrap/Modal";
import { Button, Tab, Tabs, OverlayTrigger, Tooltip } from "react-bootstrap";
import AssignmentTabComponent from "main/components/TabComponent/AssignmentTabComponent";
import EnrollmentTabComponent from "main/components/TabComponent/EnrollmentTabComponent";
import StaffTabComponent from "main/components/TabComponent/StaffTabComponent";
import GithubSettingIcon from "main/components/Common/GithubSettingIcon";
import TeamsTabComponent from "main/components/TabComponent/TeamsTabComponent";

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
      <h1
        data-testid={`${testId}-title`}
        className="d-flex align-items-center lh-1 gap-3"
      >
        {!course ? (
          "Course: Loading..."
        ) : !course.installationId ? (
          <span>
            {course.courseName}&nbsp;&nbsp;{course.term}
          </span>
        ) : (
          <>
            <span>{course.courseName}</span>
            <a
              className="ms-2"
              href={`https://github.com/${course.orgName}`}
              target="_blank"
              rel="noopener noreferrer"
              data-testid={`${testId}-github-org-link`}
            >
              {course.orgName}
            </a>
            <OverlayTrigger
              placement="right"
              overlay={
                <Tooltip id={`${testId}-tooltip-github-settings`}>
                  Manage settings for association between your GitHub
                  organization and this web application.
                </Tooltip>
              }
            >
              <a
                href={`https://github.com/organizations/${course.orgName}/settings/installations/${course.installationId}`}
                target="_blank"
                rel="noopener noreferrer"
                data-testid={`${testId}-github-settings-link`}
              >
                <GithubSettingIcon
                  size={45}
                  data-testid={`${testId}-github-settings-icon`}
                />
              </a>
            </OverlayTrigger>
            <span> {course.term} </span>
          </>
        )}
      </h1>
      <Tabs defaultActiveKey={"default"}>
        <Tab eventKey={"students"} title={"Students"} className="pt-2">
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
        <Tab eventKey={"teams"} title={"Teams"} className="pt-2">
          <TeamsTabComponent
            courseId={courseId}
            testIdPrefix={testId}
            currentUser={currentUser}
          />
        </Tab>
        <Tab eventKey={"default"} title={"Assignments"} className="pt-2">
          <AssignmentTabComponent
            courseId={courseId}
            testIdPrefix={testId}
            currentUser={currentUser}
          />
        </Tab>
      </Tabs>
    </BasicLayout>
  );
}
