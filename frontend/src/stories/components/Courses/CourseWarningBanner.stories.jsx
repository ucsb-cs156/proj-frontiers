import React from "react";
import { http, HttpResponse } from "msw";
import { CourseWarningBanner } from "main/components/Courses/CourseWarningBanner";
import {
  showOrganizationAgeWarning,
  hideOrganizationAgeWarning,
  showDefaultBasePermissionsWarning,
  showFreePlanWarning,
} from "fixtures/courseWarningFixtures";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

export default {
  title: "components/Courses/CourseWarningBanner",
  component: CourseWarningBanner,
};

const Template = (args) => {
  const queryClient = new QueryClient();
  return (
    <QueryClientProvider client={queryClient}>
      <CourseWarningBanner {...args} />
    </QueryClientProvider>
  );
};

export const Default = Template.bind({});

export const Empty = Template.bind({});

export const DefaultBasePermissionWarning = Template.bind({});

export const FreePlanWarning = Template.bind({});

Default.args = {
  courseId: 1,
};

Default.parameters = {
  msw: {
    handlers: [
      http.get("/api/courses/warnings/1", () =>
        HttpResponse.json(showOrganizationAgeWarning),
      ),
    ],
  },
};

Empty.args = {
  courseId: 1,
};

Empty.parameters = {
  msw: {
    handlers: [
      http.get("/api/courses/warnings/1", () =>
        HttpResponse.json(hideOrganizationAgeWarning),
      ),
    ],
  },
};

DefaultBasePermissionWarning.args = {
  courseId: 1,
  orgName: "ucsb-cs156-s26",
};

DefaultBasePermissionWarning.parameters = {
  msw: {
    handlers: [
      http.get("/api/courses/warnings/1", () =>
        HttpResponse.json(showDefaultBasePermissionsWarning),
      ),
    ],
  },
};

FreePlanWarning.args = {
  courseId: 1,
  orgName: "ucsb-cs156-s26",
};

FreePlanWarning.parameters = {
  msw: {
    handlers: [
      http.get("/api/courses/warnings/1", () =>
        HttpResponse.json(showFreePlanWarning),
      ),
      http.post(
        "/api/courses/warnings/hideFreePlanWarning/1",
        ({ request }) => {
          window.alert(
            `Would have made HTTP request: ${request.method} ${request.url}`,
          );
          return HttpResponse.json(
            { message: "hideFreePlanWarning set to true for course with id 1" },
            { status: 200 },
          );
        },
      ),
    ],
  },
};
