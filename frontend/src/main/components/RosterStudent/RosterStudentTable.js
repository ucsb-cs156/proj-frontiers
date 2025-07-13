import React from "react";
import OurTable, { ButtonColumn } from "main/components/OurTable";

import { useBackendMutation } from "main/utils/useBackend";
import {
  cellToAxiosParamsDelete,
  onDeleteSuccess,
} from "main/utils/rosterStudentUtils";
import { hasRole } from "main/utils/currentUser";

export default function RosterStudentTable({
  students,
  currentUser,
  courseId,
  testIdPrefix = "RosterStudentTable",
}) {
  const editCallback = (cell) => {
    const url = `/rosterstudents/edit/${cell.row.values.id}`;
    window.alert(
      "Edit not implemented yet, but would have navigated to: " + url,
    );
    // Future implementation: navigate(url);
  };

  // Stryker disable all : hard to test for query caching
  const deleteMutation = useBackendMutation(
    cellToAxiosParamsDelete,
    { onSuccess: onDeleteSuccess },
    [`/api/rosterstudents/course/${courseId}`],
  );
  // Stryker restore all

  // Stryker disable next-line all
  const deleteCallback = async (cell) => {
    deleteMutation.mutate(cell);
  };

  const columns = [
    {
      Header: "id",
      accessor: "id",
    },

    {
      Header: "Student Id",
      accessor: "studentId",
    },

    {
      Header: "First Name",
      accessor: "firstName",
    },
    {
      Header: "Last Name",
      accessor: "lastName",
    },
    {
      Header: "Email",
      accessor: "email",
    },
  ];

  if (hasRole(currentUser, "ROLE_ADMIN")) {
    columns.push(ButtonColumn("Edit", "primary", editCallback, testIdPrefix));
    columns.push(
      ButtonColumn("Delete", "danger", deleteCallback, testIdPrefix),
    );
  }

  return <OurTable data={students} columns={columns} testid={testIdPrefix} />;
}
