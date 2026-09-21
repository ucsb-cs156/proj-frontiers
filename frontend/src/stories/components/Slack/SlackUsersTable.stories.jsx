import React from "react";
import SlackUsersTable from "main/components/Slack/SlackUsersTable";
import slackFixtures from "fixtures/slackFixtures";

export default {
  title: "components/Slack/SlackUsersTable",
  component: SlackUsersTable,
};

const Template = (args) => {
  return <SlackUsersTable {...args} />;
};

export const Empty = Template.bind({});
Empty.args = { users: [] };

export const FourUsers = Template.bind({});
FourUsers.args = { users: slackFixtures.fourUsers };
