import React from "react";
import OurTable, { ButtonColumn } from "main/components/OurTable";

import { useBackendMutation } from "main/utils/useBackend";
import { toast } from "react-toastify";

export default function RoleEmailTable({
  data,
  deleteEndpoint = "/api/admin/delete",
  getEndpoint = "/api/admin/all",
  testIdPrefix = "RoleEmailTable",
  customDeleteCallback = null, // optional deleteCallback, used in AdminsIndexPage
}) {
  const cellToAxiosParamsDelete = (cell, deleteEndpoint) => {
    return {
      url: deleteEndpoint,
      method: "DELETE",
      params: {
        email: cell.row.values.email,
      },
    };
  };

  const onDeleteSuccess = (message) => {
    console.log(message);
    toast(message);
  };

  const deleteMutation = useBackendMutation(
    (cell) => cellToAxiosParamsDelete(cell, deleteEndpoint),
    { onSuccess: onDeleteSuccess },
    [getEndpoint],
  );
  const defaultDeleteCallback = async (cell) => {
    deleteMutation.mutate(cell);
  };

  const deleteCallback = customDeleteCallback || defaultDeleteCallback;

  const columns = [
    {
      Header: "Email",
      accessor: "email", // accessor is the "key" in the data
    },
    ButtonColumn("Delete", "danger", deleteCallback, testIdPrefix),
  ];

  return (
    <OurTable
      data={Array.isArray(data) ? data : []}
      columns={columns}
      testid={testIdPrefix}
    />
  );
}
