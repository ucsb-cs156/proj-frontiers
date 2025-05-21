import React from "react";
import OurTable, { ButtonColumn } from "main/components/OurTable";

import { useBackendMutation } from "main/utils/useBackend";
import {
  cellToAxiosParamsDelete,
  onDeleteSuccess,
} from "main/utils/RoleEmailUtils";
import { hasRole } from "main/utils/currentUser";

export default function RoleEmailTable({
  roleemails,
  currentUser,
  testIdPrefix = "RoleEmailTable",
}) {
  const deleteMutation = useBackendMutation(
    cellToAxiosParamsDelete,
    { onSuccess: onDeleteSuccess },
    ["/api/roleemails/all"],
  );

  // Stryker disable next-line all : TODO try to make a good test for this
  const deleteCallback = async (cell) => {
    deleteMutation.mutate(cell);
  };

  const columns = [
    {
      Header: "Email",
      accessor: "email",
    },
  ];

  if (hasRole(currentUser, "ROLE_ADMIN")) {
    columns.push(
      ButtonColumn("Delete", "danger", deleteCallback, testIdPrefix),
    );
  }
  return <OurTable data={roleemails} columns={columns} testid={testIdPrefix} />;
}
