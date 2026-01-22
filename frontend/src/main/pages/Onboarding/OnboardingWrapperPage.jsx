import { hasRole, useCurrentUser } from "main/utils/currentUser";
import OnboardingSelectSchoolComponent from "main/components/Onboarding/OnboardingSelectSchoolComponent";
import OnboardingSelectCoursesComponent from "main/components/Onboarding/OnboardingSelectCoursesComponent";
import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import { ProgressBar, Row } from "react-bootstrap";
import OnboardingGithubSignInComponent from "main/components/Onboarding/OnboardingGithubSignInComponent";

export default function OnboardingWrapperPage() {
  const userData = useCurrentUser();
  let progress;
  const renderElement = () => {
    if (!userData.loggedIn) {
      progress = 25;
      return <OnboardingSelectSchoolComponent />;
    } else if (!hasRole(userData, "ROLE_GITHUB")) {
      progress = 50;
      return <OnboardingGithubSignInComponent />;
    } else {
      progress = 75;
      return <OnboardingSelectCoursesComponent />;
    }
  };

  return (
    <BasicLayout>
      {renderElement()}
      <Row className="mt-auto pb-3">
        <ProgressBar now={progress} />
      </Row>
    </BasicLayout>
  );
}
