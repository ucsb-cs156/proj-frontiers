import React from "react";
import JobLogModal from "main/components/Jobs/JobLogModal";
import { http, HttpResponse } from "msw";

export default {
  title: "components/Jobs/JobLogModal",
  component: JobLogModal,
};

const Template = (args) => {
  return <JobLogModal {...args} />;
};

export const Open = Template.bind({});
Open.args = {
  show: true,
  onHide: () => window.alert("The modal would have been closed"),
  courseId: 7,
  jobId: 12,
};
Open.parameters = {
  msw: [
    http.get("/api/jobs/course/logs/tail", () => {
      return HttpResponse.json(
        {
          status: "complete",
          lines: [
            { id: 1, jobId: 12, message: "Creating repository lab01-cgaucho" },
            { id: 2, jobId: 12, message: "Done" },
          ],
        },
        { status: 200 },
      );
    }),
  ],
};
