import React from "react";
import SingleButtonJobForm from "main/components/Jobs/SingleButtonJobForm";

export default {
  title: "components/Jobs/SingleButtonJobForm",
  component: SingleButtonJobForm,
};

const Template = (args) => <SingleButtonJobForm {...args} />;

export const Default = Template.bind({});
Default.args = {
  text: "Launch Job",
  callback: () => console.log("Button clicked"),
};

export const UpdateAllUsers = Template.bind({});
UpdateAllUsers.args = {
  text: "Update All Users",
  callback: () => console.log("Update All Users button clicked"),
};

export const AuditAllCourses = Template.bind({});
AuditAllCourses.args = {
  text: "Audit All Courses",
  callback: () => console.log("Audit All Courses button clicked"),
};
