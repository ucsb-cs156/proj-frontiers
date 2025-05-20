import React from "react";
import OurTable, { ButtonColumn } from "main/components/OurTable";

import { useBackendMutation } from "main/utils/useBackend";
import {
  cellToAxiosParamsDelete,
  onDeleteSuccess,
} from "main/utils/RoleEmailUtils";
import { hasRole } from "main/utils/currentUser";

export default function RoleEmailTable({
  items,
  currentUser,
  role,
  testIdPrefix = "RoleEmailTable",
  deleteCallback: customDeleteCallback, // optional deleteCallback, used in AdminsIndexPage
}) {
  // Stryker disable all : hard to test for query caching

  const defaultDeleteMutation = useBackendMutation(
    (cell) => cellToAxiosParamsDelete(cell, role),
    { onSuccess: onDeleteSuccess },
    [`/api/admin/${role.toLowerCase()}/all`],
  );
  // Stryker restore all

  // Stryker disable next-line all
  const defaultDeleteCallback = async (cell) => {
    defaultDeleteMutation.mutate(cell);
  };

  const deleteCallback = customDeleteCallback || defaultDeleteCallback;

  const columns = [
    {
      Header: "Email",
      accessor: "email", // accessor is the "key" in the data
    },
  ];

  if (hasRole(currentUser, "ROLE_ADMIN")) {
    columns.push(
      ButtonColumn("Delete", "danger", deleteCallback, testIdPrefix),
    );
  }

  return (
    <OurTable
      data={Array.isArray(items) ? items : []}
      columns={columns}
      testid={testIdPrefix}
    />
  );
}
