import React from "react";
import SectionsTable from "main/components/Sections/SectionsTable";
import { sectionsFixtures } from "fixtures/sectionsFixtures";
import { http, HttpResponse } from "msw";

export default {
  title: "components/Sections/SectionsTable",
  component: SectionsTable,
};

const Template = (args) => {
  return <SectionsTable {...args} />;
};

const alertingHandlers = [
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
  sections: [],
  courseId: 7,
};

export const ThreeSections = Template.bind({});
ThreeSections.args = {
  sections: sectionsFixtures.threeSections,
  courseId: 7,
};
ThreeSections.parameters = {
  msw: alertingHandlers,
};
