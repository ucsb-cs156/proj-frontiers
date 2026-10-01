import React from "react";
import { http, HttpResponse } from "msw";
import ApiKeysTable from "main/components/ApiKeys/ApiKeysTable";
import { apiKeysFixtures } from "fixtures/apiKeysFixtures";

export default {
  title: "components/ApiKeys/ApiKeysTable",
  component: ApiKeysTable,
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
