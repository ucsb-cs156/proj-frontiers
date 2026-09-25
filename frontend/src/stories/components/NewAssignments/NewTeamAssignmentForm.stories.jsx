import React from "react";
import NewTeamAssignmentForm from "main/components/NewAssignments/NewTeamAssignmentForm";
import { assignmentToFormData } from "main/utils/newAssignmentsUtils";
import { newAssignmentsFixtures } from "fixtures/newAssignmentsFixtures";

export default {
  title: "components/NewAssignments/NewTeamAssignmentForm",
  component: NewTeamAssignmentForm,
};

const Template = (args) => {
  return <NewTeamAssignmentForm {...args} />;
};

const submitAction = (data) => {
  console.log("Submit was clicked with data: ", data);
  window.alert("Submit was clicked with data: " + JSON.stringify(data));
};

export const Create = Template.bind({});
Create.args = {
  buttonLabel: "Create",
  submitAction,
};

export const Update = Template.bind({});
Update.args = {
  initialContents: assignmentToFormData(
    newAssignmentsFixtures.threeAssignments[2],
  ),
  buttonLabel: "Update",
  submitAction,
};
