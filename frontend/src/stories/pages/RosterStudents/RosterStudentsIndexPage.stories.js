import React from "react";
import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import { rosterStudentsFixtures } from "fixtures/rosterStudentsFixtures";
import { http, HttpResponse } from "msw";

import RosterStudentsIndexPage from "main/pages/RosterStudents/RosterStudentsIndexPage";

export default {
  title: "pages/RosterStudents/RosterStudentsIndexPage",
  component: RosterStudentsIndexPage,
};

const Template = () => <RosterStudentsIndexPage storybook={true} />;

export const Empty = Template.bind({});
Empty.parameters = {
  msw: [
    http.get("/api/currentUser", () => {
      return HttpResponse.json(apiCurrentUserFixtures.userOnly, {
        status: 200,
      });
    }),
    http.get("/api/systemInfo", () => {
      return HttpResponse.json(systemInfoFixtures.showingNeither, {
        status: 200,
      });
    }),
    http.get("/api/rosterstudents/course", () => {
      return HttpResponse.json([], { status: 200 });
    }),
  ],
};

export const ThreeItemsAdminUser = Template.bind({});

ThreeItemsAdminUser.parameters = {
  msw: [
    http.get("/api/currentUser", () => {
      return HttpResponse.json(apiCurrentUserFixtures.adminUser);
    }),
    http.get("/api/systemInfo", () => {
      return HttpResponse.json(systemInfoFixtures.showingNeither);
    }),
    http.get("/api/rosterstudents/course", () => {
      return HttpResponse.json(rosterStudentsFixtures.threeRosterStudents, {
        status: 200,
      });
    }),
    http.delete("/api/rosterstudents", () => {
      return HttpResponse.json(
        { message: "RosterStudent with id 1 deleted" },
        { status: 200 },
      );
    }),
  ],
};
