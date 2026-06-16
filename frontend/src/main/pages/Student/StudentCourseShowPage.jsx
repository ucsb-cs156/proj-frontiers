import React from "react";
import { useBackend } from "main/utils/useBackend";

import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import { useParams } from "react-router";
import { useCurrentUser } from "main/utils/currentUser";

import { Tab, Tabs } from "react-bootstrap";
import TeamsTabComponent from "main/components/TabComponent/TeamsTabComponent";

export default function StudentCourseShowPage() {
  const currentUser = useCurrentUser();
  const courseId = useParams().id;

  const { data: course } = useBackend(
    [`/api/courses/${courseId}`],
    // Stryker disable next-line StringLiteral : GET and empty string are equivalent
    { method: "GET", url: `/api/courses/${courseId}` },
    null,
    true,
  );

  const testId = "StudentCourseShowPage";
  return (
    <BasicLayout>
      {!course ? (
        <div data-testid={`${testId}-loading`}>Course: Loading...</div>
      ) : (
        <div className="border rounded-3 p-4 mb-4">
          <div className="d-flex align-items-center gap-2">
            <h1 data-testid={`${testId}-title`} className="h3 mb-0 fw-semibold">
              {course.courseName}
            </h1>
            <span className="badge bg-primary-subtle text-primary-emphasis rounded-pill">
              {course.term}
            </span>
          </div>
        </div>
      )}
      <Tabs defaultActiveKey={"teams"}>
        <Tab eventKey={"teams"} title={"My Teams"} className="pt-2">
          <TeamsTabComponent
            courseId={courseId}
            testIdPrefix={testId}
            currentUser={currentUser}
            instructorView={false}
          />
        </Tab>
      </Tabs>
    </BasicLayout>
  );
}
