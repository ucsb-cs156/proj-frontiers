import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import { http, HttpResponse } from "msw";

import {
  withRouter,
  reactRouterParameters,
} from "storybook-addon-remix-react-router";

import InstructorCourseShowPage from "main/pages/Instructor/InstructorCourseShowPage";
import coursesFixtures from "fixtures/coursesFixtures";
import { rosterStudentFixtures } from "fixtures/rosterStudentFixtures";

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

const rosterStudents = rosterStudentFixtures.threeStudents;

export const ExampleCourseNoStudents = Template.bind({});
ExampleCourseNoStudents.args = {
  suppressMemoryRouter: true,
};
ExampleCourseNoStudents.parameters = {
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
      http.get("/api/rosterStudents/course/7", () => {
        return HttpResponse.json([], {
          status: 200,
        });
      }),
    ],
  },
};

export const ExampleCourseThreeStudents = Template.bind({});
ExampleCourseThreeStudents.args = {
  suppressMemoryRouter: true,
};
ExampleCourseThreeStudents.parameters = {
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
      http.get("/api/rosterstudents/course/7", () => {
        return HttpResponse.json(rosterStudents, {
          status: 200,
        });
      }),
      http.delete("/api/rosterstudents/delete", ({ request }) => {
        const url = new URL(request.url);
        window.alert(
          "Invoked delete with URL: " +
            url +
            " and params: " +
            JSON.stringify(Object.fromEntries(url.searchParams)),
        );
        return HttpResponse.json(
          {},
          {
            status: 200,
          },
        );
      }),
    ],
  },
};
