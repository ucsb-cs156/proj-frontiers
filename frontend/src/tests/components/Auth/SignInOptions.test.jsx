import { render, screen, fireEvent } from "@testing-library/react";
import SignInOptions from "main/components/Auth/SignInOptions";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import { vi } from "vitest";

const mockNavigate = vi.fn();
vi.mock("react-router", async () => {
  const actual = await vi.importActual("react-router");
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

describe("SignInOptions tests", () => {
  const axiosMock = new AxiosMockAdapter(axios);
  const queryClient = new QueryClient();

  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    queryClient.clear();
  });

  test("renders correctly with no login options", async () => {
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.showingNeither);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <SignInOptions />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(screen.getByTestId("SignInOptions-cardDisplay")).toBeInTheDocument();
    expect(screen.queryByText("Sign in with Google")).not.toBeInTheDocument();
    expect(
      screen.queryByText("Sign in with Microsoft"),
    ).not.toBeInTheDocument();
  });

  test("renders correctly with Google login", async () => {
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.showingBoth);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <SignInOptions />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText("Sign in with Google");
    expect(screen.getByTestId("SignInOptions-googleIcon")).toBeInTheDocument();
  });

  test("renders correctly with Microsoft login", async () => {
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.withActiveDirectory);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <SignInOptions />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText("Sign in with Microsoft");
    expect(
      screen.getByTestId("SignInOptions-microsoftIcon"),
    ).toBeInTheDocument();
  });

  test("calls onSignIn when Google button is clicked", async () => {
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.showingBoth);
    const onSignIn = vi.fn();

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <SignInOptions onSignIn={onSignIn} />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText("Sign in with Google");
    const googleButton = screen
      .getByTestId("SignInCard-base-google")
      .querySelector("a");
    fireEvent.click(googleButton);

    expect(onSignIn).toHaveBeenCalled();
  });
});
