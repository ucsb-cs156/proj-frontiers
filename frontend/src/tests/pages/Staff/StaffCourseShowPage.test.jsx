import {
  act,
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import StaffCourseShowPage from "main/pages/Staff/StaffCourseShowPage";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router";
import coursesFixtures from "fixtures/coursesFixtures";

import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import { expect, vi } from "vitest";

const mockedNavigate = vi.fn();
vi.mock("react-router", async (importOriginal) => ({
  ...(await importOriginal()),
  useNavigate: () => mockedNavigate,
}));
const axiosMock = new AxiosMockAdapter(axios);
const queryClient = new QueryClient();

const mockToast = vi.fn();
vi.mock("react-toastify", async (importOriginal) => {
  return {
    ...(await importOriginal()),
    toast: (x) => mockToast(x),
  };
});

describe("StaffCourseShowPage tests", () => {
  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    queryClient.clear();
    axiosMock.onGet("/api/jobs/course").reply(200, []);
  });

  const setupInstructorUser = () => {
    axiosMock
      .onGet("/api/currentUser")
      .reply(200, apiCurrentUserFixtures.instructorUser);
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.showingNeither);
  };

  const setupUserOnly = () => {
    axiosMock
      .onGet("/api/currentUser")
      .reply(200, apiCurrentUserFixtures.userOnly);
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.showingNeither);
  };

  test("renders correctly for instructor user", async () => {
    vi.useFakeTimers({
      shouldAdvanceTime: true,
      toFake: ["setTimeout", "clearTimeout"],
    });
    setupInstructorUser();
    const theCourse = {
      ...coursesFixtures.oneCourseWithEachStatus[0],
      id: 7,
    };
    axiosMock.onGet("/api/courses/7").reply(200, theCourse);

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

    const testId = "StaffCourseShowPage";

    await waitFor(() => {
      expect(screen.getByTestId(`${testId}-title`)).toHaveTextContent(
        "CMPSC 156",
      );
    });

    expect(screen.queryByText("Course Not Found")).not.toBeInTheDocument();
    vi.advanceTimersByTime(3000);
    expect(mockedNavigate).not.toHaveBeenCalled();
    vi.useRealTimers();
  });

  test("Returns to course page on timeout", async () => {
    vi.useFakeTimers({
      shouldAdvanceTime: true,
      toFake: ["setTimeout", "clearTimeout"],
    });
    axiosMock.onGet("/api/courses/7").timeout();
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

    expect(queryClient.getQueryData(["/api/courses/7"])).toBe(null);
    const testId = "StaffCourseShowPage";

    await screen.findByTestId(`${testId}-loading`);

    const loadingDiv = screen.getByTestId(`${testId}-loading`);
    expect(loadingDiv).toHaveTextContent("Course: Loading...");

    await screen.findByText(
      "Course not found. You will be returned to the course list in 3 seconds.",
    );
    expect(mockToast).not.toHaveBeenCalled();
    expect(screen.getByText("Course Not Found")).toBeInTheDocument();
    expect(screen.getByText("Close")).toHaveClass("btn-primary");
    fireEvent.click(screen.getByText("Close"));
    await waitFor(() =>
      expect(screen.queryByText("Course Not Found")).not.toBeInTheDocument(),
    );
    act(() => {
      vi.advanceTimersByTime(5000);
    });
    expect(mockedNavigate).toHaveBeenCalledWith("/", {
      replace: true,
    });
    expect(mockedNavigate).toHaveBeenCalledTimes(1);
    vi.useRealTimers();
  });

  test("Cleans up correctly on unmount", async () => {
    vi.useFakeTimers({
      shouldAdvanceTime: true,
      toFake: ["setTimeout", "clearTimeout"],
    });
    axiosMock.onGet("/api/courses/7").timeout();
    const specificQueryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });
    const setTimeoutSpy = vi.spyOn(globalThis, "setTimeout");
    const clearTimeoutSpy = vi.spyOn(globalThis, "clearTimeout");
    render(
      <QueryClientProvider client={specificQueryClient}>
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

    await screen.findByText(
      "Course not found. You will be returned to the course list in 3 seconds.",
    );
    expect(
      screen.queryByTestId(`StaffCourseShowPage-cell-row-0-col-id`),
    ).not.toBeInTheDocument();
    fireEvent.keyPress(screen.getByText("Course Not Found"), {
      key: "Escape",
      code: 27,
      charCode: 27,
    });
    fireEvent.click(
      within(screen.getByTestId("AppNavbar")).getByText("Frontiers"),
    );
    await waitFor(() =>
      expect(clearTimeoutSpy.mock.results.length).toBeGreaterThanOrEqual(12),
    );
    setTimeoutSpy.mockRestore();
    clearTimeoutSpy.mockRestore();
    vi.useRealTimers();
    specificQueryClient.clear();
  });

  test("Tab assertions", () => {
    setupInstructorUser();

    const theCourse = {
      ...coursesFixtures.oneCourseWithEachStatus[0],
      id: 7,
    };

    axiosMock.onGet("/api/courses/7").reply(200, theCourse);

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

    expect(screen.getByText("Students")).toHaveAttribute(
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
    expect(screen.getByText("Assignments")).toHaveAttribute(
      "data-rr-ui-event-key",
      "default",
    );
    expect(screen.getByText("Assignments")).toHaveAttribute(
      "aria-selected",
      "true",
    );
    expect(screen.getByText("Jobs")).toHaveAttribute(
      "data-rr-ui-event-key",
      "jobs",
    );

    expect(screen.queryByText("Settings")).not.toBeInTheDocument();
  });

  test("header displays correct info when course is loaded without an orgName", async () => {
    setupInstructorUser();

    axiosMock
      .onGet("/api/courses/7")
      .reply(200, coursesFixtures.severalCourses[2]);

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

    await screen.findByText("CMPSC 156");

    expect(screen.getByTestId("StaffCourseShowPage-title")).toBeInTheDocument();
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

  test("header displays correct info when course is loaded with orgName and installationId", async () => {
    setupInstructorUser();

    axiosMock
      .onGet("/api/courses/7")
      .reply(200, coursesFixtures.severalCourses[0]);

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

    await screen.findByText("CMPSC 156");

    expect(
      screen.getByTestId("StaffCourseShowPage-github-org-link"),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId("StaffCourseShowPage-github-org-link"),
    ).toHaveAttribute("href", "https://github.com/ucsb-cs156-s25");
    expect(screen.getByText("Spring 2025")).toBeInTheDocument();

    const githubImage = screen.getByTestId(
      "StaffCourseShowPage-github-org-image",
    );
    expect(githubImage).toHaveAttribute(
      "src",
      "https://github.com/ucsb-cs156-s25.png?size=64",
    );
    expect(githubImage).toHaveAttribute("alt", "ucsb-cs156-s25");
    expect(githubImage).toHaveStyle("width: 48px; height: 48px;");
  });

  test("expect the correct URL to the organization for the course", async () => {
    setupInstructorUser();

    axiosMock
      .onGet("/api/courses/7")
      .reply(200, coursesFixtures.severalCourses[0]);

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

    await screen.findByText("CMPSC 156");

    const githubLink = screen.getByTestId(
      `StaffCourseShowPage-github-settings-link`,
    );
    expect(githubLink).toBeInTheDocument();
    expect(githubLink).toHaveAttribute(
      "href",
      "https://github.com/organizations/ucsb-cs156-s25/settings/installations/123456",
    );
  });

  test("expect the correct tooltip ID and message for the github icon", async () => {
    setupInstructorUser();

    axiosMock
      .onGet("/api/courses/7")
      .reply(200, coursesFixtures.severalCourses[0]);

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

    await screen.findByText("CMPSC 156");

    expect(
      screen.getByTestId("StaffCourseShowPage-github-settings-icon"),
    ).toBeInTheDocument();

    fireEvent.mouseOver(
      screen.getByTestId(`StaffCourseShowPage-github-settings-icon`),
    );

    const tooltip = await screen.findByRole("tooltip");
    expect(tooltip).toHaveAttribute(
      "id",
      "StaffCourseShowPage-tooltip-github-settings",
    );
    expect(tooltip).toHaveTextContent(
      "Manage settings for association between your GitHub organization and this web application.",
    );
  });

  test("does not show error modal on initial render", async () => {
    setupInstructorUser();

    axiosMock
      .onGet("/api/courses/7")
      .reply(200, coursesFixtures.severalCourses[0]);

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

    await screen.findByText("CMPSC 156");

    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });

  test("Add Staff Member button is visible for instructor users", async () => {
    setupInstructorUser();

    axiosMock
      .onGet("/api/courses/7")
      .reply(200, coursesFixtures.severalCourses[0]);

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

    const postButton = await screen.findByRole("button", {
      name: "Add Staff Member",
    });
    expect(postButton).toBeInTheDocument();
  });

  test("Add Staff Member button is not visible for non-instructor users", async () => {
    setupUserOnly();

    axiosMock
      .onGet("/api/courses/7")
      .reply(200, coursesFixtures.severalCourses[0]);

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

    await screen.findByText("CMPSC 156");
    await waitFor(() => {
      expect(
        screen.queryByRole("button", { name: "Add Staff Member" }),
      ).not.toBeInTheDocument();
    });
  });
});
