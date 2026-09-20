import React from "react";
import SlackSetupInstructions from "main/components/Settings/SlackSetupInstructions";

export default {
  title: "components/Settings/SlackSetupInstructions",
  component: SlackSetupInstructions,
};

const Template = (args) => {
  return <SlackSetupInstructions {...args} />;
};

export const Default = Template.bind({});

Default.args = {};
