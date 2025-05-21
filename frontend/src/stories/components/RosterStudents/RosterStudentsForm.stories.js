import React from "react";
import RosterStudentsForm from "main/components/RosterStudents/RosterStudentsForm";
import rosterStudentsFixtures from "fixtures/rosterStudentsFixtures";

export default {
  title: "components/RosterStudents/RosterStudentsForm",
  component: RosterStudentsForm,
};

const Template = (args) => {
  return <RosterStudentsForm {...args} />;
};

export const Create = Template.bind({});

Create.args = {
  buttonLabel: "Create",
  submitAction: (data) => {
    console.log("Submit was clicked with data: ", data);
    window.alert("Submit was clicked with data: " + JSON.stringify(data));
  },
};

export const Update = Template.bind({});

Update.args = {
  initialContents: rosterStudentsFixtures.threeRosterStudents[0],
  buttonLabel: "Update",
  submitAction: (data) => {
    console.log("Submit was clicked with data: ", data);
    window.alert("Submit was clicked with data: " + JSON.stringify(data));
  },
};
