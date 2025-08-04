import { render, screen, waitFor } from "@testing-library/react";
import CoursesIndexPage from "main/pages/Instructors/CoursesIndexPage";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import mockConsole from "jest-mock-console";
import coursesFixtures from "fixtures/coursesFixtures";

import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";

let axiosMock = new AxiosMockAdapter(axios);

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
  const testId = "InstructorCoursesTable";

  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    queryClient.clear();
  });

  const setupAdminUser = () => {
    axiosMock
      .onGet("/api/currentUser")
      .reply(200, apiCurrentUserFixtures.adminUser);
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.showingNeither);
  };

  const queryClient = new QueryClient();

  test("Renders for admin user", async () => {
    setupAdminUser();
    axiosMock.onGet("/api/courses/allForAdmins").reply(200, []);

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

  test("renders correctly for admin user", async () => {
    setupAdminUser();
    axiosMock
      .onGet("/api/courses/allForAdmins")
      .reply(200, coursesFixtures.severalCourses);

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

    const orgName = screen.getByText("wsu-cpts489-fa20");
    expect(orgName).toBeInTheDocument();

    // For an admin user, the next two courses should have a button, not an org name
    const button3 = screen.queryByTestId(
      "InstructorCoursesTable-cell-row-2-col-orgName-button",
    );
    expect(button3).toBeInTheDocument();
    expect(button3).toHaveTextContent("Install GitHub App");

    const button4 = screen.queryByTestId(
      "InstructorCoursesTable-cell-row-3-col-orgName-button",
    );
    expect(button4).toBeInTheDocument();
    expect(button4).toHaveTextContent("Install GitHub App");
  });

  test("renders empty table when backend unavailable, admin only", async () => {
    setupAdminUser();

    axiosMock.onGet("/api/courses/allForAdmins").timeout();

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
      "Error communicating with backend via GET on /api/courses/allForAdmins",
    );
    restoreConsole();
  });
});
