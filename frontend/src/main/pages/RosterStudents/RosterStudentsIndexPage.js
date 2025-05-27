import React from "react";
import { useBackend } from "main/utils/useBackend";

import { useParams } from "react-router-dom";
import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import RosterStudentsTable from "main/components/RosterStudents/RosterStudentsTable";
import { Button } from "react-bootstrap";

export default function RosterStudentsIndexPage() {
  const { courseId } = useParams();
  const key = `/api/rosterstudents/course?courseId=${courseId}`;

  const {
    data: rosterStudents,
    error: _error,
    status: _status,
  } = useBackend(
    // Stryker disable all: don't test internal caching of React Query
    key,
    {
      method: "GET",
      url: "/api/rosterstudents/course",
      params: { courseId: courseId },
    },
    [],
    // Stryker enable all
  );

  // Do not need to check whether user is admin or not. The only way to access index page is if the user is an admin.
  const createButton = () => {
    return (
      <Button
        variant="primary"
        href="roster_students/new"
        style={{ float: "right" }}
      >
        Create Roster Student
      </Button>
    );
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
