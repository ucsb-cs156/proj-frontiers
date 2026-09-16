import React from "react";
import SectionsTabComponent from "main/components/TabComponent/SectionsTabComponent";
import { sectionsFixtures } from "fixtures/sectionsFixtures";
import { http, HttpResponse } from "msw";

export default {
  title: "components/TabComponent/SectionsTabComponent",
  component: SectionsTabComponent,
};

const Template = (args) => {
  return <SectionsTabComponent {...args} />;
};

const mutationHandlers = [
  http.post("/api/courses/7/sections", ({ request }) => {
    const url = new URL(request.url);
    window.alert(
      "Invoked POST with URL: " +
        url +
        " and params: " +
        JSON.stringify(Object.fromEntries(url.searchParams)),
    );
    return HttpResponse.json({}, { status: 200 });
  }),
  http.put("/api/courses/7/sections/:id", ({ request }) => {
    const url = new URL(request.url);
    window.alert(
      "Invoked PUT with URL: " +
        url +
        " and params: " +
        JSON.stringify(Object.fromEntries(url.searchParams)),
    );
    return HttpResponse.json({}, { status: 200 });
  }),
  http.delete("/api/courses/7/sections/:id", ({ request }) => {
    window.alert("Invoked DELETE with URL: " + request.url);
    return HttpResponse.json({}, { status: 200 });
  }),
];

export const Empty = Template.bind({});
Empty.args = {
  courseId: 7,
  testIdPrefix: "SectionsTabComponent",
};
Empty.parameters = {
  msw: [
    http.get("/api/courses/7/sections", () => {
      return HttpResponse.json([], { status: 200 });
    }),
    ...mutationHandlers,
  ],
};

export const ThreeSections = Template.bind({});
ThreeSections.args = {
  courseId: 7,
  testIdPrefix: "SectionsTabComponent",
};
ThreeSections.parameters = {
  msw: [
    http.get("/api/courses/7/sections", () => {
      return HttpResponse.json(sectionsFixtures.threeSections, {
        status: 200,
      });
    }),
    ...mutationHandlers,
  ],
};
