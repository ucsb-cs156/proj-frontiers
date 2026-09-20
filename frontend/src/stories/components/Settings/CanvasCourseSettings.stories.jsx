import React from "react";
import { http, HttpResponse } from "msw";
import CanvasCourseSettings from "main/components/Settings/CanvasCourseSettings";

export default {
  title: "components/Settings/CanvasCourseSettings",
  component: CanvasCourseSettings,
};

const Template = (args) => {
  return <CanvasCourseSettings {...args} />;
};

export const Default = Template.bind({});

Default.args = {
  courseId: 1,
  testIdPrefix: "CanvasApiForm",
  submitAction: (data) =>
    window.alert(`Submitted Canvas credentials: ${JSON.stringify(data)}`),
};

Default.parameters = {
  msw: [
    http.get("/api/courses/getCanvasInfo", () => {
      return HttpResponse.json({
        canvasApiToken: "***************1234",
        canvasCourseId: "1234567",
      });
    }),
  ],
};
