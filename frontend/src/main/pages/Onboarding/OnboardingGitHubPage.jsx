import { Container, Row, Button } from "react-bootstrap";
import { FaGithubSquare } from "react-icons/fa";
import { useLocation } from "react-router";
import OnboardingLayout from "main/layouts/OnboardingLayout/OnboardingLayout";
import SignInCard from "main/components/Auth/SignInCard";
import { useSystemInfo } from "main/utils/systemInfo";

export default function OnboardingGitHubPage() {
  const location = useLocation();
  const { data: systemInfo } = useSystemInfo();

  const setRedirect = () => {
    sessionStorage.setItem("redirect", location.pathname);
  };

  const githubIcon = () => {
    return (
      <span data-testid="OnboardingGitHub-githubIcon">
        <FaGithubSquare size="10em" role="img" />
      </span>
    );
  };

  const githubOauthLogin =
    systemInfo.githubOauthLogin || "/oauth2/authorization/github";

  return (
    <OnboardingLayout currentStep={3} totalSteps={5}>
      <Container className="text-center">
        <p className="mb-4">
          Now, we need a bit more information about you. Please sign into the
          GitHub account you'd like to use for your courses:
        </p>
        <Row
          xs={1}
          className="g-5 d-flex gap-5 justify-content-center align-items-center"
          data-testid="OnboardingGitHub-cardDisplay"
        >
          <SignInCard
            Icon={githubIcon}
            title="Sign in with GitHub"
            description="Connect your GitHub account to continue."
            url={githubOauthLogin}
            testid="github"
            onClick={setRedirect}
          />
        </Row>
        <div className="mt-4">
          <p>Don't have a GitHub account?</p>
          <Button
            variant="outline-primary"
            href="https://github.com/signup"
            target="_blank"
            rel="noopener noreferrer"
            data-testid="OnboardingGitHub-createAccount"
          >
            Create GitHub Account
          </Button>
        </div>
      </Container>
    </OnboardingLayout>
  );
}
