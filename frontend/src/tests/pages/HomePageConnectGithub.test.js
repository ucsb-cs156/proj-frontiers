import { render, screen, within } from "@testing-library/react";
import HomePageConnectGithub from "main/pages/HomePageConnectGithub";
import { QueryClient, QueryClientProvider } from "react-query";
import { MemoryRouter } from "react-router-dom";

import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";

describe("HomePageConnectGithub tests", () => {
  const axiosMock = new AxiosMockAdapter(axios);
  axiosMock
    .onGet("/api/currentUser")
    .reply(200, apiCurrentUserFixtures.userOnly);
  axiosMock
    .onGet("/api/systemInfo")
    .reply(200, systemInfoFixtures.showingNeither);

  const queryClient = new QueryClient();
  test("renders without crashing", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <HomePageConnectGithub />
        </MemoryRouter>
      </QueryClientProvider>,
    );
    await screen.findByText(/Sign in with Github/);
    expect(
      screen.getByTestId("HomePageConnectGithub-githubIcon"),
    ).toBeInTheDocument();
    expect(screen.getByText("Sign in with Github")).toBeInTheDocument();
    expect(
      screen.getByText(
        "Please connect your account with a GitHub account to continue to Frontiers.",
      ),
    ).toBeInTheDocument();
    expect(
      within(screen.getByTestId("SignInCard-base-github")).getByText("Log In"),
    ).toHaveAttribute("href", "/oauth2/authorization/github");
    expect(screen.getByRole("img")).toHaveAttribute("height", "10em");
    expect(screen.getByRole("img")).toHaveAttribute("width", "10em");
  });
  test("Base environment test", () => {
    axiosMock.onGet("/api/systemInfo").timeout();
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <HomePageConnectGithub />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(screen.getByTestId("HomePageConnectGithub-cardDisplay")).toHaveClass(
      "g-5 justify-content-center align-items-center",
      "d-flex",
      "gap-5",
    );
  });
  test("If systemInfo is not available, use the default githubOauthLogin", async () => {
    axiosMock.onGet("/api/systemInfo").reply(200, undefined);
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <HomePageConnectGithub />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const footer = await screen.findByTestId("SignInCard-footer-github");
    const button = within(footer).getByRole("button", { name: /log in/i });
    expect(button).toHaveAttribute("href", "/oauth2/authorization/github");
  });
});
