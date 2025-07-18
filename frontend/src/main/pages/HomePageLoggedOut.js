import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import { http, HttpResponse } from "msw";

import HomePageLoggedOut from "main/pages/HomePageLoggedOut";

export default {
  title: "pages/HomePageLoggedOut",
  component: HomePageLoggedOut,
};

const Template = () => <HomePageLoggedOut />;

export const LoggedOut = Template.bind({});
LoggedOut.parameters = {
  msw: {
    handlers: [
      http.get("/api/currentUser", () => {
        return HttpResponse.json(null, { status: 403 });
      }),
      http.get("/api/systemInfo", () => {
        return HttpResponse.json(systemInfoFixtures.showingNeither);
      }),
    ],
  },
};