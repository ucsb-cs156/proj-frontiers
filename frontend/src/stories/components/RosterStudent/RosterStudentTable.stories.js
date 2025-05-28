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
  items: [],
  currentUser: currentUserFixtures.userOnly,
};

export const ThreeStudentsAdminUser = Template.bind({});
ThreeStudentsAdminUser.args = {
  items: rosterStudentFixtures.threeRosterStudents,
  currentUser: currentUserFixtures.adminUser,
};

ThreeStudentsAdminUser.parameters = {
  msw: [
    http.delete("/api/rosterstudent", () => {
      return HttpResponse.json(
        { message: "Student deleted successfully" },
        { status: 200 },
      );
    }),
  ],
};
