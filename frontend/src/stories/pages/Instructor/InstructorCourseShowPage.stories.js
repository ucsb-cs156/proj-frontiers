import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import { http, HttpResponse } from "msw";

import {
  withRouter,
  reactRouterParameters,
} from "storybook-addon-remix-react-router";

import InstructorCourseShowPage from "main/pages/Instructor/InstructorCourseShowPage";
import coursesFixtures from "fixtures/coursesFixtures";

export default {
  title: "pages/Instructor/InstructorCourseShowPage",
  component: InstructorCourseShowPage,
  decorators: [withRouter],
  parameters: {
    reactRouter: reactRouterParameters({
      location: {
        pathParams: { id: "7" },
      },
      routing: { path: "/instructor/courses/:id" },
    }),
  },
};

const Template = () => <InstructorCourseShowPage />;

const exampleCourse = {
  ...coursesFixtures.oneCourseWithEachStatus[0],
  id: 7,
  createdByEmail: "phtcon@ucsb.edu",
};

export const ExampleCourse = Template.bind({});
ExampleCourse.args = {
  suppressMemoryRouter: true,
};
ExampleCourse.parameters = {
  msw: {
    handlers: [
      http.get("/api/currentUser", () => {
        return HttpResponse.json(apiCurrentUserFixtures.adminUser);
      }),
      http.get("/api/currentUser", () => {
        return HttpResponse.json(apiCurrentUserFixtures.adminUser);
      }),
      http.get("/api/systemInfo", () => {
        return HttpResponse.json(systemInfoFixtures.showingNeither);
      }),
      http.get("/api/courses/7", () => {
        return HttpResponse.json(exampleCourse, {
          status: 200,
        });
      }),
    ],
  },
};
