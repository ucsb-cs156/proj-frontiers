import { render, screen } from "@testing-library/react";
import OnboardingLayout from "main/layouts/OnboardingLayout/OnboardingLayout";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router";
import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";

describe("OnboardingLayout tests", () => {
  const axiosMock = new AxiosMockAdapter(axios);
  const queryClient = new QueryClient();

  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    queryClient.clear();
  });

  test("renders without crashing", async () => {
    axiosMock
      .onGet("/api/currentUser")
      .reply(200, apiCurrentUserFixtures.userOnly);
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.showingNeither);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <OnboardingLayout currentStep={1} totalSteps={5}>
            <h1>Test Content</h1>
          </OnboardingLayout>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText("Test Content");
    expect(screen.getByTestId("OnboardingProgressBar")).toBeInTheDocument();
  });

  test("renders progress bar with correct step", async () => {
    axiosMock
      .onGet("/api/currentUser")
      .reply(200, apiCurrentUserFixtures.userOnly);
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.showingNeither);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <OnboardingLayout currentStep={3} totalSteps={5}>
            <h1>Step 3 Content</h1>
          </OnboardingLayout>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText("Step 3 Content");
    expect(screen.getByTestId("OnboardingProgressBar-bar")).toBeInTheDocument();
  });
});
