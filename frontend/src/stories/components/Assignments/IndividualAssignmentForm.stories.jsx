import React from "react";
import IndividualAssignmentForm from "main/components/Assignments/IndividualAssignmentForm";

export default {
  title: "components/Assignments/IndividualAssignmentForm",
  component: IndividualAssignmentForm,
  parameters: {
    docs: {
      description: {
        component:
          "Form for creating individual assignments with repository creation. Supports three creation targets: Students Only (default), Staff Only, or Both Students and Staff.",
      },
    },
  },
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

Create.parameters = {
  docs: {
    description: {
      story:
        "Default form with all fields. The Repository Creation Target dropdown allows the selection of who will receive repositories: Students Only (default), Staff Only, or Both Students and Staff.",
    },
  },
};
