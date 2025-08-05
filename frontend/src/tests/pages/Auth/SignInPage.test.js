import AxiosMockAdapter from "axios-mock-adapter";
import axios from "axios";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import { render, screen, within } from "@testing-library/react";
import SignInPage from "main/pages/Auth/SignInPage";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router";
import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";

const axiosMock = new AxiosMockAdapter(axios);
const queryClient = new QueryClient();
describe("SignInPage Tests", () => {
  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    queryClient.clear();
    axiosMock
      .onGet("/api/currentUser")
      .reply(200, apiCurrentUserFixtures.userOnly);
  });

  test("Renders Google card with oauthLogin present", async () => {
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.showingNeither);
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <SignInPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText("Sign in with Google");
    expect(screen.getByTestId("SignInPage-googleIcon")).toBeInTheDocument();
    expect(screen.getByText("Sign in with Google")).toBeInTheDocument();
    expect(
      screen.getByText(
        "If you have Chico State or University of California-Santa Barbara login credentials, sign in with Google",
      ),
    ).toBeInTheDocument();
    expect(
      within(screen.getByTestId("SignInCard-base-google")).getByText("Log In"),
    ).toHaveAttribute("href", "/oauth2/authorization/google");
    expect(screen.getByRole("img")).toHaveAttribute("height", "10em");
    expect(screen.getByRole("img")).toHaveAttribute("width", "10em");
  });

  test("Renders Active Directory card with activeDirectoryUrl present", async () => {
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.withActiveDirectory);
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <SignInPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );
    await screen.findByText("Sign in with Microsoft");
    expect(screen.getByTestId("SignInPage-microsoftIcon")).toBeInTheDocument();
    expect(screen.getByTestId("SignInCard-base-microsoft")).toBeInTheDocument();
    expect(screen.getByText("Sign in with Microsoft")).toBeInTheDocument();
    expect(
      screen.getByText(
        "If you have Oregon State University login credentials, sign in with Microsoft.",
      ),
    ).toBeInTheDocument();
    expect(
      within(screen.getByTestId("SignInCard-base-microsoft")).getByText(
        "Log In",
      ),
    ).toHaveAttribute("href", "/oauth2/authorization/azure-dev");
    expect(screen.getByRole("img")).toHaveAttribute("height", "10em");
    expect(screen.getByRole("img")).toHaveAttribute("width", "10em");
  });

  test("Base environment test", () => {
    axiosMock.onGet("/api/systemInfo").timeout();
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <SignInPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(screen.getByTestId("SignInPage-cardDisplay")).toHaveClass(
      "g-5 justify-content-center align-items-center",
      "d-flex",
      "gap-5",
    );
  });
});
