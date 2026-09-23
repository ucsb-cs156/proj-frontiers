import React from "react";
import { http, HttpResponse } from "msw";
import SlackCourseSettings from "main/components/Settings/SlackCourseSettings";

export default {
  title: "components/Settings/SlackCourseSettings",
  component: SlackCourseSettings,
};

const Template = (args) => {
  return <SlackCourseSettings {...args} />;
};

const submitAction = (data) =>
  window.alert(`Submitted Slack token: ${JSON.stringify(data)}`);

export const NotConnected = Template.bind({});

NotConnected.args = {
  courseId: 1,
  submitAction,
};

NotConnected.parameters = {
  msw: [
    http.get("/api/courses/slack/info", () => {
      return HttpResponse.json({
        courseId: "1",
        slackBotToken: "",
        slackTeamId: "",
        slackTeamName: "",
      });
    }),
  ],
};

export const Connected = Template.bind({});

Connected.args = {
  courseId: 1,
  submitAction,
};

Connected.parameters = {
  msw: [
    http.get("/api/courses/slack/info", () => {
      return HttpResponse.json({
        courseId: "1",
        slackBotToken: "xoxb******************ghij",
        slackTeamId: "T12345678",
        slackTeamName: "ucsb-cs156-f26",
      });
    }),
  ],
};

export const InvalidToken = Template.bind({});

InvalidToken.args = {
  courseId: 1,
  submitAction,
  errorMessage:
    "Slack rejected this token as invalid. Copy the Bot User OAuth Token (xoxb-...) from your Slack app's OAuth & Permissions page and try again. The token was not saved.",
};

InvalidToken.parameters = NotConnected.parameters;
