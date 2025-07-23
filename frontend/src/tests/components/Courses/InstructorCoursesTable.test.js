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
      `${testId}-cell-row-0-col-orgName-github-link`,
    );
    expect(row0_already_installed).toBeInTheDocument();
    expect(row0_already_installed).toHaveTextContent("ucsb-cs156-s25");
    expect(row0_already_installed).toHaveAttribute(
      "href",
      "https://github.com/ucsb-cs156-s25",
    );

    const div0 = screen.getByTestId(`${testId}-cell-row-0-col-orgName-div`);
    expect(div0).toBeInTheDocument();
    expect(div0).toHaveAttribute(
      "style",
      "display: flex; justify-content: space-between; width: 100%;",
    );

    const button3 = screen.getByTestId(
      `${testId}-cell-row-2-col-orgName-button`,
    );
    expect(button3).toBeInTheDocument();
    expect(button3).toHaveTextContent("Install Github App");
    expect(button3).toHaveAttribute("class", "btn btn-primary");

    const noOrgSpan = screen.getByTestId(
      `${testId}-cell-row-3-col-orgName-no-org`,
    );
    expect(noOrgSpan).toBeInTheDocument();
    expect(noOrgSpan).toBeEmptyDOMElement();

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

    const button4 = screen.getByTestId(
      `${testId}-cell-row-3-col-orgName-button`,
    );
    expect(button4).toBeInTheDocument();
    expect(button4).toHaveTextContent("Install Github App");
    expect(button4).toHaveAttribute("class", "btn btn-primary");
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

  test("Tests for Github link and icon", async () => {
    render(
      <BrowserRouter>
        <InstructorCoursesTable
          courses={coursesFixtures.severalCourses}
          currentUser={currentUserFixtures.instructorUser}
          storybook={false}
        />
      </BrowserRouter>,
    );

    const githubIcon = screen.getByTestId(
      `CoursesTable-cell-row-0-col-orgName-github-icon`,
    );
    expect(githubIcon).toBeInTheDocument();
    expect(githubIcon).toHaveAttribute("height", "1.5em");
    expect(githubIcon).toHaveAttribute("width", "1.5em");

    const githubLink = screen.getByTestId(
      `CoursesTable-cell-row-0-col-orgName-github-settings-link`,
    );
    expect(githubLink).toBeInTheDocument();
    expect(githubLink).toHaveAttribute(
      "href",
      "https://github.com/organizations/ucsb-cs156-s25/settings/installations/123456",
    );
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
