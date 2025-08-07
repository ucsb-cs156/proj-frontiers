import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import CoursesIndexPage from "main/pages/Admin/CoursesIndexPage";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router";
import mockConsole from "jest-mock-console";
import coursesFixtures from "fixtures/coursesFixtures";

import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";

let axiosMock = new AxiosMockAdapter(axios);

const mockToast = jest.fn();

jest.mock("react-toastify", () => {
  const originalModule = jest.requireActual("react-toastify");
  return {
    __esModule: true,
    ...originalModule,
    toast: (x) => mockToast(x),
  };
});

describe("CoursesIndexPage tests", () => {
  const testId = "InstructorCoursesTable";

  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    queryClient.clear();
  });

  const setupAdminUser = () => {
    axiosMock
      .onGet("/api/currentUser")
      .reply(200, apiCurrentUserFixtures.adminUser);
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.showingNeither);
  };

  const queryClient = new QueryClient();

  test("Renders for admin user", async () => {
    setupAdminUser();
    axiosMock.onGet("/api/courses/allForAdmins").reply(200, []);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <CoursesIndexPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await waitFor(() => {
      expect(screen.getByText(/Courses/)).toBeInTheDocument();
    });
  });

  test("renders correctly for admin user", async () => {
    setupAdminUser();
    axiosMock
      .onGet("/api/courses/allForAdmins")
      .reply(200, coursesFixtures.severalCourses);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <CoursesIndexPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await waitFor(() => {
      expect(
        screen.getByTestId(`${testId}-cell-row-0-col-id`),
      ).toHaveTextContent("1");
    });
    expect(screen.getByTestId(`${testId}-cell-row-1-col-id`)).toHaveTextContent(
      "2",
    );
    expect(screen.getByTestId(`${testId}-cell-row-2-col-id`)).toHaveTextContent(
      "3",
    );

    const orgName = screen.getByText("wsu-cpts489-fa20");
    expect(orgName).toBeInTheDocument();

    // For an admin user, the next two courses should have a button, not an org name
    const button3 = screen.queryByTestId(
      `${testId}-cell-row-2-col-orgName-button`,
    );
    expect(button3).toBeInTheDocument();
    expect(button3).toHaveTextContent("Install GitHub App");

    const button4 = screen.queryByTestId(
      `${testId}-cell-row-3-col-orgName-button`,
    );
    expect(button4).toBeInTheDocument();
    expect(button4).toHaveTextContent("Install GitHub App");
  });

  test("renders empty table when backend unavailable, admin only", async () => {
    setupAdminUser();

    axiosMock.onGet("/api/courses/allForAdmins").timeout();

    const restoreConsole = mockConsole();

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <CoursesIndexPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await waitFor(() => {
      expect(axiosMock.history.get.length).toBeGreaterThanOrEqual(1);
    });

    const errorMessage = console.error.mock.calls[0][0];
    expect(errorMessage).toMatch(
      "Error communicating with backend via GET on /api/courses/allForAdmins",
    );
    restoreConsole();
  });

  test("Can submit new course", async () => {
    setupAdminUser();
    axiosMock
      .onPost("/api/courses/post")
      .reply(200, coursesFixtures.severalCourses[0]);
    axiosMock
      .onGet("/api/courses/allForAdmins")
      .reply(200, coursesFixtures.severalCourses);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <CoursesIndexPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

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
      queryClient.getQueryState(["/api/courses/allForAdmins"]),
    ).toBeTruthy();
    expect(screen.queryByTestId("CourseModal-base")).not.toBeInTheDocument();
  });

  test("Can edit an existing course", async () => {
    setupAdminUser();

    // Mock the GET request for courses
    axiosMock
      .onGet("/api/courses/all")
      .reply(200, coursesFixtures.severalCourses);

    // Mock the PUT request for updating a course
    const updatedCourse = {
      ...coursesFixtures.severalCourses[0],
      courseName: "Updated Course Name",
      term: "Fall 2025",
      school: "Updated School",
    };

    axiosMock.onPut("/api/courses").reply(200, updatedCourse);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <CoursesIndexPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    // Wait for the table to load
    await waitFor(() => {
      expect(
        screen.getByTestId(`${testId}-cell-row-0-col-id`),
      ).toHaveTextContent("1");
    });

    // Find and click the edit button for the first course
    const editButton = screen.getByTestId(
      `${testId}-cell-row-0-col-actions-edit-button`,
    );
    expect(editButton).toBeInTheDocument();
    fireEvent.click(editButton);

    // Wait for the modal to open and verify it has the correct title (testing title prop)
    await screen.findByText("Edit Course");
    
    // Wait for the form fields to be populated with the course data
    await waitFor(() => {
      const inputs = screen.getAllByRole("textbox");
      expect(inputs[0].value).toBe("CMPSC 156");
      expect(inputs[1].value).toBe("Spring 2025");
      expect(inputs[2].value).toBe("UCSB");
    });

    // Change the course values
    const courseName = screen.getByLabelText("Course Name");
    const courseTerm = screen.getByLabelText("Term");
    const school = screen.getByLabelText("School");

    fireEvent.change(courseName, { target: { value: "Updated Course Name" } });
    fireEvent.change(courseTerm, { target: { value: "Fall 2025" } });
    fireEvent.change(school, { target: { value: "Updated School" } });

    // Submit the form
    fireEvent.click(screen.getByText("Update"));

    // Verify the correct API call was made
    await waitFor(() => expect(axiosMock.history.put.length).toBe(1));
    expect(axiosMock.history.put[0].url).toBe("/api/courses");
    expect(axiosMock.history.put[0].params).toEqual({
      courseId: 1,
      courseName: "Updated Course Name",
      term: "Fall 2025",
      school: "Updated School",
    });

    // Verify the success toast was shown
    await waitFor(() =>
      expect(mockToast).toBeCalledWith("Course Updated Course Name updated"),
    );

    // Verify the modal is closed
    await waitFor(() =>
      expect(screen.queryByTestId("CourseModal-base")).not.toBeInTheDocument(),
    );
  });
});
