import React from "react";

import InstructorCoursesTable from "main/components/Courses/InstructorCoursesTable";
import coursesFixtures from "fixtures/coursesFixtures";
import { currentUserFixtures } from "fixtures/currentUserFixtures";
import { http, HttpResponse } from "msw";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { schoolList } from "fixtures/schoolFixtures";

export default {
  title: "components/Courses/InstructorCoursesTable",
  component: InstructorCoursesTable,
};

const QueryWrapper = ({ children }) => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false, // Don't retry failed queries in Storybook
      },
    },
  });
  return (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
};

const Template = (args) => {
  return (
    <QueryWrapper>
      <InstructorCoursesTable {...args} />
    </QueryWrapper>
  );
};

export const AdminUser = Template.bind({});
export const InstructorUser = Template.bind({});
export const EmptyTable = Template.bind({});
export const AdminUserWithBadEmailError = Template.bind({});

AdminUser.args = {
  courses: coursesFixtures.severalCourses,
  currentUser: currentUserFixtures.adminUser,
  storybook: true,
  enableInstructorUpdate: true,
};

InstructorUser.args = {
  courses: coursesFixtures.severalCourses,
  currentUser: currentUserFixtures.instructorUser,
  storybook: true,
};
InstructorUser.parameters = {
  msw: [
    http.get("/api/systemInfo/schools", () => {
      return HttpResponse.json(schoolList, {
        status: 200,
      });
    }),
  ],
};

EmptyTable.args = {
  courses: [],
  currentUser: currentUserFixtures.adminUser,
  storybook: true,
  enableInstructorUpdate: true,
};
EmptyTable.parameters = {};

AdminUser.args = {
  courses: coursesFixtures.severalCourses,
  currentUser: currentUserFixtures.adminUser,
  storybook: true,
  enableInstructorUpdate: true,
};
AdminUser.parameters = {
  msw: [
    http.put("/api/courses/updateInstructor", ({ request }) => {
      window.alert(
        `Would have made HTTP request: ${request.method} ${request.url}`,
      );
      return HttpResponse.text("Mocked response for storybook", {
        status: 200,
      });
    }),
    http.get("/api/systemInfo/schools", () => {
      return HttpResponse.json(schoolList, {
        status: 200,
      });
    }),
  ],
};

AdminUserWithBadEmailError.args = {
  courses: coursesFixtures.severalCourses,
  currentUser: currentUserFixtures.adminUser,
  storybook: true,
  enableInstructorUpdate: true,
};
AdminUserWithBadEmailError.parameters = {
  msw: [
    http.put("/api/courses/updateInstructor", ({ request }) => {
      window.alert(
        `Would have made HTTP request: ${request.method} ${request.url}`,
      );
      return HttpResponse.json(
        {
          message: "Email must belong to either an instructor or admin",
          type: "IllegalArgumentException",
        },
        {
          status: 400,
        },
      );
    }),
    http.get("/api/systemInfo/schools", () => {
      return HttpResponse.json(schoolList, {
        status: 200,
      });
    }),
  ],
};
