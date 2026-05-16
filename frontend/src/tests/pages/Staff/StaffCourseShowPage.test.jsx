import {
  act,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import StaffCourseShowPage from "main/pages/Staff/StaffCourseShowPage";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router";
import coursesFixtures from "fixtures/coursesFixtures";
import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import { courseStaffFixtures } from "fixtures/courseStaffFixtures";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import { expect, vi } from "vitest";

const mockedNavigate = vi.fn();
vi.mock("react-router", async (importOriginal) => ({
  ...(await importOriginal()),
  useNavigate: () => mockedNavigate,
}));

const axiosMock = new AxiosMockAdapter(axios);

describe("StaffCourseShowPage tests", () => {
  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    mockedNavigate.mockClear();
    axiosMock.onGet("/api/courses/warnings/7").reply(200, {
      showOrganizationAgeWarning: false,
    });
    axiosMock.onGet("/api/rosterstudents/course/7").reply(200, []);
    axiosMock.onGet("/api/teams/all?courseId=7").reply(200, []);
    axiosMock.onGet("/api/jobs/course").reply(200, []);
  });

  const renderPage = () => {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });

    render(
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

  const setupStaffUser = () => {
    axiosMock
      .onGet("/api/currentUser")
      .reply(200, apiCurrentUserFixtures.userOnly);
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.showingNeither);
  };

  test("renders course details with staff tabs and hides settings", async () => {
    setupStaffUser();
    axiosMock
      .onGet("/api/courses/7")
      .reply(200, coursesFixtures.severalCourses[0]);
    axiosMock.onGet("/api/coursestaff/course?courseId=7").reply(200, []);

    renderPage();

    await waitFor(() => {
      expect(screen.getByTestId("StaffCourseShowPage-title")).toHaveTextContent(
        "CMPSC 156",
      );
    });

    expect(screen.getByRole("tab", { name: "Students" })).toBeInTheDocument();
    expect(screen.getByRole("tab", { name: "Staff" })).toBeInTheDocument();
    expect(screen.getByRole("tab", { name: "Teams" })).toBeInTheDocument();
    expect(
      screen.getByRole("tab", { name: "Assignments" }),
    ).toBeInTheDocument();
    expect(screen.getByRole("tab", { name: "Jobs" })).toBeInTheDocument();
    expect(
      screen.queryByRole("tab", { name: "Settings" }),
    ).not.toBeInTheDocument();

    expect(
      screen.getByTestId("StaffCourseShowPage-github-org-image"),
    ).toHaveAttribute("src", "https://github.com/ucsb-cs156-s25.png?size=64");
    expect(
      screen.getByTestId("StaffCourseShowPage-github-org-link"),
    ).toHaveAttribute("href", "https://github.com/ucsb-cs156-s25");
    expect(
      screen.getByTestId("StaffCourseShowPage-github-settings-link"),
    ).toHaveAttribute(
      "href",
      "https://github.com/organizations/ucsb-cs156-s25/settings/installations/123456",
    );

    fireEvent.mouseOver(
      screen.getByTestId("StaffCourseShowPage-github-settings-icon"),
    );
    expect(await screen.findByRole("tooltip")).toHaveTextContent(
      "Manage settings for association between your GitHub organization and this web application.",
    );
  });

  test("renders course details when course has no GitHub org", async () => {
    setupStaffUser();
    axiosMock
      .onGet("/api/courses/7")
      .reply(200, coursesFixtures.severalCourses[2]);
    axiosMock.onGet("/api/coursestaff/course?courseId=7").reply(200, []);

    renderPage();

    await waitFor(() => {
      expect(screen.getByTestId("StaffCourseShowPage-title")).toHaveTextContent(
        "CMPSC 156",
      );
    });

    expect(
      screen.queryByTestId("StaffCourseShowPage-github-org-image"),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByTestId("StaffCourseShowPage-github-org-link"),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByTestId("StaffCourseShowPage-github-settings-link"),
    ).not.toBeInTheDocument();
  });

  test("does not allow staff users to add, edit, or delete staff", async () => {
    setupStaffUser();
    axiosMock
      .onGet("/api/courses/7")
      .reply(200, coursesFixtures.severalCourses[0]);
    axiosMock
      .onGet("/api/coursestaff/course?courseId=7")
      .reply(200, courseStaffFixtures.threeStaff);

    renderPage();

    await screen.findByTestId("StaffCourseShowPage-CourseStaffTable");

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

  test("returns to home page when course lookup fails", async () => {
    vi.useFakeTimers({
      shouldAdvanceTime: true,
      toFake: ["setTimeout", "clearTimeout"],
    });
    setupStaffUser();
    axiosMock.onGet("/api/courses/7").timeout();
    axiosMock.onGet("/api/coursestaff/course?courseId=7").reply(200, []);

    renderPage();

    expect(screen.getByTestId("StaffCourseShowPage-loading")).toHaveTextContent(
      "Course: Loading...",
    );

    await screen.findByText(
      "Course not found. You will be returned to the course list in 3 seconds.",
    );
    expect(screen.getByText("Course Not Found")).toBeInTheDocument();

    fireEvent.click(screen.getByText("Close"));
    await waitFor(() =>
      expect(screen.queryByText("Course Not Found")).not.toBeInTheDocument(),
    );

    act(() => {
      vi.advanceTimersByTime(3000);
    });

    expect(mockedNavigate).toHaveBeenCalledWith("/", { replace: true });
    vi.useRealTimers();
  });
});
