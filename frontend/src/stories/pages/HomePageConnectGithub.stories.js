import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import { http, HttpResponse } from "msw";

import HomePageConnectGithub from "main/pages/HomePageConnectGithub";

export default {
  title: "pages/HomePageConnectGithub",
  component: HomePageConnectGithub,
};

const Template = () => <HomePageConnectGithub />;

export const NoGitHubRegularUser = Template.bind({});
NoGitHubRegularUser.parameters = {
  msw: {
    handlers: [
      http.get("/api/currentUser", () => {
        return HttpResponse.json(apiCurrentUserFixtures.userOnly);
      }),
      http.get("/api/systemInfo", () => {
        return HttpResponse.json(systemInfoFixtures.showingNeither);
      }),
    ],
  },
};
