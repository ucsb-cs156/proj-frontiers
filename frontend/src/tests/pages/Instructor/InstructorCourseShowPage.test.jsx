import {
  act,
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import InstructorCourseShowPage from "main/pages/Instructor/InstructorCourseShowPage";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import {
  MemoryRouter,
  Route,
  Routes,
  useLocation,
  useNavigationType,
} from "react-router";
import coursesFixtures from "fixtures/coursesFixtures";

import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import { rosterStudentFixtures } from "fixtures/rosterStudentFixtures";
import { courseStaffFixtures } from "fixtures/courseStaffFixtures";
import { sectionsFixtures } from "fixtures/sectionsFixtures";
import { dokkuAccountTranslationsFixtures } from "fixtures/dokkuAccountTranslationsFixtures";
import slackFixtures from "fixtures/slackFixtures";
import { expect, vi } from "vitest";

const mockedNavigate = vi.fn();
vi.mock("react-router", async (importOriginal) => ({
  ...(await importOriginal()),
  useNavigate: () => mockedNavigate,
}));
import * as useBackendModule from "main/utils/useBackend";

const useBackendSpy = vi.spyOn(useBackendModule, "useBackend");
const axiosMock = new AxiosMockAdapter(axios);
const queryClient = new QueryClient();

const mockToast = vi.fn();
vi.mock("react-toastify", async (importOriginal) => {
  return {
    ...(await importOriginal()),
    toast: (x) => mockToast(x),
  };
});

describe("InstructorCourseShowPage tests", () => {
  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    queryClient.clear();
    window.localStorage.clear();
    useBackendSpy.mockClear();
    axiosMock.onGet(/\/api\/courses\/getCanvasInfo/).reply(200, {
      courseId: "",
      canvasApiToken: "",
      canvasCourseId: "",
    });
    axiosMock.onGet("/api/jobs/course").reply(200, []);
    axiosMock.onGet("/api/courses/emails").reply(200, "");
    axiosMock.onGet("/api/course/options").reply(200, {
      ENABLE_CANVAS: false,
      TRANSLATE_SECTIONS: false,
      DOKKU_MANAGER: false,
      ENABLE_API_KEYS: false,
    });
  });

  const setupInstructorUser = () => {
    axiosMock
      .onGet("/api/currentUser")
      .reply(200, apiCurrentUserFixtures.instructorUser);
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.showingNeither);
  };

  const setupAdminUser = () => {
    axiosMock
      .onGet("/api/currentUser")
      .reply(200, apiCurrentUserFixtures.adminUser);
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
      id: 1,
      createdByEmail: "phtcon@ucsb.edu",
    };
    axiosMock.onGet("/api/courses/1").reply(200, theCourse);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/instructor/courses/1"]}>
          <Routes>
            <Route
              path="/instructor/courses/:id"
              element={<InstructorCourseShowPage />}
            />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const testId = "InstructorCourseShowPage";

    await waitFor(() => {
      expect(screen.getByTestId(`${testId}-title`)).toHaveTextContent(
        "CMPSC 156",
      );
    });

    expect(screen.queryByText("ucsb-cs156-s25")).toBeInTheDocument();

    const githubImage = screen.getByTestId(`${testId}-github-org-image`);
    expect(githubImage).toHaveAttribute(
      "src",
      "https://github.com/ucsb-cs156-s25.png?size=64",
    );
    expect(githubImage).toHaveAttribute("alt", "ucsb-cs156-s25");
    expect(githubImage).toHaveStyle("width: 48px; height: 48px;");

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
    axiosMock.onGet("/api/rosterstudents/course/7").timeout();
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/instructor/courses/7"]}>
          <Routes>
            <Route
              path="/instructor/courses/:id"
              element={<InstructorCourseShowPage />}
            />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );
    //Great time to also check initial values
    expect(queryClient.getQueryData(["/api/courses/7"])).toBe(null);
    const testId = "InstructorCourseShowPage";

    await screen.findByTestId(`${testId}-loading`);

    const courseName = screen.getByTestId(`${testId}-loading`);
    expect(courseName).toHaveTextContent("Course: Loading...");

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
    axiosMock.onGet("/api/rosterstudents/course/7").timeout();
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
        <MemoryRouter initialEntries={["/instructor/courses/7"]}>
          <Routes>
            <Route
              path="/instructor/courses/:id"
              element={<InstructorCourseShowPage />}
            />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText(
      "Course not found. You will be returned to the course list in 3 seconds.",
    );
    expect(
      screen.queryByTestId(`InstructorCourseShowPage-cell-row-0-col-id`),
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
      id: 1,
      createdByEmail: "phtcon@ucsb.edu",
    };

    axiosMock.onGet("/api/courses/7").reply(200, theCourse);

    axiosMock
      .onGet("/api/rosterstudents/course/7")
      .reply(200, rosterStudentFixtures.threeStudents);

    //here
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/instructor/courses/7"]}>
          <Routes>
            <Route
              path="/instructor/courses/:id"
              element={<InstructorCourseShowPage />}
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
      "assignments",
    );
    expect(screen.getByText("Assignments")).toHaveAttribute(
      "aria-selected",
      "true",
    );
    expect(screen.getByText("Jobs")).toHaveAttribute(
      "data-rr-ui-event-key",
      "jobs",
    );
    expect(screen.getByText("Settings")).toHaveAttribute(
      "data-rr-ui-event-key",
      "settings",
    );
    expect(screen.getByText("Downloads")).toHaveAttribute(
      "data-rr-ui-event-key",
      "downloads",
    );
    const changeTabs = screen.getByText("Students");
    fireEvent.click(changeTabs);

    const downloadsTab = screen.getByText("Downloads");
    fireEvent.click(downloadsTab);
    expect(downloadsTab).toHaveAttribute("aria-selected", "true");
  });

  test("Tab Components are Present", async () => {
    const queryClientSpecific = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
          staleTime: Infinity,
        },
      },
    });
    setupInstructorUser();
    const theCourse = {
      ...coursesFixtures.oneCourseWithEachStatus[0],
      id: 1,
      createdByEmail: "phtcon@ucsb.edu",
    };

    axiosMock.onGet("/api/courses/7").reply(200, theCourse);
    render(
      <QueryClientProvider client={queryClientSpecific}>
        <MemoryRouter initialEntries={["/instructor/courses/7"]}>
          <Routes>
            <Route
              path="/instructor/courses/:id"
              element={<InstructorCourseShowPage />}
            />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByTestId("AssignmentTabComponent");
    expect(
      screen.getByTestId("InstructorCourseShowPage-EnrollmentTabComponent"),
    ).toBeInTheDocument();
  });
  test("staff tab defaults to instructor controls", async () => {
    setupInstructorUser();
    axiosMock
      .onGet("/api/courses/7")
      .reply(200, coursesFixtures.oneCourseWithEachStatus[0]);
    axiosMock
      .onGet("/api/coursestaff/course?courseId=7")
      .reply(200, courseStaffFixtures.staffWithEachStatus);
    axiosMock.onGet("/api/courses/warnings/7").reply(200, {
      showOrganizationAgeWarning: false,
    });
    axiosMock.onGet("/api/rosterstudents/course/7").reply(200, []);
    axiosMock.onGet("/api/teams/all?courseId=7").reply(200, []);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/instructor/courses/7"]}>
          <Routes>
            <Route
              path="/instructor/courses/:id"
              element={<InstructorCourseShowPage />}
            />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    fireEvent.click(await screen.findByRole("tab", { name: "Staff" }));

    const postButtons = await screen.findAllByTestId(
      "InstructorCourseShowPage-post-button",
    );
    expect(
      postButtons.some((button) => button.textContent === "Add Staff Member"),
    ).toBe(true);
    expect(
      screen.getByTestId(
        "InstructorCourseShowPage-CourseStaffTable-cell-row-0-col-Edit-button",
      ),
    ).toBeInTheDocument();
  });
  test("header displays correct info when course is loaded without an installationId", async () => {
    setupInstructorUser();

    axiosMock
      .onGet("/api/courses/7")
      .reply(200, coursesFixtures.severalCourses[2]);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/instructor/courses/7"]}>
          <Routes>
            <Route
              path="/instructor/courses/:id"
              element={<InstructorCourseShowPage />}
            />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText("CMPSC 156");

    expect(
      screen.getByTestId("InstructorCourseShowPage-title"),
    ).toBeInTheDocument();
    expect(
      screen.queryByTestId("InstructorCourseShowPage-github-org-link"),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByTestId("InstructorCourseShowPage-tooltip-github-settings"),
    ).not.toBeInTheDocument();
  });
  test("header displays correct info when course is loaded (and displays warning)", async () => {
    setupInstructorUser();

    axiosMock
      .onGet("/api/courses/7")
      .reply(200, coursesFixtures.severalCourses[0]);

    axiosMock
      .onGet("/api/courses/warnings/7")
      .reply(200, { showOrganizationAgeWarning: true });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/instructor/courses/7"]}>
          <Routes>
            <Route
              path="/instructor/courses/:id"
              element={<InstructorCourseShowPage />}
            />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText("CMPSC 156");

    expect(
      screen.getByTestId("InstructorCourseShowPage-title"),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId("InstructorCourseShowPage-github-org-link"),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId("InstructorCourseShowPage-github-org-link"),
    ).toHaveAttribute("href", "https://github.com/ucsb-cs156-s25");
    expect(screen.getByText("Spring 2025")).toBeInTheDocument();
    expect(screen.getByText(/This GitHub Organization/i)).toBeInTheDocument();
  });

  test("displays default base permission warning when hideBasePermissionWarning is omitted", async () => {
    setupInstructorUser();

    axiosMock
      .onGet("/api/courses/7")
      .reply(200, coursesFixtures.severalCourses[0]);

    axiosMock.onGet("/api/courses/warnings/7").reply(200, {
      showDefaultBasePermissions: true,
    });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/instructor/courses/7"]}>
          <Routes>
            <Route
              path="/instructor/courses/:id"
              element={<InstructorCourseShowPage />}
            />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByTestId("CourseWarningBanner-defaultBasePermission");
  });

  test("displays default base permission warning when hideBasePermissionWarning is false", async () => {
    setupInstructorUser();

    axiosMock.onGet("/api/courses/7").reply(200, {
      ...coursesFixtures.severalCourses[0],
      hideBasePermissionWarning: false,
    });

    axiosMock.onGet("/api/courses/warnings/7").reply(200, {
      showOrganizationAgeWarning: false,
      showDefaultBasePermissions: true,
    });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/instructor/courses/7"]}>
          <Routes>
            <Route
              path="/instructor/courses/:id"
              element={<InstructorCourseShowPage />}
            />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByTestId("CourseWarningBanner-defaultBasePermission");
    expect(
      screen.getByText(/Default Base Permission is not the recommended value/i),
    ).toBeInTheDocument();
  });

  test("hides default base permission warning when hideBasePermissionWarning is true", async () => {
    setupInstructorUser();

    axiosMock.onGet("/api/courses/7").reply(200, {
      ...coursesFixtures.severalCourses[0],
      hideBasePermissionWarning: true,
    });

    axiosMock.onGet("/api/courses/warnings/7").reply(200, {
      showDefaultBasePermissions: true,
    });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/instructor/courses/7"]}>
          <Routes>
            <Route
              path="/instructor/courses/:id"
              element={<InstructorCourseShowPage />}
            />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText("CMPSC 156");
    expect(
      screen.queryByTestId("CourseWarningBanner-defaultBasePermission"),
    ).not.toBeInTheDocument();
  });

  test("expect the correct URL to the organization for the course", async () => {
    setupInstructorUser();

    axiosMock
      .onGet("/api/courses/7")
      .reply(200, coursesFixtures.severalCourses[0]);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/instructor/courses/7"]}>
          <Routes>
            <Route
              path="/instructor/courses/:id"
              element={<InstructorCourseShowPage />}
            />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText("CMPSC 156");

    const githubLink = screen.getByTestId(
      `InstructorCourseShowPage-github-settings-link`,
    );
    expect(githubLink).toBeInTheDocument();
    expect(githubLink).toHaveAttribute(
      "href",
      "https://github.com/organizations/ucsb-cs156-s25/settings/installations/123456",
    );
  });
  test("expect the correct tooltip ID and message for the github icon (that redirects to github installation settings)", async () => {
    setupInstructorUser();

    axiosMock
      .onGet("/api/courses/7")
      .reply(200, coursesFixtures.severalCourses[0]);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/instructor/courses/7"]}>
          <Routes>
            <Route
              path="/instructor/courses/:id"
              element={<InstructorCourseShowPage />}
            />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText("CMPSC 156");

    expect(
      screen.getByTestId("InstructorCourseShowPage-github-settings-icon"),
    ).toBeInTheDocument();

    fireEvent.mouseOver(
      screen.getByTestId(`InstructorCourseShowPage-github-settings-icon`),
    );

    const tooltip = await screen.findByRole("tooltip");
    expect(tooltip).toHaveAttribute(
      "id",
      "InstructorCourseShowPage-tooltip-github-settings",
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
        <MemoryRouter initialEntries={["/instructor/courses/7"]}>
          <Routes>
            <Route
              path="/instructor/courses/:id"
              element={<InstructorCourseShowPage />}
            />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText("CMPSC 156");

    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });

  test("instructor assigned to course can edit course option toggles", async () => {
    setupInstructorUser();

    axiosMock.onGet("/api/courses/7").reply(200, {
      ...coursesFixtures.severalCourses[0],
      instructorEmail: "diba@ucsb.edu",
    });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/instructor/courses/7"]}>
          <Routes>
            <Route
              path="/instructor/courses/:id"
              element={<InstructorCourseShowPage />}
            />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    fireEvent.click(await screen.findByRole("tab", { name: "Settings" }));
    const enableCanvasToggle = await screen.findByTestId(
      "CourseOptionsForm-toggle-ENABLE_CANVAS",
    );
    expect(enableCanvasToggle).not.toBeDisabled();
    const dokkuManagerToggle = await screen.findByTestId(
      "CourseOptionsForm-toggle-DOKKU_MANAGER",
    );
    expect(dokkuManagerToggle).not.toBeDisabled();
    const enableApiKeysToggle = await screen.findByTestId(
      "CourseOptionsForm-toggle-ENABLE_API_KEYS",
    );
    expect(enableApiKeysToggle).not.toBeDisabled();
  });

  test("admin can edit course option toggles for non-owned course", async () => {
    setupAdminUser();

    axiosMock.onGet("/api/courses/7").reply(200, {
      ...coursesFixtures.severalCourses[0],
      instructorEmail: "someoneelse@ucsb.edu",
    });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/instructor/courses/7"]}>
          <Routes>
            <Route
              path="/instructor/courses/:id"
              element={<InstructorCourseShowPage />}
            />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    fireEvent.click(await screen.findByRole("tab", { name: "Settings" }));
    const toggle = await screen.findByTestId(
      "CourseOptionsForm-toggle-ENABLE_CANVAS",
    );
    expect(toggle).not.toBeDisabled();
  });

  test("non-admin non-instructor cannot edit course option toggles", async () => {
    setupUserOnly();

    axiosMock.onGet("/api/courses/7").reply(200, {
      ...coursesFixtures.severalCourses[0],
      instructorEmail: "someoneelse@ucsb.edu",
    });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/instructor/courses/7"]}>
          <Routes>
            <Route
              path="/instructor/courses/:id"
              element={<InstructorCourseShowPage />}
            />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    fireEvent.click(await screen.findByRole("tab", { name: "Settings" }));
    const toggle = await screen.findByTestId(
      "CourseOptionsForm-toggle-ENABLE_CANVAS",
    );
    expect(toggle).toBeDisabled();
  });
  test("shows the Sections tab when TRANSLATE_SECTIONS option is enabled", async () => {
    setupInstructorUser();
    axiosMock.onGet("/api/courses/7").reply(200, {
      ...coursesFixtures.severalCourses[0],
      id: 7,
    });
    axiosMock.onGet("/api/course/options").reply(200, {
      ENABLE_CANVAS: false,
      TRANSLATE_SECTIONS: true,
      DOKKU_MANAGER: false,
      ENABLE_API_KEYS: false,
    });
    axiosMock
      .onGet("/api/courses/7/sections")
      .reply(200, sectionsFixtures.threeSections);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/instructor/courses/7"]}>
          <Routes>
            <Route
              path="/instructor/courses/:id"
              element={<InstructorCourseShowPage />}
            />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const sectionsTab = await screen.findByRole("tab", { name: "Sections" });
    expect(sectionsTab).toHaveAttribute("data-rr-ui-event-key", "sections");

    const optionsRequests = axiosMock.history.get.filter(
      (request) => request.url === "/api/course/options",
    );
    expect(optionsRequests.length).toBeGreaterThan(0);
    expect(optionsRequests[0].params).toEqual({ courseId: "7" });

    fireEvent.click(sectionsTab);
    expect(sectionsTab).toHaveAttribute("aria-selected", "true");
    expect(
      screen.getByTestId("InstructorCourseShowPage-sections-tab-component"),
    ).toBeInTheDocument();
    expect(
      await screen.findByTestId(
        "InstructorCourseShowPage-sections-table-cell-row-0-col-section",
      ),
    ).toHaveTextContent("0100");
  });

  test("hides the Sections tab when TRANSLATE_SECTIONS option is disabled", async () => {
    setupInstructorUser();
    axiosMock.onGet("/api/courses/7").reply(200, {
      ...coursesFixtures.severalCourses[0],
      id: 7,
    });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/instructor/courses/7"]}>
          <Routes>
            <Route
              path="/instructor/courses/:id"
              element={<InstructorCourseShowPage />}
            />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByTestId("InstructorCourseShowPage-title");
    await waitFor(() =>
      expect(
        axiosMock.history.get.some(
          (request) => request.url === "/api/course/options",
        ),
      ).toBe(true),
    );

    expect(
      screen.queryByRole("tab", { name: "Sections" }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByTestId("InstructorCourseShowPage-sections-tab-component"),
    ).not.toBeInTheDocument();
  });

  test("hides the Sections tab when course options cannot be loaded", async () => {
    setupInstructorUser();
    axiosMock.onGet("/api/courses/7").reply(200, {
      ...coursesFixtures.severalCourses[0],
      id: 7,
    });
    axiosMock.onGet("/api/course/options").reply(403);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/instructor/courses/7"]}>
          <Routes>
            <Route
              path="/instructor/courses/:id"
              element={<InstructorCourseShowPage />}
            />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByTestId("InstructorCourseShowPage-title");
    await waitFor(() =>
      expect(
        axiosMock.history.get.some(
          (request) => request.url === "/api/course/options",
        ),
      ).toBe(true),
    );

    expect(
      screen.queryByRole("tab", { name: "Sections" }),
    ).not.toBeInTheDocument();
  });

  test("Sections tab appears after enabling TRANSLATE_SECTIONS on the Settings tab", async () => {
    setupInstructorUser();
    axiosMock.onGet("/api/courses/7").reply(200, {
      ...coursesFixtures.severalCourses[0],
      id: 7,
      instructorEmail: "diba@ucsb.edu",
    });
    // Simulate backend state: the option flips to true once the POST arrives.
    let translateSections = false;
    axiosMock.onGet("/api/course/options").reply(() => [
      200,
      {
        ENABLE_CANVAS: false,
        TRANSLATE_SECTIONS: translateSections,
        DOKKU_MANAGER: false,
        ENABLE_API_KEYS: false,
      },
    ]);
    axiosMock.onPost("/api/course/options").reply(() => {
      translateSections = true;
      return [200, { TRANSLATE_SECTIONS: true }];
    });
    axiosMock.onGet("/api/courses/7/sections").reply(200, []);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/instructor/courses/7"]}>
          <Routes>
            <Route
              path="/instructor/courses/:id"
              element={<InstructorCourseShowPage />}
            />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    fireEvent.click(await screen.findByRole("tab", { name: "Settings" }));
    const toggle = await screen.findByTestId(
      "CourseOptionsForm-toggle-TRANSLATE_SECTIONS",
    );
    await waitFor(() => expect(toggle).not.toBeDisabled());
    expect(toggle).not.toBeChecked();
    expect(
      screen.queryByRole("tab", { name: "Sections" }),
    ).not.toBeInTheDocument();

    fireEvent.click(toggle);

    await waitFor(() => expect(axiosMock.history.post.length).toBe(1));
    expect(axiosMock.history.post[0].params).toEqual({
      courseId: "7",
      option: "TRANSLATE_SECTIONS",
      enabled: true,
    });

    expect(
      await screen.findByRole("tab", { name: "Sections" }),
    ).toBeInTheDocument();
  });
  const renderCourse7 = () =>
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/instructor/courses/7"]}>
          <Routes>
            <Route
              path="/instructor/courses/:id"
              element={<InstructorCourseShowPage />}
            />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

  const slackRequests = (path) =>
    axiosMock.history.get.filter((request) => request.url.includes(path));

  describe("Dokku tab", () => {
    const setupCourse7WithDokkuOption = (dokkuManager) => {
      setupInstructorUser();
      axiosMock.onGet("/api/courses/7").reply(200, {
        ...coursesFixtures.severalCourses[0],
        id: 7,
      });
      axiosMock.onGet("/api/course/options").reply(200, {
        ENABLE_CANVAS: false,
        TRANSLATE_SECTIONS: false,
        DOKKU_MANAGER: dokkuManager,
        ENABLE_API_KEYS: false,
      });
      axiosMock
        .onGet("/api/dokku/translations")
        .reply(200, dokkuAccountTranslationsFixtures.threeTranslations);
      axiosMock.onGet("/api/dokku/users_list_header").reply(200, "");
    };

    const renderPage = () =>
      render(
        <QueryClientProvider client={queryClient}>
          <MemoryRouter initialEntries={["/instructor/courses/7"]}>
            <Routes>
              <Route
                path="/instructor/courses/:id"
                element={<InstructorCourseShowPage />}
              />
            </Routes>
          </MemoryRouter>
        </QueryClientProvider>,
      );

    test("is shown, between Downloads and Settings, when the DOKKU_MANAGER option is enabled", async () => {
      setupCourse7WithDokkuOption(true);

      renderPage();

      const dokkuTab = await screen.findByRole("tab", { name: "Dokku" });
      expect(dokkuTab).toHaveAttribute("data-rr-ui-event-key", "dokku");
      const tabNames = screen.getAllByRole("tab").map((tab) => tab.textContent);
      expect(tabNames).toEqual([
        "Students",
        "Staff",
        "Teams",
        "Assignments",
        "Jobs",
        "Downloads",
        "Dokku",
        "Settings",
      ]);

      fireEvent.click(dokkuTab);
      expect(dokkuTab).toHaveAttribute("aria-selected", "true");
      expect(
        screen.getByTestId("InstructorCourseShowPage-dokku-tab-component"),
      ).toBeInTheDocument();
      expect(
        await screen.findByTestId(
          "InstructorCourseShowPage-dokku-translations-table-cell-row-0-col-email",
        ),
      ).toHaveTextContent("cgaucho@ucsb.edu");
      const translationsRequests = axiosMock.history.get.filter(
        (request) => request.url === "/api/dokku/translations",
      );
      expect(translationsRequests.length).toBe(1);
      expect(translationsRequests[0].params).toEqual({ courseId: "7" });
    });

    test("is hidden when the DOKKU_MANAGER option is disabled", async () => {
      setupCourse7WithDokkuOption(false);

      renderPage();

      await screen.findByTestId("InstructorCourseShowPage-title");
      await waitFor(() =>
        expect(
          axiosMock.history.get.some(
            (request) => request.url === "/api/course/options",
          ),
        ).toBe(true),
      );

      expect(
        screen.queryByRole("tab", { name: "Dokku" }),
      ).not.toBeInTheDocument();
      expect(
        screen.queryByTestId("InstructorCourseShowPage-dokku-tab-component"),
      ).not.toBeInTheDocument();
      expect(
        axiosMock.history.get.some(
          (request) => request.url === "/api/dokku/translations",
        ),
      ).toBe(false);
    });

    test("is hidden when the DOKKU_MANAGER option is not strictly true", async () => {
      setupCourse7WithDokkuOption("unexpected");

      renderPage();

      await screen.findByTestId("InstructorCourseShowPage-title");
      await waitFor(() =>
        expect(
          axiosMock.history.get.some(
            (request) => request.url === "/api/course/options",
          ),
        ).toBe(true),
      );

      expect(
        screen.queryByRole("tab", { name: "Dokku" }),
      ).not.toBeInTheDocument();
    });
  });

  test("shows the Slack tab when SLACK_INTEGRATION is enabled and a token is set; loads Slack data only when the tab is opened", async () => {
    setupInstructorUser();
    axiosMock.onGet("/api/courses/7").reply(200, {
      ...coursesFixtures.severalCourses[0],
      id: 7,
    });
    axiosMock.onGet("/api/course/options").reply(200, {
      ENABLE_CANVAS: false,
      TRANSLATE_SECTIONS: false,
      DOKKU_MANAGER: false,
      ENABLE_API_KEYS: false,
      SLACK_INTEGRATION: true,
    });
    axiosMock
      .onGet("/api/courses/slack/info?courseId=7")
      .reply(200, slackFixtures.connectedInfo);
    axiosMock
      .onGet("/api/courses/slack/users?courseId=7")
      .reply(200, slackFixtures.fourUsers);
    axiosMock
      .onGet("/api/courses/slack/missing?courseId=7")
      .reply(200, slackFixtures.threeMissingMembers);

    renderCourse7();

    const slackTab = await screen.findByRole("tab", { name: "Slack" });
    expect(slackTab).toHaveAttribute("data-rr-ui-event-key", "slack");
    expect(slackRequests("/slack/info")[0].url).toBe(
      "/api/courses/slack/info?courseId=7",
    );
    // The Slack card on the Settings tab shares this query key (and so its
    // cache), which means the page's own query has to be checked directly:
    // toasts suppressed, and enabled once the option is known to be on.
    expect(useBackendSpy).toHaveBeenCalledWith(
      ["/api/courses/slack/info?courseId=7"],
      { method: "GET", url: "/api/courses/slack/info?courseId=7" },
      {},
      true,
      { enabled: true },
    );

    // The Slack API is only called (via the backend) once the tab is opened
    expect(
      screen.queryByTestId("InstructorCourseShowPage-slack-tab-component"),
    ).not.toBeInTheDocument();
    expect(slackRequests("/slack/users").length).toBe(0);
    expect(slackRequests("/slack/missing").length).toBe(0);

    fireEvent.click(slackTab);
    expect(slackTab).toHaveAttribute("aria-selected", "true");

    const tab = screen.getByTestId(
      "InstructorCourseShowPage-slack-tab-component",
    );
    expect(tab.parentElement).toHaveClass("pt-2");
    // TRANSLATE_SECTIONS is off, so there is no Slack Section Channels card
    expect(
      screen.queryByTestId(
        "InstructorCourseShowPage-slack-section-channels-card",
      ),
    ).not.toBeInTheDocument();
    const link = screen.getByTestId(
      "InstructorCourseShowPage-slack-workspace-link",
    );
    expect(link).toHaveTextContent("ucsb-cs156-f26");
    expect(link).toHaveAttribute("href", "https://ucsb-cs156-f26.slack.com/");

    expect(
      await screen.findByTestId(
        "InstructorCourseShowPage-slack-users-table-cell-row-0-col-courseRole",
      ),
    ).toHaveTextContent("Instructor");
    expect(
      await screen.findByTestId(
        "InstructorCourseShowPage-slack-missing-table-cell-row-0-col-slackStatus",
      ),
    ).toHaveTextContent("Not in Slack");
  });

  test("hides the Slack tab, and does not ask for Slack info, when SLACK_INTEGRATION is disabled", async () => {
    setupInstructorUser();
    axiosMock.onGet("/api/courses/7").reply(200, {
      ...coursesFixtures.severalCourses[0],
      id: 7,
    });
    axiosMock.onGet("/api/course/options").reply(200, {
      ENABLE_CANVAS: false,
      TRANSLATE_SECTIONS: false,
      DOKKU_MANAGER: false,
      ENABLE_API_KEYS: false,
      SLACK_INTEGRATION: false,
    });
    axiosMock
      .onGet("/api/courses/slack/info?courseId=7")
      .reply(200, slackFixtures.connectedInfo);

    renderCourse7();

    await screen.findByText(coursesFixtures.severalCourses[0].courseName);
    await waitFor(() =>
      expect(
        axiosMock.history.get.filter(
          (request) => request.url === "/api/course/options",
        ).length,
      ).toBeGreaterThan(0),
    );
    await screen.findByRole("tab", { name: "Settings" });

    expect(
      screen.queryByRole("tab", { name: "Slack" }),
    ).not.toBeInTheDocument();
    expect(slackRequests("/slack/info").length).toBe(0);
    expect(useBackendSpy).toHaveBeenCalledWith(
      ["/api/courses/slack/info?courseId=7"],
      { method: "GET", url: "/api/courses/slack/info?courseId=7" },
      {},
      true,
      { enabled: false },
    );
    expect(useBackendSpy).not.toHaveBeenCalledWith(
      ["/api/courses/slack/info?courseId=7"],
      expect.anything(),
      {},
      true,
      { enabled: true },
    );
  });

  test("hides the Slack tab when SLACK_INTEGRATION is enabled but no token has been set", async () => {
    setupInstructorUser();
    axiosMock.onGet("/api/courses/7").reply(200, {
      ...coursesFixtures.severalCourses[0],
      id: 7,
    });
    axiosMock.onGet("/api/course/options").reply(200, {
      ENABLE_CANVAS: false,
      TRANSLATE_SECTIONS: false,
      DOKKU_MANAGER: false,
      ENABLE_API_KEYS: false,
      SLACK_INTEGRATION: true,
    });
    axiosMock
      .onGet("/api/courses/slack/info?courseId=7")
      .reply(200, slackFixtures.notConnectedInfo);

    renderCourse7();

    await waitFor(() => expect(slackRequests("/slack/info").length).toBe(1));
    await screen.findByRole("tab", { name: "Settings" });
    // wait until the (not connected) info has arrived
    await waitFor(() =>
      expect(
        queryClient.getQueryData(["/api/courses/slack/info?courseId=7"]),
      ).toEqual(slackFixtures.notConnectedInfo),
    );

    expect(
      screen.queryByRole("tab", { name: "Slack" }),
    ).not.toBeInTheDocument();
  });

  test("hides the Slack tab when a token is set but the SLACK_INTEGRATION option is not strictly true", async () => {
    setupInstructorUser();
    axiosMock.onGet("/api/courses/7").reply(200, {
      ...coursesFixtures.severalCourses[0],
      id: 7,
    });
    axiosMock.onGet("/api/course/options").reply(200, {
      SLACK_INTEGRATION: "unexpected",
    });
    axiosMock
      .onGet("/api/courses/slack/info?courseId=7")
      .reply(200, slackFixtures.connectedInfo);

    renderCourse7();

    await screen.findByRole("tab", { name: "Settings" });
    // wait until the course options have arrived
    await waitFor(() =>
      expect(
        queryClient.getQueryData(["/api/course/options/?courseId=7"]),
      ).toEqual({ SLACK_INTEGRATION: "unexpected" }),
    );

    expect(
      screen.queryByRole("tab", { name: "Slack" }),
    ).not.toBeInTheDocument();
  });

  test("Slack tab has the Slack Section Channels card when TRANSLATE_SECTIONS is also enabled", async () => {
    setupInstructorUser();
    axiosMock.onGet("/api/courses/7").reply(200, {
      ...coursesFixtures.severalCourses[0],
      id: 7,
    });
    axiosMock.onGet("/api/course/options").reply(200, {
      ENABLE_CANVAS: false,
      TRANSLATE_SECTIONS: true,
      DOKKU_MANAGER: false,
      ENABLE_API_KEYS: false,
      SLACK_INTEGRATION: true,
    });
    axiosMock.onGet("/api/courses/7/sections").reply(200, []);
    axiosMock
      .onGet("/api/courses/slack/info?courseId=7")
      .reply(200, slackFixtures.connectedInfo);
    axiosMock.onGet("/api/courses/slack/users?courseId=7").reply(200, []);
    axiosMock.onGet("/api/courses/slack/missing?courseId=7").reply(200, []);

    renderCourse7();

    fireEvent.click(await screen.findByRole("tab", { name: "Slack" }));

    expect(
      screen.getByTestId(
        "InstructorCourseShowPage-slack-section-channels-card",
      ),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Slack Section Channels" }),
    ).toBeInTheDocument();
  });

  describe("Load Students from Canvas button on the Students tab", () => {
    const setupCourse7WithCanvasOption = (enableCanvas) => {
      setupInstructorUser();
      axiosMock.onGet("/api/courses/7").reply(200, {
        ...coursesFixtures.severalCourses[0],
        id: 7,
      });
      axiosMock.onGet("/api/course/options").reply(200, {
        ENABLE_CANVAS: enableCanvas,
        TRANSLATE_SECTIONS: false,
        DOKKU_MANAGER: false,
        ENABLE_API_KEYS: false,
        SLACK_INTEGRATION: false,
      });
      axiosMock
        .onGet("/api/rosterstudents/course/7")
        .reply(200, rosterStudentFixtures.threeStudents);
    };

    // wait until the course options have arrived, so that their effect can be seen
    const courseOptionsLoaded = (expected) =>
      waitFor(() =>
        expect(
          queryClient.getQueryData(["/api/course/options/?courseId=7"]),
        ).toEqual(expect.objectContaining({ ENABLE_CANVAS: expected })),
      );

    test("is shown when the ENABLE_CANVAS course option is enabled, and loads students for this course", async () => {
      setupCourse7WithCanvasOption(true);
      axiosMock
        .onPost("/api/courses/canvas/sync/students")
        .reply(200, { created: 1, updated: 2, dropped: 0, rejected: [] });

      renderCourse7();

      const button = await screen.findByTestId(
        "InstructorCourseShowPage-canvas-sync-button",
      );
      expect(button).toHaveTextContent("Load Students from Canvas");

      fireEvent.click(button);
      fireEvent.click(await screen.findByText("Yes, I'd like to do this"));
      await waitFor(() => expect(axiosMock.history.post.length).toBe(1));
      expect(axiosMock.history.post[0].url).toBe(
        "/api/courses/canvas/sync/students",
      );
      expect(axiosMock.history.post[0].params).toEqual({ courseId: "7" });
    });

    test("is not shown when the ENABLE_CANVAS course option is disabled", async () => {
      setupCourse7WithCanvasOption(false);

      renderCourse7();

      // (the Staff tab has a csv-button too, so look for a button only the Students tab has)
      await screen.findByText("Add Individual Student");
      await courseOptionsLoaded(false);
      expect(
        screen.queryByTestId("InstructorCourseShowPage-canvas-sync-button"),
      ).not.toBeInTheDocument();
    });

    test("is not shown when the ENABLE_CANVAS course option is not strictly true", async () => {
      setupCourse7WithCanvasOption("unexpected");

      renderCourse7();

      // (the Staff tab has a csv-button too, so look for a button only the Students tab has)
      await screen.findByText("Add Individual Student");
      await courseOptionsLoaded("unexpected");
      expect(
        screen.queryByTestId("InstructorCourseShowPage-canvas-sync-button"),
      ).not.toBeInTheDocument();
    });
  });

  describe("?tab= in the URL", () => {
    // Shows the current URL, so that tests can see changes to ?tab=
    const LocationProbe = () => {
      const location = useLocation();
      const navigationType = useNavigationType();
      return (
        <>
          <div data-testid="location-probe">
            {location.pathname}
            {location.search}
          </div>
          <div data-testid="navigation-type-probe">{navigationType}</div>
        </>
      );
    };

    const renderAt = (url, props = {}) =>
      render(
        <QueryClientProvider client={queryClient}>
          <MemoryRouter initialEntries={[url]}>
            <LocationProbe />
            <Routes>
              <Route
                path="/instructor/courses/:id"
                element={<InstructorCourseShowPage {...props} />}
              />
            </Routes>
          </MemoryRouter>
        </QueryClientProvider>,
      );

    const setupCourse7 = (options = {}) => {
      setupInstructorUser();
      axiosMock.onGet("/api/courses/7").reply(200, {
        ...coursesFixtures.severalCourses[0],
        id: 7,
      });
      axiosMock.onGet("/api/course/options").reply(200, {
        ENABLE_CANVAS: false,
        TRANSLATE_SECTIONS: false,
        DOKKU_MANAGER: false,
        ENABLE_API_KEYS: false,
        SLACK_INTEGRATION: false,
        ...options,
      });
      axiosMock.onGet("/api/courses/7/sections").reply(200, []);
      axiosMock.onGet("/api/dokku/translations").reply(200, []);
      axiosMock.onGet("/api/dokku/users_list_header").reply(200, "");
      axiosMock
        .onGet("/api/courses/slack/info?courseId=7")
        .reply(200, slackFixtures.connectedInfo);
      axiosMock.onGet("/api/courses/slack/users?courseId=7").reply(200, []);
      axiosMock.onGet("/api/courses/slack/missing?courseId=7").reply(200, []);
    };

    const selectedTab = () =>
      screen
        .getAllByRole("tab")
        .filter((tab) => tab.getAttribute("aria-selected") === "true")
        .map((tab) => tab.textContent);

    test("with no ?tab=, and nothing remembered, the Assignments tab is shown and the URL is left alone", async () => {
      setupCourse7();
      renderAt("/instructor/courses/7");

      await screen.findByRole("tab", { name: "Assignments" });
      expect(selectedTab()).toEqual(["Assignments"]);
      expect(screen.getByTestId("location-probe")).toHaveTextContent(
        "/instructor/courses/7",
      );
      expect(screen.getByTestId("location-probe").textContent).toBe(
        "/instructor/courses/7",
      );
      expect(window.localStorage.getItem("frontiers.courseTab.7")).toBeNull();
    });

    test.each([
      ["students", "Students"],
      ["staff", "Staff"],
      ["teams", "Teams"],
      ["sections", "Sections"],
      ["assignments", "Assignments"],
      ["jobs", "Jobs"],
      ["downloads", "Downloads"],
      ["dokku", "Dokku"],
      ["slack", "Slack"],
      ["settings", "Settings"],
    ])("?tab=%s opens the %s tab, and remembers it", async (tab, title) => {
      setupCourse7({
        TRANSLATE_SECTIONS: true,
        DOKKU_MANAGER: true,
        SLACK_INTEGRATION: true,
      });
      renderAt(`/instructor/courses/7?tab=${tab}`);

      await waitFor(() => expect(selectedTab()).toEqual([title]));
      expect(screen.getByTestId("location-probe").textContent).toBe(
        `/instructor/courses/7?tab=${tab}`,
      );
      expect(window.localStorage.getItem("frontiers.courseTab.7")).toBe(tab);
    });

    test("clicking a tab puts it in the URL, so that a refresh shows the same tab", async () => {
      setupCourse7();
      const { unmount } = renderAt("/instructor/courses/7");

      fireEvent.click(await screen.findByRole("tab", { name: "Jobs" }));

      await waitFor(() => expect(selectedTab()).toEqual(["Jobs"]));
      const url = screen.getByTestId("location-probe").textContent;
      expect(url).toBe("/instructor/courses/7?tab=jobs");
      expect(window.localStorage.getItem("frontiers.courseTab.7")).toBe("jobs");

      fireEvent.click(screen.getByRole("tab", { name: "Staff" }));
      await waitFor(() => expect(selectedTab()).toEqual(["Staff"]));
      expect(screen.getByTestId("location-probe").textContent).toBe(
        "/instructor/courses/7?tab=staff",
      );

      // "refresh": open the page again at the URL it now has
      unmount();
      window.localStorage.clear();
      renderAt("/instructor/courses/7?tab=staff");
      await waitFor(() => expect(selectedTab()).toEqual(["Staff"]));
    });

    test("clicking a tab replaces the history entry instead of adding one", async () => {
      setupCourse7();
      renderAt("/instructor/courses/7");
      // the page was opened, not navigated to
      expect(screen.getByTestId("navigation-type-probe")).toHaveTextContent(
        "POP",
      );

      fireEvent.click(await screen.findByRole("tab", { name: "Jobs" }));
      await waitFor(() => expect(selectedTab()).toEqual(["Jobs"]));

      // so that the Back button leaves the course page, rather than stepping
      // back through every tab that was clicked
      expect(screen.getByTestId("navigation-type-probe")).toHaveTextContent(
        "REPLACE",
      );
    });

    test("an unknown ?tab= shows the Assignments tab, and is not remembered", async () => {
      setupCourse7();
      window.localStorage.setItem("frontiers.courseTab.7", "jobs");
      renderAt("/instructor/courses/7?tab=bogus");

      await screen.findByRole("tab", { name: "Assignments" });
      expect(selectedTab()).toEqual(["Assignments"]);
      expect(window.localStorage.getItem("frontiers.courseTab.7")).toBe("jobs");
    });

    test("?tab= for a tab the course does not have shows the Assignments tab", async () => {
      setupCourse7({ TRANSLATE_SECTIONS: false, SLACK_INTEGRATION: false });
      const { unmount } = renderAt("/instructor/courses/7?tab=sections");
      await screen.findByRole("tab", { name: "Settings" });
      await waitFor(() =>
        expect(
          queryClient.getQueryData(["/api/course/options/?courseId=7"]),
        ).toEqual(expect.objectContaining({ TRANSLATE_SECTIONS: false })),
      );
      expect(selectedTab()).toEqual(["Assignments"]);
      unmount();

      renderAt("/instructor/courses/7?tab=slack");
      await screen.findByRole("tab", { name: "Settings" });
      expect(selectedTab()).toEqual(["Assignments"]);
    });

    test("?tab=dokku shows the Assignments tab when the Dokku Manager option is off", async () => {
      setupCourse7({ DOKKU_MANAGER: false });
      renderAt("/instructor/courses/7?tab=dokku");
      await screen.findByRole("tab", { name: "Settings" });
      await waitFor(() =>
        expect(
          queryClient.getQueryData(["/api/course/options/?courseId=7"]),
        ).toEqual(expect.objectContaining({ DOKKU_MANAGER: false })),
      );
      expect(selectedTab()).toEqual(["Assignments"]);
      expect(
        screen.queryByRole("tab", { name: "Dokku" }),
      ).not.toBeInTheDocument();
      expect(screen.getByTestId("location-probe").textContent).toBe(
        "/instructor/courses/7?tab=dokku",
      );
    });

    test("?tab=settings shows the Assignments tab when there is no Settings tab", async () => {
      setupCourse7();
      renderAt("/instructor/courses/7?tab=settings", {
        showSettingsTab: false,
      });

      await screen.findByRole("tab", { name: "Assignments" });
      expect(
        screen.queryByRole("tab", { name: "Settings" }),
      ).not.toBeInTheDocument();
      expect(selectedTab()).toEqual(["Assignments"]);
    });

    test("the other tabs can still be opened with ?tab= when there is no Settings tab (as for staff)", async () => {
      setupCourse7();
      renderAt("/instructor/courses/7?tab=jobs", { showSettingsTab: false });

      await waitFor(() => expect(selectedTab()).toEqual(["Jobs"]));
      expect(
        screen.queryByRole("tab", { name: "Settings" }),
      ).not.toBeInTheDocument();

      fireEvent.click(screen.getByRole("tab", { name: "Students" }));
      await waitFor(() => expect(selectedTab()).toEqual(["Students"]));
      expect(screen.getByTestId("location-probe").textContent).toBe(
        "/instructor/courses/7?tab=students",
      );
    });

    test("with no ?tab=, the tab remembered for this course is shown, without changing the URL", async () => {
      setupCourse7();
      window.localStorage.setItem("frontiers.courseTab.7", "downloads");
      window.localStorage.setItem("frontiers.courseTab.8", "staff");
      renderAt("/instructor/courses/7");

      await waitFor(() => expect(selectedTab()).toEqual(["Downloads"]));
      expect(screen.getByTestId("location-probe").textContent).toBe(
        "/instructor/courses/7",
      );
    });

    test("?tab= wins over the remembered tab", async () => {
      setupCourse7();
      window.localStorage.setItem("frontiers.courseTab.7", "downloads");
      renderAt("/instructor/courses/7?tab=teams");

      await waitFor(() => expect(selectedTab()).toEqual(["Teams"]));
      expect(window.localStorage.getItem("frontiers.courseTab.7")).toBe(
        "teams",
      );
    });

    test("a remembered tab that the course no longer has gives the Assignments tab", async () => {
      setupCourse7({ SLACK_INTEGRATION: false });
      window.localStorage.setItem("frontiers.courseTab.7", "slack");
      renderAt("/instructor/courses/7");

      await screen.findByRole("tab", { name: "Settings" });
      expect(selectedTab()).toEqual(["Assignments"]);
      // and it is still remembered, in case the tab comes back
      expect(window.localStorage.getItem("frontiers.courseTab.7")).toBe(
        "slack",
      );
    });
  });
});
