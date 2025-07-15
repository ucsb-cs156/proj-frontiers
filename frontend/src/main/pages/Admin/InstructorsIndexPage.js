import React from "react";
import { useBackend } from "main/utils/useBackend";

import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import RoleEmailTable from "main/components/Users/RoleEmailTable";
import { Button } from "react-bootstrap";

export default function InstructorsIndexPage() {
  const {
    data: instructors,
    error: _error,
    status: _status,
  } = useBackend(
    // Stryker disable next-line all : don't test internal caching of React Query
    ["/api/admin/instructors/get"],
    { method: "GET", url: "/api/admin/instructors/get" },
    // Stryker disable next-line all : don't test default value of empty list
    [],
  );

  const createButton = () => {
    return (
      <Button
        variant="primary"
        href="/swagger-ui/index.html#/Instructors/postInstructor"
        style={{ float: "right" }}
      >
        New Instructor
      </Button>
    );
  };

  return (
    <BasicLayout>
      <div className="pt-2">
        {createButton()}
        <h1>Instructors</h1>
        <RoleEmailTable
          data={instructors}
          deleteEndpoint="/api/admin/instructors/delete"
          getEndpoint="/api/admin/instructors/get"
          testIdPrefix="InstructorsIndexPage"
        />
      </div>
    </BasicLayout>
  );
}
