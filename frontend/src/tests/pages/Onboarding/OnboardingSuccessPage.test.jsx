import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import OnboardingSuccessPage from "main/pages/Onboarding/OnboardingSuccessPage";
import { MemoryRouter } from "react-router";

const queryClient = new QueryClient();

test("OnboardingSuccessPage static checks", async () => {
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <OnboardingSuccessPage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
  await screen.findByText(
    /Congratulations on completing the onboarding process!/,
  );
});
