import React from "react";
import { useBackend } from "main/utils/useBackend";

import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import InstructorCoursesTable from "main/components/Courses/InstructorCoursesTable";
import { useCurrentUser } from "main/utils/currentUser";
import { useParams } from "react-router-dom";

export default function InstructorCourseShowPage() {
  const { data: currentUser } = useCurrentUser();
  const courseId = useParams().id;

  const {
    data: course,
    error: _error,
    status: _status,
  } = useBackend(
    // Stryker disable next-line all : don't test internal caching of React Query
    ["/api/courses/all"],
    // Stryker disable next-line StringLiteral : GET and empty string are equivalent
    { method: "GET", url: `/api/courses/${courseId}` },
    // Stryker disable next-line all : don't test default value of empty list
    null,
  );

  const testId = "InstructorCourseShowPage";
  return (
    <BasicLayout>
      <div className="pt-2">
        <h1>Course</h1>
        <InstructorCoursesTable
          courses={course ? [course] : []}
          currentUser={currentUser}
          testId={testId}
        />
        <h2>Roster Students</h2>
        <p>Coming soon: Roster Students for this course will appear here.</p>
      </div>
    </BasicLayout>
  );
}
