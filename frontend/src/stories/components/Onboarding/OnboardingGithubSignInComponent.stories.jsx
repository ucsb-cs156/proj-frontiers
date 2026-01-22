import OnboardingGithubSignInComponent from "main/components/Onboarding/OnboardingGithubSignInComponent";
import { Container } from "react-bootstrap";

export default {
  title: "components/Onboarding/OnboardingGithubSignInComponent",
  component: OnboardingGithubSignInComponent,
};

const Template = ({ args }) => (
  <Container>
    <OnboardingGithubSignInComponent {...args} />
  </Container>
);

export const Default = Template.bind({});
