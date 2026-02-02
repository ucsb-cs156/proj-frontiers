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
import { courseStaffFixtures } from "fixtures/courseStaffFixtures";
import { teamsFixtures } from "fixtures/TeamsFixtures";
import { showOrganizationAgeWarning } from "fixtures/courseWarningFixtures";

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
const courseStaff = courseStaffFixtures.threeStaff;

export const ExampleCourseNoStudents = Template.bind({});
ExampleCourseNoStudents.args = {
  suppressMemoryRouter: true,
};

const basicHandlers = [
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
  http.post("/api/repos/createRepos", ({ request }) => {
    window.alert(
      `Would have made HTTP request: ${request.method} ${request.url}`,
    );
    return HttpResponse.json([], {
      status: 200,
    });
  }),
  http.post("/api/rosterstudents/post", ({ request }) => {
    window.alert(
      `Would have made HTTP request: ${request.method} ${request.url}`,
    );
    return HttpResponse.json([], {
      status: 200,
    });
  }),
  http.put("/api/rosterstudents/update", ({ request }) => {
    window.alert(
      `Would have made HTTP request: ${request.method} ${request.url}`,
    );
    return HttpResponse.json([], {
      status: 200,
    });
  }),
  http.post("/api/coursestaff/post", ({ request }) => {
    window.alert(
      `Would have made HTTP request: ${request.method} ${request.url}`,
    );
    return HttpResponse.json([], {
      status: 200,
    });
  }),
  http.put("/api/coursestaff", ({ request }) => {
    window.alert(
      `Would have made HTTP request: ${request.method} ${request.url}`,
    );
    return HttpResponse.json([], {
      status: 200,
    });
  }),

  http.delete("/api/rosterstudents/delete", ({ request }) => {
    window.alert(
      `Would have made HTTP request: ${request.method} ${request.url}`,
    );
    return HttpResponse.json([], {
      status: 200,
    });
  }),
  http.delete("/api/coursestaff/delete", ({ request }) => {
    window.alert(
      `Would have made HTTP request: ${request.method} ${request.url}`,
    );
    return HttpResponse.json([], {
      status: 200,
    });
  }),
  http.post("/api/rosterstudents/upload/csv", async ({ request }) => {
    window.alert(
      `Would have made HTTP request: ${request.method} ${request.url}`,
    );
    return HttpResponse.json([], {
      status: 200,
    });
  }),
];

ExampleCourseNoStudents.parameters = {
  msw: {
    handlers: [
      ...basicHandlers,
      http.get("/api/rosterStudents/course/7", () => {
        return HttpResponse.json([], {
          status: 200,
        });
      }),
      http.get("/api/coursestaff/course", () => {
        return HttpResponse.json([], { status: 200 });
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
      ...basicHandlers,
      http.get("/api/rosterStudents/course/7", () => {
        return HttpResponse.json(rosterStudents, {
          status: 200,
        });
      }),
      http.get("/api/coursestaff/course", () => {
        return HttpResponse.json([], { status: 200 });
      }),
    ],
  },
};

export const ExampleCourseThreeStudentsThreeStaff = Template.bind({});
ExampleCourseThreeStudentsThreeStaff.args = {
  suppressMemoryRouter: true,
};
ExampleCourseThreeStudentsThreeStaff.parameters = {
  msw: {
    handlers: [
      ...basicHandlers,
      http.get("/api/rosterStudents/course/7", () => {
        return HttpResponse.json(rosterStudents, {
          status: 200,
        });
      }),
      http.get("/api/coursestaff/course", ({ request }) => {
        const url = new URL(request.url);
        const courseId = url.searchParams.get("courseId");
        if (courseId === "7") {
          return HttpResponse.json(courseStaff, {
            status: 200,
          });
        }
        return HttpResponse.json([], { status: 200 });
      }),
      http.get("/api/teams/all", () => {
        return HttpResponse.json(teamsFixtures.threeTeams, { status: 200 });
      }),
    ],
  },
};

export const ExampleWithOrganizationAgeWarning = Template.bind({});
ExampleWithOrganizationAgeWarning.args = {
  suppressMemoryRouter: true,
};
ExampleWithOrganizationAgeWarning.parameters = {
  msw: {
    handlers: [
      ...basicHandlers,
      http.get("/api/rosterStudents/course/7", () => {
        return HttpResponse.json(rosterStudents, {
          status: 200,
        });
      }),
      http.get("/api/coursestaff/course", ({ request }) => {
        const url = new URL(request.url);
        const courseId = url.searchParams.get("courseId");
        if (courseId === "7") {
          return HttpResponse.json(courseStaff, {
            status: 200,
          });
        }
        return HttpResponse.json([], { status: 200 });
      }),
      http.get("/api/teams/all", () => {
        return HttpResponse.json(teamsFixtures.threeTeams, { status: 200 });
      }),
      http.get("/api/courses/warnings/1", () =>
        HttpResponse.json(showOrganizationAgeWarning),
      ),
    ],
  },
};
