import React from "react";
import ApiKeysTabComponent from "main/components/TabComponent/ApiKeysTabComponent";
import { apiKeysFixtures, apiKeysFixturesNow } from "fixtures/apiKeysFixtures";
import { http, HttpResponse } from "msw";
import MockDate from "mockdate";

export default {
  title: "components/TabComponent/ApiKeysTabComponent",
  component: ApiKeysTabComponent,
  // The table of keys warns about keys that expire soon, counting from the
  // current date. Freeze the date so that the stories look the same every day.
  beforeEach: () => {
    MockDate.set(apiKeysFixturesNow);
    return () => MockDate.reset();
  },
};

const Template = (args) => {
  return <ApiKeysTabComponent {...args} />;
};

const mutationHandlers = [
  http.post("/api/courses/key", ({ request }) => {
    const url = new URL(request.url);
    window.alert(
      "Invoked POST with URL: " +
        url +
        " and params: " +
        JSON.stringify(Object.fromEntries(url.searchParams)),
    );
    return HttpResponse.json(
      {
        ...apiKeysFixtures.issuedKey,
        label: url.searchParams.get("label"),
      },
      { status: 200 },
    );
  }),
  http.delete("/api/courses/key", ({ request }) => {
    window.alert("Invoked DELETE with URL: " + request.url);
    return HttpResponse.json({ message: "API key revoked" }, { status: 200 });
  }),
];

export const Empty = Template.bind({});
Empty.args = {
  courseId: 7,
  testIdPrefix: "ApiKeysTabComponent",
};
Empty.parameters = {
  msw: [
    http.get("/api/courses/key", () => {
      return HttpResponse.json([], { status: 200 });
    }),
    ...mutationHandlers,
  ],
};

export const SeveralKeys = Template.bind({});
SeveralKeys.args = {
  courseId: 7,
  testIdPrefix: "ApiKeysTabComponent",
};
SeveralKeys.parameters = {
  msw: [
    http.get("/api/courses/key", () => {
      return HttpResponse.json(apiKeysFixtures.severalKeys, { status: 200 });
    }),
    ...mutationHandlers,
  ],
};
