import React from "react";
import TeamsTable from "main/components/Teams/TeamsTable";
import { teamsFixtures } from "fixtures/TeamsFixtures";
import { currentUserFixtures } from "fixtures/currentUserFixtures";
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
