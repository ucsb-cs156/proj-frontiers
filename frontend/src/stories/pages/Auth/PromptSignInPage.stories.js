import React from "react";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import { http, HttpResponse } from "msw";
import PromptSignInPage from "main/pages/Auth/PromptSignInPage";

export default {
  title: "pages/Auth/PromptSignInPage",
  component: PromptSignInPage,
};

const Template = () => <PromptSignInPage />;

export const Default = Template.bind({});
Default.parameters = {
  msw: {
    handlers: [
      http.get("/api/currentUser", () => {
        return HttpResponse.json(
          {},
          {
            status: 403,
          },
        );
      }),
      http.get("/api/systemInfo", () => {
        return HttpResponse.json(systemInfoFixtures.showingNeither, {
          status: 200,
        });
      }),
    ],
  },
};

export const WithGoogleLogin = Template.bind({});
WithGoogleLogin.parameters = {
  msw: {
    handlers: [
      http.get("/api/currentUser", () => {
        return HttpResponse.json(
          {},
          {
            status: 403,
          },
        );
      }),
      http.get("/api/systemInfo", () => {
        return HttpResponse.json(systemInfoFixtures.showingBoth, {
          status: 200,
        });
      }),
    ],
  },
};

export const WithActiveDirectory = Template.bind({});
WithActiveDirectory.parameters = {
  msw: {
    handlers: [
      http.get("/api/currentUser", () => {
        return HttpResponse.json(
          {},
          {
            status: 403,
          },
        );
      }),
      http.get("/api/systemInfo", () => {
        return HttpResponse.json(systemInfoFixtures.withActiveDirectory, {
          status: 200,
        });
      }),
    ],
  },
};

export const WithBothActiveDirectoryAndGoogle = Template.bind({});
WithBothActiveDirectoryAndGoogle.parameters = {
  msw: {
    handlers: [
      http.get("/api/currentUser", () => {
        return HttpResponse.json(
          {},
          {
            status: 403,
          },
        );
      }),
      http.get("/api/systemInfo", () => {
        return HttpResponse.json(
          systemInfoFixtures.withActiveDirectoryAndGoogle,
          {
            status: 200,
          },
        );
      }),
    ],
  },
};
