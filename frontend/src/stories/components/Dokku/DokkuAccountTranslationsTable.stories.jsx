import React from "react";
import DokkuAccountTranslationsTable from "main/components/Dokku/DokkuAccountTranslationsTable";
import { dokkuAccountTranslationsFixtures } from "fixtures/dokkuAccountTranslationsFixtures";
import { http, HttpResponse } from "msw";

export default {
  title: "components/Dokku/DokkuAccountTranslationsTable",
  component: DokkuAccountTranslationsTable,
};

const Template = (args) => {
  return <DokkuAccountTranslationsTable {...args} />;
};

const alertingHandlers = [
  http.put("/api/dokku/translations", ({ request }) => {
    const url = new URL(request.url);
    window.alert(
      "Invoked PUT with URL: " +
        url +
        " and params: " +
        JSON.stringify(Object.fromEntries(url.searchParams)),
    );
    return HttpResponse.json({}, { status: 200 });
  }),
  http.delete("/api/dokku/translations", ({ request }) => {
    window.alert("Invoked DELETE with URL: " + request.url);
    return HttpResponse.json({}, { status: 200 });
  }),
];

export const Empty = Template.bind({});
Empty.args = {
  translations: [],
  courseId: 7,
};

export const ThreeTranslations = Template.bind({});
ThreeTranslations.args = {
  translations: dokkuAccountTranslationsFixtures.threeTranslations,
  courseId: 7,
};
ThreeTranslations.parameters = {
  msw: alertingHandlers,
};
