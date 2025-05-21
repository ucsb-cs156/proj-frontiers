import React from "react";
import { useBackend, useBackendMutation } from "main/utils/useBackend";

import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import RoleEmailTable from "main/components/Users/RoleEmailTable";
import { useCurrentUser } from "main/utils/currentUser";
import { Button } from "react-bootstrap";
import { toast } from "react-toastify";

export default function AdminsIndexPage() {
  const currentUser = useCurrentUser();

  const {
    data: admins,
    error: _error,
    status: _status,
  } = useBackend(
    // Stryker disable next-line all : don't test internal caching of React Query
    ["/api/admin/admins"],
    { method: "GET", url: "/api/admin/admins" },
    // Stryker disable next-line all : don't test default value of empty list
    [],
  );

  const objectToAxiosParams = (cell) => ({
    url: "/api/admin/admins",
    method: "DELETE",
    params: {
      email: cell.row.values.email,
    },
  });

  const onSuccess = (response) => {
    toast(`Admin with email ${response.email} deleted`);
  };

  const onError = (error) => {
    const message = error.response.data;

    if (error.response.status === 403 && typeof message === "string") {
      toast.error(message);
    } else {
      toast.error("Error deleting admin.");
    }
  };

  const deleteMutation = useBackendMutation(objectToAxiosParams, {
    onSuccess,
    onError,
  });

  const deleteCallback = async (cell) => {
    deleteMutation.mutate(cell);
  };

  const createButton = () => {
    return (
      <Button
        variant="primary"
        href="/admin/admins/create"
        style={{ float: "right" }}
      >
        New Admin
      </Button>
    );
  };

  return (
    <BasicLayout>
      <div className="pt-2">
        {createButton()}
        <h1>Admins</h1>
        <RoleEmailTable
          items={admins}
          currentUser={currentUser}
          role="admins"
          deleteCallback={deleteCallback}
        />
      </div>
    </BasicLayout>
  );
}
