import React from "react";
import NewAssignmentsTable from "main/components/NewAssignments/NewAssignmentsTable";
import { newAssignmentsFixtures } from "fixtures/newAssignmentsFixtures";
import { http, HttpResponse } from "msw";

export default {
  title: "components/NewAssignments/NewAssignmentsTable",
  component: NewAssignmentsTable,
};

const Template = (args) => {
  return <NewAssignmentsTable {...args} />;
};

const alertingHandlers = [
  http.get("/api/jobs/course/logs/tail", () => {
    return HttpResponse.json(
      {
        status: "complete",
        lines: [
          { id: 1, jobId: 12, message: "Creating repository lab01-cgaucho" },
          { id: 2, jobId: 12, message: "Done" },
        ],
      },
      { status: 200 },
    );
  }),
  http.post("/api/assignments/launch", ({ request }) => {
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
  assignments: [],
  courseId: 7,
};

export const ThreeAssignments = Template.bind({});
ThreeAssignments.args = {
  assignments: newAssignmentsFixtures.threeAssignments,
  courseId: 7,
};
ThreeAssignments.parameters = {
  msw: alertingHandlers,
};
