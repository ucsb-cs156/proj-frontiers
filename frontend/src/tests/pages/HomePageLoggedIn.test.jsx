import {
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router";

import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import coursesFixtures from "fixtures/coursesFixtures";
import HomePageLoggedIn from "main/pages/HomePageLoggedIn";
import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import { React } from "react";
import { vi } from "vitest";
import * as useBackendModule from "main/utils/useBackend";
import { schoolList } from "fixtures/schoolFixtures";

const axiosMock = new AxiosMockAdapter(axios);
const queryClient = new QueryClient();

const useBackendSpy = vi.spyOn(useBackendModule, "useBackend");
const useBackendMutationSpy = vi.spyOn(useBackendModule, "useBackendMutation");

const mockToast = vi.fn();
vi.mock("react-toastify", async (importOriginal) => {
  return {
    ...(await importOriginal()),
    toast: (x) => mockToast(x),
  };
});

describe("HomePageLoggedIn tests", () => {
  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    queryClient.clear();
    mockToast.mockReset();
  });
  afterEach(() => {
    useBackendSpy.mockClear();
    useBackendMutationSpy.mockClear();
  });

  const setupUserOnly = () => {
    axiosMock
      .onGet("/api/currentUser")
      .reply(200, apiCurrentUserFixtures.userOnly);
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.showingNeither);
  };

  const setupInstructorUser = () => {
    axiosMock
      .onGet("/api/currentUser")
      .reply(200, apiCurrentUserFixtures.instructorUser);
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.showingNeither);
  };

  test("tables render correctly", async () => {
    setupUserOnly();
    axiosMock
      .onGet("/api/courses/staffCourses")
      .reply(200, coursesFixtures.severalCourses);
    axiosMock
      .onGet("/api/courses/list")
      .reply(200, coursesFixtures.oneCourseWithEachStatus);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <HomePageLoggedIn />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await waitFor(() => {
      expect(
        screen.getByTestId("CoursesTable-cell-row-0-col-id"),
      ).toHaveTextContent("1");
    });
    expect(
      screen.getByTestId(`StaffCoursesTable-cell-row-0-col-id`),
    ).toHaveTextContent("1");
    expect(
      screen.getByTestId(`StaffCoursesTable-cell-row-1-col-id`),
    ).toHaveTextContent("2");
    expect(
      screen.getByTestId(`StaffCoursesTable-cell-row-2-col-id`),
    ).toHaveTextContent("3");
    expect(
      screen.getByTestId(`StaffCoursesTable-cell-row-0-col-courseName`),
    ).toHaveTextContent("CMPSC 156");
    expect(
      screen
        .getAllByTestId(`CoursesTable-cell-row-0-col-courseName-link`)
        .some((link) => link.getAttribute("href") === "/staff/courses/1"),
    ).toBe(true);
    expect(
      screen.getByTestId(`StaffCoursesTable-cell-row-0-col-term`),
    ).toHaveTextContent("Spring 2025");
    expect(
      screen.getByTestId(`StaffCoursesTable-cell-row-0-col-school`),
    ).toHaveTextContent("UCSB");
    expect(
      screen.getByTestId(`StaffCoursesTable-cell-row-0-col-numStudents`),
    ).toHaveTextContent("25");
    expect(
      screen.getByTestId(`StaffCoursesTable-cell-row-0-col-numStaff`),
    ).toHaveTextContent("3");
    expect(screen.getByText("GitHub Org")).toBeInTheDocument();
    expect(screen.getByText("Instructor")).toBeInTheDocument();
    expect(
      screen.getByTestId(`StaffCoursesTable-cell-row-0-col-edit-button`),
    ).toBeInTheDocument();
    expect(
      screen.queryByTestId(`StaffCoursesTable-cell-row-0-col-delete-button`),
    ).not.toBeInTheDocument();
  });

  test("staff courses section renders empty message when there are no staffCourses", async () => {
    setupUserOnly();
    axiosMock.onGet("/api/courses/staffCourses").reply(200, []);
    axiosMock.onGet("/api/courses/list").reply(200, []);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <HomePageLoggedIn />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(screen.getByText("Your Staff Courses")).toBeInTheDocument();
    expect(screen.getByText("No staff courses yet.")).toBeInTheDocument();
    expect(
      screen.queryByTestId(`StaffCoursesTable-cell-row-0-col-id`),
    ).not.toBeInTheDocument();
  });

  test("tables render correctly for instructor when courses exist", async () => {
    setupInstructorUser();
    axiosMock
      .onGet("/api/courses/allForInstructors")
      .reply(200, coursesFixtures.severalCourses);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <HomePageLoggedIn />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await waitFor(() => {
      expect(
        screen.getByTestId(`InstructorCoursesTable-cell-row-0-col-id`),
      ).toHaveTextContent("1");
    });
    expect(screen.getByText("Create Course")).toBeInTheDocument();
    expect(screen.getByText("Your Instructor Courses")).toBeInTheDocument();
    expect(
      screen.queryByText(
        "No instructor courses yet. Click the button above to create one.",
      ),
    ).not.toBeInTheDocument();
    expect(
      screen.getByTestId(`InstructorCoursesTable-cell-row-1-col-id`),
    ).toHaveTextContent("2");
    expect(
      screen.getByTestId(`InstructorCoursesTable-cell-row-2-col-id`),
    ).toHaveTextContent("3");

    const orgName = screen.getByText("wsu-cpts489-fa20");
    expect(orgName).toBeInTheDocument();

    const button2 = screen.queryByTestId(
      "InstructorCoursesTable-cell-row-2-col-orgName-button",
    );
    expect(button2).toBeInTheDocument();
    expect(button2).toHaveTextContent("Install GitHub App");

    expect(screen.getByTestId("InstructorCoursesTable")).toBeInTheDocument();
  });

  test("table doesn't render for instructors when courses don't exist", async () => {
    setupInstructorUser();
    axiosMock.onGet("/api/courses/allForInstructors").reply(200, []);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <HomePageLoggedIn />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await waitFor(() => {
      expect(screen.getByText("Your Instructor Courses")).toBeInTheDocument();
    });

    expect(
      screen.queryByTestId(`InstructorCoursesTable-cell-row-0-col-id`),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByTestId(`InstructorCoursesTable-cell-row-1-col-id`),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByTestId(`InstructorCoursesTable-cell-row-2-col-id`),
    ).not.toBeInTheDocument();

    const orgName = screen.queryByText("wsu-cpts489-fa20");
    expect(orgName).not.toBeInTheDocument();

    expect(screen.getByText("Create Course")).toBeInTheDocument();
    expect(
      screen.getByText(
        "No instructor courses yet. Click the button above to create one.",
      ),
    ).toBeInTheDocument();

    expect(
      screen.queryByTestId("InstructorCoursesTable"),
    ).not.toBeInTheDocument();
  });

  test("Can submit new course", async () => {
    setupInstructorUser();
    axiosMock
      .onPost("/api/courses/post")
      .reply(200, coursesFixtures.severalCourses[0]);
    axiosMock
      .onGet("/api/courses/allForInstructors")
      .reply(200, coursesFixtures.severalCourses);
    axiosMock.onGet("/api/systemInfo/schools").reply(200, schoolList);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <HomePageLoggedIn />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await waitFor(() => {
      expect(
        screen.getByTestId(`InstructorCoursesTable-cell-row-0-col-id`),
      ).toHaveTextContent("1");
    });

    const createCourse = screen.getByText("Create Course");
    expect(createCourse).toHaveClass("btn btn-primary");
    expect(createCourse).toHaveStyle("float: right; margin-bottom: 10px;");
    fireEvent.click(createCourse);

    await screen.findByLabelText("Course Name");
    const courseName = screen.getByLabelText("Course Name");
    const courseTerm = screen.getByLabelText("Term");
    const school = screen.getByTestId("CourseModal-school");
    fireEvent.change(courseName, { target: { value: "CMPSC 156" } });
    fireEvent.change(courseTerm, { target: { value: "Spring 2025" } });
    fireEvent.change(school, { target: { value: "UCSB" } });
    fireEvent.click(
      await within(screen.getByTestId("CourseModal-base")).findByText("UCSB"),
    );
    fireEvent.click(screen.getByText("Create"));
    await waitFor(() => expect(axiosMock.history.post.length).toBe(1));
    expect(axiosMock.history.post[0].url).toBe("/api/courses/post");
    expect(axiosMock.history.post[0].params).toEqual({
      courseName: "CMPSC 156",
      term: "Spring 2025",
      school: "UCSB",
    });
    await waitFor(() =>
      expect(mockToast).toBeCalledWith("Course CMPSC 156 created"),
    );
    expect(
      queryClient.getQueryState(["/api/courses/allForInstructors"]),
    ).toBeTruthy();
    expect(screen.queryByTestId("CourseModal-base")).not.toBeInTheDocument();
  });

  test("toast called on instructor error", async () => {
    setupInstructorUser();
    axiosMock.onGet("/api/courses/allForInstructors").reply(500);
    axiosMock
      .onGet("/api/courses/staffCourses")
      .reply(200, coursesFixtures.oneCourseWithEachStatus);
    axiosMock
      .onGet("/api/courses/list")
      .reply(200, coursesFixtures.oneCourseWithEachStatus);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <HomePageLoggedIn />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await waitFor(() => expect(mockToast).toHaveBeenCalled());
  });
  test("useBackend and useBackendMutation are called with correct cache query key", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <HomePageLoggedIn />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(useBackendSpy).toHaveBeenNthCalledWith(
      1,
      ["/api/courses/staffCourses"],
      { method: "GET", url: "/api/courses/staffCourses" },
      [],
    );

    expect(useBackendSpy).toHaveBeenNthCalledWith(
      2,
      ["/api/courses/allForInstructors"],
      { method: "GET", url: "/api/courses/allForInstructors" },
      [],
      false,
      {
        enabled: false,
      },
    );

    expect(useBackendMutationSpy).toHaveBeenNthCalledWith(
      1,
      expect.any(Function),
      { onSuccess: expect.any(Function) },
      ["/api/courses/allForInstructors"],
    );
  });
});
