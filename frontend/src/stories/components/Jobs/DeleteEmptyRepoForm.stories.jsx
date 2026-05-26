import React from "react";
import { http, HttpResponse } from "msw";
import DeleteEmptyRepoForm from "main/components/Jobs/DeleteEmptyRepoForm";

export default {
  title: "components/Jobs/DeleteEmptyRepoForm",
  component: DeleteEmptyRepoForm,
  parameters: {
    msw: {
      handlers: [
        http.delete("/api/repos", ({ request }) => {
          window.alert(
            `Would have made HTTP request: ${request.method} ${request.url}`,
          );
          return HttpResponse.json(
            { message: "Job Launched" },
            { status: 200 },
          );
        }),
      ],
    },
  },
};

const Template = (args) => <DeleteEmptyRepoForm {...args} />;

export const Default = Template.bind({});
Default.args = {
  courseId: 17,
};