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
export const ManyCourses = Template.bind({});

Empty.args = {
  courses: [],
  joinCallback: () => {
    "Join button pressed!";
  },
};
Empty.parameters = {};

ManyCourses.args = {
  courses: coursesFixtures.oneCourseWithEachStatus,
  joinCallback: () => {
    "Join button pressed!";
  },
};
ManyCourses.parameters = {};
