import React from "react";
import OurTable from "main/components/OurTable";
import { courseRoleLabel } from "main/utils/slackUtils";

export default function SlackUsersTable({
  users,
  testIdPrefix = "SlackUsersTable",
}) {
  const columns = [
    {
      header: "Name",
      accessorKey: "realName",
    },
    {
      header: "Display Name",
      accessorKey: "displayName",
    },
    {
      header: "Slack Username",
      accessorKey: "name",
    },
    {
      header: "Email",
      accessorKey: "email",
    },
    {
      header: "Course Role",
      id: "courseRole",
      accessorFn: (row) => courseRoleLabel(row.courseRole),
    },
  ];

  return <OurTable data={users} columns={columns} testid={testIdPrefix} />;
}
