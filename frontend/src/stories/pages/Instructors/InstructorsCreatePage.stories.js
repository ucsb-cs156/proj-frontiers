import React from "react";
import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import { http, HttpResponse } from "msw";

import InstructorsCreatePage from "main/pages/Instructors/InstructorsCreatePage";

import { roleEmailFixtures } from "fixtures/roleEmailFixtures";

export default {
  title: "pages/Instructors/InstructorsCreatePage",
  component: InstructorsCreatePage,
};

const Template = () => <InstructorsCreatePage storybook={true} />;

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
    http.post("/api/admin/instructors/post", () => {
      return HttpResponse.json(roleEmailFixtures.oneItem, {
        status: 200,
      });
    }),
  ],
};
