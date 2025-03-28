import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "react-query";
import { MemoryRouter } from "react-router-dom";
import { currentUserFixtures } from "fixtures/currentUserFixtures";

import GoogleLogin from "main/components/Nav/GoogleLogin";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";

describe("GoogleLogin tests", () => {
  const queryClient = new QueryClient();

  const systemInfo = systemInfoFixtures.showingNeither; // Default system info for tests

  test("renders correctly for regular logged in user", async () => {
    const currentUser = currentUserFixtures.userOnly;

    const doLogin = jest.fn();

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <GoogleLogin currentUser={currentUser} systemInfo={systemInfo} doLogin={doLogin} />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText("Welcome, pconrad.cis@gmail.com");
  });

  test("renders correctly for admin user", async () => {
    const currentUser = currentUserFixtures.adminUser;
    const doLogin = jest.fn();

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <GoogleLogin currentUser={currentUser} systemInfo={systemInfo} doLogin={doLogin} />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText("Welcome, phtcon@ucsb.edu");
  });

  test("renders login button when not logged in", async () => {
    const currentUser = { loggedIn: false };
    const doLogin = jest.fn();

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <GoogleLogin currentUser={currentUser} systemInfo={systemInfo} doLogin={doLogin} />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    // Check for the login button
    const loginButton = screen.getByText("Log In");
    expect(loginButton).toBeInTheDocument();
    expect(loginButton).toHaveAttribute("href", "/oauth2/authorization/google");
  });
});
