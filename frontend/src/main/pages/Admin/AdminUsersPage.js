import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import UsersTable from "main/components/Users/UsersTable";

import { useBackend } from "main/utils/useBackend";
import OurPagination from "main/components/Common/OurPagination";
import { useState } from "react";
const AdminUsersPage = () => {
  const [currentPage, setCurrentPage] = useState(1);
  const {
    data: users,
    error: _error,
    status: _status,
  } = useBackend(
    // Stryker disable next-line all : don't test internal caching of React Query
    [`/api/admin/users/${currentPage - 1}`],
    {
      method: "GET",
      url: `/api/admin/users`,
      params: { page: currentPage - 1, size: 50, sort: "id" },
    },
    { content: [], page: { totalPages: 1 } },
  );

  return (
    <BasicLayout>
      <h2>Users</h2>
      <UsersTable users={users.content} />
      <div className="d-flex justify-content-evenly">
        <OurPagination
          currentActivePage={currentPage}
          updateActivePage={setCurrentPage}
          totalPages={users.page.totalPages}
        />
      </div>
    </BasicLayout>
  );
};

export default AdminUsersPage;
