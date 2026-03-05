import React from "react";
import { useBackend } from "main/utils/useBackend";
import JobsTable from "main/components/Jobs/JobsTable";
import { Button } from "react-bootstrap";

export default function JobTabComponent({ courseId, testIdPrefix }) {
  const { data: jobs, refetch } = useBackend(
    ["/api/jobs/course", courseId],
    {
      method: "GET",
      url: "/api/jobs/course",
      params: { courseId },
    },
    [],
  );

  const refreshJobs = () => {
    refetch();
  };

  return (
    <div data-testid={`${testIdPrefix}-jobs-tab`}>
      <h4 className="mb-3">Job Status</h4>

      <Button
        className="mb-3"
        onClick={refreshJobs}
        data-testid={`${testIdPrefix}-refresh-jobs`}
      >
        Refresh
      </Button>

      <JobsTable jobs={jobs} />
    </div>
  );
}
