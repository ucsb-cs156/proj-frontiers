import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import StaffCourseShowPage from "main/pages/Staff/StaffCourseShowPage";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router";
import coursesFixtures from "fixtures/coursesFixtures";

import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import { rosterStudentFixtures } from "fixtures/rosterStudentFixtures";
import { courseStaffFixtures } from "fixtures/courseStaffFixtures";
import { teamsFixtures } from "fixtures/TeamsFixtures";
import { expect, vi } from "vitest";

const mockedNavigate = vi.fn();
vi.mock("react-router", async (importOriginal) => ({
  ...(await importOriginal()),
  useNavigate: () => mockedNavigate,
}));

const axiosMock = new AxiosMockAdapter(axios);
const queryClient = new QueryClient();

describe("StaffCourseShowPage tests", () => {
  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    queryClient.clear();
    mockedNavigate.mockClear();
    axiosMock
      .onGet("/api/currentUser")
      .reply(200, apiCurrentUserFixtures.userOnly);
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.showingNeither);
    axiosMock.onGet("/api/jobs/course").reply(200, []);
    axiosMock.onGet("/api/courses/warnings/7").reply(200, {
      showOrganizationAgeWarning: false,
    });
    axiosMock
      .onGet("/api/rosterstudents/course/7")
      .reply(200, rosterStudentFixtures.threeStudents);
    axiosMock
      .onGet("/api/coursestaff/course?courseId=7")
      .reply(200, courseStaffFixtures.staffWithEachStatus);
    axiosMock
      .onGet("/api/teams/all?courseId=7")
      .reply(200, teamsFixtures.teams);
  });

  const renderPage = () => {
    return render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/staff/courses/7"]}>
          <Routes>
            <Route
              path="/staff/courses/:id"
              element={<StaffCourseShowPage />}
            />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );
  };

  test("renders course header and staff-appropriate tabs", async () => {
    axiosMock.onGet("/api/courses/7").reply(200, {
      ...coursesFixtures.oneCourseWithEachStatus[0],
      id: 7,
      installationId: "123456789",
    });

    renderPage();

    await waitFor(() => {
      expect(screen.getByTestId("StaffCourseShowPage-title")).toHaveTextContent(
        "CMPSC 156",
      );
    });

    expect(screen.getByRole("tab", { name: "Students" })).toHaveAttribute(
      "data-rr-ui-event-key",
      "students",
    );
    expect(screen.getByRole("tab", { name: "Staff" })).toHaveAttribute(
      "data-rr-ui-event-key",
      "staff",
    );
    expect(screen.getByRole("tab", { name: "Teams" })).toHaveAttribute(
      "data-rr-ui-event-key",
      "teams",
    );
    expect(screen.getByRole("tab", { name: "Assignments" })).toHaveAttribute(
      "data-rr-ui-event-key",
      "default",
    );
    expect(screen.getByRole("tab", { name: "Jobs" })).toHaveAttribute(
      "data-rr-ui-event-key",
      "jobs",
    );
    expect(
      screen.queryByRole("tab", { name: "Settings" }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByTestId("StaffCourseShowPage-canvasForm"),
    ).not.toBeInTheDocument();
    expect(screen.queryByText("Delete Course")).not.toBeInTheDocument();
  });

  test("returns to course list when course lookup fails", async () => {
    vi.useFakeTimers({
      shouldAdvanceTime: true,
      toFake: ["setTimeout", "clearTimeout"],
    });
    axiosMock.onGet("/api/courses/7").timeout();

    const { unmount } = renderPage();

    expect(await screen.findByText("Course Not Found")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Close" }));
    vi.advanceTimersByTime(3000);
    expect(mockedNavigate).toHaveBeenCalledWith("/", { replace: true });

    unmount();
    vi.useRealTimers();
  });

  test("staff tab is visible but does not allow staff add edit or delete", async () => {
    axiosMock
      .onGet("/api/courses/7")
      .reply(200, coursesFixtures.oneCourseWithEachStatus[0]);
    renderPage();

    fireEvent.click(await screen.findByRole("tab", { name: "Staff" }));

    await waitFor(() => {
      expect(
        screen.getByTestId(
          "StaffCourseShowPage-CourseStaffTable-cell-row-0-col-id",
        ),
      ).toBeInTheDocument();
    });

    expect(screen.queryByText("Add Staff Member")).not.toBeInTheDocument();
    expect(
      screen.queryByTestId(
        "StaffCourseShowPage-CourseStaffTable-cell-row-0-col-Edit-button",
      ),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByTestId(
        "StaffCourseShowPage-CourseStaffTable-cell-row-0-col-Delete-button",
      ),
    ).not.toBeInTheDocument();
  });

  test("staff users can still manage students and teams", async () => {
    axiosMock
      .onGet("/api/courses/7")
      .reply(200, coursesFixtures.oneCourseWithEachStatus[0]);
    axiosMock
      .onGet("/api/rosterstudents/course/7")
      .reply(200, rosterStudentFixtures.threeStudents);
    axiosMock
      .onGet("/api/teams/all?courseId=7")
      .reply(200, teamsFixtures.teams);

    renderPage();

    fireEvent.click(await screen.findByRole("tab", { name: "Students" }));
    await waitFor(() => {
      expect(
        screen.getByTestId(
          "StaffCourseShowPage-RosterStudentTable-cell-row-0-col-Edit-button",
        ),
      ).toBeInTheDocument();
    });
    expect(
      screen.getByTestId(
        "StaffCourseShowPage-RosterStudentTable-cell-row-0-col-Delete-button",
      ),
    ).toBeInTheDocument();

    fireEvent.click(screen.getByRole("tab", { name: "Teams" }));
    await waitFor(() => {
      expect(
        screen.getByTestId(
          "StaffCourseShowPage-teams-table-3-add-member-button",
        ),
      ).toBeInTheDocument();
    });
    expect(
      screen.getByTestId("StaffCourseShowPage-teams-table-3-delete-button"),
    ).toBeInTheDocument();
  });
});
