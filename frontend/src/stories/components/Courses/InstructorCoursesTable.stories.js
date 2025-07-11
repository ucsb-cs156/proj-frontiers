import React from "react";

import InstructorCoursesTable from "main/components/Courses/InstructorCoursesTable";
import coursesFixtures from "fixtures/coursesFixtures";
import { currentUserFixtures } from "fixtures/currentUserFixtures";

export default {
  title: "components/Courses/InstructorCoursesTable",
  component: InstructorCoursesTable,
};

const Template = (args) => {
  return <InstructorCoursesTable {...args} />;
};

export const Empty = Template.bind({});
export const AdminCoursesAdminUser = Template.bind({});
export const AdminCoursesInstructorUser = Template.bind({});

Empty.args = {
  courses: [],
};
Empty.parameters = {};

AdminCoursesAdminUser.args = {
  courses: coursesFixtures.severalCourses,
  currentUser: currentUserFixtures.adminUser,
  storybook: true,
};
AdminCoursesAdminUser.parameters = {};

// This story is for testing the install button
AdminCoursesInstructorUser.args = {
  courses: coursesFixtures.severalCourses,
  currentUser: currentUserFixtures.instructorUser,
  storybook: true,
};
AdminCoursesInstructorUser.parameters = {};
