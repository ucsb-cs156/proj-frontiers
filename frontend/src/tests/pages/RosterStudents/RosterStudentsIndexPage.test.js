import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import RosterStudentsIndexPage from "main/pages/RosterStudents/RosterStudentsIndexPage";
import { QueryClient, QueryClientProvider } from "react-query";
import { MemoryRouter } from "react-router-dom";
import { Route, Routes } from "react-router-dom";
import rosterStudentsFixtures from "fixtures/rosterStudentsFixtures";

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

describe("RosterStudentsIndexPage tests", () => {
  const axiosMock = new AxiosMockAdapter(axios);
  const courseId = 1;

  const testId = "RosterStudentsTable";

  // Only admins can access this roster students, do not need to test for non admin users

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

  const queryClient = new QueryClient();

  test("Renders with Create Button for admin user", async () => {
    setupAdminUser();
    axiosMock
      .onGet("/api/rosterstudents/course", { params: { courseId: "1" } })
      .reply(200, rosterStudentsFixtures.threeRosterStudents);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter
          initialEntries={[`/admin/courses/${courseId}/roster_students`]}
        >
          <Routes>
            <Route
              path="/admin/courses/:courseId/roster_students"
              element={<RosterStudentsIndexPage />}
            />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await waitFor(() => {
      expect(screen.getByText(/Create Roster Student/)).toBeInTheDocument();
    });
    const button = screen.getByText(/Create Roster Student/);
    expect(button).toHaveAttribute("href", "roster_students/new");
    expect(button).toHaveAttribute("style", "float: right;");
  });

  test("renders three rosterStudents correctly for admin user", async () => {
    setupAdminUser();
    axiosMock
      .onGet("/api/rosterstudents/course", { params: { courseId: "1" } })
      .reply(200, rosterStudentsFixtures.threeRosterStudents);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter
          initialEntries={[`/admin/courses/${courseId}/roster_students`]}
        >
          <Routes>
            <Route
              path="/admin/courses/:courseId/roster_students"
              element={<RosterStudentsIndexPage />}
            />
          </Routes>
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

    const createRosterStudentsButton = screen.queryByText(
      "Create Roster Student",
    );
    expect(createRosterStudentsButton).toBeInTheDocument();

    expect(
      screen.getByTestId("RosterStudentsTable-cell-row-0-col-Delete-button"),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId("RosterStudentsTable-cell-row-0-col-Edit-button"),
    ).toBeInTheDocument();
  });

  test("what happens when you click delete, admin", async () => {
    setupAdminUser();

    axiosMock
      .onGet("/api/rosterstudents/course", { params: { courseId: "1" } })
      .reply(200, rosterStudentsFixtures.threeRosterStudents);

    axiosMock
      .onDelete("/api/rosterstudents")
      .reply(200, "RosterStudent with id 1 was deleted");

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter
          initialEntries={[`/admin/courses/${courseId}/roster_students`]}
        >
          <Routes>
            <Route
              path="/admin/courses/:courseId/roster_students"
              element={<RosterStudentsIndexPage />}
            />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await waitFor(() => {
      expect(
        screen.getByTestId(`${testId}-cell-row-0-col-id`),
      ).toBeInTheDocument();
    });

    expect(screen.getByTestId(`${testId}-cell-row-0-col-id`)).toHaveTextContent(
      "1",
    );

    const deleteButton = screen.getByTestId(
      `${testId}-cell-row-0-col-Delete-button`,
    );
    expect(deleteButton).toBeInTheDocument();

    fireEvent.click(deleteButton);

    await waitFor(() => {
      expect(mockToast).toHaveBeenCalledWith(
        "RosterStudent with id 1 was deleted",
      );
    });

    await waitFor(() => {
      expect(axiosMock.history.delete.length).toBe(1);
    });
    expect(axiosMock.history.delete[0].url).toBe("/api/rosterstudents");
    expect(axiosMock.history.delete[0].url).toBe("/api/rosterstudents");
    expect(axiosMock.history.delete[0].params).toEqual({ id: 1 });
  });
});
