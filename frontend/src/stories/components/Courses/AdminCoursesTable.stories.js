import React from "react";

import AdminCoursesTable from "main/components/Courses/AdminCoursesTable";
import coursesFixtures from "fixtures/coursesFixtures";

export default {
  title: "components/Courses/AdminCoursesTable",
  component: AdminCoursesTable,
};

const Template = (args) => {
  return <AdminCoursesTable {...args} />;
};

export const Empty = Template.bind({});
export const ThreeCourses = Template.bind({});
export const ThreeCoursesWithInstallButton = Template.bind({});

Empty.args = {
  courses: [],
};
Empty.parameters = {};

ThreeCourses.args = {
  courses: coursesFixtures.threeCourses,
};
ThreeCourses.parameters = {};

// This story is for testing the install button
ThreeCoursesWithInstallButton.args = {
  courses: coursesFixtures.threeCourses,
  showInstallButton: true,
  storybook: true,
};
ThreeCoursesWithInstallButton.parameters = {};
