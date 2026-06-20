import React from "react";
import { http, HttpResponse } from "msw";
import CourseOptionsForm from "main/components/Settings/CourseOptionsForm";

const optionsState = {
  ENABLE_CANVAS: false,
  TRANSLATE_SECTIONS: true,
};

export default {
  title: "components/Settings/CourseOptionsForm",
  component: CourseOptionsForm,
};

const Template = (args) => {
  return <CourseOptionsForm {...args} />;
};

export const CanEdit = Template.bind({});

CanEdit.args = {
  courseId: 1,
  canEdit: true,
};

CanEdit.parameters = {
  msw: [
    // Make sure that the GET request to fetch course options for courseId=1 returns the expected options
    http.get("/api/course/options?courseId=1", () => {
      return HttpResponse.json(optionsState);
    }),
    // Make sure that the POST request to update course options for courseId=1, option=ENABLE_CANVAS, enabled=true returns the expected response
    http.post("/api/course/options", ({ request }) => {
      const url = new URL(request.url);
      window.alert(`Invoked post with URL: ${url}`);
      const enabled = url.searchParams.get("enabled") === "true";
      const option = url.searchParams.get("option");
      optionsState[option] = url.searchParams.get("enabled") === "true";
      return HttpResponse.json(
        {
          [option]: enabled,
        },
        {
          status: 200,
        },
      );
    }),
  ],
};

export const CannotEdit = Template.bind({});

CannotEdit.args = {
  courseId: 1,
  canEdit: false,
};

CannotEdit.parameters = {
  msw: [
    http.get("/api/course/options/", () => {
      return HttpResponse.json(optionsState);
    }),
  ],
};
