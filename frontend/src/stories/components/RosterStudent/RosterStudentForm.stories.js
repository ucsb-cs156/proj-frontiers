import React from "react";
import RosterStudentForm from "main/components/RosterStudent/RosterStudentForm";
import { rosterStudentFixtures } from "fixtures/rosterStudentFixtures";

export default {
  title: "components/RosterStudent/RosterStudentForm",
  component: RosterStudentForm,
};

const Template = (args) => {
  return <RosterStudentForm {...args} />;
};

export const Create = Template.bind({});

Create.args = {
  buttonLabel: "Create",
  submitAction: () => {
    console.log("Submit was clicked");
  },
};

export const Update = Template.bind({});

Update.args = {
  initialContents: rosterStudentFixtures.oneRosterStudent[0],
  buttonLabel: "Update",
  submitAction: () => {
    console.log("Submit was clicked");
  },
};
