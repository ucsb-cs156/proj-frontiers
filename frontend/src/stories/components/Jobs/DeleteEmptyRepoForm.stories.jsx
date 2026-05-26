import React from "react";
import DeleteEmptyRepoForm from "main/components/Jobs/DeleteEmptyRepoForm";

export default {
  title: "components/Jobs/DeleteEmptyRepoForm",
  component: DeleteEmptyRepoForm,
  parameters: {
    // If your team uses MSW to mock API calls in Storybook, you can mock the DELETE route here
    // so that clicking the button in Storybook simulates a successful job launch!
    mockData: [
      {
        url: "/api/repos",
        method: "DELETE",
        status: 200,
        response: {
          message: "Job Launched",
        },
      },
    ],
  },
};

const Template = (args) => <DeleteEmptyRepoForm {...args} />;

export const Default = Template.bind({});
Default.args = {
  courseId: 17,
};
