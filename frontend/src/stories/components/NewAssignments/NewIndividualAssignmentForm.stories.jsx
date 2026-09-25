import React from "react";
import NewIndividualAssignmentForm from "main/components/NewAssignments/NewIndividualAssignmentForm";
import { assignmentToFormData } from "main/utils/newAssignmentsUtils";
import { newAssignmentsFixtures } from "fixtures/newAssignmentsFixtures";

export default {
  title: "components/NewAssignments/NewIndividualAssignmentForm",
  component: NewIndividualAssignmentForm,
};

const Template = (args) => {
  return <NewIndividualAssignmentForm {...args} />;
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
    newAssignmentsFixtures.threeAssignments[1],
  ),
  buttonLabel: "Update",
  submitAction,
};
