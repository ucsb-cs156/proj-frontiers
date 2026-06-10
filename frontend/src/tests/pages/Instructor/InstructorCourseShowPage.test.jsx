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
import { MemoryRouter, Route, Routes } from "react-router";
import coursesFixtures from "fixtures/coursesFixtures";

import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import { rosterStudentFixtures } from "fixtures/rosterStudentFixtures";
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

describe("InstructorCourseShowPage tests", () => {
  describe("InstructorCourseShowPage tests for instructors", () => {
    beforeEach(() => {
      axiosMock.reset();
      axiosMock.resetHistory();
      queryClient.clear();
      axiosMock.onGet(/\/api\/courses\/getCanvasInfo/).reply(200, {
        courseId: "",
        canvasApiToken: "",
        canvasCourseId: "",
      });
      axiosMock.onGet("/api/jobs/course").reply(200, []);
      axiosMock.onGet("/api/course/options").reply(200, {
        ENABLE_CANVAS: false,
        TRANSLATE_SECTIONS: false,
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

    test("renders correctly for instructor user", async () => {
      vi.useFakeTimers({
        shouldAdvanceTime: true,
        toFake: ["setTimeout", "clearTimeout"],
      });
      setupInstructorUser();
      const theCourse = {
        ...coursesFixtures.oneCourseWithEachStatus[0],
        id: 1,
        instructorEmail: "phtcon@ucsb.edu",
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
        instructorEmail: "phtcon@ucsb.edu",
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
      expect(screen.getByText("Settings")).toHaveAttribute(
        "data-rr-ui-event-key",
        "settings",
      );
      const changeTabs = screen.getByText("Students");
      fireEvent.click(changeTabs);
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
        instructorEmail: "phtcon@ucsb.edu",
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
        screen.queryByTestId(
          "InstructorCourseShowPage-tooltip-github-settings",
        ),
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

    test("renders correctly for instructor user on course they did not create", async () => {
      vi.useFakeTimers({
        shouldAdvanceTime: true,
        toFake: ["setTimeout", "clearTimeout"],
      });
      setupInstructorUser();
      const theCourse = {
        ...coursesFixtures.oneCourseWithEachStatus[0],
        id: 1,
        instructorEmail: "someOneThatIsNotPhtcon@ucsb.edu",
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

      const toggle = await screen.findByTestId(
        "CourseOptionsForm-toggle-ENABLE_CANVAS",
      );
      expect(toggle).toBeDisabled();
    });

    test("renders correctly for instructor user on course they did create", async () => {
      setupInstructorUser();
      const theCourse = {
        ...coursesFixtures.oneCourseWithEachStatus[0],
        id: 1,
        instructorEmail: apiCurrentUserFixtures.instructorUser.user.email,
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

      const toggle = await screen.findByTestId(
        "CourseOptionsForm-toggle-ENABLE_CANVAS",
      );
      expect(toggle).toBeEnabled();
    });
  });

  describe("InstructorCourseShowPage tests when user is not an instructor", () => {
    beforeEach(() => {
      axiosMock.reset();
      axiosMock.resetHistory();
      queryClient.clear();
      axiosMock.onGet(/\/api\/courses\/getCanvasInfo/).reply(200, {
        courseId: "",
        canvasApiToken: "",
        canvasCourseId: "",
      });
      axiosMock.onGet("/api/jobs/course").reply(200, []);
      axiosMock.onGet("/api/course/options").reply(200, {
        ENABLE_CANVAS: false,
        TRANSLATE_SECTIONS: false,
      });
    });

    const setupNonInstructorUser = () => {
      axiosMock
        .onGet("/api/currentUser")
        .reply(200, apiCurrentUserFixtures.userOnly);
      axiosMock
        .onGet("/api/systemInfo")
        .reply(200, systemInfoFixtures.showingNeither);
    };

    test("renders correctly for non-admin user on course they didn't create", async () => {
      vi.useFakeTimers({
        shouldAdvanceTime: true,
        toFake: ["setTimeout", "clearTimeout"],
      });
      setupNonInstructorUser();
      const theCourse = {
        ...coursesFixtures.oneCourseWithEachStatus[0],
        id: 1,
        instructorEmail: "phtcon@ucsb.edu",
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

      const toggle = await screen.findByTestId(
        "CourseOptionsForm-toggle-ENABLE_CANVAS",
      );
      expect(toggle).toBeDisabled();
    });
  });

  describe("InstructorCourseShowPage tests when user is an admin", () => {
    beforeEach(() => {
      axiosMock.reset();
      axiosMock.resetHistory();
      queryClient.clear();
      axiosMock.onGet(/\/api\/courses\/getCanvasInfo/).reply(200, {
        courseId: "",
        canvasApiToken: "",
        canvasCourseId: "",
      });
      axiosMock.onGet("/api/jobs/course").reply(200, []);
      axiosMock.onGet("/api/course/options").reply(200, {
        ENABLE_CANVAS: false,
        TRANSLATE_SECTIONS: false,
      });
      //  '/api/teams/all?courseId=1' is called by the TeamsTabComponent, so we need to mock it here to prevent errors in the console
      axiosMock.onGet("/api/teams/all?courseId=1").reply(200, []);
      // Same with /api/coursestaff/course?courseId=1 which is called by the StaffTabComponent
      axiosMock.onGet("/api/coursestaff/course?courseId=1").reply(200, []);
      // And /api/rosterstudents/course/1' which is called by the EnrollmentTabComponent
      axiosMock
        .onGet("/api/rosterstudents/course/1")
        .reply(200, rosterStudentFixtures.threeStudents);
      // And /api/courses/warnings/1 which is called by InstructorCourseShowPage to determine whether to show the organization age warning tooltip
      axiosMock.onGet("/api/courses/warnings/1").reply(200, {
        showOrganizationAgeWarning: false,
      });
    });

    const setupAdminUser = () => {
      axiosMock
        .onGet("/api/currentUser")
        .reply(200, apiCurrentUserFixtures.adminUser);
      axiosMock
        .onGet("/api/systemInfo")
        .reply(200, systemInfoFixtures.showingNeither);
    };

    test("renders correctly for admin user on course they did create", async () => {
      vi.useFakeTimers({
        shouldAdvanceTime: true,
        toFake: ["setTimeout", "clearTimeout"],
      });
      setupAdminUser();
      const theCourse = {
        ...coursesFixtures.oneCourseWithEachStatus[0],
        id: 1,
        instructorEmail: "phtcon@ucsb.edu",
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

      const toggle = await screen.findByTestId(
        "CourseOptionsForm-toggle-ENABLE_CANVAS",
      );
      expect(toggle).toBeEnabled();
    });

    test("renders correctly for admin user on course they didn't create", async () => {
      vi.useFakeTimers({
        shouldAdvanceTime: true,
        toFake: ["setTimeout", "clearTimeout"],
      });
      setupAdminUser();
      const theCourse = {
        ...coursesFixtures.oneCourseWithEachStatus[0],
        id: 1,
        instructorEmail: "notPhtcon@ucsb.edu",
      };
      axiosMock.onGet("/api/courses/1").reply(200, theCourse);

      // On POST to /api/course/options with parameter courseId 1, option ENABLE_CANVAS, and enabled true, reply with a 200 status code
      // and reply with a payload of { courseId: 1, option: "ENABLE_CANVAS", enabled: true }
      axiosMock
        .onPost("/api/course/options", {
          courseId: 1,
          option: "ENABLE_CANVAS",
          enabled: true,
        })
        .reply(200, {
          courseId: 1,
          option: "ENABLE_CANVAS",
          enabled: true,
        });

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

      // Navigate to the settings tab and check that the course options form is present and enabled
      const settingsTab = screen.getByRole("tab", { name: "Settings" });
      fireEvent.click(settingsTab);

      await screen.findByTestId("CourseOptionsForm-toggle-ENABLE_CANVAS");
      expect(
        screen.getByTestId("CourseOptionsForm-toggle-ENABLE_CANVAS"),
      ).toBeEnabled();

      // Click the toggle and check that the toast is called with the correct message
      fireEvent.click(
        screen.getByTestId("CourseOptionsForm-toggle-ENABLE_CANVAS"),
      );

      // Expect the axiosMock to have been called with a POST request to /api/course/options with parameter courseId 1, option ENABLE_CANVAS, and enabled true
      await waitFor(() =>
        expect(axiosMock.history.post.length).toBeGreaterThan(0),
      );
      expect(axiosMock.history.post[0].url).toBe("/api/course/options");
      expect(axiosMock.history.post[0].method).toBe("post");
    });
  });
});
