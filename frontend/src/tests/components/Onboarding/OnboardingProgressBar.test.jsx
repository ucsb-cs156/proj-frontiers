import { render, screen } from "@testing-library/react";
import OnboardingProgressBar from "main/components/Onboarding/OnboardingProgressBar";

describe("OnboardingProgressBar tests", () => {
  test("renders without crashing with default props", () => {
    render(<OnboardingProgressBar currentStep={1} />);
    expect(screen.getByTestId("OnboardingProgressBar")).toBeInTheDocument();
    expect(screen.getByTestId("OnboardingProgressBar-bar")).toBeInTheDocument();
  });

  test("renders with custom testId", () => {
    render(<OnboardingProgressBar currentStep={2} testId="CustomTestId" />);
    expect(screen.getByTestId("CustomTestId")).toBeInTheDocument();
    expect(screen.getByTestId("CustomTestId-bar")).toBeInTheDocument();
  });

  test("shows 20% progress for step 1 of 5", () => {
    render(<OnboardingProgressBar currentStep={1} totalSteps={5} />);
    const progressBar = screen.getByTestId("OnboardingProgressBar-bar");
    expect(progressBar).toBeInTheDocument();
  });

  test("shows 60% progress for step 3 of 5", () => {
    render(<OnboardingProgressBar currentStep={3} totalSteps={5} />);
    const progressBar = screen.getByTestId("OnboardingProgressBar-bar");
    expect(progressBar).toBeInTheDocument();
  });

  test("shows 100% progress for step 5 of 5", () => {
    render(<OnboardingProgressBar currentStep={5} totalSteps={5} />);
    const progressBar = screen.getByTestId("OnboardingProgressBar-bar");
    expect(progressBar).toBeInTheDocument();
  });
});
