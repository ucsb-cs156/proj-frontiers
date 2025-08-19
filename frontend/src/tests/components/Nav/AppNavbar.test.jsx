import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router";
import {
  currentUserFixtures,
  currentUserFixturesWithGithub,
} from "fixtures/currentUserFixtures";

import AppNavbar from "main/components/Nav/AppNavbar";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import { vi } from "vitest";

const mockedNavigate = vi.fn();
vi.mock("react-router", async (importOriginal) => ({
  ...(await importOriginal()),
  useNavigate: () => mockedNavigate,
}));

describe("AppNavbar tests", () => {
  const queryClient = new QueryClient();
  const doLogin = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  test("renders correctly for regular logged in user", async () => {
    const currentUser = currentUserFixtures.userOnly;
    const doLogin = vi.fn();

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AppNavbar currentUser={currentUser} doLogin={doLogin} />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText("Welcome, pconrad.cis@gmail.com");
  });

  test("renders correctly for admin user", async () => {
    const currentUser = currentUserFixtures.adminUser;

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AppNavbar currentUser={currentUser} doLogin={doLogin} />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText("Welcome, phtcon@ucsb.edu");
    const adminMenu = screen.getByTestId("appnavbar-admin-dropdown");
    expect(adminMenu).toBeInTheDocument();
  });

  test("renders correctly for instructor user", async () => {
    const currentUser = currentUserFixtures.instructorUser;

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AppNavbar currentUser={currentUser} doLogin={doLogin} />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText("Welcome, diba@ucsb.edu");

    // Admin menu should not be present for instructor
    const adminMenu = screen.queryByTestId("appnavbar-admin-dropdown");
    expect(adminMenu).not.toBeInTheDocument();
  });

  test("renders H2Console and Swagger links correctly", async () => {
    const currentUser = currentUserFixtures.adminUser;
    const systemInfo = systemInfoFixtures.showingBoth;

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AppNavbar
            currentUser={currentUser}
            systemInfo={systemInfo}
            doLogin={doLogin}
          />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText("H2Console");
    const swaggerMenu = screen.getByText("Swagger");
    expect(swaggerMenu).toBeInTheDocument();
  });

  test("renders the AppNavbarLocalhost when on http://localhost:3000", async () => {
    const currentUser = currentUserFixtures.userOnly;
    const systemInfo = systemInfoFixtures.showingBoth;

    delete window.location;
    window.location = new URL("http://localhost:3000");

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AppNavbar
            currentUser={currentUser}
            systemInfo={systemInfo}
            doLogin={doLogin}
          />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByTestId("AppNavbarLocalhost");
    expect(screen.getByTestId("AppNavbarLocalhost-message1").textContent).toBe(
      "Running on http://localhost:3000/ with no backend.",
    );
    expect(screen.getByTestId("AppNavbarLocalhost-message2").textContent).toBe(
      "You probably want http://localhost:8080 instead.",
    );
  });

  test("renders the AppNavbarLocalhost when on http://127.0.0.1:3000", async () => {
    const currentUser = currentUserFixtures.userOnly;
    const systemInfo = systemInfoFixtures.showingBoth;

    delete window.location;
    window.location = new URL("http://127.0.0.1:3000");

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AppNavbar
            currentUser={currentUser}
            systemInfo={systemInfo}
            doLogin={doLogin}
          />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByTestId("AppNavbarLocalhost");
  });

  test("does NOT render the AppNavbarLocalhost when on localhost:8080", async () => {
    const currentUser = currentUserFixtures.userOnly;
    const systemInfo = systemInfoFixtures.showingBoth;

    delete window.location;
    window.location = new URL("http://localhost:8080");

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AppNavbar
            currentUser={currentUser}
            systemInfo={systemInfo}
            doLogin={doLogin}
          />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByTestId("AppNavbar");
    expect(screen.queryByTestId(/AppNavbarLocalhost/i)).toBeNull();
  });

  test("onclick navigates to login", async () => {
    const currentUser = currentUserFixtures.notLoggedIn;
    const systemInfo = systemInfoFixtures.oauthLoginUndefined;

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/example/return"]}>
          <AppNavbar currentUser={currentUser} systemInfo={systemInfo} />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText("Log In");
    fireEvent.click(screen.getByText("Log In"));
    await waitFor(() => expect(mockedNavigate).toHaveBeenCalledWith("/login"));
    expect(sessionStorage.getItem("redirect")).toBe("/example/return");
    sessionStorage.clear();
  });
  test("If user does have ROLE_GITHUB, it does renders the connected to github", async () => {
    const currentUser = currentUserFixturesWithGithub.userOnly;

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AppNavbar currentUser={currentUser} />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(screen.getByText("Github: cgaucho-github")).toBeInTheDocument();
  });

  test("renders Help dropdown for all users", async () => {
    const currentUser = currentUserFixtures.userOnly;

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AppNavbar currentUser={currentUser} doLogin={doLogin} />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const helpMenu = screen.getByTestId("appnavbar-help-dropdown");
    expect(helpMenu).toBeInTheDocument();
    expect(screen.getByText("Help")).toBeInTheDocument();
  });

  test("renders Help dropdown for admin users", async () => {
    const currentUser = currentUserFixtures.adminUser;

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AppNavbar currentUser={currentUser} doLogin={doLogin} />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const helpMenu = screen.getByTestId("appnavbar-help-dropdown");
    expect(helpMenu).toBeInTheDocument();
    expect(screen.getByText("Help")).toBeInTheDocument();
  });

  test("renders Help dropdown for logged out users", async () => {
    const currentUser = currentUserFixtures.notLoggedIn;

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AppNavbar currentUser={currentUser} doLogin={doLogin} />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const helpMenu = screen.getByTestId("appnavbar-help-dropdown");
    expect(helpMenu).toBeInTheDocument();
    expect(screen.getByText("Help")).toBeInTheDocument();
  });
});
