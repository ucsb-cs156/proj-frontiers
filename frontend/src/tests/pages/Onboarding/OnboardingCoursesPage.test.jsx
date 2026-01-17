import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import OnboardingCoursesPage from "main/pages/Onboarding/OnboardingCoursesPage";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router";
import { apiCurrentUserFixturesWithGithub } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import coursesFixtures from "fixtures/coursesFixtures";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";

const mockedNavigate = vi.fn();
vi.mock("react-router", async () => {
  const actual = await vi.importActual("react-router");
  return {
    ...actual,
    useNavigate: () => mockedNavigate,
  };
});

describe("OnboardingCoursesPage tests", () => {
  const axiosMock = new AxiosMockAdapter(axios);
  const queryClient = new QueryClient();

  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    queryClient.clear();
    mockedNavigate.mockReset();
  });

  const setUpBaseEnvironment = () => {
    axiosMock
      .onGet("/api/currentUser")
      .reply(200, apiCurrentUserFixturesWithGithub.userOnly);
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.showingNeither);
    axiosMock
      .onGet("/api/courses/list")
      .reply(200, coursesFixtures.oneRosterStudentWithEachStatus);
  };

  test("renders without crashing", async () => {
    setUpBaseEnvironment();
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <OnboardingCoursesPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText("Great! You're on the roster for these courses:");
    expect(screen.getByTestId("OnboardingProgressBar")).toBeInTheDocument();
    expect(
      screen.getByTestId("OnboardingCourses-continueButton"),
    ).toBeInTheDocument();
  });

  test("shows courses table when courses exist", async () => {
    setUpBaseEnvironment();
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <OnboardingCoursesPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByTestId("OnboardingCoursesTable");
    expect(screen.getByTestId("OnboardingCoursesTable")).toBeInTheDocument();
  });

  test("shows message when no courses", async () => {
    axiosMock
      .onGet("/api/currentUser")
      .reply(200, apiCurrentUserFixturesWithGithub.userOnly);
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.showingNeither);
    axiosMock.onGet("/api/courses/list").reply(200, []);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <OnboardingCoursesPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText("You are not enrolled in any student courses yet.");
  });

  test("clicking continue navigates to complete page", async () => {
    setUpBaseEnvironment();
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <OnboardingCoursesPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText("Great! You're on the roster for these courses:");

    const continueButton = screen.getByTestId(
      "OnboardingCourses-continueButton",
    );
    fireEvent.click(continueButton);

    await waitFor(() => {
      expect(mockedNavigate).toHaveBeenCalledWith("/onboarding/complete");
    });
  });
});
