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

const mockedNavigate = jest.fn();
jest.mock("react-router", () => ({
  ...jest.requireActual("react-router"),
  useNavigate: () => mockedNavigate,
}));
const axiosMock = new AxiosMockAdapter(axios);
const queryClient = new QueryClient();

const mockToast = jest.fn();
jest.mock("react-toastify", () => {
  const originalModule = jest.requireActual("react-toastify");
  return {
    __esModule: true,
    ...originalModule,
    toast: (x) => mockToast(x),
  };
});

describe("InstructorCourseShowPage tests", () => {
  afterEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    queryClient.clear();
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
    setupInstructorUser();
    jest.useFakeTimers();
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
      expect(screen.getByTestId(`${testId}-title`)).toBeInTheDocument();
    });

    const courseName = screen.getByTestId(`${testId}-title`);
    expect(courseName).toHaveTextContent("Course: Loading...");

    await waitFor(() => {
      expect(screen.getByTestId(`${testId}-title`)).toHaveTextContent(
        "Course: CMPSC 156 (1)",
      );
    });

    const orgName = screen.getByText("ucsb-cs156-s25");
    expect(orgName).toBeInTheDocument();

    expect(screen.queryByText("Course Not Found")).not.toBeInTheDocument();

    jest.advanceTimersByTime(3000);
    expect(mockedNavigate).not.toHaveBeenCalled();
    jest.useRealTimers();
  });

  test("handles fallback for courseId", async () => {
    setupInstructorUser();

    axiosMock.onGet("/api/courses/7").reply(200, null);

    axiosMock
      .onGet("/api/rosterstudents/course/7")
      .reply(200, rosterStudentFixtures.threeStudents);

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

    const rsTestId = "InstructorCourseShowPage-RosterStudentTable";
    await waitFor(() => {
      expect(screen.getByTestId(`${rsTestId}-courseId`)).toBeInTheDocument();
    });

    const courseIdHiddenElement = screen.getByTestId(`${rsTestId}-courseId`);
    expect(courseIdHiddenElement).toBeInTheDocument();
    expect(courseIdHiddenElement).toHaveAttribute("data-course-id", "");
  });

  test("Returns to course page on timeout", async () => {
    axiosMock.onGet("/api/courses/7").timeout();
    axiosMock.onGet("/api/rosterstudents/course/7").timeout();
    jest.useFakeTimers();
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
      jest.advanceTimersByTime(5000);
    });
    expect(mockedNavigate).toHaveBeenCalledWith("/", {
      replace: true,
    });
    expect(mockedNavigate).toHaveBeenCalledTimes(1);
    jest.useRealTimers();
  });

  test("Cleans up correctly on unmount", async () => {
    axiosMock.onGet("/api/courses/7").timeout();
    axiosMock.onGet("/api/rosterstudents/course/7").timeout();
    jest.useFakeTimers({
      advanceTimers: false,
    });
    const specificQueryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });
    const setTimeoutSpy = jest.spyOn(global, "setTimeout");
    const clearTimeoutSpy = jest.spyOn(global, "clearTimeout");
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
      within(screen.getByRole("navigation")).getByText("Frontiers"),
    );
    await waitFor(() =>
      expect(clearTimeoutSpy.mock.results.length).toBeGreaterThanOrEqual(12),
    );
    setTimeoutSpy.mockRestore();
    clearTimeoutSpy.mockRestore();
    jest.useRealTimers();
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
    expect(screen.getByText("Management")).toHaveAttribute(
      "data-rr-ui-event-key",
      "default",
    );
    expect(screen.getByText("Enrollment")).toHaveAttribute(
      "data-rr-ui-event-key",
      "enrollment",
    );
    expect(screen.getByText("Assignments")).toHaveAttribute(
      "data-rr-ui-event-key",
      "assignments",
    );
    expect(screen.getByText("Management")).toHaveAttribute(
      "aria-selected",
      "true",
    );
    const changeTabs = screen.getByText("Enrollment");
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
});
