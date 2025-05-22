import { render, screen, waitFor } from "@testing-library/react";
import CoursesIndexPage from "main/pages/Courses/CoursesIndexPage";
import { QueryClient, QueryClientProvider } from "react-query";
import { MemoryRouter } from "react-router-dom";
import mockConsole from "jest-mock-console";
import coursesFixtures from "fixtures/coursesFixtures";

import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";

const mockToast = jest.fn();
jest.mock("react-toastify", () => {
  const originalModule = jest.requireActual("react-toastify");
  return {
    __esModule: true,
    ...originalModule,
    toast: (x) => mockToast(x),
  };
});

describe("CoursesIndexPage tests", () => {
  const axiosMock = new AxiosMockAdapter(axios);

  const testId = "CoursesTable";

  const setupAdminUser = () => {
    axiosMock.reset();
    axiosMock.resetHistory();
    axiosMock
      .onGet("/api/currentUser")
      .reply(200, apiCurrentUserFixtures.adminUser);
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.showingNeither);
  };

  const setupNonAdminUser = () => {
    axiosMock.reset();
    axiosMock.resetHistory();
    axiosMock
      .onGet("/api/currentUser")
      .reply(200, apiCurrentUserFixtures.userOnly);
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.showingNeither);
  };
  const queryClient = new QueryClient();

  test("Renders for admin user", async () => {
    setupAdminUser();
    axiosMock.onGet("/api/courses/all").reply(200, []);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <CoursesIndexPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await waitFor(() => {
      expect(screen.getByText(/Courses/)).toBeInTheDocument();
    });
  });

  test("renders three courses correctly for admin user", async () => {
    setupAdminUser();
    axiosMock
      .onGet("/api/courses/all")
      .reply(200, coursesFixtures.threeCourses);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <CoursesIndexPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await waitFor(() => {
      expect(
        screen.getByTestId(`${testId}-cell-row-0-col-id`),
      ).toHaveTextContent("1");
    });
    expect(screen.getByTestId(`${testId}-cell-row-1-col-id`)).toHaveTextContent(
      "2",
    );
    expect(screen.getByTestId(`${testId}-cell-row-2-col-id`)).toHaveTextContent(
      "3",
    );

    const installationId = screen.getByText("654321");
    expect(installationId).toBeInTheDocument();

    const orgName = screen.getByText("wsu-cpts489-fa20");
    expect(orgName).toBeInTheDocument();

    // expect that the button for "Install Github App" is present
    const button = screen.getByTestId(
      "CoursesTable-cell-row-0-col-Install Github App-button",
    );
    expect(button).toBeInTheDocument();
    expect(button).toHaveTextContent("Install Github App");

    // expect that the button for "Roster Students" is present
    const button2 = screen.getByTestId(
      "CoursesTable-cell-row-0-col-Roster Students-button",
    );
    expect(button2).toBeInTheDocument();
    expect(button2).toHaveTextContent("Roster Students");
  });

  test("renders empty table when backend unavailable, admin only", async () => {
    setupAdminUser();

    axiosMock.onGet("/api/courses/all").timeout();

    const restoreConsole = mockConsole();

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <CoursesIndexPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await waitFor(() => {
      expect(axiosMock.history.get.length).toBeGreaterThanOrEqual(1);
    });

    const errorMessage = console.error.mock.calls[0][0];
    expect(errorMessage).toMatch(
      "Error communicating with backend via GET on /api/courses/all",
    );
    restoreConsole();
  });

  test("no button to connect to Github App or Roster Students for non-admin user", async () => {
    setupNonAdminUser();
    axiosMock
      .onGet("/api/courses/all")
      .reply(200, coursesFixtures.threeCourses);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <CoursesIndexPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await waitFor(() => {
      expect(
        screen.getByTestId(`${testId}-cell-row-0-col-id`),
      ).toHaveTextContent("1");
    });

    // expect that the button for "Install Github App" is not present
    const button = screen.queryByTestId(
      "CoursesTable-cell-row-0-col-Install Github App-button",
    );
    expect(button).not.toBeInTheDocument();

    // expect that the button for "Roster Students" is not present
    const button2 = screen.queryByTestId(
      "CoursesTable-cell-row-0-col-Roster Students-button",
    );
    expect(button2).not.toBeInTheDocument();
  });
});
