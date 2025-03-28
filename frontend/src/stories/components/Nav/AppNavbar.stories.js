import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import AppNavbar from "main/components/Nav/AppNavbar";

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories#default-export
export default {
  title: "components/Nav/AppNavbar",
  component: AppNavbar,
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

const Template = (args) => {
  return <AppNavbar {...args} />;
};

export const LoggedOut = Template.bind({});

export const User = Template.bind({});
User.args = {
  currentUser: {
    loggedIn: true,
    root: apiCurrentUserFixtures.userOnly,
  },
};

export const Admin = Template.bind({});
Admin.args = {
  currentUser: {
    loggedIn: true,
    root: apiCurrentUserFixtures.adminUser,
  },
};

export const Localhost3000 = Template.bind({});
Localhost3000.args = {
  currentUrl: "http://localhost:3000",
};

export const LocalhostNumeric3000 = Template.bind({});
LocalhostNumeric3000.args = {
  currentUrl: "http://127.0.0.1:3000",
};

export const Localhost8080 = Template.bind({});
Localhost8080.args = {
  currentUrl: "http://localhost:8080",
};
