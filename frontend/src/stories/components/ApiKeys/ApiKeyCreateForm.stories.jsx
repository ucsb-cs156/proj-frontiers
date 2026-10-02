import React from "react";
import ApiKeyCreateForm from "main/components/ApiKeys/ApiKeyCreateForm";

export default {
  title: "components/ApiKeys/ApiKeyCreateForm",
  component: ApiKeyCreateForm,
};

const Template = (args) => <ApiKeyCreateForm {...args} />;

export const Default = Template.bind({});
Default.args = {
  submitAction: (data) => {
    window.alert("Submit was clicked with data: " + JSON.stringify(data));
  },
};
