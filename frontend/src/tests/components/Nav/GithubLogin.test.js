import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "react-query";
import { MemoryRouter } from "react-router-dom";
import {
  currentUserFixtures,
  currentUserFixturesWithGithub,
} from "fixtures/currentUserFixtures";

import GithubLogin from "main/components/Nav/GithubLogin";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";

describe("GithubLogin tests", () => {
  const queryClient = new QueryClient();

  const systemInfo = systemInfoFixtures.showingNeither; // Default system info for tests

  test("renders correctly when not logged in to Google", async () => {
    const currentUser = { loggedIn: false };
    const doLogin = jest.fn();

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

  test("renders loading when currentUser is null", async () => {
    const currentUser = null;
    const doLogin = jest.fn();

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

    // Check for the logged-out span
    const loggedOutElement = screen.getByTestId("GithubLogin-logged-out");
    expect(loggedOutElement).toBeInTheDocument();
  });

  test("renders loading when currentUser.root is null", async () => {
    const currentUser = {
      loggedIn: true,
      root: null,
    };
    const doLogin = jest.fn();

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

    // Check for the loading span
    const loadingElement = screen.getByTestId("GithubLogin-loading");
    expect(loadingElement).toBeInTheDocument();
  });

  test("renders loading when currentUser.root.user is null", async () => {
    const currentUser = {
      loggedIn: true,
      root: {
        user: null,
      },
    };
    const doLogin = jest.fn();

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

    // Check for the loading span
    const loadingElement = screen.getByTestId("GithubLogin-loading");
    expect(loadingElement).toBeInTheDocument();
  });

  test("default link is the correct value", async () => {
    const currentUser = currentUserFixtures.userOnly;

    const doLogin = jest.fn();

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

    const doLogin = jest.fn();

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

    const doLogin = jest.fn();

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
    const doLogin = jest.fn();

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

    const doLogin = jest.fn();

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
    const doLogin = jest.fn();

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
});
