import React from "react";
import DokkuAccountTranslationsForm from "main/components/Dokku/DokkuAccountTranslationsForm";
import { dokkuAccountTranslationsFixtures } from "fixtures/dokkuAccountTranslationsFixtures";

export default {
  title: "components/Dokku/DokkuAccountTranslationsForm",
  component: DokkuAccountTranslationsForm,
};

const Template = (args) => {
  return <DokkuAccountTranslationsForm {...args} />;
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
  initialContents: dokkuAccountTranslationsFixtures.oneTranslation[0],
  buttonLabel: "Update",
  submitAction: (data) => {
    console.log("Submit was clicked with data: ", data);
    window.alert("Submit was clicked with data: " + JSON.stringify(data));
  },
};
