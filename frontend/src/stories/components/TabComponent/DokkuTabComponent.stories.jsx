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
  http.put("/api/dokku/users_list_header", async ({ request }) => {
    const body = await request.text();
    window.alert(
      "Invoked PUT with URL: " + request.url + " and body:\n" + body,
    );
    return HttpResponse.text(body, { status: 200 });
  }),
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
    http.get("/api/dokku/users_list_header", () => {
      return HttpResponse.text("", { status: 200 });
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
    http.get("/api/dokku/users_list_header", () => {
      return HttpResponse.text("eci,dokku-00\neci,dokku-01", { status: 200 });
    }),
    ...mutationHandlers,
  ],
};
