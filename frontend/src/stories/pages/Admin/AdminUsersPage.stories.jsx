import React from "react";
import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import { http, HttpResponse } from "msw";

import AdminUsersPage from "main/pages/Admin/AdminUsersPage";
import usersFixtures from "fixtures/usersFixtures";

export default {
  title: "pages/Admins/AdminUsersPage",
  component: AdminUsersPage,
};

const Template = () => <AdminUsersPage storybook={true} />;

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
    http.get("/api/admin/users", () => {
      return HttpResponse.json(
        { content: [], page: { totalPages: 1 } },
        { status: 200 },
      );
    }),
  ],
};

export const TwoItemsAdminUser = Template.bind({});

TwoItemsAdminUser.parameters = {
  msw: [
    http.get("/api/currentUser", () => {
      return HttpResponse.json(apiCurrentUserFixtures.adminUser);
    }),
    http.get("/api/systemInfo", () => {
      return HttpResponse.json(systemInfoFixtures.showingNeither);
    }),
    http.get("/api/admin/users", ({ request }) => {
      const url = new URL(request.url);
      if (url.searchParams.get("page") === "0") {
        return HttpResponse.json({
          content: [usersFixtures.threeUsers[0]],
          page: { totalPages: 2 },
        });
      } else {
        return HttpResponse.json({
          content: [usersFixtures.threeUsers[1]],
          page: { totalPages: 2 },
        });
      }
    }),
  ],
};
