import AxiosMockAdapter from "axios-mock-adapter";
import axios from "axios";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import OnboardingWrapperPage from "main/pages/Onboarding/OnboardingWrapperPage";
import { MemoryRouter } from "react-router";
import {
  apiCurrentUserFixtures,
  apiCurrentUserFixturesWithGithub,
} from "fixtures/currentUserFixtures";

const axiosMock = new AxiosMockAdapter(axios);
const queryClient = new QueryClient();
describe("OnboardingWrapperPage tests", () => {
  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    queryClient.clear();
  });

  test("displays signin on not logged in", async () => {
    axiosMock.onGet("/api/currentUser").reply(401);
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <OnboardingWrapperPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );
    await screen.findByText(
      "We need a bit of information from you. Please select the school you have credentials with:",
    );
    const container = screen.getByTestId("BasicLayout-container");
    expect(container).toBeInTheDocument();
    expect(container).toHaveClass("pt-4 flex-grow-1 d-flex flex-column");
  });

  test("displays GitHub on no Github role", async () => {
    axiosMock
      .onGet("/api/currentUser")
      .reply(200, apiCurrentUserFixtures.userOnly);
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <OnboardingWrapperPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );
    await screen.findByText("Next, you'll need to sign in with GitHub");
  });

  test("displays courses page otherwise", async () => {
    axiosMock
      .onGet("/api/currentUser")
      .reply(200, apiCurrentUserFixturesWithGithub.userOnly);
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <OnboardingWrapperPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );
    await screen.findByText("Joining Courses");
  });
});
