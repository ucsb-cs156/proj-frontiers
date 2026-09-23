import React from "react";
import OurTable from "main/components/OurTable";
import { courseRoleLabel, slackStatusLabel } from "main/utils/slackUtils";

export default function SlackMissingMembersTable({
  members,
  testIdPrefix = "SlackMissingMembersTable",
}) {
  const columns = [
    {
      header: "Course Role",
      id: "courseRole",
      accessorFn: (row) => courseRoleLabel(row.courseRole),
    },
    {
      header: "First Name",
      accessorKey: "firstName",
    },
    {
      header: "Last Name",
      accessorKey: "lastName",
    },
    {
      header: "Email",
      accessorKey: "email",
    },
    {
      header: "Slack Status",
      id: "slackStatus",
      accessorFn: (row) => slackStatusLabel(row.slackStatus),
    },
  ];

  return <OurTable data={members} columns={columns} testid={testIdPrefix} />;
}
