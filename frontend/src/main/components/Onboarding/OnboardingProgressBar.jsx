import { ProgressBar } from "react-bootstrap";

export default function OnboardingProgressBar({
  currentStep,
  totalSteps = 5,
  testId = "OnboardingProgressBar",
}) {
  const progressPercent = (currentStep / totalSteps) * 100;

  return (
    <div className="my-4" data-testid={testId}>
      <ProgressBar
        now={progressPercent}
        data-testid={`${testId}-bar`}
        style={{ height: "20px" }}
      />
    </div>
  );
}
