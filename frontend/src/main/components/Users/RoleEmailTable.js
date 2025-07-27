import React from "react";
import OurTable from "main/components/OurTable";

import { useBackendMutation } from "main/utils/useBackend";
import { toast } from "react-toastify";
import { Button } from "react-bootstrap";

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
        email: cell.row.original.email,
      },
    };
  };

  const onDeleteSuccess = (message) => {
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
      header: "Email",
      accessorKey: "email", // accessor is the "key" in the data
    },
    {
      header: "Delete",
      accessorKey: "isInAdminEmails",
      cell: ({ cell }) => {
        if (!cell.row.original.isInAdminEmails) {
          return (
            <Button
              className="btn btn-danger"
              onClick={() => deleteCallback(cell)}
              data-testid={`${testIdPrefix}-cell-row-${cell.row.index}-col-delete-button`}
            >
              Delete
            </Button>
          );
        }
        return (
          <span
            data-testid={`${testIdPrefix}-cell-row-${cell.row.index}-cannot-delete`}
          >
            In <code>ADMIN_EMAILS</code>
          </span>
        );
      },
    },
  ];

  return (
    <OurTable
      data={Array.isArray(data) ? data : []}
      columns={columns}
      testid={testIdPrefix}
    />
  );
}
