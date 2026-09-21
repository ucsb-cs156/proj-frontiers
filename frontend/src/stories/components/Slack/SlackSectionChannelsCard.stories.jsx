import React from "react";
import { http, HttpResponse } from "msw";
import SlackSectionChannelsCard from "main/components/Slack/SlackSectionChannelsCard";

export default {
  title: "components/Slack/SlackSectionChannelsCard",
  component: SlackSectionChannelsCard,
};

const Template = (args) => {
  return <SlackSectionChannelsCard {...args} />;
};

export const Default = Template.bind({});
Default.args = { courseId: 7 };
Default.parameters = {
  msw: [
    http.post("/api/courses/slack/sectionChannels", () => {
      return HttpResponse.json({ id: 17, status: "running" });
    }),
  ],
};

export const CannotLaunch = Template.bind({});
CannotLaunch.args = { courseId: 7 };
CannotLaunch.parameters = {
  msw: [
    http.post("/api/courses/slack/sectionChannels", () => {
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
