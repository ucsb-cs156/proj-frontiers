import React from "react";
import RoleEmailForm from "main/components/Users/RoleEmailForm";
import { roleEmailFormFixtures } from "fixtures/roleEmailFormFixtures";

export default {
  title: "components/Users/RoleEmailForm",
  component: RoleEmailForm,
};

const Template = (args) => {
  return <RoleEmailForm {...args} />;
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
  initialContents: roleEmailFormFixtures.oneRoleEmailForm,
  buttonLabel: "Update",
  submitAction: (data) => {
    console.log("Submit was clicked with data: ", data);
    window.alert("Submit was clicked with data: " + JSON.stringify(data));
  },
};
