import React from "react";
import { useBackend } from "main/utils/useBackend";

import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import CoursesTable from "main/components/Courses/CoursesTable";
import { useCurrentUser, hasRole } from "main/utils/currentUser";

export default function CoursesIndexPage() {
  const currentUser = useCurrentUser();

  const {
    data: courses,
    error: _error,
    status: _status,
  } = useBackend(
    // Stryker disable next-line all : don't test internal caching of React Query
    ["/api/courses/all"],
    { method: "GET", url: "/api/courses/all" },
    // Stryker disable next-line all : don't test default value of empty list
    [],
  );

  return (
    <BasicLayout>
      <div className="pt-2">
        <h1>Courses</h1>
        <CoursesTable courses={courses} showInstallButton={hasRole(currentUser, "ROLE_ADMIN")} />
      </div>
    </BasicLayout>
  );
}
