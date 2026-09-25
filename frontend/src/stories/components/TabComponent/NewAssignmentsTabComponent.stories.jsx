import React from "react";
import NewAssignmentsTabComponent from "main/components/TabComponent/NewAssignmentsTabComponent";
import { newAssignmentsFixtures } from "fixtures/newAssignmentsFixtures";
import { http, HttpResponse } from "msw";

export default {
  title: "components/TabComponent/NewAssignmentsTabComponent",
  component: NewAssignmentsTabComponent,
};

const Template = (args) => {
  return <NewAssignmentsTabComponent {...args} />;
};

const mutationHandlers = [
  http.post("/api/assignments/post", ({ request }) => {
    const url = new URL(request.url);
    window.alert(
      "Invoked POST with URL: " +
        url +
        " and params: " +
        JSON.stringify(Object.fromEntries(url.searchParams)),
    );
    return HttpResponse.json(newAssignmentsFixtures.savedWithJob, {
      status: 200,
    });
  }),
  http.put("/api/assignments/put", ({ request }) => {
    const url = new URL(request.url);
    window.alert(
      "Invoked PUT with URL: " +
        url +
        " and params: " +
        JSON.stringify(Object.fromEntries(url.searchParams)),
    );
    return HttpResponse.json(newAssignmentsFixtures.savedWithJob, {
      status: 200,
    });
  }),
  http.delete("/api/assignments/:id", ({ request }) => {
    window.alert("Invoked DELETE with URL: " + request.url);
    return HttpResponse.json({}, { status: 200 });
  }),
];

export const Empty = Template.bind({});
Empty.args = {
  courseId: 7,
  testIdPrefix: "NewAssignmentsTabComponent",
};
Empty.parameters = {
  msw: [
    http.get("/api/assignments", () => {
      return HttpResponse.json([], { status: 200 });
    }),
    ...mutationHandlers,
  ],
};

export const ThreeAssignments = Template.bind({});
ThreeAssignments.args = {
  courseId: 7,
  testIdPrefix: "NewAssignmentsTabComponent",
};
ThreeAssignments.parameters = {
  msw: [
    http.get("/api/assignments", () => {
      return HttpResponse.json(newAssignmentsFixtures.threeAssignments, {
        status: 200,
      });
    }),
    ...mutationHandlers,
  ],
};
