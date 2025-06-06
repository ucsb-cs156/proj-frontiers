import React from "react";
import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import { http, HttpResponse } from "msw";

import AdminsCreatePage from "main/pages/Admins/AdminsCreatePage";

import { roleEmailFixtures } from "fixtures/roleEmailFixtures";

export default {
  title: "pages/Admins/AdminsCreatePage",
  component: AdminsCreatePage,
};

const Template = () => <AdminsCreatePage storybook={true} />;

export const Default = Template.bind({});
Default.parameters = {
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
    http.post("/api/admin/admins/post", () => {
      return HttpResponse.json(roleEmailFixtures.oneItem, {
        status: 200,
      });
    }),
  ],
};
