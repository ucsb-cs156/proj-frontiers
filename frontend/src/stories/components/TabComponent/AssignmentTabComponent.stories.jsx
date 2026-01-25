import React from "react";
import AssignmentTabComponent from "main/components/TabComponent/AssignmentTabComponent";
import { http, HttpResponse } from "msw";

export default {
  title: "components/TabComponent/AssignmentTabComponent",
  component: AssignmentTabComponent,
};

const Template = (args) => {
  return <AssignmentTabComponent {...args} />;
};

export const Default = Template.bind({});

Default.args = {
  courseId: 1,
};

Default.parameters = {
  msw: {
    handlers: [
      http.post("/api/repos/createRepos", ({ request }) => {
        const url = new URL(request.url);
        window.alert(
          "Invoked with URL: " +
            url +
            " and params: " +
            JSON.stringify(Object.fromEntries(url.searchParams)),
        );

        return HttpResponse.json(
          {
            message: "started individual repo creation",
          },
          {
            status: 200,
          },
        );
      }),

      http.post("/api/repos/createTeamRepos", ({ request }) => {
        const url = new URL(request.url);
        window.alert(
          "Invoked with URL: " +
            url +
            " and params: " +
            JSON.stringify(Object.fromEntries(url.searchParams)),
        );

        return HttpResponse.json(
          {
            message: "started team repo creation",
          },
          {
            status: 200,
          },
        );
      }),
    ],
  },
};
