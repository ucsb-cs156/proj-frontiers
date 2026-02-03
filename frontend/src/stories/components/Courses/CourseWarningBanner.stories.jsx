import React from "react";
import { http, HttpResponse } from "msw";
import { CourseWarningBanner } from "main/components/Courses/CourseWarningBanner";
import {
  showOrganizationAgeWarning,
  hideOrganizationAgeWarning,
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
