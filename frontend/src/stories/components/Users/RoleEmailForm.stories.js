import React from "react";
import RoleEmailForm from "main/components/Users/RoleEmailForm";

export default {
  title: "components/Users/RoleEmailForm",
  component: RoleEmailForm,
};

const Template = (args) => {
  return <RoleEmailForm {...args} />;
};

export const Create = Template.bind({});

Create.args = {
  initialContents: {},
  buttonLabel: "Add Email",
  submitAction: (data) => {
    console.log("Submitted with: ", data);
    window.alert("Submitted with: " + JSON.stringify(data));
  },
};
