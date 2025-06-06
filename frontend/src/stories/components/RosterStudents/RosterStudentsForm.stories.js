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
const contents = {
  studentId: rosterStudentsFixtures.threeRosterStudents[0].studentId,
  firstName: rosterStudentsFixtures.threeRosterStudents[0].firstName,
  lastName: rosterStudentsFixtures.threeRosterStudents[0].lastName,
  email: rosterStudentsFixtures.threeRosterStudents[0].email,
};

Update.args = {
  initialContents: contents,
  buttonLabel: "Update",
  submitAction: (data) => {
    console.log("Submit was clicked with data: ", data);
    window.alert("Submit was clicked with data: " + JSON.stringify(data));
  },
};
