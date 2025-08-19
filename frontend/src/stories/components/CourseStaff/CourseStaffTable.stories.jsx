import React from "react";
import CourseStaffTable from "main/components/CourseStaff/CourseStaffTable";
import { courseStaffFixtures } from "fixtures/courseStaffFixtures";
import { currentUserFixtures } from "fixtures/currentUserFixtures";
import { http, HttpResponse } from "msw";

export default {
  title: "components/CourseStaff/CourseStaffTable",
  component: CourseStaffTable,
};

const Template = (args) => {
  return <CourseStaffTable {...args} />;
};

export const Empty = Template.bind({});

Empty.args = {
  staff: [],
  courseId: "",
  currentUser: currentUserFixtures.userOnly,
};

export const ThreeItemsOrdinaryUser = Template.bind({});

ThreeItemsOrdinaryUser.args = {
  staff: courseStaffFixtures.threeStaff,
  currentUser: currentUserFixtures.userOnly,
  courseId: "1",
};

export const ThreeItemsAdminUser = Template.bind({});
ThreeItemsAdminUser.args = {
  staff: courseStaffFixtures.threeStaff,
  currentUser: currentUserFixtures.adminUser,
  courseId: "1",
};

ThreeItemsAdminUser.parameters = {
  msw: [
    http.delete("/api/coursestaff/delete", ({ request }) => {
      const url = new URL(request.url);
      window.alert(
        "Invoked delete with URL: " +
          url +
          " and params: " +
          JSON.stringify(Object.fromEntries(url.searchParams)),
      );
      return HttpResponse.json(
        {},
        {
          status: 200,
        },
      );
    }),
  ],
};

export const ItemWithEachStatusAdminUser = Template.bind({});
ItemWithEachStatusAdminUser.args = {
  staff: courseStaffFixtures.staffWithEachStatus,
  currentUser: currentUserFixtures.adminUser,
};

ItemWithEachStatusAdminUser.parameters = {
  msw: [
    http.delete("/api/coursestaff/delete", ({ request }) => {
      const url = new URL(request.url);
      window.alert(
        "Invoked delete with URL: " +
          url +
          " and params: " +
          JSON.stringify(Object.fromEntries(url.searchParams)),
      );
      return HttpResponse.json(
        {},
        {
          status: 200,
        },
      );
    }),
  ],
};
