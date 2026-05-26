import React from "react";
import DownloadsTabComponent from "main/components/TabComponent/DownloadsTabComponent";

export default {
  title: "components/TabComponent/DownloadsTabComponent",
  component: DownloadsTabComponent,
};

const Template = (args) => {
  return <DownloadsTabComponent {...args} />;
};

export const Default = Template.bind({});

Default.args = {
  courseId: 1,
  testIdPrefix: "InstructorCourseShowPage",
};
