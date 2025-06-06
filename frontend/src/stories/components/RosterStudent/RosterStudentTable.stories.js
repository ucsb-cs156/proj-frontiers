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
  currentUser: currentUserFixtures.userOnly,
};

export const ThreeItemsOrdinaryUser = Template.bind({});

ThreeItemsOrdinaryUser.args = {
  students: rosterStudentFixtures.threeStudents,
  currentUser: currentUserFixtures.userOnly,
};

export const ThreeItemsAdminUser = Template.bind({});
ThreeItemsAdminUser.args = {
  students: rosterStudentFixtures.threeStudents,
  currentUser: currentUserFixtures.adminUser,
};

ThreeItemsAdminUser.parameters = {
  msw: [
    http.delete("/api/rosterstudents", () => {
      return HttpResponse.json(
        { message: "Student deleted successfully" },
        { status: 200 },
      );
    }),
  ],
};
