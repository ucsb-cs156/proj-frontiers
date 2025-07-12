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
};

export const ThreeItems = Template.bind({});
ThreeItems.args = {
  data: roleEmailFixtures.threeItems,
  customDeleteCallback: async (cell) => {
    // Simulate a delete operation
    window.alert(`Would invoke callback on ${cell.row.values.email}`);
  },
};

export const ThreeItemsWithIsInAdminEmailsField = Template.bind({});
ThreeItemsWithIsInAdminEmailsField.args = {
  data: roleEmailFixtures.threeItemsWithIsInAdminEmailField,
  customDeleteCallback: async (cell) => {
    // Simulate a delete operation
    window.alert(`Would invoke callback on ${cell.row.values.email}`);
  },
};
