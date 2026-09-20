import React from "react";
import SlackMissingMembersTable from "main/components/Slack/SlackMissingMembersTable";
import slackFixtures from "fixtures/slackFixtures";

export default {
  title: "components/Slack/SlackMissingMembersTable",
  component: SlackMissingMembersTable,
};

const Template = (args) => {
  return <SlackMissingMembersTable {...args} />;
};

export const Empty = Template.bind({});
Empty.args = { members: [] };

export const ThreeMissingMembers = Template.bind({});
ThreeMissingMembers.args = { members: slackFixtures.threeMissingMembers };
