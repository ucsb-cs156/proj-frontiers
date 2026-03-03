import React from "react";
import { useBackend } from "main/utils/useBackend";
import JobsTable from "main/components/Jobs/JobsTable";

export default function JobTabComponent({ courseId, testIdPrefix }) {
  const { data: jobs } = useBackend(
    [`/api/jobs/course`, courseId],
    {
      method: "GET",
      url: "/api/jobs/course",
      params: { courseId },
    },
    [],
  );

  return (
    <div data-testid={`${testIdPrefix}-jobs-tab`}>
      <h4 className="mb-3">Job Status</h4>
      <JobsTable jobs={jobs} />
    </div>
  );
}