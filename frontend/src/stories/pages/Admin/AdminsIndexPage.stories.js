import React from "react";
import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import { roleEmailFixtures } from "fixtures/roleEmailFixtures";
import { http, HttpResponse } from "msw";

import AdminsIndexPage from "main/pages/Admin/AdminsIndexPage";

export default {
  title: "pages/Admins/AdminsIndexPage",
  component: AdminsIndexPage,
};

const Template = () => <AdminsIndexPage storybook={true} />;

export const Empty = Template.bind({});

Empty.parameters = {
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
    http.get("/api/admin/all", () => {
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
    http.get("/api/admin/all", () => {
      return HttpResponse.json(
        roleEmailFixtures.threeItemsWithIsInAdminEmailField,
      );
    }),
    http.delete("/api/admin/delete", () => {
      return HttpResponse.json(
        { message: "Item deleted successfully" },
        { status: 200 },
      );
    }),
  ],
};
