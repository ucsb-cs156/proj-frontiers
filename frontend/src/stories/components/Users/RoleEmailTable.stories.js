import React from "react";
import RoleEmailTable from "main/components/Users/RoleEmailTable";
import { roleEmailFixtures } from "fixtures/roleEmailFixtures";

export default {
  title: "components/Users/RoleEmailTable",
  component: RoleEmailTable,
};

const Template = (args) => {
  return <RoleEmailTable {...args} />;
};

export const Empty = Template.bind({});

Empty.args = {
  data: [],
  role: "admin",
};

export const ThreeItemsAdminUser = Template.bind({});
ThreeItemsAdminUser.args = {
  data: roleEmailFixtures.threeItems,
  customDeleteCallback: async (cell) => {
    // Simulate a delete operation
    window.alert(`Would invoke callback on ${cell.row.values.email}`);
  },
};

export const Weird = Template.bind({});
Weird.args = {
  data: ["Stryker was here"],
  customDeleteCallback: async (cell) => {
    // Simulate a delete operation
    window.alert(`Would invoke callback on ${cell.row.values.email}`);
  },
};
