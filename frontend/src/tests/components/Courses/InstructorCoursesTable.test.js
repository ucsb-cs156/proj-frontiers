import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import coursesFixtures from "fixtures/coursesFixtures";
import { currentUserFixtures } from "fixtures/currentUserFixtures";
import InstructorCoursesTable from "main/components/Courses/InstructorCoursesTable";
import { BrowserRouter } from "react-router";

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
      "Actions",
    ];
    const expectedFields = [
      "id",
      "courseName",
      "term",
      "school",
      "createdByEmail",
      "actions",
    ];

    expectedHeaders.forEach((headerText) => {
      const header = screen.getByText(headerText);
      expect(header).toBeInTheDocument();
    });

    expectedFields.forEach((field) => {
      const header = screen.getByTestId(`${testId}-cell-row-0-col-${field}`);
      expect(header).toBeInTheDocument();
    });

    expect(screen.getByText("GitHub Org")).toBeInTheDocument();

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

    expect(screen.getByText("ucsb-cs156-s25")).toBeInTheDocument();
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
    expect(button3).toHaveTextContent("Install GitHub App");
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
    expect(button3).toHaveTextContent("Install GitHub App");
    expect(button3).toHaveAttribute("class", "btn btn-primary");

    const button4 = screen.getByTestId(
      `${testId}-cell-row-3-col-orgName-button`,
    );
    expect(button4).toBeInTheDocument();
    expect(button4).toHaveTextContent("Install GitHub App");
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
    expect(button).toHaveTextContent("Install GitHub App");
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
    expect(button3).toHaveTextContent("Install GitHub App");
    expect(button3).toHaveAttribute("class", "btn btn-primary");

    fireEvent.click(button3);

    await waitFor(() => {
      expect(window.alert).not.toHaveBeenCalled();
    });

    expect(window.location.href).toBe("/api/courses/redirect?courseId=3");
  });

  test("Tests for GitHub link and icon", async () => {
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
    expect(button3).toHaveTextContent("Install GitHub App");
    expect(button3).toHaveAttribute("class", "btn btn-primary");

    fireEvent.click(button3);

    await waitFor(() => {
      expect(window.alert).not.toHaveBeenCalled();
    });

    expect(window.location.href).toBe("/api/courses/redirect?courseId=3");
  });
  test("expect the correct tooltip ID for the courseName tooltips", async () => {
    render(
      <BrowserRouter>
        <InstructorCoursesTable
          courses={coursesFixtures.severalCourses}
          currentUser={currentUserFixtures.instructorUser}
          storybook={false}
        />
      </BrowserRouter>,
    );

    fireEvent.mouseOver(screen.getByText("CPTS 489"));

    const tooltip = await screen.findByRole("tooltip");
    expect(tooltip).toHaveAttribute("id", "tooltip-coursename-1");
  });
  test("expect the correct tooltip ID for the orgName tooltips", async () => {
    render(
      <BrowserRouter>
        <InstructorCoursesTable
          courses={coursesFixtures.severalCourses}
          currentUser={currentUserFixtures.instructorUser}
          storybook={false}
        />
      </BrowserRouter>,
    );

    fireEvent.mouseOver(screen.getByText("wsu-cpts489-fa20"));

    const tooltip = await screen.findByRole("tooltip");
    expect(tooltip).toHaveAttribute("id", "tooltip-orgname-1");
  });
  test("expect the correct tooltip ID for the github icon (that redirects to github installation settings)", async () => {
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

    fireEvent.mouseOver(githubIcon);

    const tooltip = await screen.findByRole("tooltip");
    expect(tooltip).toHaveAttribute("id", "tooltip-githubicon-0");
  });
  test("the correct tooltip renders for courseName", async () => {
    render(
      <BrowserRouter>
        <InstructorCoursesTable
          courses={coursesFixtures.severalCourses}
          currentUser={currentUserFixtures.instructorUser}
          storybook={false}
        />
      </BrowserRouter>,
    );

    fireEvent.mouseOver(screen.getByText("CPTS 489"));

    await waitFor(() => {
      expect(screen.getByText("View course details")).toBeInTheDocument();
    });
  });
  test("the correct tooltip renders for orgName when a GitHub organization exists for the course", async () => {
    render(
      <BrowserRouter>
        <InstructorCoursesTable
          courses={coursesFixtures.severalCourses}
          currentUser={currentUserFixtures.instructorUser}
          storybook={false}
        />
      </BrowserRouter>,
    );

    fireEvent.mouseOver(screen.getByText("wsu-cpts489-fa20"));

    await waitFor(() => {
      expect(
        screen.getByText("View GitHub organization: wsu-cpts489-fa20"),
      ).toBeInTheDocument();
    });
  });
  test("the correct tooltip renders for orgName when a GitHub organization does NOT exist for the course", async () => {
    render(
      <BrowserRouter>
        <InstructorCoursesTable
          courses={coursesFixtures.severalCourses}
          currentUser={currentUserFixtures.instructorUser}
          storybook={false}
        />
      </BrowserRouter>,
    );

    fireEvent.mouseOver(screen.getByText("Install GitHub App"));

    await waitFor(() => {
      expect(
        screen.getByText(
          "Click to install the GitHub app for the course: CMPSC 156",
        ),
      ).toBeInTheDocument();
    });
  });
  test("the correct tooltip renders for GitHub icon (that redirects to github installation settings)", async () => {
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

    fireEvent.mouseOver(githubIcon);

    await waitFor(() => {
      expect(
        screen.getByText(
          "Manage installation settings for the frontiers app, including the option to uninstall it from this GitHub organization.",
        ),
      ).toBeInTheDocument();
    });
  });
  test("Edit button is visible for admin user and calls onEditCourse when clicked", async () => {
    const mockEditCourse = jest.fn();
    
    render(
      <BrowserRouter>
        <InstructorCoursesTable
          courses={coursesFixtures.severalCourses}
          currentUser={currentUserFixtures.adminUser}
          onEditCourse={mockEditCourse}
        />
      </BrowserRouter>,
    );

    // Check that the edit button is visible for all courses for admin
    const editButton0 = screen.getByTestId(`${testId}-cell-row-0-col-actions-edit-button`);
    expect(editButton0).toBeInTheDocument();
    expect(editButton0).toHaveTextContent("Edit");
    
    const editButton1 = screen.getByTestId(`${testId}-cell-row-1-col-actions-edit-button`);
    expect(editButton1).toBeInTheDocument();
    
    // Click the edit button and check that onEditCourse is called with the correct course
    fireEvent.click(editButton0);
    expect(mockEditCourse).toHaveBeenCalledTimes(1);
    expect(mockEditCourse).toHaveBeenCalledWith(coursesFixtures.severalCourses[0]);
  });

  test("Edit button is visible only for courses created by the instructor", async () => {
    const mockEditCourse = jest.fn();
    
    // Create a modified fixture where the first course is created by the instructor
    const modifiedCourses = [...coursesFixtures.severalCourses];
    modifiedCourses[0] = {
      ...modifiedCourses[0],
      createdByEmail: currentUserFixtures.instructorUser.root.user.email
    };
    
    render(
      <BrowserRouter>
        <InstructorCoursesTable
          courses={modifiedCourses}
          currentUser={currentUserFixtures.instructorUser}
          onEditCourse={mockEditCourse}
        />
      </BrowserRouter>,
    );

    // Check that the edit button is visible only for the course created by the instructor
    const editButton0 = screen.getByTestId(`${testId}-cell-row-0-col-actions-edit-button`);
    expect(editButton0).toBeInTheDocument();
    expect(editButton0).toHaveTextContent("Edit");
    
    // Check that the edit button is not visible for other courses
    const editButton1 = screen.queryByTestId(`${testId}-cell-row-1-col-actions-edit-button`);
    expect(editButton1).not.toBeInTheDocument();
    
    // Click the edit button and check that onEditCourse is called with the correct course
    fireEvent.click(editButton0);
    expect(mockEditCourse).toHaveBeenCalledTimes(1);
    expect(mockEditCourse).toHaveBeenCalledWith(modifiedCourses[0]);
  });

  test("Edit button is not rendered when onEditCourse is not provided", async () => {
    render(
      <BrowserRouter>
        <InstructorCoursesTable
          courses={coursesFixtures.severalCourses}
          currentUser={currentUserFixtures.adminUser}
        />
      </BrowserRouter>,
    );

    // Check that no edit buttons are rendered
    const editButton = screen.queryByText("Edit");
    expect(editButton).not.toBeInTheDocument();
  });
});
