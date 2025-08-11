import React from "react";
import OurTable from "main/components/OurTable";
import { formatTime } from "main/utils/dateUtils";

export default function JobsTable({ jobs }) {

  const columns = [
    {
      Header: "id",
      accessor: "id",
    },
    {
      Header: "Created",
      accessor: (row) => formatTime(row.createdAt),
      id: "createdAt",
    },
    {
      Header: "Updated",
      accessor: (row) => formatTime(row.updatedAt),
      id: "updatedAt",
    },
    {
      Header: "Status",
      accessor: "status",
    },
    {
      Header: "Log",
      accessor: "log",
      Cell: ({ cell }) => (
        <div style={{ maxWidth: 450, maxHeight: 100, overflowY: "auto" }}>
          <pre style={{ whiteSpace: "pre-wrap" }}>{cell.value}</pre>
        </div>
      ),
    },
  ];

  const testid = "JobsTable";

  return <OurTable data={jobs || []} columns={columns} testid={testid} />;
}