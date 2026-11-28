import React from "react";
import { http, HttpResponse } from "msw";
import MockDate from "mockdate";
import ApiKeysTable from "main/components/ApiKeys/ApiKeysTable";
import { apiKeysFixtures, apiKeysFixturesNow } from "fixtures/apiKeysFixtures";

export default {
  title: "components/ApiKeys/ApiKeysTable",
  component: ApiKeysTable,
  // The table warns about keys that expire soon, counting from the current
  // date. Freeze the date so that the stories look the same every day.
  beforeEach: () => {
    MockDate.set(apiKeysFixturesNow);
    return () => MockDate.reset();
  },
  parameters: {
    msw: [
      http.delete("/api/courses/key", ({ request }) => {
        window.alert("Invoked DELETE with URL: " + request.url);
        return HttpResponse.json(
          { message: "API key revoked" },
          { status: 200 },
        );
      }),
    ],
  },
};

const Template = (args) => <ApiKeysTable {...args} />;

export const Empty = Template.bind({});
Empty.args = { apiKeys: [], courseId: 7 };

export const OneKey = Template.bind({});
OneKey.args = { apiKeys: apiKeysFixtures.oneKey, courseId: 7 };

export const SeveralKeys = Template.bind({});
SeveralKeys.args = { apiKeys: apiKeysFixtures.severalKeys, courseId: 7 };
