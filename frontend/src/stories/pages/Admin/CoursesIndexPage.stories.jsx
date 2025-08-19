import React from "react";
import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import coursesFixtures from "fixtures/coursesFixtures";
import { http, HttpResponse } from "msw";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

import CoursesIndexPage from "main/pages/Admin/CoursesIndexPage";

import { roleEmailFixtures } from "fixtures/roleEmailFixtures";

export default {
  title: "pages/Admin/CoursesIndexPage",
  component: CoursesIndexPage,
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
    <CoursesIndexPage />
  </QueryWrapper>
);

export const AdminViewWithCourses = Template.bind({});
AdminViewWithCourses.parameters = {
  msw: [
    http.get("/api/currentUser", () => {
      return HttpResponse.json(apiCurrentUserFixtures.adminUser, {
        status: 200,
      });
    }),
    http.get("/api/systemInfo", () => {
      return HttpResponse.json(systemInfoFixtures.showingNeither, {
        status: 200,
      });
    }),
    http.get("/api/courses/allForAdmins", () => {
      return HttpResponse.json(coursesFixtures.oneStaffMemberWithEachStatus);
    }),
    http.post("/api/admin/courses/post", () => {
      return HttpResponse.json(roleEmailFixtures.oneItem, {
        status: 200,
      });
    }),
  ],
};

export const AdminViewWithNoCourses = Template.bind({});
AdminViewWithNoCourses.parameters = {
  msw: [
    http.get("/api/currentUser", () => {
      return HttpResponse.json(apiCurrentUserFixtures.adminUser, {
        status: 200,
      });
    }),
    http.get("/api/systemInfo", () => {
      return HttpResponse.json(systemInfoFixtures.showingNeither, {
        status: 200,
      });
    }),
    http.get("/api/courses/allForAdmins", () => {
      return HttpResponse.json([]);
    }),
    http.post("/api/admin/courses/post", () => {
      return HttpResponse.json(roleEmailFixtures.oneItem, {
        status: 200,
      });
    }),
  ],
};
