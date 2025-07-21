import SignInCard from "main/components/Auth/SignInCard";
import { FcGoogle } from "react-icons/fc";
import { FaMicrosoft } from "react-icons/fa";

export default {
  title: "components/Auth/SignInCard",
  component: SignInCard,
};

const Template = (args) => <SignInCard {...args} />;

export const Google = Template.bind({});

Google.args = {
  url: "/oauth2/authorization/google",
  Icon: () => {
    return <FcGoogle size={"10em"} />;
  },
  title: "Sign in with Google",
  description:
    "If you have University of California-Santa Barbara login credentials, sign in with Google",
};

export const Microsoft = Template.bind({});

Microsoft.args = {
  url: "/oauth2/authorization/azure-dev",
  Icon: () => {
    return <FaMicrosoft size={"10em"} />;
  },
  title: "Sign in with Microsoft",
  description:
    "If you have Oregon State University login credentials, sign in with Microsoft.",
};
