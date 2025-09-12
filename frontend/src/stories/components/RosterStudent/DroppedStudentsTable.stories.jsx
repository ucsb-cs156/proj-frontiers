import { currentUserFixtures } from "fixtures/currentUserFixtures";
import { rosterStudentFixtures } from "fixtures/rosterStudentFixtures";
import React from "react";
import DroppedStudentsTable from "main/components/RosterStudent/DroppedStudentsTable";
import { http, HttpResponse } from "msw";

export default {
  title: "components/RosterStudent/DroppedStudentsTable",
  component: DroppedStudentsTable,
};

const Template = (args) => {
  return <DroppedStudentsTable {...args} />;
};

export const Empty = Template.bind({});

Empty.args = {
  students: [],
  courseId: "",
  currentUser: currentUserFixtures.userOnly,
};

Empty.parameters = {
  msw: [
    http.put("/api/rosterstudents/restore", ({ request }) => {
      const url = new URL(request.url);
      window.alert(
        "Invoked put with URL: " +
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

export const ThreeItems = Template.bind({});

ThreeItems.args = {
  students: rosterStudentFixtures.threeStudents,
  currentUser: currentUserFixtures.userOnly,
  courseId: "1",
};

ThreeItems.parameters = {
  msw: [
    http.put("/api/rosterstudents/restore", ({ request }) => {
      const url = new URL(request.url);
      window.alert(
        "Invoked put with URL: " +
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
