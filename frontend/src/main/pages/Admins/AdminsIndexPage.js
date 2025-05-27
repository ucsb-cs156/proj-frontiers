import React from "react";
import { useBackend, useBackendMutation } from "main/utils/useBackend";

import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import RoleEmailTable from "main/components/Users/RoleEmailTable";
import { useCurrentUser } from "main/utils/currentUser";
import { Button } from "react-bootstrap";
import { toast } from "react-toastify";

import { useQueryClient } from "react-query";

export default function AdminsIndexPage() {
  const queryClient = useQueryClient();
  const currentUser = useCurrentUser();

  const {
    data: admins,
    error: _error,
    status: _status,
  } = useBackend(
    // Stryker disable next-line all : don't test internal caching of React Query
    ["/api/admin/admins/all"],
    { method: "GET", url: "/api/admin/admins/all" },
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

  const onSuccess = (email) => {
    toast(`Admin with email ${email} deleted`);
    queryClient.invalidateQueries({ queryKey: ["/api/admin/admins/all"] });
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
    onError,
  });

  const deleteCallback = async (cell) => {
    deleteMutation.mutate(cell, {
      onSuccess: () => onSuccess(cell.row.values.email),
    });
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
