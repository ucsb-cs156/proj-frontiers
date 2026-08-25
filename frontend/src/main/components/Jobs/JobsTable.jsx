import React from "react";
import OurTable from "main/components/OurTable";
import { formatTime } from "main/utils/dateUtils";
import { Button } from "react-bootstrap";
import { useBackendMutation } from "main/utils/useBackend";
import { toast } from "react-toastify";

const CANCELLABLE_STATUSES = ["queued", "running"];

export default function JobsTable({ jobs, onCancelled = () => {} }) {
  const cellToAxiosParamsCancel = (cell) => ({
    url: `/api/jobs/${cell.row.original.id}/cancel`,
    method: "POST",
  });

  const cancelSuccess = () => {
    toast("Cancellation requested.");
    onCancelled();
  };

  // Stryker disable all : hard to test for query caching
  const cancelMutation = useBackendMutation(cellToAxiosParamsCancel, {
    onSuccess: cancelSuccess,
  });
  // Stryker restore all

  const cancelCallback = (cell) => {
    cancelMutation.mutate(cell);
  };

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
      accessorKey: "createdByEmail",
    },
    {
      header: "Course Id",
      accessorFn: (row) =>
        row.scopeType === "course" ? String(row.scopeId) : "",
      id: "courseId",
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
      header: "Cancel",
      id: "cancel",
      cell: ({ cell }) =>
        CANCELLABLE_STATUSES.includes(cell.row.original.status) ? (
          <Button
            variant="danger"
            size="sm"
            onClick={() => cancelCallback(cell)}
            data-testid={`JobsTable-cell-row-${cell.row.index}-col-cancel-button`}
          >
            Cancel
          </Button>
        ) : null,
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
