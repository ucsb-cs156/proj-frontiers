import { render, screen } from "@testing-library/react";
import OnboardingSchoolSelectionPage from "main/pages/Onboarding/OnboardingSchoolSelectionPage";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router";
import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";

describe("OnboardingSchoolSelectionPage tests", () => {
  const axiosMock = new AxiosMockAdapter(axios);
  const queryClient = new QueryClient();

  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    queryClient.clear();
    sessionStorage.clear();
  });

  const setUpBaseEnvironment = () => {
    axiosMock
      .onGet("/api/currentUser")
      .reply(200, apiCurrentUserFixtures.userOnly);
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.showingNeither);
  };

  test("renders without crashing", async () => {
    setUpBaseEnvironment();
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <OnboardingSchoolSelectionPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText("Welcome to Frontiers!");
    expect(
      screen.getByText("Please pick the school you have credentials with:"),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId("OnboardingSchoolSelection-typeahead"),
    ).toBeInTheDocument();
    expect(screen.getByTestId("OnboardingProgressBar")).toBeInTheDocument();
  });

  test("typeahead has correct placeholder", async () => {
    setUpBaseEnvironment();
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <OnboardingSchoolSelectionPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText("Welcome to Frontiers!");
    const typeahead = screen.getByTestId("OnboardingSchoolSelection-typeahead");
    expect(typeahead).toHaveAttribute(
      "placeholder",
      "Start typing to select a school...",
    );
  });
});
