import React from "react";
import { rest } from "msw";
import AdminJobsPage from "main/pages/Admin/AdminJobsPage";
import { jobsFixtures } from "fixtures/jobsFixtures";
import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";

export default {
  title: "pages/Admin/AdminJobsPage",
  component: AdminJobsPage,
  parameters: {
    msw: [
      rest.get("/api/currentUser", (_req, res, ctx) => {
        return res(ctx.json(apiCurrentUserFixtures.adminUser));
      }),
      rest.get("/api/systemInfo", (_req, res, ctx) => {
        return res(ctx.json(systemInfoFixtures.showingNeither));
      }),
      rest.get("/api/jobs/all", (_req, res, ctx) => {
        return res(ctx.json(jobsFixtures.threeJobs));
      }),
      rest.post("/api/jobs/launch/updateAll", (_req, res, ctx) => {
        return res(ctx.json({
          id: 4,
          createdAt: "2023-01-04T10:00:00",
          updatedAt: "2023-01-04T10:00:00",
          status: "running",
          log: "Job is starting...",
        }));
      }),
      rest.post("/api/jobs/launch/auditAllCourses", (_req, res, ctx) => {
        return res(ctx.json({
          id: 5,
          createdAt: "2023-01-05T10:00:00",
          updatedAt: "2023-01-05T10:00:00",
          status: "running",
          log: "Job is starting...",
        }));
      }),
      rest.delete("/api/jobs/all", (_req, res, ctx) => {
        return res(ctx.json({ message: "All jobs deleted" }));
      }),
    ],
  },
};

const Template = () => <AdminJobsPage />;

export const Default = Template.bind({});