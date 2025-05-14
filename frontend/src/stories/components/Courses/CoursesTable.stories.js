import React from "react";

import CoursesTable from "main/components/Courses/CoursesTable";
import coursesFixtures from "fixtures/coursesFixtures";

export default {
  title: "components/Courses/CoursesTable",
  component: CoursesTable,
};

const Template = (args) => {
  return <CoursesTable {...args} />;
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
