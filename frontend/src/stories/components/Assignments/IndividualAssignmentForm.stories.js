import React from "react";
import IndividualAssignmentForm from "main/components/Assignments/IndividualAssignmentForm";

export default {
  title: "components/Assignments/IndividualAssignmentForm",
  component: IndividualAssignmentForm,
};

const Template = (args) => {
  return <IndividualAssignmentForm {...args} />;
};

export const Create = Template.bind({});

Create.args = {
  buttonLabel: "Create",
  submitAction: (data) => {
    console.log("Submit was clicked with data: ", data);
    window.alert("Submit was clicked with data: " + JSON.stringify(data));
  },
};
