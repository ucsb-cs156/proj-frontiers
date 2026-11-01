import React from "react";
import EduDoraTabComponent from "main/components/TabComponent/EduDoraTabComponent";

export default {
  title: "components/TabComponent/EduDoraTabComponent",
  component: EduDoraTabComponent,
};

const Template = (args) => {
  return <EduDoraTabComponent {...args} />;
};

export const Default = Template.bind({});
Default.args = {
  testIdPrefix: "EduDoraTabComponent",
};
