import {
  render,
  screen,
  fireEvent,
  waitFor,
  within,
} from "@testing-library/react";
import OnboardingGitHubPage from "main/pages/Onboarding/OnboardingGitHubPage";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router";
import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";

describe("OnboardingGitHubPage tests", () => {
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
          <OnboardingGitHubPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText(/Now, we need a bit more information about you/);
    expect(screen.getByText("Sign in with GitHub")).toBeInTheDocument();
    expect(
      screen.getByTestId("OnboardingGitHub-githubIcon"),
    ).toBeInTheDocument();
    expect(screen.getByTestId("OnboardingProgressBar")).toBeInTheDocument();
  });

  test("shows create GitHub account button", async () => {
    setUpBaseEnvironment();
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <OnboardingGitHubPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText("Don't have a GitHub account?");
    const createAccountButton = screen.getByTestId(
      "OnboardingGitHub-createAccount",
    );
    expect(createAccountButton).toBeInTheDocument();
    expect(createAccountButton).toHaveTextContent("Create GitHub Account");
    expect(createAccountButton).toHaveAttribute(
      "href",
      "https://github.com/signup",
    );
  });

  test("clicking sign in button sets sessionStorage redirect", async () => {
    setUpBaseEnvironment();
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/onboarding/github"]}>
          <OnboardingGitHubPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText("Sign in with GitHub");
    const signInCard = screen.getByTestId("SignInCard-base-github");
    const logInButton = within(signInCard).getByRole("button", {
      name: /log in/i,
    });
    fireEvent.click(logInButton);

    await waitFor(() =>
      expect(sessionStorage.getItem("redirect")).toBe("/onboarding/github"),
    );
  });

  test("uses default github oauth url when not in systemInfo", async () => {
    axiosMock
      .onGet("/api/currentUser")
      .reply(200, apiCurrentUserFixtures.userOnly);
    axiosMock.onGet("/api/systemInfo").reply(200, {});

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <OnboardingGitHubPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText("Sign in with GitHub");
    const signInCard = screen.getByTestId("SignInCard-base-github");
    const logInButton = within(signInCard).getByRole("button", {
      name: /log in/i,
    });
    expect(logInButton).toHaveAttribute("href", "/oauth2/authorization/github");
  });
});
