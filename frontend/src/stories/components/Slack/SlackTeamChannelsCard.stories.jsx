import React from "react";
import { http, HttpResponse } from "msw";
import SlackTeamChannelsCard from "main/components/Slack/SlackTeamChannelsCard";

export default {
  title: "components/Slack/SlackTeamChannelsCard",
  component: SlackTeamChannelsCard,
};

const Template = (args) => {
  return <SlackTeamChannelsCard {...args} />;
};

export const Default = Template.bind({});
Default.args = { courseId: 7 };
Default.parameters = {
  msw: [
    http.post("/api/courses/slack/teamChannels", () => {
      return HttpResponse.json({ id: 17, status: "running" });
    }),
  ],
};

export const CannotLaunch = Template.bind({});
CannotLaunch.args = { courseId: 7 };
CannotLaunch.parameters = {
  msw: [
    http.post("/api/courses/slack/teamChannels", () => {
      return HttpResponse.json(
        {
          type: "IllegalArgumentException",
          message:
            "No Slack token has been set for this course; enter one on the Settings tab.",
        },
        { status: 400 },
      );
    }),
  ],
};
