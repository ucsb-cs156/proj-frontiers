import { useState, useEffect } from "react";
import { useNavigate } from "react-router";
import { Container } from "react-bootstrap";
import OnboardingLayout from "main/layouts/OnboardingLayout/OnboardingLayout";

export default function OnboardingCompletePage() {
  const navigate = useNavigate();
  const [countdown, setCountdown] = useState(3);

  useEffect(() => {
    if (countdown <= 0) {
      // Clear onboarding session storage
      sessionStorage.removeItem("onboardingSchool");
      sessionStorage.removeItem("onboardingProvider");
      navigate("/");
      return;
    }

    const timer = setTimeout(() => {
      setCountdown(countdown - 1);
    }, 1000);

    return () => clearTimeout(timer);
  }, [countdown, navigate]);

  return (
    <OnboardingLayout currentStep={5} totalSteps={5}>
      <Container className="text-center">
        <h1 className="mb-4">Great! You're done.</h1>
        <p className="mb-4" data-testid="OnboardingComplete-countdown">
          Redirecting to home in {countdown}...
        </p>
      </Container>
    </OnboardingLayout>
  );
}
