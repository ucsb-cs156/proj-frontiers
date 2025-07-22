import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "react-query";
import { MemoryRouter } from "react-router-dom";

import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import coursesFixtures from "fixtures/coursesFixtures";
import HomePageLoggedIn from "main/pages/HomePageLoggedIn";
import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";

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

describe("HomePageLoggedIn tests", () => {
  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    queryClient.clear();
    mockToast.mockReset();
  });

  const setupUserOnly = () => {
    axiosMock
      .onGet("/api/currentUser")
      .reply(200, apiCurrentUserFixtures.userOnly);
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.showingNeither);
  };

  test("tables render correctly", async () => {
    setupUserOnly();
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

    await waitFor(() => {
      expect(
        screen.getByTestId(`CoursesTable-cell-row-0-col-id`),
      ).toHaveTextContent("1");
    });
    expect(
      screen.getByTestId(`CoursesTable-cell-row-1-col-id`),
    ).toHaveTextContent("2");
    expect(
      screen.getByTestId(`CoursesTable-cell-row-2-col-id`),
    ).toHaveTextContent("3");
    expect(
      screen.getByTestId(`CoursesTable-cell-row-0-col-courseName`),
    ).toHaveTextContent("CMPSC 156");
    expect(
      screen.getByTestId(`CoursesTable-cell-row-0-col-term`),
    ).toHaveTextContent("Spring 2025");
    expect(
      screen.getByTestId(`CoursesTable-cell-row-0-col-school`),
    ).toHaveTextContent("UCSB");
    expect(
      screen.getByTestId(`CoursesTable-cell-row-0-col-studentStatus`),
    ).toHaveTextContent("Pending");

    await waitFor(() => {
      expect(
        screen.getByTestId(`StaffCoursesTable-cell-row-0-col-id`),
      ).toHaveTextContent("1");
    });
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
      screen.getByTestId(`StaffCoursesTable-cell-row-0-col-term`),
    ).toHaveTextContent("Spring 2025");
    expect(
      screen.getByTestId(`StaffCoursesTable-cell-row-0-col-school`),
    ).toHaveTextContent("UCSB");
    expect(
      screen.getByTestId(`StaffCoursesTable-cell-row-0-col-studentStatus`),
    ).toHaveTextContent("Pending");
  });

  test("join callbacks work", async () => {
    setupUserOnly();
    axiosMock
      .onGet("/api/courses/staffCourses")
      .reply(200, coursesFixtures.oneStaffMemberWithEachStatus);
    axiosMock
      .onGet("/api/courses/list")
      .reply(200, coursesFixtures.oneRosterStudentWithEachStatus);
    axiosMock
      .onPut("/api/rosterstudents/joinCourse")
      .reply(200, "Successfully invited student to Organization");
    axiosMock
      .onPut("/api/coursestaff/joinCourse")
      .reply(200, "Successfully invited staff member to Organization");

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <HomePageLoggedIn />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findAllByText("Join Course");
    const studentJoinButton = screen.getByTestId(
      "CoursesTable-cell-row-1-col-studentStatus-button",
    );
    fireEvent.click(studentJoinButton);
    await waitFor(() => expect(axiosMock.history.put.length).toBe(1));
    expect(axiosMock.history.put[0].url).toBe("/api/rosterstudents/joinCourse");
    expect(axiosMock.history.put[0].params).toEqual({ rosterStudentId: 2 });
    await waitFor(() => {
      expect(mockToast).toBeCalledWith(
        "Successfully invited student to Organization",
      );
    });

    const courseStaffJoinButton = screen.getByTestId(
      "StaffCoursesTable-cell-row-1-col-studentStatus-button",
    );

    fireEvent.click(courseStaffJoinButton);
    await waitFor(() => expect(axiosMock.history.put.length).toBe(2));
    expect(axiosMock.history.put[1].url).toBe("/api/coursestaff/joinCourse");
    expect(axiosMock.history.put[1].params).toEqual({ courseStaffId: 2 });
    await waitFor(() => {
      expect(mockToast).toBeCalledWith(
        "Successfully invited staff member to Organization",
      );
    });

    expect(queryClient.getQueryState("/api/courses/list").dataUpdateCount).toBe(
      2,
    );
    expect(
      queryClient.getQueryState("/api/courses/staffCourses").dataUpdateCount,
    ).toBe(2);
  });

  test("right message on 400", async () => {
    setupUserOnly();
    axiosMock
      .onGet("/api/courses/staffCourses")
      .reply(200, coursesFixtures.oneStaffMemberWithEachStatus);
    axiosMock
      .onGet("/api/courses/list")
      .reply(200, coursesFixtures.oneRosterStudentWithEachStatus);
    axiosMock
      .onPut("/api/rosterstudents/joinCourse")
      .reply(
        400,
        "Course has not been set up. Please ask your instructor for help.",
      );

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <HomePageLoggedIn />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findAllByText("Join Course");
    const studentJoinButton = screen.getByTestId(
      "CoursesTable-cell-row-1-col-studentStatus-button",
    );
    fireEvent.click(studentJoinButton);
    await waitFor(() => expect(axiosMock.history.put.length).toBe(1));
    expect(axiosMock.history.put[0].url).toBe("/api/rosterstudents/joinCourse");
    expect(axiosMock.history.put[0].params).toEqual({ rosterStudentId: 2 });
    await waitFor(() => {
      expect(mockToast).toBeCalledWith(
        "Course has not been set up. Please ask your instructor for help.",
      );
    });
  });

  test("right message on 404", async () => {
    setupUserOnly();
    axiosMock
      .onGet("/api/courses/staffCourses")
      .reply(200, coursesFixtures.oneStaffMemberWithEachStatus);
    axiosMock
      .onGet("/api/courses/list")
      .reply(200, coursesFixtures.oneRosterStudentWithEachStatus);
    axiosMock.onPut("/api/rosterstudents/joinCourse").reply(404);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <HomePageLoggedIn />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findAllByText("Join Course");
    const studentJoinButton = screen.getByTestId(
      "CoursesTable-cell-row-1-col-studentStatus-button",
    );
    fireEvent.click(studentJoinButton);
    await waitFor(() => expect(axiosMock.history.put.length).toBe(1));
    expect(axiosMock.history.put[0].url).toBe("/api/rosterstudents/joinCourse");
    expect(axiosMock.history.put[0].params).toEqual({ rosterStudentId: 2 });
    await waitFor(() => {
      expect(mockToast).toBeCalledWith("Request failed with status code 404");
    });
  });
});
