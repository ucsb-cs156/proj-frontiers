// frontend/src/tests/components/Auth/SignInCardDisplay.test.jsx
import AxiosMockAdapter from "axios-mock-adapter";
import axios from "axios";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import { fireEvent, render, screen, within } from "@testing-library/react";
import SignInCardDisplay from "main/components/Auth/SignInCardDisplay";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";

const axiosMock = new AxiosMockAdapter(axios);
const queryClient = new QueryClient();

describe("SignInCardDisplay Tests", () => {
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
        <SignInCardDisplay />
      </QueryClientProvider>,
    );

    await screen.findByText("Sign in with Google");
    expect(screen.getByTestId("SignInPage-googleIcon")).toBeInTheDocument();
    expect(screen.getByText("Sign in with Google")).toBeInTheDocument();
    expect(
      screen.getByText(
        "If you have credentials with these schools, sign in with Google",
      ),
    ).toBeInTheDocument();
    expect(screen.getByText("Chico State University")).toBeInTheDocument();
    expect(
      screen.getByText("University of California, Santa Barbara"),
    ).toBeInTheDocument();
    expect(
      within(screen.getByTestId("SignInCard-base-google")).getByText("Log In"),
    ).toHaveAttribute("href", "/oauth2/authorization/google");
  });

  test("Renders Microsoft card with activeDirectoryUrl present", async () => {
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.withActiveDirectory);
    render(
      <QueryClientProvider client={queryClient}>
        <SignInCardDisplay />
      </QueryClientProvider>,
    );

    await screen.findByText("Sign in with Microsoft");
    expect(screen.getByTestId("SignInPage-microsoftIcon")).toBeInTheDocument();
    expect(screen.getByTestId("SignInCard-base-microsoft")).toBeInTheDocument();
    expect(screen.getByText("Sign in with Microsoft")).toBeInTheDocument();
    expect(
      screen.getByText(
        "If you have credentials with these schools, sign in with Microsoft",
      ),
    ).toBeInTheDocument();
    expect(screen.getByText("Oregon State University")).toBeInTheDocument();
    expect(
      within(screen.getByTestId("SignInCard-base-microsoft")).getByText(
        "Log In",
      ),
    ).toHaveAttribute("href", "/oauth2/authorization/azure-dev");
  });

  test("Renders alert when alertMessage prop is provided", () => {
    axiosMock.onGet("/api/systemInfo").timeout();
    render(
      <QueryClientProvider client={queryClient}>
        <SignInCardDisplay alertMessage="Test alert message" />
      </QueryClientProvider>,
    );

    expect(screen.getByText("Test alert message")).toBeInTheDocument();
    expect(screen.getByText("Test alert message")).toHaveClass("alert-danger");
  });

  test("Does not render alert when alertMessage prop is not provided", () => {
    axiosMock.onGet("/api/systemInfo").timeout();
    render(
      <QueryClientProvider client={queryClient}>
        <SignInCardDisplay />
      </QueryClientProvider>,
    );

    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  test("Calls onClick handler when provided and button clicked", async () => {
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.showingNeither);

    const mockOnClick = vi.fn();

    render(
      <QueryClientProvider client={queryClient}>
        <SignInCardDisplay onClick={mockOnClick} />
      </QueryClientProvider>,
    );

    await screen.findByText("Sign in with Google");
    const button = within(
      screen.getByTestId("SignInCard-base-google"),
    ).getByText("Log In");

    fireEvent.click(button);
    expect(mockOnClick).toHaveBeenCalledTimes(1);
  });

  test("Renders card display container with correct classes", () => {
    axiosMock.onGet("/api/systemInfo").timeout();
    render(
      <QueryClientProvider client={queryClient}>
        <SignInCardDisplay />
      </QueryClientProvider>,
    );

    expect(screen.getByTestId("SignInPage-cardDisplay")).toHaveClass(
      "g-5",
      "justify-content-center",
      "align-items-center",
      "d-flex",
      "gap-5",
    );
  });
});
