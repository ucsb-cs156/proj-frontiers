import {
  act,
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import InstructorCourseShowPage from "main/pages/Instructor/InstructorCourseShowPage";
import { QueryClient, QueryClientProvider } from "react-query";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import coursesFixtures from "fixtures/coursesFixtures";

import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import { rosterStudentFixtures } from "fixtures/rosterStudentFixtures";
import userEvent from "@testing-library/user-event";

const mockedNavigate = jest.fn();
jest.mock("react-router-dom", () => ({
  ...jest.requireActual("react-router-dom"),
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
  const testId = "InstructorCourseShowPage";

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
    axiosMock
      .onGet("/api/rosterstudents/course/1")
      .reply(200, rosterStudentFixtures.threeStudents);

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

    const rsTestId = "InstructorCourseShowPage-RosterStudentTable";

    await waitFor(() => {
      expect(
        screen.getByTestId(`${rsTestId}-cell-row-0-col-id`),
      ).toHaveTextContent(rosterStudentFixtures.threeStudents[0].id);
    });

    const orgName = screen.getByText("ucsb-cs156-s25");
    expect(orgName).toBeInTheDocument();

    const studentFirstName0 = screen.getByText(
      rosterStudentFixtures.threeStudents[0].firstName,
    );
    expect(studentFirstName0).toBeInTheDocument();

    const studentId0 = screen.getByTestId(
      `${rsTestId}-cell-row-0-col-studentId`,
    );
    expect(studentId0).toHaveTextContent(
      rosterStudentFixtures.threeStudents[0].studentId,
    );
    expect(screen.queryByText("Course Not Found")).not.toBeInTheDocument();

    jest.advanceTimersByTime(3000);
    expect(mockedNavigate).not.toHaveBeenCalled();
    jest.useRealTimers();
  });

  test("renders correctly for empty roster student list", async () => {
    setupInstructorUser();

    axiosMock.onGet("/api/courses/7").reply(200, "");

    axiosMock.onGet("/api/rosterstudents/course/7").reply(200, []);

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

    await waitFor(() => {
      expect(screen.getByText("Course Name")).toBeInTheDocument();
    });

    expect(
      screen.queryByTestId(`${testId}-cell-row-0-col-id`),
    ).not.toBeInTheDocument();

    const expectedHeaders = ["Student Id", "First Name", "Last Name", "Email"];
    const expectedFields = ["studentId", "firstName", "lastName", "email"];

    // assert
    expectedHeaders.forEach((headerText) => {
      const header = screen.getByText(headerText);
      expect(header).toBeInTheDocument();
    });

    expectedFields.forEach((field) => {
      const fieldElement = screen.queryByTestId(
        `${testId}-cell-row-0-col-${field}`,
      );
      expect(fieldElement).not.toBeInTheDocument();
    });
  });

  test("renders correctly for default empty list when roster student variable is undefined", async () => {
    setupInstructorUser();
    axiosMock.onGet("/api/courses/7").reply(200, null);

    axiosMock.onGet("/api/rosterstudents/course/7").reply(200, null);

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

    await waitFor(() => {
      expect(screen.getByText("Course Name")).toBeInTheDocument();
    });

    expect(
      screen.queryByTestId(`${testId}-cell-row-0-col-id`),
    ).not.toBeInTheDocument();

    const expectedHeaders = ["Student Id", "First Name", "Last Name", "Email"];
    const expectedFields = ["studentId", "firstName", "lastName", "email"];

    // assert
    expectedHeaders.forEach((headerText) => {
      const header = screen.getByText(headerText);
      expect(header).toBeInTheDocument();
    });

    expectedFields.forEach((field) => {
      const fieldElement = screen.queryByTestId(
        `${testId}-cell-row-0-col-${field}`,
      );
      expect(fieldElement).not.toBeInTheDocument();
    });
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
    expect(queryClient.getQueryData("/api/courses/7")).toBe(null);
    expect(queryClient.getQueryData("/api/rosterstudents/course/7")).toBe(null);

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
    expect(mockedNavigate).toHaveBeenCalledWith("/instructor/courses", {
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
    const setTimeoutSpy = jest.spyOn(global, "setTimeout");
    const clearTimeoutSpy = jest.spyOn(global, "clearTimeout");
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

    await screen.findByText(
      "Course not found. You will be returned to the course list in 3 seconds.",
    );
    fireEvent.keyPress(screen.getByText("Course Not Found"), {
      key: "Escape",
      code: 27,
      charCode: 27,
    });
    fireEvent.click(
      within(screen.getByRole("navigation")).getByText("Frontiers"),
    );
    await waitFor(() =>
      expect(clearTimeoutSpy.mock.results.length).toBeGreaterThanOrEqual(8),
    );
    setTimeoutSpy.mockRestore();
    clearTimeoutSpy.mockRestore();
    jest.useRealTimers();
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
    expect(screen.getByText("Management")).toHaveAttribute(
      "aria-selected",
      "true",
    );
    const changeTabs = screen.getByText("Enrollment");
    fireEvent.click(changeTabs);
    expect(
      screen.getByText("Temporary Text for Manually Adding Student"),
    ).toBeInTheDocument();
  });

  test("Successfully makes a call to the backend on submit", async () => {
    const queryClientSpecific = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
          staleTime: Infinity,
        },
      },
    });
    const file = new File(["there"], "egrades.csv", { type: "text/csv" });
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

    axiosMock.onPost("/api/rosterstudents/upload/egrades").reply(200);

    const user = userEvent.setup();
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
    await screen.findByTestId("RosterStudentCSVUploadForm-upload");

    const alternateUpdateCount =
      queryClientSpecific.getQueryState("/api/courses/7").dataUpdateCount;
    const updateCountStudent = queryClientSpecific.getQueryState(
      "/api/rosterstudents/course/7",
    ).dataUpdateCount;
    const upload = screen.getByTestId("RosterStudentCSVUploadForm-upload");
    const submitButton = screen.getByTestId(
      "RosterStudentCSVUploadForm-submit",
    );
    await user.upload(upload, file);
    fireEvent.click(submitButton);
    await waitFor(() => {
      expect(axiosMock.history.post[0].params).toEqual({
        courseId: "7",
      });
    });
    expect(axiosMock.history.post[0].data.get("file")).toEqual(file);
    expect(mockToast).toBeCalledWith("Roster successfully updated.");
    expect(
      queryClientSpecific.getQueryState("/api/courses/7").dataUpdateCount,
    ).toEqual(alternateUpdateCount);
    expect(
      queryClientSpecific.getQueryState("/api/rosterstudents/course/7")
        .dataUpdateCount,
    ).toEqual(updateCountStudent + 1);
  });
});
