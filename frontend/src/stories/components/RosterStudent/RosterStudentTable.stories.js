import React from "react";
import RosterStudentTable from "main/components/RosterStudent/RosterStudentTable";
import { rosterStudentFixtures } from "fixtures/rosterStudentFixtures";
import { currentUserFixtures } from "fixtures/currentUserFixtures";
// Remove the 'rest' import since it's not being used

export default {
  title: "components/RosterStudent/RosterStudentTable",
  component: RosterStudentTable,
};

const Template = (args) => {
  return <RosterStudentTable {...args} />;
};

export const Empty = Template.bind({});

Empty.args = {
  rosterStudents: [],
};

export const ThreeRosterStudents = Template.bind({});

ThreeRosterStudents.args = {
  rosterStudents: rosterStudentFixtures.threeRosterStudents,
};

export const ThreeRosterStudentsAsAdmin = Template.bind({});

ThreeRosterStudentsAsAdmin.args = {
  rosterStudents: rosterStudentFixtures.threeRosterStudents,
  currentUser: currentUserFixtures.adminUser,
};

export const ThreeRosterStudentsAsUser = Template.bind({});

ThreeRosterStudentsAsUser.args = {
  rosterStudents: rosterStudentFixtures.threeRosterStudents,
  currentUser: currentUserFixtures.userOnly,
};
