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
    "If you are a University of California-Santa Barbara student, sign in with your NetID.",
};

export const Microsoft = Template.bind({});

Microsoft.args = {
  url: "/oauth2/authorization/azure-dev",
  Icon: () => {
    return <FaMicrosoft size={"10em"} />;
  },
  title: "Sign in with Microsoft",
  description:
    "If you are an Oregon State University student, sign in with your ONID.",
};
