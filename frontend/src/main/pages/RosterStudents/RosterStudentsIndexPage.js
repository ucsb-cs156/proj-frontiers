import React from "react";
import { useBackend } from "main/utils/useBackend";

import { useParams } from "react-router-dom";
import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import RosterStudentsTable from "main/components/RosterStudents/RosterStudentsTable";
import { useCurrentUser, hasRole } from "main/utils/currentUser";
import { Button } from "react-bootstrap";

export default function RosterStudentsIndexPage() {
  const { courseId } = useParams();
  const currentUser = useCurrentUser();

  const {
    data: rosterStudents,
    error: _error,
    status: _status,
  } = useBackend(
    // Stryker disable next-line all : don't test internal caching of React Query
    ["/api/rosterStudents/all", { courseId }],
    {
      method: "GET",
      url: "/api/rosterstudents/course",
      params: { courseId: courseId },
    },
    // Stryker disable next-line all : don't test default value of empty list
    [],
  );

  const createButton = () => {
    if (hasRole(currentUser, "ROLE_ADMIN")) {
      return (
        <Button
          variant="primary"
          href="roster_students/new"
          style={{ float: "right" }}
        >
          Create Roster Student
        </Button>
      );
    }
  };

  return (
    <BasicLayout>
      <div className="pt-2">
        {createButton()}
        <h1>Roster Students</h1>
        <RosterStudentsTable
          rosterStudents={rosterStudents}
          showButtons={true}
        />
      </div>
    </BasicLayout>
  );
}
