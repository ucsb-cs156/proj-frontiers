import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import GoogleLogin from "main/components/Nav/GoogleLogin";

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories#default-export
export default {
  title: "components/Nav/GoogleLogin",
  component: GoogleLogin,
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

const Template = (args) => <GoogleLogin {...args} />;

export const LoggedOut = Template.bind({});
LoggedOut.parameters = {
  currentUser: {
    loggedIn: false
  },
  systemInfo: null,
  doLogout: () => { }
};


export const LoggedInUser = Template.bind({});
LoggedInUser.args = {
  currentUser: {
    loggedIn: true,
    root: apiCurrentUserFixtures.userOnly
  },
  systemInfo: {
    oauthLogin: "/oauth2/authorization/test" // This simulates the oauth login URL
  },
  doLogout: () => { window.alert("Logging out"); console.log("Logged out"); }
};


export const LoggedInAdmin = Template.bind({});
LoggedInAdmin.args = {
  currentUser: {
    loggedIn: true,
    root: apiCurrentUserFixtures.adminUser
  },
  systemInfo: {
    oauthLogin: "/oauth2/authorization/test" // This simulates the oauth login URL
  },
  doLogout: () => { window.alert("Logging out"); console.log("Logged out"); }
};




