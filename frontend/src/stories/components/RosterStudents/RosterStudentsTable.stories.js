import React from "react";

import RosterStudentsTable from "main/components/RosterStudents/RosterStudentsTable";
import rosterStudentsFixtures from "fixtures/rosterStudentsFixtures";

export default {
  title: "components/RosterStudents/RosterStudentsTable",
  component: RosterStudentsTable,
};

const Template = (args) => {
  return <RosterStudentsTable {...args} />;
};

export const Empty = Template.bind({});
export const ThreeRosterStudents = Template.bind({});
export const ThreeRosterStudentsWithButtons = Template.bind({});

Empty.args = {
  rosterStudents: [],
};
Empty.parameters = {};

ThreeRosterStudents.args = {
  rosterStudents: rosterStudentsFixtures.threeRosterStudents,
};
ThreeRosterStudents.parameters = {};

// This story is for testing the Edit/Delete buttons
ThreeRosterStudentsWithButtons.args = {
  rosterStudents: rosterStudentsFixtures.threeRosterStudents,
  showButtons: true,
  storybook: true,
};
ThreeRosterStudentsWithButtons.parameters = {};
