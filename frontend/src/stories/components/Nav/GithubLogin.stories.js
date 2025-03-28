import { apiCurrentUserFixtures, apiCurrentUserFixturesWithGithub } from "fixtures/currentUserFixtures";
import GithubLogin from "main/components/Nav/GithubLogin";

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories#default-export
export default {
  title: "components/Nav/GithubLogin",
  component: GithubLogin,
  parameters: {
    // Optional parameter to center the component in the Canvas. More info: https://storybook.js.org/docs/configure/story-layout
    layout: "centered",
  },
  // This component will have an automatically generated Autodocs entry: https://storybook.js.org/docs/writing-docs/autodocs
  tags: ["autodocs"],
  // More on argTypes: https://storybook.js.org/docs/api/argtypes
  argTypes: {},
  // See: https://storybook.js.org/docs/essentials/actions#action-args
  args: {},
};

// More on writing stories with args: https://storybook.js.org/docs/writing-stories/args

const Template = (args) => <GithubLogin {...args} />;

export const LoggedOut = Template.bind({});
LoggedOut.parameters = {
  currentUser: {
    loggedIn: false,
  },
  systemInfo: null,
  doLogout: () => { },
};

export const LoggedInUserNoGithub = Template.bind({});
LoggedInUserNoGithub.args = {
  currentUser: {
    loggedIn: true,
    root: apiCurrentUserFixtures.userOnly,
  },
  systemInfo: {
    oauthLogin: "/oauth2/authorization/test", // This simulates the oauth login URL
  },
  doLogout: () => {
    window.alert("Logging out");
    console.log("Logged out");
  },
};

export const LoggedInAdminNoGithub = Template.bind({});
LoggedInAdminNoGithub.args = {
  currentUser: {
    loggedIn: true,
    root: apiCurrentUserFixtures.adminUser,
  },
  systemInfo: {
    oauthLogin: "/oauth2/authorization/test", // This simulates the oauth login URL
  },
  doLogout: () => {
    window.alert("Logging out");
    console.log("Logged out");
  },
};

export const LoggedInUserWithGithub = Template.bind({});
LoggedInUserWithGithub.args = {
  currentUser: {
    loggedIn: true,
    root: apiCurrentUserFixturesWithGithub.userOnly,
  },
  systemInfo: {
    oauthLogin: "/oauth2/authorization/test", // This simulates the oauth login URL
  },
  doLogout: () => {
    window.alert("Logging out");
    console.log("Logged out");
  },
};

export const LoggedInAdminWithGithub = Template.bind({});
LoggedInAdminWithGithub.args = {
  currentUser: {
    loggedIn: true,
    root: apiCurrentUserFixturesWithGithub.adminUser,
  },
  systemInfo: {
    oauthLogin: "/oauth2/authorization/test", // This simulates the oauth login URL
  },
  doLogout: () => {
    window.alert("Logging out");
    console.log("Logged out");
  },
};
