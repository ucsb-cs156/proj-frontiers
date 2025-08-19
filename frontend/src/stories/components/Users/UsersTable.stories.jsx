import React from "react";
import UsersTable from "main/components/Users/UsersTable";
import usersFixtures from "fixtures/usersFixtures";

export default {
  title: "components/Users/UsersTable",
  component: UsersTable,
};

const Template = (args) => {
  return <UsersTable {...args} />;
};

export const Empty = Template.bind({});

Empty.args = {
  users: [],
};

export const ThreeItems = Template.bind({});
ThreeItems.args = {
  users: usersFixtures.threeUsers,
};
