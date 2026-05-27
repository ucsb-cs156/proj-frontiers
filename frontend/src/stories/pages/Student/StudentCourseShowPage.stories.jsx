import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import { http, HttpResponse } from "msw";

import {
  withRouter,
  reactRouterParameters,
} from "storybook-addon-remix-react-router";

import StudentCourseShowPage from "main/pages/Student/StudentCourseShowPage";
import coursesFixtures from "fixtures/coursesFixtures";

export default {
  title: "pages/Student/StudentCourseShowPage",
  component: StudentCourseShowPage,
  decorators: [withRouter],
  parameters: {
    reactRouter: reactRouterParameters({
      location: {
        pathParams: { id: "1" },
      },
      routing: { path: "/student/courses/:id" },
    }),
  },
};

const Template = () => <StudentCourseShowPage />;

const exampleCourse = {
  ...coursesFixtures.oneCourseWithEachStatus[0],
  id: 1,
};

const basicHandlers = [
  http.get("/api/currentUser", () => {
    return HttpResponse.json(apiCurrentUserFixtures.userOnly);
  }),
  http.get("/api/systemInfo", () => {
    return HttpResponse.json(systemInfoFixtures.showingNeither);
  }),
];

export const CourseLoaded = Template.bind({});
CourseLoaded.args = {
  suppressMemoryRouter: true,
};
CourseLoaded.parameters = {
  msw: {
    handlers: [
      ...basicHandlers,
      http.get("/api/courses/1", () => {
        return HttpResponse.json(exampleCourse, { status: 200 });
      }),
    ],
  },
};

export const CourseLoading = Template.bind({});
CourseLoading.args = {
  suppressMemoryRouter: true,
};
CourseLoading.parameters = {
  msw: {
    handlers: [
      ...basicHandlers,
      http.get("/api/courses/1", async () => {
        await new Promise(() => {});
      }),
    ],
  },
};
