import React from "react";
import DokkuTabComponent from "main/components/TabComponent/DokkuTabComponent";
import { dokkuAccountTranslationsFixtures } from "fixtures/dokkuAccountTranslationsFixtures";
import { http, HttpResponse } from "msw";

export default {
  title: "components/TabComponent/DokkuTabComponent",
  component: DokkuTabComponent,
};

const Template = (args) => {
  return <DokkuTabComponent {...args} />;
};

const mutationHandlers = [
  http.post("/api/dokku/translations", ({ request }) => {
    const url = new URL(request.url);
    window.alert(
      "Invoked POST with URL: " +
        url +
        " and params: " +
        JSON.stringify(Object.fromEntries(url.searchParams)),
    );
    return HttpResponse.json({}, { status: 200 });
  }),
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
  courseId: 7,
  testIdPrefix: "DokkuTabComponent",
};
Empty.parameters = {
  msw: [
    http.get("/api/dokku/translations", () => {
      return HttpResponse.json([], { status: 200 });
    }),
    ...mutationHandlers,
  ],
};

export const ThreeTranslations = Template.bind({});
ThreeTranslations.args = {
  courseId: 7,
  testIdPrefix: "DokkuTabComponent",
};
ThreeTranslations.parameters = {
  msw: [
    http.get("/api/dokku/translations", () => {
      return HttpResponse.json(
        dokkuAccountTranslationsFixtures.threeTranslations,
        { status: 200 },
      );
    }),
    ...mutationHandlers,
  ],
};
