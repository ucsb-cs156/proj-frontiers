import React from "react";
import CourseStaffCSVUploadForm from "main/components/CourseStaff/CourseStaffCSVUploadForm";

export default {
  title: "components/CourseStaff/CourseStaffCSVUploadForm",
  component: CourseStaffCSVUploadForm,
};

const Template = (args) => {
  return <CourseStaffCSVUploadForm {...args} />;
};

export const Default = Template.bind({});
Default.args = {
  submitAction: (data) => {
    console.log("Submit", data);
  },
};
