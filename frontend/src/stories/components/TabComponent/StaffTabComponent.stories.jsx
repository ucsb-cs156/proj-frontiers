import React from "react";
import StaffTabComponent from "main/components/TabComponent/StaffTabComponent";
import { http, HttpResponse } from "msw";
import { courseStaffFixtures } from "fixtures/courseStaffFixtures";
import { currentUserFixtures } from "fixtures/currentUserFixtures";

export default {
  title: "components/TabComponent/StaffTabComponent",
  component: StaffTabComponent,
};

const Template = (args) => {
  return <StaffTabComponent {...args} />;
};

export const Default = Template.bind({});

Default.args = {
  courseId: 1,
  testIdPrefix: "StaffTabComponent",
  currentUser: currentUserFixtures.instructorUser,
};

Default.parameters = {
  msw: {
    handlers: [
      http.get("/api/coursestaff/course", () => {
        return HttpResponse.json(courseStaffFixtures.threeStaff);
      }),
      http.post("/api/coursestaff/upload/csv", ({ request }) => {
        const url = new URL(request.url);
        window.alert(
          "CSV upload invoked with courseId: " +
            url.searchParams.get("courseId"),
        );
        return HttpResponse.json({ inserted: 1 }, { status: 200 });
      }),
      http.post("/api/coursestaff/post", ({ request }) => {
        const url = new URL(request.url);
        window.alert(
          "Post invoked with params: " +
            JSON.stringify(Object.fromEntries(url.searchParams)),
        );
        return HttpResponse.json({}, { status: 200 });
      }),
    ],
  },
};