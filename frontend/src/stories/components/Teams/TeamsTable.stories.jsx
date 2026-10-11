import React from "react";
import TeamsTable from "main/components/Teams/TeamsTable";
import { teamsFixtures } from "fixtures/TeamsFixtures";
import { currentUserFixtures } from "fixtures/currentUserFixtures";
import { courseStaffFixtures } from "fixtures/courseStaffFixtures";
import { http, HttpResponse } from "msw";

export default {
  title: "components/Teams/TeamsTable",
  component: TeamsTable,
};

const Template = (args) => {
  return <TeamsTable {...args} />;
};

export const ItemWithEachStatusAdminUser = Template.bind({});
ItemWithEachStatusAdminUser.args = {
  teams: teamsFixtures.teams,
  currentUser: currentUserFixtures.adminUser,
};

ItemWithEachStatusAdminUser.parameters = {
  msw: [
    http.delete("/api/teams/delete", ({ request }) => {
      const url = new URL(request.url);
      window.alert(
        "Invoked delete with URL: " +
          url +
          " and params: " +
          JSON.stringify(Object.fromEntries(url.searchParams)),
      );
      return HttpResponse.json(
        {},
        {
          status: 200,
        },
      );
    }),
  ],
};

export const WithStaffTeam = Template.bind({});
WithStaffTeam.args = {
  teams: [...teamsFixtures.teams, teamsFixtures.staffTeam],
  currentUser: currentUserFixtures.adminUser,
  courseId: 12,
};

WithStaffTeam.parameters = {
  msw: [
    http.get("/api/coursestaff/course", () => {
      return HttpResponse.json(courseStaffFixtures.staffWithEachStatus, {
        status: 200,
      });
    }),
    http.get("/api/rosterstudents/course/12", () => {
      return HttpResponse.json([], { status: 200 });
    }),
  ],
};
