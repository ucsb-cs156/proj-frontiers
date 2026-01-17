import {
  render,
  screen,
  fireEvent,
  waitFor,
  within,
} from "@testing-library/react";
import OnboardingSignInPage from "main/pages/Onboarding/OnboardingSignInPage";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router";
import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";

describe("OnboardingSignInPage tests", () => {
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
      .reply(200, systemInfoFixtures.withActiveDirectoryAndGoogle);
  };

  test("renders without crashing with Google provider", async () => {
    setUpBaseEnvironment();
    sessionStorage.setItem(
      "onboardingSchool",
      "University of California, Santa Barbara",
    );
    sessionStorage.setItem("onboardingProvider", "google");

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <OnboardingSignInPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText("Welcome to Frontiers!");
    expect(
      screen.getByText("University of California, Santa Barbara"),
    ).toBeInTheDocument();
    expect(screen.getByText("Sign in with Google")).toBeInTheDocument();
    expect(
      screen.getByTestId("OnboardingSignIn-googleIcon"),
    ).toBeInTheDocument();
    expect(screen.getByTestId("OnboardingProgressBar")).toBeInTheDocument();
  });

  test("renders with Microsoft provider", async () => {
    setUpBaseEnvironment();
    sessionStorage.setItem("onboardingSchool", "Oregon State University");
    sessionStorage.setItem("onboardingProvider", "microsoft");

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <OnboardingSignInPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText("Welcome to Frontiers!");
    expect(screen.getByText("Oregon State University")).toBeInTheDocument();
    expect(screen.getByText("Sign in with Microsoft")).toBeInTheDocument();
    expect(
      screen.getByTestId("OnboardingSignIn-microsoftIcon"),
    ).toBeInTheDocument();
  });

  test("clicking sign in button sets sessionStorage redirect", async () => {
    setUpBaseEnvironment();
    sessionStorage.setItem(
      "onboardingSchool",
      "University of California, Santa Barbara",
    );
    sessionStorage.setItem("onboardingProvider", "google");

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/onboarding/signin"]}>
          <OnboardingSignInPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText("Sign in with Google");
    const signInCard = screen.getByTestId("SignInCard-base-google");
    const logInButton = within(signInCard).getByRole("button", {
      name: /log in/i,
    });
    fireEvent.click(logInButton);

    await waitFor(() =>
      expect(sessionStorage.getItem("redirect")).toBe("/onboarding/signin"),
    );
  });

  test("defaults to google if no provider in sessionStorage", async () => {
    setUpBaseEnvironment();

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <OnboardingSignInPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText("Sign in with Google");
    expect(
      screen.getByTestId("OnboardingSignIn-googleIcon"),
    ).toBeInTheDocument();
  });
});
