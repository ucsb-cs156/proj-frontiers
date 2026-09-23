import React from "react";
import { useNavigate, useParams } from "react-router";
import { Button } from "react-bootstrap";
import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import { useBackend } from "main/utils/useBackend";

/**
 * Shows the full log of a single job.
 *
 * The Jobs tables (admin-global and per-course) only receive a short tail
 * preview of each log from the backend (lib-jobs populates Job.log with the
 * last few lines only), so this page is where the complete log lives.
 *
 * It serves two routes and decides what to fetch from the route params:
 *  - /admin/jobs/logs/:id
 *      -> GET /api/jobs/logs/{id} (lib-jobs endpoint, ROLE_ADMIN only)
 *  - /instructor/courses/:courseId/jobs/:jobId/logs
 *      -> GET /api/jobs/course/logs?courseId=&jobId= (app endpoint, guarded by
 *         course manage permissions, so non-admin instructors/staff can use it)
 */
export default function JobLogPage() {
  const { id, courseId, jobId } = useParams();
  const navigate = useNavigate();

  const isCourseScoped = courseId !== undefined;
  const effectiveJobId = isCourseScoped ? jobId : id;

  const queryKey = isCourseScoped
    ? ["/api/jobs/course/logs", courseId, jobId]
    : [`/api/jobs/logs/${id}`];

  const axiosParameters = isCourseScoped
    ? {
        // Stryker disable next-line StringLiteral: axios default is GET
        method: "GET",
        url: "/api/jobs/course/logs",
        params: { courseId, jobId },
      }
    : {
        // Stryker disable next-line StringLiteral: axios default is GET
        method: "GET",
        url: `/api/jobs/logs/${id}`,
      };

  const backPath = isCourseScoped
    ? `/instructor/courses/${courseId}`
    : "/admin/jobs";

  const { data: log, isError, refetch } = useBackend(queryKey, axiosParameters);

  let content;
  if (isError) {
    content = <p data-testid="JobLogPage-error">Error loading log.</p>;
  } else if (log === undefined) {
    content = <p data-testid="JobLogPage-loading">Loading...</p>;
  } else if (log === "") {
    content = <p data-testid="JobLogPage-empty">No log lines yet.</p>;
  } else {
    content = (
      <pre data-testid="JobLogPage-log" style={{ whiteSpace: "pre-wrap" }}>
        {log}
      </pre>
    );
  }

  return (
    <BasicLayout>
      <div className="pt-2">
        <h2 data-testid="JobLogPage-heading">
          Job Log for Job {effectiveJobId}
        </h2>
        <div className="mb-3">
          <Button
            variant="secondary"
            onClick={() => navigate(backPath)}
            data-testid="JobLogPage-back"
          >
            Back
          </Button>
          <Button
            className="ms-2"
            onClick={() => refetch()}
            data-testid="JobLogPage-refresh"
          >
            Refresh
          </Button>
        </div>
        {content}
      </div>
    </BasicLayout>
  );
}
