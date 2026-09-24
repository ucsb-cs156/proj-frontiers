import React, { useEffect, useState } from "react";
import { useBackend } from "main/utils/useBackend";

import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import { useCurrentUser } from "main/utils/currentUser";
import { useNavigate, useParams, useSearchParams } from "react-router";

import Modal from "react-bootstrap/Modal";
import { Button, Tab, Tabs, OverlayTrigger, Tooltip } from "react-bootstrap";
import AssignmentTabComponent from "main/components/TabComponent/AssignmentTabComponent";
import EnrollmentTabComponent from "main/components/TabComponent/EnrollmentTabComponent";
import StaffTabComponent from "main/components/TabComponent/StaffTabComponent";
import GithubSettingIcon from "main/components/Common/GithubSettingIcon";
import TeamsTabComponent from "main/components/TabComponent/TeamsTabComponent";
import { CourseWarningBanner } from "main/components/Courses/CourseWarningBanner";
import SettingsTabComponent from "main/components/TabComponent/SettingsTabComponent";
import JobTabComponent from "main/components/TabComponent/JobTabComponent";
import { hasRole } from "main/utils/currentUser";
import DownloadsTabComponent from "main/components/TabComponent/DownloadsTabComponent";
import SectionsTabComponent from "main/components/TabComponent/SectionsTabComponent";
import { useCourseOptions } from "main/utils/courseOptionsUtils";
import SlackTabComponent from "main/components/TabComponent/SlackTabComponent";
import DokkuTabComponent from "main/components/TabComponent/DokkuTabComponent";
import { slackInfoQueryKey } from "main/utils/slackUtils";
import {
  COURSE_TABS,
  chooseCourseTab,
  getStoredCourseTab,
  storeCourseTab,
} from "main/utils/courseTabUtils";

export default function InstructorCourseShowPage({
  testId = "InstructorCourseShowPage",
  showSettingsTab = true,
  staffTabIsInstructor = true,
  canEditStudents,
  canManageTeams,
}) {
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

  // The Sections tab is only shown when the TRANSLATE_SECTIONS course option
  // is enabled. Course options are only readable by instructors/admins, so the
  // query is skipped entirely for views that hide the Settings tab.
  const { data: courseOptions } = useCourseOptions(courseId, {
    enabled: showSettingsTab,
  });
  const showSectionsTab = courseOptions.TRANSLATE_SECTIONS === true;

  // The Dokku tab is only shown when the DOKKU_MANAGER course option is enabled.
  const showDokkuTab = courseOptions.DOKKU_MANAGER === true;

  // The Slack tab is only shown when the SLACK_INTEGRATION course option is
  // enabled and a Slack token has been saved. The query key is shared with the
  // Slack card on the Settings tab, so saving a token there shows the tab.
  const slackIntegrationEnabled = courseOptions.SLACK_INTEGRATION === true;
  const { data: slackInfo } = useBackend(
    [slackInfoQueryKey(courseId)],
    // Stryker disable next-line StringLiteral : GET and empty string are equivalent
    { method: "GET", url: slackInfoQueryKey(courseId) },
    {},
    true,
    { enabled: slackIntegrationEnabled },
  );
  const showSlackTab =
    slackIntegrationEnabled && Boolean(slackInfo.slackBotToken);

  // Which tab is showing is kept in the URL as ?tab=<name>, so that a tab can be
  // linked to and survives a refresh; see main/utils/courseTabUtils
  const [searchParams, setSearchParams] = useSearchParams();
  const requestedTab = searchParams.get("tab");
  const visibleTabs = COURSE_TABS.filter(
    (tab) =>
      (tab !== "sections" || showSectionsTab) &&
      (tab !== "dokku" || showDokkuTab) &&
      (tab !== "slack" || showSlackTab) &&
      (tab !== "settings" || showSettingsTab),
  );
  const activeTab = chooseCourseTab({
    requestedTab,
    storedTab: getStoredCourseTab(courseId),
    visibleTabs,
  });
  // Remember the tab for the next time this course is opened without ?tab=
  useEffect(() => {
    if (COURSE_TABS.includes(requestedTab)) {
      storeCourseTab(courseId, requestedTab);
    }
  }, [courseId, requestedTab]);

  // Stryker disable OptionalChaining -- course?.instructorEmail is more readable than course && course.instructorEmail
  const getCourseFailed = courseBackendFailureCount > 0;
  const canEditCourseOptions =
    hasRole(currentUser, "ROLE_ADMIN") ||
    currentUser?.root?.user?.email === course?.instructorEmail;
  // Stryker enable OptionalChaining

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
      <CourseWarningBanner
        courseId={courseId}
        orgName={course?.orgName}
        hideBasePermissionWarning={course?.hideBasePermissionWarning ?? false}
      />
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
      <Tabs
        activeKey={activeTab}
        onSelect={(tab) => setSearchParams({ tab }, { replace: true })}
      >
        <Tab eventKey={"students"} title={"Students"} className="pt-2">
          <EnrollmentTabComponent
            courseId={courseId}
            testIdPrefix={testId}
            currentUser={currentUser}
            canEditStudents={canEditStudents}
            translateSections={showSectionsTab}
            canvasEnabled={courseOptions.ENABLE_CANVAS === true}
          />
        </Tab>
        <Tab eventKey={"staff"} title={"Staff"} className="pt-2">
          <StaffTabComponent
            courseId={courseId}
            testIdPrefix={testId}
            currentUser={currentUser}
            isInstructor={staffTabIsInstructor}
          />
        </Tab>
        <Tab eventKey={"teams"} title={"Teams"} className="pt-2">
          <TeamsTabComponent
            courseId={courseId}
            testIdPrefix={testId}
            currentUser={currentUser}
            canManageTeams={canManageTeams}
          />
        </Tab>
        {showSectionsTab && (
          <Tab eventKey={"sections"} title={"Sections"} className="pt-2">
            <SectionsTabComponent courseId={courseId} testIdPrefix={testId} />
          </Tab>
        )}
        <Tab eventKey={"assignments"} title={"Assignments"} className="pt-2">
          <AssignmentTabComponent
            courseId={courseId}
            testIdPrefix={testId}
            currentUser={currentUser}
          />
        </Tab>
        <Tab eventKey={"jobs"} title={"Jobs"} className="pt-2">
          <JobTabComponent courseId={courseId} testIdPrefix={testId} />
        </Tab>
        <Tab eventKey={"downloads"} title={"Downloads"} className="pt-2">
          <DownloadsTabComponent courseId={courseId} testIdPrefix={testId} />
        </Tab>
        {showDokkuTab && (
          <Tab eventKey={"dokku"} title={"Dokku"} className="pt-2">
            <DokkuTabComponent courseId={courseId} testIdPrefix={testId} />
          </Tab>
        )}
        {showSlackTab && (
          <Tab
            eventKey={"slack"}
            title={"Slack"}
            className="pt-2"
            mountOnEnter={true}
          >
            <SlackTabComponent
              courseId={courseId}
              testIdPrefix={testId}
              slackTeamName={slackInfo.slackTeamName}
              slackTeamUrl={slackInfo.slackTeamUrl}
              showSectionChannels={showSectionsTab}
            />
          </Tab>
        )}
        {showSettingsTab && (
          <Tab eventKey={"settings"} title={"Settings"} className="pt-2">
            <SettingsTabComponent
              courseId={courseId}
              testIdPrefix={testId}
              canEditCourseOptions={canEditCourseOptions}
            />
          </Tab>
        )}
      </Tabs>
    </BasicLayout>
  );
}
