import React from "react";
import { useBackend } from "main/utils/useBackend";

import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import { useParams } from "react-router";

import { Tab, Tabs } from "react-bootstrap";

export default function StudentCourseShowPage() {
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
      <Tabs defaultActiveKey={"placeholder"}>
        <Tab eventKey={"placeholder"} title={"Placeholder"} className="pt-2">
          <p>More features coming soon</p>
        </Tab>
      </Tabs>
    </BasicLayout>
  );
}
