import React from "react";
import SectionsForm from "main/components/Sections/SectionsForm";
import { sectionsFixtures } from "fixtures/sectionsFixtures";

export default {
  title: "components/Sections/SectionsForm",
  component: SectionsForm,
};

const Template = (args) => {
  return <SectionsForm {...args} />;
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
  initialContents: sectionsFixtures.oneSection[0],
  buttonLabel: "Update",
  submitAction: (data) => {
    console.log("Submit was clicked with data: ", data);
    window.alert("Submit was clicked with data: " + JSON.stringify(data));
  },
};
