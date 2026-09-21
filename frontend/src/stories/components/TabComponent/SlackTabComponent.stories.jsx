import React from "react";
import { http, HttpResponse } from "msw";
import SlackTabComponent from "main/components/TabComponent/SlackTabComponent";
import slackFixtures from "fixtures/slackFixtures";

export default {
  title: "components/TabComponent/SlackTabComponent",
  component: SlackTabComponent,
};

const Template = (args) => {
  return <SlackTabComponent {...args} />;
};

const args = {
  courseId: 7,
  testIdPrefix: "InstructorCourseShowPage",
  slackTeamName: slackFixtures.connectedInfo.slackTeamName,
  slackTeamUrl: slackFixtures.connectedInfo.slackTeamUrl,
};

export const Default = Template.bind({});
Default.args = args;
Default.parameters = {
  msw: [
    http.get("/api/courses/slack/users", () => {
      return HttpResponse.json(slackFixtures.fourUsers);
    }),
    http.get("/api/courses/slack/missing", () => {
      return HttpResponse.json(slackFixtures.threeMissingMembers);
    }),
  ],
};

export const MissingScope = Template.bind({});
MissingScope.args = args;
MissingScope.parameters = {
  msw: [
    http.get("/api/courses/slack/users", () => {
      return HttpResponse.json(
        {
          ok: false,
          error: "missing_scope",
          message:
            "This Slack token is missing a required scope. Add the scope under OAuth & Permissions, reinstall the Slack app to the workspace, and enter the new token.",
        },
        { status: 502 },
      );
    }),
    http.get("/api/courses/slack/missing", () => {
      return HttpResponse.json(slackFixtures.threeMissingMembers);
    }),
  ],
};
