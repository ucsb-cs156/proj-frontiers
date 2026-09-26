import React from "react";
import JobLogTail from "main/components/Jobs/JobLogTail";
import { http, HttpResponse } from "msw";

export default {
  title: "components/Jobs/JobLogTail",
  component: JobLogTail,
};

const Template = (args) => {
  return <JobLogTail {...args} />;
};

// A job that writes one more log line each time it is asked, and is over
// after the tenth, so that the log can be watched growing.
const growingLogHandler = () => {
  let lines = 0;
  return http.get("/api/jobs/course/logs/tail", ({ request }) => {
    const afterId = Number(new URL(request.url).searchParams.get("afterId"));
    if (lines < 10) {
      lines += 1;
    }
    const newLines = [];
    for (let id = afterId + 1; id <= lines; id++) {
      newLines.push({ id, jobId: 12, message: `Creating repository ${id}` });
    }
    return HttpResponse.json(
      { status: lines < 10 ? "running" : "complete", lines: newLines },
      { status: 200 },
    );
  });
};

export const Running = Template.bind({});
Running.args = {
  courseId: 7,
  jobId: 12,
};
Running.parameters = {
  msw: [growingLogHandler()],
};

export const Complete = Template.bind({});
Complete.args = {
  courseId: 7,
  jobId: 12,
};
Complete.parameters = {
  msw: [
    http.get("/api/jobs/course/logs/tail", () => {
      return HttpResponse.json(
        {
          status: "complete",
          lines: [
            { id: 1, jobId: 12, message: "Creating repository lab01-cgaucho" },
            {
              id: 2,
              jobId: 12,
              message: "Creating repository lab01-ldelplaya",
            },
            { id: 3, jobId: 12, message: "Done" },
          ],
        },
        { status: 200 },
      );
    }),
  ],
};

export const Queued = Template.bind({});
Queued.args = {
  courseId: 7,
  jobId: 12,
};
Queued.parameters = {
  msw: [
    http.get("/api/jobs/course/logs/tail", () => {
      return HttpResponse.json(
        { status: "queued", lines: [] },
        { status: 200 },
      );
    }),
  ],
};

export const NotFound = Template.bind({});
NotFound.args = {
  courseId: 7,
  jobId: 12,
};
NotFound.parameters = {
  msw: [
    http.get("/api/jobs/course/logs/tail", () => {
      return HttpResponse.json(
        {
          message: "Job with id 12 not found",
          type: "EntityNotFoundException",
        },
        { status: 404 },
      );
    }),
  ],
};
