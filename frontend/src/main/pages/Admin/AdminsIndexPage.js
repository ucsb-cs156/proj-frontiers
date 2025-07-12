import React from "react";
import { useBackend } from "main/utils/useBackend";

import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import RoleEmailTable from "main/components/Users/RoleEmailTable";
import { Button } from "react-bootstrap";

export default function AdminsIndexPage() {
  const {
    data: instructors,
    error: _error,
    status: _status,
  } = useBackend(
    // Stryker disable next-line all : don't test internal caching of React Query
    ["/api/admin/all"],
    { method: "GET", url: "/api/admin/all" },
    // Stryker disable next-line all : don't test default value of empty list
    [],
  );

  const createButton = () => {
    return (
      <Button
        variant="primary"
        href="/swagger-ui/index.html#/Admin/postAdmin"
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
          data={instructors}
          deleteEndpoint="/api/admin/delete"
          getEndpoint="/api/admin/all"
          testIdPrefix="AdminsIndexPage"
        />
      </div>
    </BasicLayout>
  );
}
