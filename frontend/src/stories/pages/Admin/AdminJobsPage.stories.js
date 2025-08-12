import React from "react";
import { HttpResponse, http } from "msw";
import AdminJobsPage from "main/pages/Admin/AdminJobsPage";
import { jobsFixtures } from "fixtures/jobsFixtures";
import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";

export default {
  title: "pages/Admin/AdminJobsPage",
  component: AdminJobsPage,
  parameters: {
    msw: {
      handlers: [
        http.get("/api/currentUser", () => {
          return HttpResponse.json(apiCurrentUserFixtures.adminUser);
        }),
        http.get("/api/systemInfo", () => {
          return HttpResponse.json(systemInfoFixtures.showingNeither);
        }),
        http.get("/api/jobs/all", () => {
          return HttpResponse.json(jobsFixtures.threeJobs);
        }),
        http.post("/api/jobs/launch/updateAll", () => {
          return HttpResponse.json({
            id: 4,
            createdAt: "2023-01-04T10:00:00",
            updatedAt: "2023-01-04T10:00:00",
            status: "running",
            log: "Job is starting...",
          });
        }),
        http.post("/api/jobs/launch/auditAllCourses", () => {
          return HttpResponse.json({
            id: 5,
            createdAt: "2023-01-05T10:00:00",
            updatedAt: "2023-01-05T10:00:00",
            status: "running",
            log: "Job is starting...",
          });
        }),
        http.delete("/api/jobs/all", () => {
          return HttpResponse.json({ message: "All jobs deleted" });
        }),
      ],
    },
  },
};

const Template = () => <AdminJobsPage />;

export const Default = Template.bind({});
