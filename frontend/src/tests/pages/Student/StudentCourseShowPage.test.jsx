import {
  act,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import StudentCourseShowPage from "main/pages/Student/StudentCourseShowPage";
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

const mockToast = vi.fn();
vi.mock("react-toastify", async (importOriginal) => {
  return {
    ...(await importOriginal()),
    toast: (x) => mockToast(x),
  };
});

const axiosMock = new AxiosMockAdapter(axios);
const queryClient = new QueryClient();

describe("StudentCourseShowPage tests", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    axiosMock.reset();
    axiosMock.resetHistory();
    queryClient.clear();
  });

  const setupUser = () => {
    axiosMock
      .onGet("/api/currentUser")
      .reply(200, apiCurrentUserFixtures.userOnly);
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.showingNeither);
  };

  test("renders correctly for a student user", async () => {
    vi.useFakeTimers({
      shouldAdvanceTime: true,
      toFake: ["setTimeout", "clearTimeout"],
    });
    setupUser();
    const theCourse = {
      ...coursesFixtures.oneCourseWithEachStatus[0],
      id: 1,
    };
    axiosMock.onGet("/api/courses/1").reply(200, theCourse);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/student/courses/1"]}>
          <Routes>
            <Route
              path="/student/courses/:id"
              element={<StudentCourseShowPage />}
            />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const testId = "StudentCourseShowPage";

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

    // Tab assertions
    const placeholderTab = screen.getByRole("tab", { name: "Placeholder" });
    expect(placeholderTab).toHaveAttribute(
      "data-rr-ui-event-key",
      "placeholder",
    );
    expect(placeholderTab).toHaveClass("active");
    expect(screen.getByText("More features coming soon")).toBeInTheDocument();

    // Link assertions
    const link = screen.getByTestId("StudentCourseShowPage-github-org-link");
    expect(link).toHaveAttribute("href", "https://github.com/ucsb-cs156-s25");
    expect(link).toBeInTheDocument();

    vi.advanceTimersByTime(3000);
    expect(mockedNavigate).not.toHaveBeenCalled();
    vi.useRealTimers();
  });

  test("Returns to home page on timeout when course is not found", async () => {
    vi.useFakeTimers({
      shouldAdvanceTime: true,
      toFake: ["setTimeout", "clearTimeout"],
    });
    axiosMock.onGet("/api/courses/7").timeout();
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/student/courses/7"]}>
          <Routes>
            <Route
              path="/student/courses/:id"
              element={<StudentCourseShowPage />}
            />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(queryClient.getQueryData(["/api/courses/7"])).toBe(null);
    const testId = "StudentCourseShowPage";

    await screen.findByTestId(`${testId}-loading`);

    const courseName = screen.getByTestId(`${testId}-loading`);
    expect(courseName).toHaveTextContent("Course: Loading...");

    await screen.findByText(
      "Course not found. You will be returned to the course list in 3 seconds.",
    );
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
    expect(mockToast).not.toHaveBeenCalled();
    vi.useRealTimers();
  });

  test("Modal can be closed by clicking the header close button", async () => {
    vi.useFakeTimers({
      shouldAdvanceTime: true,
      toFake: ["setTimeout", "clearTimeout"],
    });
    axiosMock.onGet("/api/courses/7").timeout();
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/student/courses/7"]}>
          <Routes>
            <Route
              path="/student/courses/:id"
              element={<StudentCourseShowPage />}
            />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText(
      "Course not found. You will be returned to the course list in 3 seconds.",
    );
    expect(screen.getByText("Course Not Found")).toBeInTheDocument();

    const closeHeaderButton = screen.getByLabelText("Close");
    fireEvent.click(closeHeaderButton);
    await waitFor(() =>
      expect(screen.queryByText("Course Not Found")).not.toBeInTheDocument(),
    );
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

    const { unmount } = render(
      <QueryClientProvider client={specificQueryClient}>
        <MemoryRouter initialEntries={["/student/courses/7"]}>
          <Routes>
            <Route
              path="/student/courses/:id"
              element={<StudentCourseShowPage />}
            />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText(
      "Course not found. You will be returned to the course list in 3 seconds.",
    );

    unmount();

    await waitFor(() => expect(clearTimeoutSpy).toHaveBeenCalled());
    setTimeoutSpy.mockRestore();
    clearTimeoutSpy.mockRestore();
    vi.useRealTimers();
    specificQueryClient.clear();
  });

  test("header displays correct info when course is loaded without an organization", async () => {
    setupUser();

    axiosMock
      .onGet("/api/courses/7")
      .reply(200, coursesFixtures.severalCourses[2]); // This one probably doesn't have an org

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/student/courses/7"]}>
          <Routes>
            <Route
              path="/student/courses/:id"
              element={<StudentCourseShowPage />}
            />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText("CMPSC 156");

    expect(
      screen.getByTestId("StudentCourseShowPage-title"),
    ).toBeInTheDocument();
    expect(
      screen.queryByTestId("StudentCourseShowPage-github-org-link"),
    ).not.toBeInTheDocument();
  });
});
