import { Container } from "react-bootstrap";
import Footer from "main/components/Nav/Footer";
import AppNavbar from "main/components/Nav/AppNavbar";
import { useCurrentUser, useLogout } from "main/utils/currentUser";
import { useSystemInfo } from "main/utils/systemInfo";
import OnboardingProgressBar from "main/components/Onboarding/OnboardingProgressBar";

export default function OnboardingLayout({
  children,
  currentStep,
  totalSteps,
}) {
  const currentUser = useCurrentUser();
  const { data: systemInfo } = useSystemInfo();

  const doLogout = useLogout().mutate;

  return (
    <div className="d-flex flex-column min-vh-100">
      <AppNavbar
        currentUser={currentUser}
        systemInfo={systemInfo}
        doLogout={doLogout}
      />
      <Container expand="xl" className="pt-4 flex-grow-1">
        {children}
        <OnboardingProgressBar
          currentStep={currentStep}
          totalSteps={totalSteps}
        />
      </Container>
      <Footer />
    </div>
  );
}
