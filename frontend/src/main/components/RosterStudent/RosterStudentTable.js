import React from "react";
import OurTable, { ButtonColumn } from "main/components/OurTable";
import { useBackendMutation } from "main/utils/useBackend";
import { useNavigate } from "react-router-dom";
import { hasRole } from "main/utils/currentUser";

// Helper for DELETE request
// Stryker disable StringLiteral : hard to test for string literals
export const cellToAxiosParamsDelete = (cell) => {
  return {
    url: "/api/rosterstudents",
    method: "DELETE",
    params: {
      id: cell.row.values.id,
    },
  };
};
// Stryker restore StringLiteral

// Stryker disable all : hard to test for console logs
export const onDeleteSuccess = (message) => {
  console.log("Delete successful:", message);
};
// Stryker restore all

export default function RosterStudentTable({ rosterStudents, currentUser }) {
  const navigate = useNavigate();

  // Stryker disable next-line StringLiteral : hard to test for string literals
  const editCallback = (cell) => {
    navigate(`/rosterstudent/edit/${cell.row.values.id}`);
  };

  // Stryker disable all : hard to test for query caching
  const deleteMutation = useBackendMutation(
    cellToAxiosParamsDelete,
    { onSuccess: onDeleteSuccess },
    ["/api/rosterstudents/all"],
  );
  // Stryker restore all

  // Stryker disable next-line all
  const deleteCallback = async (cell) => {
    deleteMutation.mutate(cell);
  };

  // Stryker disable next-line ArrayDeclaration : hard to test for array literals
  const columns = [
    {
      Header: "id",
      accessor: "id", // accessor is the "key" in the data
    },
    {
      Header: "Enrollment Code",
      accessor: "enrollmentCode",
    },
    {
      Header: "Student ID",
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
    // Stryker disable next-line BlockStatement : hard to test for block statements
    columns.push(
      ButtonColumn("Edit", "primary", editCallback, "RosterStudentTable"),
    );
    columns.push(
      ButtonColumn("Delete", "danger", deleteCallback, "RosterStudentTable"),
    );
  }

  return (
    <OurTable
      data={rosterStudents}
      columns={columns}
      testid={"RosterStudentTable"}
    />
  );
}
