import { Button, Row, Col } from "react-bootstrap";
import SignInCard from "main/components/Auth/SignInCard";
import { FaGithubSquare } from "react-icons/fa";

export default function OnboardingGithubSignInComponent() {
  const githubIcon = () => {
    return (
      <span data-testid={"OnboardingGithubSignInComponent-githubIcon"}>
        <FaGithubSquare size={"10em"} role={"img"} />
      </span>
    );
  };

  const setRedirect = () => {
    sessionStorage.setItem("redirect", "/onboarding");
  };

  const githubOauthLogin = "/oauth2/authorization/github";

  return (
    <>
      <Row>
        <h1>Next, you&#39;ll need to sign in with GitHub</h1>
        <p>
          This allows your instructor to connect your GitHub account with your
          university.
        </p>
        <p>
          Please sign in with the GitHub account you intend to use for your
          classes.
        </p>
      </Row>
      <Row
        xs={1}
        md={2}
        className="g-5 d-flex gap-5 justify-content-center align-items-center"
      >
        <SignInCard
          Icon={githubIcon}
          title={"Sign in with Github"}
          description={
            "Please connect your account with a GitHub account to continue to Frontiers."
          }
          url={githubOauthLogin}
          testid={"github"}
          onClick={setRedirect}
        />
      </Row>
      <Row className="align-items-center justify-content-center py-3">
        <Col xs="auto" className="text-center">
          <p>Don&apos;t have a GitHub account?</p>
          <Button
            variant="outline-primary"
            href="https://github.com/signup"
            target="_blank"
            rel="noopener noreferrer"
            data-testid="OnboardingGithubSignInComponent-createAccount"
          >
            Create GitHub Account
          </Button>
        </Col>
      </Row>
    </>
  );
}
