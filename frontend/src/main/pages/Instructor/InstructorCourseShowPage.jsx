import React, { useEffect, useState } from "react";
import { useBackend } from "main/utils/useBackend";

import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import { useCurrentUser } from "main/utils/currentUser";
import { useNavigate, useParams } from "react-router";

import Modal from "react-bootstrap/Modal";
import { Button, Tab, Tabs } from "react-bootstrap";
import AssignmentTabComponent from "main/components/TabComponent/AssignmentTabComponent";
import EnrollmentTabComponent from "main/components/TabComponent/EnrollmentTabComponent";
import StaffTabComponent from "main/components/TabComponent/StaffTabComponent";
import { OverlayTrigger, Tooltip } from "react-bootstrap";
import GithubSettingIcon from "main/components/Common/GithubSettingIcon";

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
        style={{
          display: "flex",
          gap: "1.5rem",
          justifyContent: "flex-start",
        }}
      >
        {course ? (
          <>
            <span>
              Course:
              <a
                href={`https://github.com/${course.orgName}`}
                target="_blank"
                rel="noopener noreferrer"
                data-testid={`${testId}-github-org-link`}
              >
                {course.courseName}
              </a>
            </span>
            <span>
              Term:
              <span style={{ color: "blue" }} data-testid={`${testId}-term`}>
                {course.term}
              </span>
            </span>
            <OverlayTrigger
              placement="right"
              overlay={
                <Tooltip id={`${testId}-tooltip-github-settings`}>
                  Manage settings for association between your GitHub
                  organization and this web application.
                </Tooltip>
              }
            >
              <span>
                <a
                  href={`https://github.com/organizations/${course.orgName}/settings/installations/${course.installationId}`}
                  target="_blank"
                  rel="noopener noreferrer"
                  data-testid={`${testId}-github-settings-link`}
                >
                  <GithubSettingIcon
                    size={35}
                    data-testid={`${testId}-github-settings-icon`}
                  />
                </a>
              </span>
            </OverlayTrigger>
          </>
        ) : (
          "Course: Loading..."
        )}
      </h1>
      <Tabs defaultActiveKey={"default"}>
        <Tab eventKey={"enrollment"} title={"Students"} className="pt-2">
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
        <Tab eventKey={"default"} title={"Assignments"} className="pt-2">
          <AssignmentTabComponent courseId={courseId} />
        </Tab>
      </Tabs>
    </BasicLayout>
  );
}
