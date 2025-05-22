import React from "react";
import RoleEmailTable from "main/components/Users/RoleEmailTable";
import { roleEmailFixtures } from "fixtures/roleEmailFixtures";
import { currentUserFixtures } from "fixtures/currentUserFixtures";
import { http, HttpResponse } from "msw";

export default {
  title: "components/Users/RoleEmailTable",
  component: RoleEmailTable,
};

const Template = (args) => {
  return <RoleEmailTable {...args} />;
};

export const Empty = Template.bind({});

Empty.args = {
  items: [],
  currentUser: currentUserFixtures.userOnly,
  role: "admin",
};

export const ThreeItemsAdminUser = Template.bind({});
ThreeItemsAdminUser.args = {
  items: roleEmailFixtures.threeItems,
  currentUser: currentUserFixtures.adminUser,
  role: "admin",
};

ThreeItemsAdminUser.parameters = {
  msw: [
    http.delete(new RegExp("/api/admins/(admin|instructor)"), () => {
      return HttpResponse.json(
        { message: "Item deleted successfully" },
        { status: 200 },
      );
    }),
  ],
};
