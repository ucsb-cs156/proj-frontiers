import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";

import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import coursesFixtures from "fixtures/coursesFixtures";
import HomePageLoggedIn from "main/pages/HomePageLoggedIn";
import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import { React } from "react";

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

    expect(screen.getByText("Your Student Courses")).toBeInTheDocument();

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

    expect(screen.getByText("Your Staff Courses")).toBeInTheDocument();

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


  test("tables don't render when there are no staffCourses or courses", async () => {
    setupUserOnly();
    axiosMock
      .onGet("/api/courses/staffCourses")
      .reply(200, []);
    axiosMock
      .onGet("/api/courses/list")
      .reply(200, []);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <HomePageLoggedIn />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await waitFor(() => {
      expect(
        screen.getByText("Your Student Courses")
      ).toBeInTheDocument();
    });
    expect(
      screen.getByText("You are not enrolled in any student courses yet."),
    ).toBeInTheDocument();

    expect(
      screen.queryByText("Your Staff Courses"),
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
    expect(screen.queryByText("No instructor courses yet. Click the button above to create one.")).not.toBeInTheDocument();
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

    expect(screen.queryByTestId("InstructorCoursesTable")).toBeInTheDocument();
  });

  test("table doesn't render for instructors when courses don't exist", async () => {
    setupInstructorUser();
    axiosMock
      .onGet("/api/courses/allForInstructors")
      .reply(200, []);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <HomePageLoggedIn />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await waitFor(() => {
      expect(screen.queryByText("Your Instructor Courses")).toBeInTheDocument()
    })

    expect(screen.queryByTestId(`InstructorCoursesTable-cell-row-0-col-id`)).not.toBeInTheDocument();
    expect(
      screen.queryByTestId(`InstructorCoursesTable-cell-row-1-col-id`),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByTestId(`InstructorCoursesTable-cell-row-2-col-id`),
    ).not.toBeInTheDocument();

    const orgName = screen.queryByText("wsu-cpts489-fa20");
    expect(orgName).not.toBeInTheDocument();

    expect(screen.queryByText("Create Course")).toBeInTheDocument();
    expect(screen.queryByText("No instructor courses yet. Click the button above to create one.")).toBeInTheDocument();

    expect(screen.queryByTestId("InstructorCoursesTable")).not.toBeInTheDocument();
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
    expect(axiosMock.history.put[0].params).toEqual({ rosterStudentId: 22 });
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
    expect(axiosMock.history.put[1].params).toEqual({ courseStaffId: 32 });
    await waitFor(() => {
      expect(mockToast).toBeCalledWith(
        "Successfully invited staff member to Organization",
      );
    });

    expect(
      queryClient.getQueryState(["/api/courses/list"]).dataUpdateCount,
    ).toBe(2);
    expect(
      queryClient.getQueryState(["/api/courses/staffCourses"]).dataUpdateCount,
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
    expect(axiosMock.history.put[0].params).toEqual({ rosterStudentId: 22 });
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
    expect(axiosMock.history.put[0].params).toEqual({ rosterStudentId: 22 });
    await waitFor(() => {
      expect(mockToast).toBeCalledWith("Request failed with status code 404");
    });
  });

  test("Loading message renders, student", async () => {
    setupUserOnly();
    axiosMock.onGet("/api/courses/staffCourses").reply(200, [
      ...coursesFixtures.oneStaffMemberWithEachStatus,
      {
        id: 7,
        staffId: 36,
        courseName: "CMPSC 130B",
        term: "Spring 2026",
        school: "UCSB",
        orgName: "ucsb-cs130b-s26",
        studentStatus: "JOINCOURSE",
      },
    ]);
    axiosMock.onGet("/api/courses/list").reply(200, [
      ...coursesFixtures.oneRosterStudentWithEachStatus,
      {
        id: 7,
        rosterStudentId: 26,
        courseName: "CMPSC 130B",
        term: "Spring 2026",
        school: "UCSB",
        orgName: "ucsb-cs130b-s26",
        studentStatus: "JOINCOURSE",
      },
    ]);
    axiosMock
      .onPut("/api/rosterstudents/joinCourse")
      .withDelayInMs(5000)
      .reply(202, "Successfully invited student to Organization");

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
    await screen.findByText("Joining...");
    expect(
      screen.getByTestId("CoursesTable-cell-row-6-col-studentStatus-button"),
    ).toHaveTextContent("Join Course");
  });

  test("Loading message renders, staff", async () => {
    setupUserOnly();
    axiosMock.onGet("/api/courses/staffCourses").reply(200, [
      ...coursesFixtures.oneStaffMemberWithEachStatus,
      {
        id: 7,
        staffId: 36,
        courseName: "CMPSC 130B",
        term: "Spring 2026",
        school: "UCSB",
        orgName: "ucsb-cs130b-s26",
        studentStatus: "JOINCOURSE",
      },
    ]);
    axiosMock.onGet("/api/courses/list").reply(200, [
      ...coursesFixtures.oneRosterStudentWithEachStatus,
      {
        id: 7,
        rosterStudentId: 26,
        courseName: "CMPSC 130B",
        term: "Spring 2026",
        school: "UCSB",
        orgName: "ucsb-cs130b-s26",
        studentStatus: "JOINCOURSE",
      },
    ]);
    axiosMock
      .onPut("/api/coursestaff/joinCourse")
      .withDelayInMs(5000)
      .reply(202, "Successfully invited staff member to Organization");

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <HomePageLoggedIn />
        </MemoryRouter>
      </QueryClientProvider>,
    );
    await screen.findAllByText("Join Course");
    const studentJoinButton = screen.getByTestId(
      "StaffCoursesTable-cell-row-1-col-studentStatus-button",
    );
    fireEvent.click(studentJoinButton);
    await screen.findByText("Joining...");
    expect(
      screen.getByTestId(
        "StaffCoursesTable-cell-row-6-col-studentStatus-button",
      ),
    ).toHaveTextContent("Join Course");
  });
  test("Can submit new course", async () => {
    setupInstructorUser();
    axiosMock
      .onPost("/api/courses/post")
      .reply(200, coursesFixtures.severalCourses[0]);
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

    const createCourse = screen.getByText("Create Course");
    expect(createCourse).toHaveClass("btn btn-primary");
    expect(createCourse).toHaveStyle("float: right; margin-bottom: 10px;");
    fireEvent.click(createCourse);

    await screen.findByLabelText("Course Name");
    const courseName = screen.getByLabelText("Course Name");
    const courseTerm = screen.getByLabelText("Term");
    const school = screen.getByLabelText("School");
    fireEvent.change(courseName, { target: { value: "CMPSC 156" } });
    fireEvent.change(courseTerm, { target: { value: "Spring 2025" } });
    fireEvent.change(school, { target: { value: "UCSB" } });
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
});
