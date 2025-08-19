import React from "react";

import InstructorCoursesTable from "main/components/Courses/InstructorCoursesTable";
import coursesFixtures from "fixtures/coursesFixtures";
import { currentUserFixtures } from "fixtures/currentUserFixtures";
import { http, HttpResponse } from "msw";

export default {
  title: "components/Courses/InstructorCoursesTable",
  component: InstructorCoursesTable,
};

const Template = (args) => {
  return <InstructorCoursesTable {...args} />;
};

export const AdminUser = Template.bind({});
export const InstructorUser = Template.bind({});
export const EmptyTable = Template.bind({});
export const AdminUserWithBadEmailError = Template.bind({});
export const AdminUserWithNetworkError = Template.bind({});

AdminUser.args = {
  courses: coursesFixtures.severalCourses,
  currentUser: currentUserFixtures.adminUser,
  storybook: true,
  enableInstructorUpdate: true,
};
AdminUser.parameters = {};

InstructorUser.args = {
  courses: coursesFixtures.severalCourses,
  currentUser: currentUserFixtures.instructorUser,
  storybook: true,
};
InstructorUser.parameters = {};

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
  ],
};
