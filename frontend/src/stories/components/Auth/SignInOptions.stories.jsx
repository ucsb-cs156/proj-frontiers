import React from "react";
import SignInOptions from "main/components/Auth/SignInOptions";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import { http, HttpResponse } from "msw";

export default {
  title: "components/Auth/SignInOptions",
  component: SignInOptions,
};

const Template = (args) => <SignInOptions {...args} />;

export const Default = Template.bind({});
Default.parameters = {
  msw: {
    handlers: [
      http.get("/api/systemInfo", () => {
        return HttpResponse.json(systemInfoFixtures.showingNeither, {
          status: 200,
        });
      }),
    ],
  },
};

export const WithGoogle = Template.bind({});
WithGoogle.parameters = {
  msw: {
    handlers: [
      http.get("/api/systemInfo", () => {
        return HttpResponse.json(systemInfoFixtures.showingBoth, {
          status: 200,
        });
      }),
    ],
  },
};

export const WithMicrosoft = Template.bind({});
WithMicrosoft.parameters = {
  msw: {
    handlers: [
      http.get("/api/systemInfo", () => {
        return HttpResponse.json(systemInfoFixtures.withActiveDirectory, {
          status: 200,
        });
      }),
    ],
  },
};

export const WithBoth = Template.bind({});
WithBoth.parameters = {
  msw: {
    handlers: [
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
