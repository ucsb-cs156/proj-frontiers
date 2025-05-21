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
  roleemails: [],
  currentUser: currentUserFixtures.userOnly,
};

export const ThreeItemsOrdinaryUser = Template.bind({});
ThreeItemsOrdinaryUser.args = {
  roleemails: roleEmailFixtures.threeRoleEmails,
  currentUser: currentUserFixtures.userOnly,
};

export const ThreeItemsAdminUser = Template.bind({});
ThreeItemsAdminUser.args = {
  roleemails: roleEmailFixtures.threeRoleEmails,
  currentUser: currentUserFixtures.adminUser,
};

ThreeItemsAdminUser.parameters = {
  msw: [
    http.delete("/api/roleemails", () => {
      return HttpResponse.json(
        { message: "Organization deleted successfully" },
        { status: 200 },
      );
    }),
  ],
};
