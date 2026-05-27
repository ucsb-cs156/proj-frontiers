import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import { http, HttpResponse } from "msw";

import {
  withRouter,
  reactRouterParameters,
} from "storybook-addon-remix-react-router";

import StaffCourseShowPage from "main/pages/Staff/StaffCourseShowPage";
import coursesFixtures from "fixtures/coursesFixtures";
import { courseStaffFixtures } from "fixtures/courseStaffFixtures";
import { teamsFixtures } from "fixtures/TeamsFixtures";
import { jobsByCourseFixtures } from "fixtures/jobsByCourseFixtures";

export default {
  title: "pages/Staff/StaffCourseShowPage",
  component: StaffCourseShowPage,
  decorators: [withRouter],
  parameters: {
    reactRouter: reactRouterParameters({
      location: {
        pathParams: { id: "7" },
      },
      routing: { path: "/staff/courses/:id" },
    }),
  },
};

const Template = () => <StaffCourseShowPage />;

const exampleCourse = {
  ...coursesFixtures.oneCourseWithEachStatus[0],
  id: 7,
};

const courseStaff = courseStaffFixtures.threeStaff;
const courseJobs = jobsByCourseFixtures.threeJobs;

const basicHandlers = [
  http.get("/api/currentUser", () => {
    return HttpResponse.json(apiCurrentUserFixtures.userOnly);
  }),
  http.get("/api/systemInfo", () => {
    return HttpResponse.json(systemInfoFixtures.showingNeither);
  }),
  http.get("/api/courses/7", () => {
    return HttpResponse.json(exampleCourse, {
      status: 200,
    });
  }),
];

export const StaffView = Template.bind({});
StaffView.args = {
  suppressMemoryRouter: true,
};
StaffView.parameters = {
  msw: {
    handlers: [
      ...basicHandlers,
      http.get("/api/coursestaff/course", () => {
        return HttpResponse.json([], { status: 200 });
      }),
      http.get("/api/jobs/course", () => {
        return HttpResponse.json([], { status: 200 });
      }),
    ],
  },
};

export const StaffViewWithData = Template.bind({});
StaffViewWithData.args = {
  suppressMemoryRouter: true,
};
StaffViewWithData.parameters = {
  msw: {
    handlers: [
      ...basicHandlers,
      http.get("/api/coursestaff/course", ({ request }) => {
        const url = new URL(request.url);
        const courseId = url.searchParams.get("courseId");
        if (courseId === "7") {
          return HttpResponse.json(courseStaff, { status: 200 });
        }
        return HttpResponse.json([], { status: 200 });
      }),
      http.get("/api/teams/all", () => {
        return HttpResponse.json(teamsFixtures.threeTeams, { status: 200 });
      }),
      http.get("/api/jobs/course", ({ request }) => {
        const url = new URL(request.url);
        const courseId = url.searchParams.get("courseId");
        if (courseId === "7") {
          return HttpResponse.json(courseJobs, { status: 200 });
        }
        return HttpResponse.json([], { status: 200 });
      }),
    ],
  },
};

export const InstructorView = Template.bind({});
InstructorView.args = {
  suppressMemoryRouter: true,
};
InstructorView.parameters = {
  msw: {
    handlers: [
      http.get("/api/currentUser", () => {
        return HttpResponse.json(apiCurrentUserFixtures.instructorUser);
      }),
      http.get("/api/systemInfo", () => {
        return HttpResponse.json(systemInfoFixtures.showingNeither);
      }),
      http.get("/api/courses/7", () => {
        return HttpResponse.json(exampleCourse, { status: 200 });
      }),
      http.get("/api/coursestaff/course", ({ request }) => {
        const url = new URL(request.url);
        const courseId = url.searchParams.get("courseId");
        if (courseId === "7") {
          return HttpResponse.json(courseStaff, { status: 200 });
        }
        return HttpResponse.json([], { status: 200 });
      }),
      http.get("/api/jobs/course", () => {
        return HttpResponse.json([], { status: 200 });
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
      http.get("/api/currentUser", () => {
        return HttpResponse.json(apiCurrentUserFixtures.userOnly);
      }),
      http.get("/api/systemInfo", () => {
        return HttpResponse.json(systemInfoFixtures.showingNeither);
      }),
      http.get("/api/courses/7", async () => {
        await new Promise(() => {});
      }),
    ],
  },
};
