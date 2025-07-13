import React from "react";
import RosterStudentTable from "main/components/RosterStudent/RosterStudentTable";
import { rosterStudentFixtures } from "fixtures/rosterStudentFixtures";
import { currentUserFixtures } from "fixtures/currentUserFixtures";
import { http, HttpResponse } from "msw";

export default {
  title: "components/RosterStudent/RosterStudentTable",
  component: RosterStudentTable,
};

const Template = (args) => {
  return <RosterStudentTable {...args} />;
};

export const Empty = Template.bind({});

Empty.args = {
  students: [],
  courseId: "",
  currentUser: currentUserFixtures.userOnly,
};

export const ThreeItemsOrdinaryUser = Template.bind({});

ThreeItemsOrdinaryUser.args = {
  students: rosterStudentFixtures.threeStudents,
  currentUser: currentUserFixtures.userOnly,
  courseId: "1",
};

export const ThreeItemsAdminUser = Template.bind({});
ThreeItemsAdminUser.args = {
  students: rosterStudentFixtures.threeStudents,
  currentUser: currentUserFixtures.adminUser,
  courseId: "1",
};

ThreeItemsAdminUser.parameters = {
  msw: [
    http.delete("/api/rosterstudents/delete", ({ request }) => {
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
