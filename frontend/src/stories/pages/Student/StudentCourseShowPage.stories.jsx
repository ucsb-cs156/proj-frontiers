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
        pathParams: { id: "7" },
      },
      routing: { path: "/student/courses/:id" },
    }),
  },
};

const Template = () => <StudentCourseShowPage />;

const exampleCourse = {
  ...coursesFixtures.oneCourseWithEachStatus[0],
  id: 7,
  courseName: "CMPSC 156",
  term: "Spring 2025",
  orgName: "ucsb-cs156-s25",
};

const basicHandlers = [
  http.get("/api/currentUser", () => {
    return HttpResponse.json(apiCurrentUserFixtures.userOnly);
  }),
  http.get("/api/systemInfo", () => {
    return HttpResponse.json(systemInfoFixtures.showingNeither);
  }),
];

export const Loading = Template.bind({});
Loading.args = {
  suppressMemoryRouter: true,
};
Loading.parameters = {
  msw: {
    handlers: [
      ...basicHandlers,
      http.get("/api/courses/7", () => {
        // Return a promise that never resolves to simulate loading state
        return new Promise(() => {});
      }),
    ],
  },
};

export const CourseExists = Template.bind({});
CourseExists.args = {
  suppressMemoryRouter: true,
};
CourseExists.parameters = {
  msw: {
    handlers: [
      ...basicHandlers,
      http.get("/api/courses/7", () => {
        return HttpResponse.json(exampleCourse, { status: 200 });
      }),
    ],
  },
};

export const CourseNotFound = Template.bind({});
CourseNotFound.args = {
  suppressMemoryRouter: true,
};
CourseNotFound.parameters = {
  msw: {
    handlers: [
      ...basicHandlers,
      http.get("/api/courses/7", () => {
        return new HttpResponse(null, { status: 404 });
      }),
    ],
  },
};
