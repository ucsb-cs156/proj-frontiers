// src/stories/InstructorCourseShowPage.stories.jsx

import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import { http, HttpResponse } from "msw";

import InstructorCourseShowPage from "main/pages/Instructor/InstructorCourseShowPage";
import coursesFixtures from "fixtures/coursesFixtures";
import { rosterStudentFixtures } from "fixtures/rosterStudentFixtures";

export default {
  title: "pages/Instructor/InstructorCourseShowPage",
  component: InstructorCourseShowPage,
  
  // This is where we will pass the routing parameters to our custom decorator
  parameters: {
    // The key 'routing' is arbitrary, but we must use the same one in preview.js
    routing: { 
      path: "/instructor/courses/:id",
      initialEntries: "/instructor/courses/7",
    },
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
// suppressMemoryRouter is still used to control our custom decorator
ExampleCourseNoStudents.args = {
  suppressMemoryRouter: false, // Make sure this is false so the router is applied
};
ExampleCourseNoStudents.parameters = {
  // We can merge parameters here if we need to, but for this case, it's not needed.
  // The routing parameters are already in the default export.
  msw: {
    handlers: [
      // ... (your existing msw handlers)
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
  suppressMemoryRouter: false, // Make sure this is false
};
ExampleCourseThreeStudents.parameters = {
  // Same here, the routing parameters are in the default export.
  msw: {
    handlers: [
      // ... (your existing msw handlers)
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