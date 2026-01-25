import React from "react";
import TeamRepositoryAssignmentForm from "main/components/Assignments/TeamRepositoryAssignmentForm";

export default {
  title: "components/Assignments/TeamRepositoryAssignmentForm",
  component: TeamRepositoryAssignmentForm,
};

const Template = (args) => {
  return <TeamRepositoryAssignmentForm {...args} />;
};

export const Create = Template.bind({});

Create.args = {
  buttonLabel: "Create",
  submitAction: (data) => {
    console.log("Submit was clicked with data: ", data);
    window.alert("Submit was clicked with data: " + JSON.stringify(data));
  },
};
