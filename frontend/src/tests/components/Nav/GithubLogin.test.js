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
