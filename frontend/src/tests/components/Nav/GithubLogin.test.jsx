import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router";
import {
  currentUserFixtures,
  currentUserFixturesWithGithub,
} from "fixtures/currentUserFixtures";

import GithubLogin from "main/components/Nav/GithubLogin";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import { vi } from "vitest";

describe("GithubLogin tests", () => {
  const queryClient = new QueryClient();

  const systemInfo = systemInfoFixtures.showingNeither; // Default system info for tests

  const doLogin = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  test("renders correctly when not logged in to Google", async () => {
    const currentUser = { loggedIn: false };

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <GithubLogin
            currentUser={currentUser}
            systemInfo={systemInfo}
            doLogin={doLogin}
          />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    // Check for the login button
    const loginButton = screen.getByTestId("GithubLogin-logged-out");
    expect(loginButton).toBeInTheDocument();
  });

  test("default link is the correct value", async () => {
    const currentUser = currentUserFixtures.userOnly;

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <GithubLogin
            currentUser={currentUser}
            systemInfo={systemInfo}
            doLogin={doLogin}
          />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText("Connect Github");
    // Check if the button is rendered with the overridden link
    const loginButton = screen.getByRole("button", {
      name: "Connect Github",
    });
    expect(loginButton).toBeInTheDocument();
    // Ensure the login button has the correct href
    expect(loginButton).toHaveAttribute("href", "/oauth2/authorization/github"); // This checks if the overridden URL is used
  });

  test("link can be overridden via systemInfo", async () => {
    const currentUser = currentUserFixtures.userOnly;

    const overriddenSystemInfo = {
      ...systemInfo, // Spread the existing system info
      githubOauthLogin: "/oauth2/authorization/custom-github", // Override the oauth login URL
    };

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <GithubLogin
            currentUser={currentUser}
            systemInfo={overriddenSystemInfo}
            doLogin={doLogin}
          />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText("Connect Github");
    // Check if the button is rendered with the overridden link
    const loginButton = screen.getByRole("button", {
      name: "Connect Github",
    });
    expect(loginButton).toBeInTheDocument();
    // Ensure the login button has the correct href
    expect(loginButton).toHaveAttribute(
      "href",
      "/oauth2/authorization/custom-github",
    ); // This checks if the overridden URL is used
  });

  test("renders correctly for user logged into Google but not Github", async () => {
    const currentUser = currentUserFixtures.userOnly;

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <GithubLogin
            currentUser={currentUser}
            systemInfo={systemInfo}
            doLogin={doLogin}
          />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText("Connect Github");
  });

  test("renders correctly for admin logged into Google but not Github", async () => {
    const currentUser = currentUserFixtures.adminUser;

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <GithubLogin
            currentUser={currentUser}
            systemInfo={systemInfo}
            doLogin={doLogin}
          />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText("Connect Github");
  });

  test("renders correctly for user logged into Google and Github", async () => {
    const currentUser = currentUserFixturesWithGithub.userOnly;

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <GithubLogin
            currentUser={currentUser}
            systemInfo={systemInfo}
            doLogin={doLogin}
          />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText("Github: cgaucho-github");
  });

  test("renders correctly for admin logged into Google and Github", async () => {
    const currentUser = currentUserFixturesWithGithub.adminUser;

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <GithubLogin
            currentUser={currentUser}
            systemInfo={systemInfo}
            doLogin={doLogin}
          />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText("Github: phtcon-github");
  });
  test("If systemInfo is not available, use the default githubOauthLogin", async () => {
    const currentUser = currentUserFixtures.userOnly;

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <GithubLogin
            currentUser={currentUser}
            systemInfo={undefined}
            doLogin={doLogin}
          />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const loginButton = await screen.findByRole("button", {
      name: "Connect Github",
    });
    expect(loginButton).toBeInTheDocument();
    expect(loginButton).toHaveAttribute("href", "/oauth2/authorization/github");
  });
});
