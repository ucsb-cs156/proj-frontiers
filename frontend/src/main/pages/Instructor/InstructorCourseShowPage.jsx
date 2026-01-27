import React, { useEffect, useState } from "react";
import { useBackend } from "main/utils/useBackend";

import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import { useCurrentUser } from "main/utils/currentUser";
import { useNavigate, useParams } from "react-router";

import Modal from "react-bootstrap/Modal";
import { Button, Tab, Tabs, OverlayTrigger, Tooltip } from "react-bootstrap";
import AssignmentTabComponent from "main/components/TabComponent/AssignmentTabComponent";
import EnrollmentTabComponent from "main/components/TabComponent/EnrollmentTabComponent";
import StaffTabComponent from "main/components/TabComponent/StaffTabComponent";
import GithubSettingIcon from "main/components/Common/GithubSettingIcon";
import TeamsTabComponent from "main/components/TabComponent/TeamsTabComponent";
import { CourseWarningBanner } from "main/components/Courses/CourseWarningBanner";
import SettingsTabComponent from "main/components/TabComponent/SettingsTabComponent";

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
      <CourseWarningBanner courseId={courseId} />
      {!course ? (
        <div data-testid={`${testId}-loading`}>Course: Loading...</div>
      ) : (
        <div className="border rounded-3 p-4 mb-4">
          <div className="d-flex align-items-center gap-3">
            {course.orgName && (
              <img
                src={`https://github.com/${course.orgName}.png?size=64`}
                alt={course.orgName}
                data-testid={`${testId}-github-org-image`}
                className="rounded-circle border"
                style={{ width: 48, height: 48 }}
              />
            )}
            <div>
              <div className="d-flex align-items-center gap-2">
                <h1
                  data-testid={`${testId}-title`}
                  className="h3 mb-0 fw-semibold"
                >
                  {course.courseName}
                </h1>
                <span className="badge bg-primary-subtle text-primary-emphasis rounded-pill">
                  {course.term}
                </span>
              </div>
              {course.orgName && (
                <div className="d-flex align-items-center gap-2">
                  <a
                    href={`https://github.com/${course.orgName}`}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-primary text-decoration-none fs-5"
                    data-testid={`${testId}-github-org-link`}
                  >
                    {course.orgName}
                  </a>
                  {course.installationId && (
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
                          size={20}
                          data-testid={`${testId}-github-settings-icon`}
                        />
                      </a>
                    </OverlayTrigger>
                  )}
                </div>
              )}
            </div>
          </div>
        </div>
      )}
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
         <Tab eventKey={"settings"} title={"Settings"} className="pt-2">
          <SettingsTabComponent 
            courseId={courseId}
            testIdPrefix={testId}
          />
        </Tab>
      </Tabs>
    </BasicLayout>
  );
}
