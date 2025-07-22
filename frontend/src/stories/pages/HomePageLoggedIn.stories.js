import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import { http, HttpResponse } from "msw";
import { QueryClient, QueryClientProvider } from "react-query";

// import { useCurrentUser } from "main/utils/currentUser";

import HomePageLoggedIn from "main/pages/HomePageLoggedIn";
import coursesFixtures from "fixtures/coursesFixtures";

export default {
  title: "pages/HomePageLoggedIn",
  component: HomePageLoggedIn,
};

// Create a wrapper that provides React Query context
const QueryWrapper = ({ children }) => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false, // Don't retry failed queries in Storybook
        cacheTime: 0, // Don't cache in Storybook
      },
    },
  });

  return (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
};

const Template = () => (
  <QueryWrapper>
    <HomePageLoggedIn />
  </QueryWrapper>
);

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
        return HttpResponse.json(
          coursesFixtures.oneRosterStudentWithEachStatus,
        );
      }),
      http.get("/api/courses/staffCourses", () => {
        return HttpResponse.json(coursesFixtures.oneStaffMemberWithEachStatus);
      }),
      http.get("/api/coursestaff/joinCourse", () => {
        return HttpResponse.json("Joining course successful", {
          status: 202,
        });
      }),
      http.get("/api/rosterstudents/joinCourse", () => {
        return HttpResponse.json("Joining course successful", {
          status: 202,
        });
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
        return HttpResponse.json(
          coursesFixtures.oneRosterStudentWithEachStatus,
        );
      }),
      http.get("/api/courses/staffCourses", () => {
        return HttpResponse.json(coursesFixtures.oneStaffMemberWithEachStatus);
      }),
      http.get("/api/coursestaff/joinCourse", () => {
        return HttpResponse.json("Joining course successful", {
          status: 202,
        });
      }),
      http.get("/api/rosterstudents/joinCourse", () => {
        return HttpResponse.json("Joining course successful", {
          status: 202,
        });
      }),
    ],
  },
};
