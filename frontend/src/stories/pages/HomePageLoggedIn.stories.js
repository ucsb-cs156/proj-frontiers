import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import { http, HttpResponse } from "msw";

import HomePageLoggedIn from "main/pages/HomePageLoggedIn";
import coursesFixtures from "fixtures/coursesFixtures";

export default {
  title: "pages/HomePageLoggedIn",
  component: HomePageLoggedIn,
};

const Template = () => <HomePageLoggedIn />;
export const LoggedInRegularUser = Template.bind({});
LoggedInRegularUser.parameters = {
  msw: {
    handlers: [
      http.get("/api/currentUser", () => {
        return HttpResponse.json(apiCurrentUserFixtures.userOnly);
      }),
      http.get("/api/systemInfo", () => {
        return HttpResponse.json(systemInfoFixtures.showingNeither);
      }),
      http.get("/api/courses/list", () => {
        return HttpResponse.json(coursesFixtures.oneCourseWithEachStatus);
      }),
      http.get("/api/courses/staffCourses", () => {
        return HttpResponse.json(coursesFixtures.oneCourseWithEachStatus);
      }),
    ],
  },
};

export const LoggedInAdminUserShowingSwaggerAndH2Console = Template.bind({});
LoggedInAdminUserShowingSwaggerAndH2Console.parameters = {
  msw: {
    handlers: [
      http.get("/api/currentUser", () => {
        return HttpResponse.json(apiCurrentUserFixtures.adminUser);
      }),
      http.get("/api/systemInfo", () => {
        return HttpResponse.json(systemInfoFixtures.showingBoth);
      }),
      http.get("/api/courses/list", () => {
        return HttpResponse.json(coursesFixtures.oneCourseWithEachStatus);
      }),
      http.get("/api/courses/staffCourses", () => {
        return HttpResponse.json(coursesFixtures.oneCourseWithEachStatus);
      }),
    ],
  },
};
