import React from "react";
import RosterStudentForm from "main/components/RosterStudent/RosterStudentForm";
import { rosterStudentFixtures } from "fixtures/rosterStudentFixtures";
import { sectionsFixtures } from "fixtures/sectionsFixtures";
import { http, HttpResponse } from "msw";

export default {
  title: "components/RosterStudent/RosterStudentForm",
  component: RosterStudentForm,
};

const Template = (args) => {
  return <RosterStudentForm {...args} />;
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
  initialContents: rosterStudentFixtures.oneStudent[0],
  buttonLabel: "Update",
  submitAction: (data) => {
    console.log("Submit was clicked with data: ", data);
    window.alert("Submit was clicked with data: " + JSON.stringify(data));
  },
};

export const UpdateWithTranslatedSections = Template.bind({});

UpdateWithTranslatedSections.args = {
  courseId: 7,
  translateSections: true,
  initialContents: rosterStudentFixtures.oneStudent[0],
  buttonLabel: "Update",
  submitAction: (data) => {
    console.log("Submit was clicked with data: ", data);
    window.alert("Submit was clicked with data: " + JSON.stringify(data));
  },
};
UpdateWithTranslatedSections.parameters = {
  msw: [
    http.get("/api/courses/7/sections", () => {
      return HttpResponse.json(sectionsFixtures.threeSections, {
        status: 200,
      });
    }),
  ],
};
