import React from "react";
import { HttpResponse, http } from "msw";
import {
  withRouter,
  reactRouterParameters,
} from "storybook-addon-remix-react-router";
import JobLogPage from "main/pages/Jobs/JobLogPage";
import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";

const fullLog = Array.from(
  { length: 25 },
  (_, i) =>
    `Line ${i + 1} of a job log that is longer than the 10-line preview`,
).join("\n");

const basicHandlers = [
  http.get("/api/currentUser", () => {
    return HttpResponse.json(apiCurrentUserFixtures.adminUser);
  }),
  http.get("/api/systemInfo", () => {
    return HttpResponse.json(systemInfoFixtures.showingNeither);
  }),
];

export default {
  title: "pages/Jobs/JobLogPage",
  component: JobLogPage,
  decorators: [withRouter],
};

const Template = () => <JobLogPage />;

export const AdminRoute = Template.bind({});
AdminRoute.args = {
  suppressMemoryRouter: true,
};
AdminRoute.parameters = {
  reactRouter: reactRouterParameters({
    location: {
      pathParams: { id: "1" },
    },
    routing: { path: "/admin/jobs/logs/:id" },
  }),
  msw: {
    handlers: [
      ...basicHandlers,
      http.get("/api/jobs/logs/1", () => {
        return HttpResponse.text(fullLog);
      }),
    ],
  },
};

export const InstructorRoute = Template.bind({});
InstructorRoute.args = {
  suppressMemoryRouter: true,
};
InstructorRoute.parameters = {
  reactRouter: reactRouterParameters({
    location: {
      pathParams: { courseId: "7", jobId: "1" },
    },
    routing: { path: "/instructor/courses/:courseId/jobs/:jobId/logs" },
  }),
  msw: {
    handlers: [
      ...basicHandlers,
      http.get("/api/jobs/course/logs", () => {
        return HttpResponse.text(fullLog);
      }),
    ],
  },
};

export const EmptyLog = Template.bind({});
EmptyLog.args = {
  suppressMemoryRouter: true,
};
EmptyLog.parameters = {
  reactRouter: reactRouterParameters({
    location: {
      pathParams: { id: "2" },
    },
    routing: { path: "/admin/jobs/logs/:id" },
  }),
  msw: {
    handlers: [
      ...basicHandlers,
      http.get("/api/jobs/logs/2", () => {
        return HttpResponse.text("");
      }),
    ],
  },
};
