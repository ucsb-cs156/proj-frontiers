import { render, screen } from "@testing-library/react";
import OnboardingCompletePage from "main/pages/Onboarding/OnboardingCompletePage";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router";
import { apiCurrentUserFixturesWithGithub } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";

const mockedNavigate = vi.fn();
vi.mock("react-router", async () => {
  const actual = await vi.importActual("react-router");
  return {
    ...actual,
    useNavigate: () => mockedNavigate,
  };
});

describe("OnboardingCompletePage tests", () => {
  const axiosMock = new AxiosMockAdapter(axios);
  const queryClient = new QueryClient();

  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    queryClient.clear();
    mockedNavigate.mockReset();
    sessionStorage.clear();
  });

  const setUpBaseEnvironment = () => {
    axiosMock
      .onGet("/api/currentUser")
      .reply(200, apiCurrentUserFixturesWithGithub.userOnly);
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.showingNeither);
  };

  test("renders without crashing", async () => {
    setUpBaseEnvironment();
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <OnboardingCompletePage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText("Great! You're done.");
    expect(
      screen.getByTestId("OnboardingComplete-countdown"),
    ).toBeInTheDocument();
    expect(screen.getByTestId("OnboardingProgressBar")).toBeInTheDocument();
  });

  test("shows countdown starting at 3", async () => {
    setUpBaseEnvironment();
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <OnboardingCompletePage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText(/Redirecting to home in 3/);
  });
});
