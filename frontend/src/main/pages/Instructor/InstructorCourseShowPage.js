import React from "react";
import { useBackend } from "main/utils/useBackend";

import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import InstructorCoursesTable from "main/components/Courses/InstructorCoursesTable";
import { useCurrentUser } from "main/utils/currentUser";
import { useParams } from "react-router-dom";

import RosterStudentTable from "main/components/RosterStudent/RosterStudentTable";

export default function InstructorCourseShowPage() {
  const { data: currentUser } = useCurrentUser();
  const courseId = useParams().id;

  const {
    data: course,
    error: _errorCourses,
    status: _statusCourses,
  } = useBackend(
    // Stryker disable next-line all : don't test internal caching of React Query
    ["/api/courses/all"],
    // Stryker disable next-line StringLiteral : GET and empty string are equivalent
    { method: "GET", url: `/api/courses/${courseId}` },
    // Stryker disable next-line all : don't test default value of empty list
    null,
  );

  const {
    data: rosterStudents,
    error: _errorRosterStudents,
    status: _statusRosterStudents,
  } = useBackend(
    // Stryker disable next-line all : don't test internal caching of React Query
    [`/api/rosterstudents/course/${courseId}`],
    // Stryker disable next-line StringLiteral : GET and empty string are equivalent
    { method: "GET", url: `/api/rosterstudents/course/${courseId}` },
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
        <RosterStudentTable
          students={rosterStudents || []}
          currentUser={currentUser}
          courseId={course ? course.id : ""}
          testId={`${testId}-RosterStudentTable`}
        />
      </div>
    </BasicLayout>
  );
}
