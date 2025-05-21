import React from "react";
import RosterStudentTable from "main/components/RosterStudents/RosterStudentsTable";
import rosterStudentsFixtures from "fixtures/rosterStudentsFixtures";

export default {
  title: "components/RosterStudents/RosterStudentTable",
  component: RosterStudentTable,
};

const Template = (args) => <RosterStudentTable {...args} />;

export const Empty = Template.bind({});
Empty.args = {
  rosterStudents: [],
  showButtons: false,
  storybook: true,
};

export const ThreeRosterStudents = Template.bind({});
ThreeRosterStudents.args = {
  rosterStudents: rosterStudentsFixtures.threeRosterStudents,
  showButtons: false,
  storybook: true,
};

export const ThreeRosterStudentsWithButtons = Template.bind({});
ThreeRosterStudentsWithButtons.args = {
  rosterStudents: rosterStudentsFixtures.threeRosterStudents,
  showButtons: true,
  storybook: true,
};
ThreeRosterStudentsWithButtons.parameters = {};
