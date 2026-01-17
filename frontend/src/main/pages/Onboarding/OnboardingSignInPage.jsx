import { Container, Row } from "react-bootstrap";
import { FaMicrosoft } from "react-icons/fa";
import { FcGoogle } from "react-icons/fc";
import { useLocation } from "react-router";
import OnboardingLayout from "main/layouts/OnboardingLayout/OnboardingLayout";
import SignInCard from "main/components/Auth/SignInCard";
import { useSystemInfo } from "main/utils/systemInfo";

export default function OnboardingSignInPage() {
  const location = useLocation();
  const { data: systemInfo } = useSystemInfo();

  // Get the selected provider from session storage
  const provider = sessionStorage.getItem("onboardingProvider") || "google";
  const schoolName = sessionStorage.getItem("onboardingSchool") || "";

  const setRedirect = () => {
    sessionStorage.setItem("redirect", location.pathname);
  };

  const microsoftIcon = () => {
    return (
      <span data-testid="OnboardingSignIn-microsoftIcon">
        <FaMicrosoft size="10em" role="img" />
      </span>
    );
  };

  const googleIcon = () => {
    return (
      <span data-testid="OnboardingSignIn-googleIcon">
        <FcGoogle size="10em" role="img" />
      </span>
    );
  };

  const isGoogle = provider === "google";
  const Icon = isGoogle ? googleIcon : microsoftIcon;
  const title = isGoogle ? "Sign in with Google" : "Sign in with Microsoft";
  const url = isGoogle ? systemInfo.oauthLogin : systemInfo.activeDirectoryUrl;
  const testId = isGoogle ? "google" : "microsoft";

  return (
    <OnboardingLayout currentStep={2} totalSteps={5}>
      <Container className="text-center">
        <h1 className="mb-4">Welcome to Frontiers!</h1>
        <hr />
        <p className="mb-4">
          Please pick the school you have credentials with:
        </p>
        <p className="mb-4">
          <strong>{schoolName}</strong>
        </p>
        <Row
          xs={1}
          className="g-5 d-flex gap-5 justify-content-center align-items-center"
          data-testid="OnboardingSignIn-cardDisplay"
        >
          <SignInCard
            Icon={Icon}
            title={title}
            description={`Sign in with your ${schoolName} credentials`}
            url={url}
            testid={testId}
            onClick={setRedirect}
          />
        </Row>
      </Container>
    </OnboardingLayout>
  );
}
