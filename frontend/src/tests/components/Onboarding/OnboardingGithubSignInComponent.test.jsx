import {
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router";

import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import OnboardingGithubSignInComponent from "main/components/Onboarding/OnboardingGithubSignInComponent";

describe("OnboardingGithubSignInComponent tests", () => {
  const axiosMock = new AxiosMockAdapter(axios);
  const queryClient = new QueryClient();

  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    queryClient.clear();
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
          <OnboardingGithubSignInComponent />
        </MemoryRouter>
      </QueryClientProvider>,
    );
    await screen.findByText(/Sign in with Github/);
    expect(
      screen.getByTestId("OnboardingGithubSignInComponent-githubIcon"),
    ).toBeInTheDocument();
    expect(screen.getByText("Sign in with Github")).toBeInTheDocument();
    expect(
      within(screen.getByTestId("SignInCard-base-github")).getByText("Log In"),
    ).toHaveAttribute("href", "/oauth2/authorization/github");
    expect(screen.getByRole("img")).toHaveAttribute("height", "10em");
    expect(screen.getByRole("img")).toHaveAttribute("width", "10em");

    // Check for the "Create GitHub Account" button
    expect(
      screen.getByText("Don't have a GitHub account?"),
    ).toBeInTheDocument();
    const createAccountButton = screen.getByTestId(
      "OnboardingGithubSignInComponent-createAccount",
    );
    expect(createAccountButton).toBeInTheDocument();
    expect(createAccountButton).toHaveTextContent("Create GitHub Account");
    expect(createAccountButton).toHaveAttribute(
      "href",
      "https://github.com/signup",
    );
    expect(createAccountButton).toHaveAttribute("target", "_blank");
    expect(createAccountButton).toHaveAttribute("rel", "noopener noreferrer");
  });
  test("Base environment test", async () => {
    axiosMock.onGet("/api/systemInfo").timeout();
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <OnboardingGithubSignInComponent />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const footer = await screen.findByTestId("SignInCard-footer-github");
    const button = within(footer).getByRole("button", { name: /log in/i });
    expect(button).toHaveAttribute("href", "/oauth2/authorization/github");
    expect(
      screen.getByText(
        "Please connect your account with a GitHub account to continue to Frontiers.",
      ),
    ).toBeInTheDocument();
  });

  test("Clicking button sets sessionStorage", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/onboarding"]}>
          <OnboardingGithubSignInComponent />
        </MemoryRouter>
      </QueryClientProvider>,
    );
    await screen.findByText(/Sign in with Github/);
    fireEvent.click(
      within(screen.getByTestId("SignInCard-base-github")).getByText("Log In"),
    );
    await waitFor(() =>
      expect(sessionStorage.getItem("redirect")).toBe("/onboarding"),
    );
  });
});
