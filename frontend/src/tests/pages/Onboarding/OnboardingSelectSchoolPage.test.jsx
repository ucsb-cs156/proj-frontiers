import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import AxiosMockAdapter from "axios-mock-adapter";
import axios from "axios";
import { render } from "@testing-library/react";
import OnboardingSelectSchoolPage from "main/pages/Onboarding/OnboardingSelectSchoolPage";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import { fireEvent, screen, within } from "@testing-library/react";
import { MemoryRouter } from "react-router";

const queryClient = new QueryClient();
const axiosMock = new AxiosMockAdapter(axios);

describe("OnboardingSelectSchoolPage tests", () => {
  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    queryClient.clear();
  });

  test("renders without crashing", async () => {
    axiosMock.onGet("/api/systemInfo").timeout();

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <OnboardingSelectSchoolPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText(/We need a bit of information from you/);
  });

  test("selecting a Google school works", async () => {
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.withActiveDirectoryAndGoogle);
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/onboarding/select-school"]}>
          <OnboardingSelectSchoolPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText(/We need a bit of information from you/);
    const typeahead = screen.getByTestId("SelectSchool-typeahead");
    fireEvent.change(typeahead, {
      target: { value: "University of California, Santa Barbara" },
    });
    fireEvent.click(
      screen.getByText("University of California, Santa Barbara"),
    );

    await screen.findByText(/Sign in with Google/);
    const googleButton = within(
      screen.getByTestId("SignInCard-base-google"),
    ).getByText("Log In");
    fireEvent.click(googleButton);
    expect(sessionStorage.getItem("redirect")).toBe("/onboarding/courses");
    sessionStorage.clear();
    expect(screen.getByTestId("SignInOptions-googleIcon")).toBeInTheDocument();
    expect(screen.getByRole("img")).toHaveAttribute("height", "10em");
    expect(screen.getByRole("img")).toHaveAttribute("width", "10em");
  });
  test("selecting a Microsoft school works", async () => {
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.withActiveDirectoryAndGoogle);
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/onboarding/select-school"]}>
          <OnboardingSelectSchoolPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText(/We need a bit of information from you/);
    const typeahead = screen.getByTestId("SelectSchool-typeahead");
    fireEvent.change(typeahead, {
      target: { value: "Oregon State University" },
    });
    fireEvent.click(screen.getByText("Oregon State University"));

    await screen.findByText(/Sign in with Microsoft/);
    const googleButton = within(
      screen.getByTestId("SignInCard-base-microsoft"),
    ).getByText("Log In");
    fireEvent.click(googleButton);
    expect(sessionStorage.getItem("redirect")).toBe("/onboarding/github");
    sessionStorage.clear();
    expect(
      screen.getByTestId("SignInOptions-microsoftIcon"),
    ).toBeInTheDocument();
    expect(screen.getByRole("img")).toHaveAttribute("height", "10em");
    expect(screen.getByRole("img")).toHaveAttribute("width", "10em");
  });

  test("No setup AD displays warning", async () => {
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.showingBoth);
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/onboarding/select-school"]}>
          <OnboardingSelectSchoolPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText(/We need a bit of information from you/);
    const typeahead = screen.getByTestId("SelectSchool-typeahead");
    fireEvent.change(typeahead, {
      target: { value: "Oregon State University" },
    });
    fireEvent.click(screen.getByText("Oregon State University"));

    await screen.findByText(/Please contact your instructor/);
  });

  test("No setup Google displays warning", async () => {
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.oauthLoginUndefined);
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/onboarding/select-school"]}>
          <OnboardingSelectSchoolPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText(/We need a bit of information from you/);
    const typeahead = screen.getByTestId("SelectSchool-typeahead");
    fireEvent.change(typeahead, {
      target: { value: "University of California, Santa Barbara" },
    });
    fireEvent.click(
      screen.getByText("University of California, Santa Barbara"),
    );

    await screen.findByText(/Please contact your instructor/);
  });

  test("cleanup assertions", async () => {
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.showingBoth);
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/onboarding/select-school"]}>
          <OnboardingSelectSchoolPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText(/We need a bit of information from you/);
    const typeahead = screen.getByLabelText("Choose a school");
    fireEvent.change(typeahead, {
      target: { value: "University of California, Santa Barbara" },
    });
    fireEvent.click(
      screen.getByText("University of California, Santa Barbara"),
    );
    await screen.findByText(/Sign in with Google/);
    fireEvent.change(typeahead, {
      target: { value: "nonexistent" },
    });

    //ensure that it doesn't crash
  });
});
