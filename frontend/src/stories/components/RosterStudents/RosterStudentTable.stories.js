import React from "react";
import RosterStudentTable from "main/components/RosterStudents/RosterStudentsTable";
import RosterStudentsFixtures from "fixtures/RosterStudentsFixtures";


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
  rosterStudents: RosterStudentsFixtures.threeRosterStudents,
  showButtons: false,
  storybook: true,
};

export const ThreeRosterStudentsWithButtons = Template.bind({});
ThreeRosterStudentsWithButtons.args = {
  rosterStudents: RosterStudentsFixtures.threeRosterStudents,
  showButtons: true,
  storybook: true,
};
ThreeRosterStudentsWithButtons.parameters = {};
