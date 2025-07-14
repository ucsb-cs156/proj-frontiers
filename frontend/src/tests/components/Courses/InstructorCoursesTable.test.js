import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import coursesFixtures from "fixtures/coursesFixtures";
import { currentUserFixtures } from "fixtures/currentUserFixtures";
import InstructorCoursesTable from "main/components/Courses/InstructorCoursesTable";
import { BrowserRouter } from "react-router-dom";

window.alert = jest.fn();

describe("InstructorCoursesTable tests", () => {
  const originalLocation = window.location;

  const testId = "InstructorCoursesTable";

  beforeEach(() => {
    // Remove window.location and mock it
    delete window.location;
    window.location = { href: "" }; // Minimal mock
  });

  afterEach(() => {
    // Restore original window.location
    window.location = originalLocation;
  });

  test("Has the expected column headers and content for instructor user", async () => {
    render(
      <BrowserRouter>
        <InstructorCoursesTable
          courses={coursesFixtures.severalCourses}
          currentUser={currentUserFixtures.instructorUser}
          storybook={true}
        />
      </BrowserRouter>,
    );

    const expectedHeaders = [
      "id",
      "Course Name",
      "Term",
      "School",
      "Created By",
    ];
    const expectedFields = [
      "id",
      "courseName",
      "term",
      "school",
      "createdByEmail",
    ];

    expectedHeaders.forEach((headerText) => {
      const header = screen.getByText(headerText);
      expect(header).toBeInTheDocument();
    });

    expectedFields.forEach((field) => {
      const header = screen.getByTestId(`${testId}-cell-row-0-col-${field}`);
      expect(header).toBeInTheDocument();
    });

    expect(screen.getByText("Github Org")).toBeInTheDocument();

    expect(screen.getByTestId(`${testId}-cell-row-0-col-id`)).toHaveTextContent(
      "1",
    );
    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-courseName`),
    ).toHaveTextContent("CMPSC 156");
    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-term`),
    ).toHaveTextContent("Spring 2025");
    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-school`),
    ).toHaveTextContent("UCSB");

    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-createdByEmail`),
    ).toHaveTextContent("diba@ucsb.edu");

    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-orgName`),
    ).toHaveTextContent("ucsb-cs156-s25");

    const row0_already_installed = screen.getByTestId(
      `${testId}-cell-row-0-col-orgName-cannot-install-already-installed`,
    );
    expect(row0_already_installed).toBeInTheDocument();
    expect(row0_already_installed).toHaveTextContent("ucsb-cs156-s25");

    const button3 = screen.getByTestId(
      `${testId}-cell-row-2-col-orgName-button`,
    );
    expect(button3).toBeInTheDocument();
    expect(button3).toHaveTextContent("Install Github App");
    expect(button3).toHaveAttribute("class", "btn btn-primary");
    expect(button3).toHaveAttribute("data-reason", "instructor-created-course");

    const span4 = screen.getByTestId(
      `${testId}-cell-row-3-col-orgName-cannot-install-not-authorized`,
    );
    expect(span4).toBeInTheDocument();
    // This span should be empty since the course has no orgName
    expect(span4).toBeEmptyDOMElement();

    const firstCourseLink = screen.getByTestId(
      "CoursesTable-cell-row-0-col-courseName-link",
    );
    expect(firstCourseLink).toHaveAttribute("href", "/instructor/courses/1");

    // Make sure that the callback is called when the button is clicked
    fireEvent.click(button3);
    await waitFor(() => {
      expect(window.alert).toHaveBeenCalledTimes(1);
    });
  });

  test("Has the expected column headers and content for admin user", async () => {
    render(
      <BrowserRouter>
        <InstructorCoursesTable
          courses={coursesFixtures.severalCourses}
          currentUser={currentUserFixtures.adminUser}
          storybook={true}
        />
      </BrowserRouter>,
    );

    const button3 = screen.getByTestId(
      `${testId}-cell-row-2-col-orgName-button`,
    );
    expect(button3).toBeInTheDocument();
    expect(button3).toHaveTextContent("Install Github App");
    expect(button3).toHaveAttribute("class", "btn btn-primary");
    expect(button3).toHaveAttribute("data-reason", "admin");

    const button4 = screen.getByTestId(
      `${testId}-cell-row-3-col-orgName-button`,
    );
    expect(button4).toBeInTheDocument();
    expect(button4).toHaveTextContent("Install Github App");
    expect(button4).toHaveAttribute("class", "btn btn-primary");
    expect(button4).toHaveAttribute("data-reason", "admin");
  });

  test("Calls window.alert when the button is pressed on storybook", async () => {
    render(
      <BrowserRouter>
        <InstructorCoursesTable
          courses={coursesFixtures.severalCourses}
          currentUser={currentUserFixtures.instructorUser}
          storybook={true}
        />
      </BrowserRouter>,
    );

    const button = screen.getByTestId(
      `${testId}-cell-row-2-col-orgName-button`,
    );
    expect(button).toBeInTheDocument();
    expect(button).toHaveTextContent("Install Github App");
    expect(button).toHaveAttribute("class", "btn btn-primary");
    fireEvent.click(button);
    await waitFor(() => {
      expect(window.alert).toHaveBeenCalledTimes(1);
    });
    expect(window.alert).toHaveBeenCalledWith(
      "would have navigated to: /api/courses/redirect?courseId=3",
    );
  });

  test("Tests that when storybook is explictly false all still works as expected", async () => {
    render(
      <BrowserRouter>
        <InstructorCoursesTable
          courses={coursesFixtures.severalCourses}
          currentUser={currentUserFixtures.instructorUser}
          storybook={false}
        />
      </BrowserRouter>,
    );

    const button3 = screen.getByTestId(
      `${testId}-cell-row-2-col-orgName-button`,
    );
    expect(button3).toBeInTheDocument();
    expect(button3).toHaveTextContent("Install Github App");
    expect(button3).toHaveAttribute("class", "btn btn-primary");

    fireEvent.click(button3);

    await waitFor(() => {
      expect(window.alert).not.toHaveBeenCalled();
    });

    expect(window.location.href).toBe("/api/courses/redirect?courseId=3");
  });

  test("Tests that when storybook is false by default all works as expected", async () => {
    render(
      <BrowserRouter>
        <InstructorCoursesTable
          courses={coursesFixtures.severalCourses}
          currentUser={currentUserFixtures.instructorUser}
        />
      </BrowserRouter>,
    );

    const button3 = screen.getByTestId(
      `${testId}-cell-row-2-col-orgName-button`,
    );
    expect(button3).toBeInTheDocument();
    expect(button3).toHaveTextContent("Install Github App");
    expect(button3).toHaveAttribute("class", "btn btn-primary");

    fireEvent.click(button3);

    await waitFor(() => {
      expect(window.alert).not.toHaveBeenCalled();
    });

    expect(window.location.href).toBe("/api/courses/redirect?courseId=3");
  });
});
