import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import coursesFixtures from "fixtures/coursesFixtures";
import { currentUserFixtures } from "fixtures/currentUserFixtures";
import InstructorCoursesTable from "main/components/Courses/InstructorCoursesTable";
import { BrowserRouter } from "react-router";
import axios from "axios";
import MockAdapter from "axios-mock-adapter";

window.alert = jest.fn();

describe("InstructorCoursesTable tests", () => {
  const originalLocation = window.location;
  let mockAxios;

  const testId = "InstructorCoursesTable";

  beforeEach(() => {
    // Remove window.location and mock it
    delete window.location;
    window.location = { href: "" }; // Minimal mock
    
    // Setup mock for axios
    mockAxios = new MockAdapter(axios);
    
    // Mock the URL.createObjectURL function
    global.URL.createObjectURL = jest.fn(() => "mock-url");
  });

  afterEach(() => {
    // Restore original window.location
    window.location = originalLocation;
    mockAxios.restore();
    jest.restoreAllMocks();
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

  test("Download button is rendered for courses with an organization", () => {
    render(
      <BrowserRouter>
        <InstructorCoursesTable
          courses={coursesFixtures.severalCourses}
          currentUser={currentUserFixtures.instructorUser}
          storybook={false}
        />
      </BrowserRouter>,
    );

    // Course with orgName should have a download button
    const downloadButton = screen.getByTestId(
      `${testId}-cell-row-0-col-download-download-button`
    );
    expect(downloadButton).toBeInTheDocument();
    expect(downloadButton).toHaveTextContent("CSV");
  });

  test("Download button is not rendered for courses without an organization", () => {
    render(
      <BrowserRouter>
        <InstructorCoursesTable
          courses={coursesFixtures.severalCourses}
          currentUser={currentUserFixtures.instructorUser}
          storybook={false}
        />
      </BrowserRouter>,
    );

    // Course without orgName should not have a download button
    // Course at index 2 has no orgName based on the fixtures
    const downloadButtonCell = screen.queryByTestId(
      `${testId}-cell-row-2-col-download-download-button`
    );
    expect(downloadButtonCell).not.toBeInTheDocument();
  });

  test("Download button tooltip shows correct text", async () => {
    render(
      <BrowserRouter>
        <InstructorCoursesTable
          courses={coursesFixtures.severalCourses}
          currentUser={currentUserFixtures.instructorUser}
          storybook={false}
        />
      </BrowserRouter>,
    );

    const downloadButton = screen.getByTestId(
      `${testId}-cell-row-0-col-download-download-button`
    );
    
    fireEvent.mouseOver(downloadButton);

    await waitFor(() => {
      expect(
        screen.getByText("Download CSV with student GitHub IDs")
      ).toBeInTheDocument();
    });
  });

  test("Download function calls the API with the correct URL", async () => {
    // Mock the API response for roster students
    const mockRosterStudents = [
      {
        id: 1,
        studentId: "1234567",
        firstName: "John",
        lastName: "Doe",
        email: "johndoe@example.com",
        githubId: 12345,
        githubLogin: "johndoe"
      }
    ];

    mockAxios.onGet("/api/rosterstudents/course/1").reply(200, mockRosterStudents);

    // Mock window.URL.createObjectURL to prevent actual download
    const originalCreateObjectURL = URL.createObjectURL;
    URL.createObjectURL = jest.fn(() => "mock-url");

    render(
      <BrowserRouter>
        <InstructorCoursesTable
          courses={coursesFixtures.severalCourses}
          currentUser={currentUserFixtures.instructorUser}
          storybook={false}
        />
      </BrowserRouter>,
    );

    const downloadButton = screen.getByTestId(
      `${testId}-cell-row-0-col-download-download-button`
    );
    
    // Mock the actual download functionality to prevent errors
    const originalCreateElement = document.createElement;
    document.createElement = jest.fn(() => ({
      setAttribute: jest.fn(),
      style: {},
      click: jest.fn()
    }));
    document.body.appendChild = jest.fn();
    document.body.removeChild = jest.fn();
    
    fireEvent.click(downloadButton);

    await waitFor(() => {
      // Verify that the API was called with the correct URL
      expect(mockAxios.history.get.length).toBe(1);
      expect(mockAxios.history.get[0].url).toBe("/api/rosterstudents/course/1");
    });
    
    // Restore original functions
    URL.createObjectURL = originalCreateObjectURL;
    document.createElement = originalCreateElement;
  });
});
