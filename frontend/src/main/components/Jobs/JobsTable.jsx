import React from "react";
import OurTable from "main/components/OurTable";
import { formatTime } from "main/utils/dateUtils";

export default function JobsTable({ jobs }) {
  const columns = [
    {
      header: "id",
      accessorKey: "id",
    },
    {
      header: "Job Name",
      accessorKey: "jobName",
    },
    {
      header: "User Email",
      accessorKey: "userEmail",
    },
    {
      header: "Course Name",
      accessorKey: "courseName",
    },
    {
      header: "Created",
      accessorFn: (row) => formatTime(row.createdAt),
      id: "createdAt",
    },
    {
      header: "Updated",
      accessorFn: (row) => formatTime(row.updatedAt),
      id: "updatedAt",
    },
    {
      header: "Status",
      accessorKey: "status",
    },
    {
      header: "Log",
      accessorKey: "log",
      cell: ({ cell }) => (
        <div
          style={{ maxWidth: 450, maxHeight: 100, overflowY: "auto" }}
          data-testid={`JobsTable-cell-row-${cell.row.index}-col-${cell.column.id}-div`}
        >
          <pre style={{ whiteSpace: "pre-wrap" }}>{cell.getValue()}</pre>
        </div>
      ),
    },
  ];

  const testid = "JobsTable";

  return <OurTable data={jobs} columns={columns} testid={testid} />;
}
