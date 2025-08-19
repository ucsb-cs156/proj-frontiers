import React from "react";
import CourseStaffForm from "main/components/CourseStaff/CourseStaffForm";
import { courseStaffFixtures } from "fixtures/courseStaffFixtures";

export default {
  title: "components/CourseStaff/CourseStaffForm",
  component: CourseStaffForm,
};

const Template = (args) => {
  return <CourseStaffForm {...args} />;
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
  initialContents: courseStaffFixtures.oneStaff[0],
  buttonLabel: "Update",
  submitAction: (data) => {
    console.log("Submit was clicked with data: ", data);
    window.alert("Submit was clicked with data: " + JSON.stringify(data));
  },
};
