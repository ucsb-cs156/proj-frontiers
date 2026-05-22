import React from "react";
import { http, HttpResponse } from "msw";
import { CourseWarningBanner } from "main/components/Courses/CourseWarningBanner";
import {
  showOrganizationAgeWarning,
  hideOrganizationAgeWarning,
  readBasePermission,
  writeBasePermission,
  adminBasePermission,
  bothWarnings,
  noOrgLinked,
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
Default.args = {
  courseId: 1,
  orgName: "ucsb-cs156-s25",
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

export const Empty = Template.bind({});
Empty.args = {
  courseId: 1,
  orgName: "ucsb-cs156-s25",
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

export const ReadPermission = Template.bind({});
ReadPermission.args = {
  courseId: 1,
  orgName: "ucsb-cs156-s25",
};
ReadPermission.parameters = {
  msw: {
    handlers: [
      http.get("/api/courses/warnings/1", () =>
        HttpResponse.json(readBasePermission),
      ),
    ],
  },
};

export const WritePermission = Template.bind({});
WritePermission.args = {
  courseId: 1,
  orgName: "ucsb-cs156-s25",
};
WritePermission.parameters = {
  msw: {
    handlers: [
      http.get("/api/courses/warnings/1", () =>
        HttpResponse.json(writeBasePermission),
      ),
    ],
  },
};

export const AdminPermission = Template.bind({});
AdminPermission.args = {
  courseId: 1,
  orgName: "ucsb-cs156-s25",
};
AdminPermission.parameters = {
  msw: {
    handlers: [
      http.get("/api/courses/warnings/1", () =>
        HttpResponse.json(adminBasePermission),
      ),
    ],
  },
};

export const BothWarnings = Template.bind({});
BothWarnings.args = {
  courseId: 1,
  orgName: "ucsb-cs156-s25",
};
BothWarnings.parameters = {
  msw: {
    handlers: [
      http.get("/api/courses/warnings/1", () =>
        HttpResponse.json(bothWarnings),
      ),
    ],
  },
};

export const NoOrgLinked = Template.bind({});
NoOrgLinked.args = {
  courseId: 1,
};
NoOrgLinked.parameters = {
  msw: {
    handlers: [
      http.get("/api/courses/warnings/1", () =>
        HttpResponse.json(noOrgLinked),
      ),
    ],
  },
};
