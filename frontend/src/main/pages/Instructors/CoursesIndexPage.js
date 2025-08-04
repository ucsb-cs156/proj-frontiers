import React from "react";
import { useBackend } from "main/utils/useBackend";

import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import InstructorCoursesTable from "main/components/Courses/InstructorCoursesTable";
import { useCurrentUser } from "main/utils/currentUser";

export default function CoursesIndexPage() {
  const currentUser = useCurrentUser();

  const {
    data: courses,
    error: _error,
    status: _status,
  } = useBackend(
    // Stryker disable next-line all : don't test internal caching of React Query
    ["/api/courses/allForAdmins"],
    { method: "GET", url: "/api/courses/allForAdmins" },
    // Stryker disable next-line all : don't test default value of empty list
    [],
  );

  return (
    <BasicLayout>
      <div className="pt-2">
        <h1>Courses</h1>
        <InstructorCoursesTable courses={courses} currentUser={currentUser} />
      </div>
    </BasicLayout>
  );
}
