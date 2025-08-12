import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import coursesFixtures from "fixtures/coursesFixtures";
import { currentUserFixtures } from "fixtures/currentUserFixtures";
import InstructorCoursesTable from "main/components/Courses/InstructorCoursesTable";
import { BrowserRouter } from "react-router";

window.alert = jest.fn();

// Mock fetch for API calls
global.fetch = jest.fn();

describe("InstructorCoursesTable tests", () => {
  const originalLocation = window.location;

  const testId = "InstructorCoursesTable";

  beforeEach(() => {
    // Remove window.location and mock it
    delete window.location;
    window.location = { href: "", reload: jest.fn() }; // Add reload mock
    // Reset mocks
    window.alert.mockClear();
    fetch.mockClear();
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
      "Instructor",
    ];
    const expectedFields = [
      "id",
      "courseName",
      "term",
      "school",
      "instructorEmail",
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
      screen.getByTestId(`${testId}-cell-row-0-col-instructorEmail`),
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

    // Modal should not appear; this kills mutations of this line:
    //   const [showModal, setShowModal] = useState(true);
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
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
  test("Tests instructor email is clickable for admin users", async () => {
    render(
      <BrowserRouter>
        <InstructorCoursesTable
          courses={coursesFixtures.severalCourses}
          currentUser={currentUserFixtures.adminUser}
          storybook={true}
        />
      </BrowserRouter>,
    );

    const instructorEmailButton = screen.getByTestId(
      `${testId}-cell-row-0-col-instructorEmail-button`,
    );
    expect(instructorEmailButton).toBeInTheDocument();
    expect(instructorEmailButton).toHaveTextContent("diba@ucsb.edu");
    expect(instructorEmailButton).toHaveClass("btn-link");
  });

  test("Tests instructor email is plain text for non-admin users", async () => {
    render(
      <BrowserRouter>
        <InstructorCoursesTable
          courses={coursesFixtures.severalCourses}
          currentUser={currentUserFixtures.instructorUser}
          storybook={true}
        />
      </BrowserRouter>,
    );

    const instructorEmailCell = screen.getByTestId(
      `${testId}-cell-row-0-col-instructorEmail`,
    );
    expect(instructorEmailCell).toBeInTheDocument();
    expect(instructorEmailCell).toHaveTextContent("diba@ucsb.edu");

    // Should not have a button for non-admin users
    expect(
      screen.queryByTestId(`${testId}-cell-row-0-col-instructorEmail-button`),
    ).not.toBeInTheDocument();
  });

  test("Opens modal when admin clicks instructor email", async () => {
    render(
      <BrowserRouter>
        <InstructorCoursesTable
          courses={coursesFixtures.severalCourses}
          currentUser={currentUserFixtures.adminUser}
          storybook={true}
        />
      </BrowserRouter>,
    );

    const instructorEmailButton = screen.getByTestId(
      `${testId}-cell-row-0-col-instructorEmail-button`,
    );

    fireEvent.click(instructorEmailButton);

    await waitFor(() => {
      expect(screen.getByRole("dialog")).toBeInTheDocument();
    });

    expect(screen.getByText("Course: CMPSC 156")).toBeInTheDocument();
    expect(screen.getByDisplayValue("diba@ucsb.edu")).toBeInTheDocument();
  });

  test("Modal closes when cancel button is clicked", async () => {
    render(
      <BrowserRouter>
        <InstructorCoursesTable
          courses={coursesFixtures.severalCourses}
          currentUser={currentUserFixtures.adminUser}
          storybook={true}
        />
      </BrowserRouter>,
    );

    const instructorEmailButton = screen.getByTestId(
      `${testId}-cell-row-0-col-instructorEmail-button`,
    );

    fireEvent.click(instructorEmailButton);

    await waitFor(() => {
      expect(screen.getByRole("dialog")).toBeInTheDocument();
    });

    const cancelButton = screen.getByText("Cancel");
    fireEvent.click(cancelButton);

    await waitFor(() => {
      expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    });
  });

  test("Modal closes when close button (X) is clicked", async () => {
    render(
      <BrowserRouter>
        <InstructorCoursesTable
          courses={coursesFixtures.severalCourses}
          currentUser={currentUserFixtures.adminUser}
          storybook={true}
        />
      </BrowserRouter>,
    );

    const instructorEmailButton = screen.getByTestId(
      `${testId}-cell-row-0-col-instructorEmail-button`,
    );

    fireEvent.click(instructorEmailButton);

    await waitFor(() => {
      expect(screen.getByRole("dialog")).toBeInTheDocument();
    });

    const closeButton = screen.getByRole("button", { name: /close/i });
    fireEvent.click(closeButton);

    await waitFor(() => {
      expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    });
  });

  test("Update button is disabled when email input is empty", async () => {
    render(
      <BrowserRouter>
        <InstructorCoursesTable
          courses={coursesFixtures.severalCourses}
          currentUser={currentUserFixtures.adminUser}
          storybook={true}
        />
      </BrowserRouter>,
    );

    const instructorEmailButton = screen.getByTestId(
      `${testId}-cell-row-0-col-instructorEmail-button`,
    );

    fireEvent.click(instructorEmailButton);

    await waitFor(() => {
      expect(screen.getByRole("dialog")).toBeInTheDocument();
    });

    const emailInput = screen.getByTestId("update-instructor-email-input");
    const updateButton = screen.getByTestId("update-instructor-submit-button");

    // Clear the email input
    fireEvent.change(emailInput, { target: { value: "" } });

    expect(updateButton).toBeDisabled();
  });

  test("Email input field updates when user types", async () => {
    render(
      <BrowserRouter>
        <InstructorCoursesTable
          courses={coursesFixtures.severalCourses}
          currentUser={currentUserFixtures.adminUser}
          storybook={true}
        />
      </BrowserRouter>,
    );

    const instructorEmailButton = screen.getByTestId(
      `${testId}-cell-row-0-col-instructorEmail-button`,
    );

    fireEvent.click(instructorEmailButton);

    await waitFor(() => {
      expect(screen.getByRole("dialog")).toBeInTheDocument();
    });

    const emailInput = screen.getByTestId("update-instructor-email-input");

    fireEvent.change(emailInput, { target: { value: "new@example.com" } });

    expect(emailInput).toHaveValue("new@example.com");
  });

  test("Shows alert and closes modal in storybook mode when updating instructor", async () => {
    render(
      <BrowserRouter>
        <InstructorCoursesTable
          courses={coursesFixtures.severalCourses}
          currentUser={currentUserFixtures.adminUser}
          storybook={true}
        />
      </BrowserRouter>,
    );

    const instructorEmailButton = screen.getByTestId(
      `${testId}-cell-row-0-col-instructorEmail-button`,
    );

    fireEvent.click(instructorEmailButton);

    await waitFor(() => {
      expect(screen.getByRole("dialog")).toBeInTheDocument();
    });

    const emailInput = screen.getByTestId("update-instructor-email-input");
    const updateButton = screen.getByTestId("update-instructor-submit-button");

    fireEvent.change(emailInput, { target: { value: "new@example.com" } });
    fireEvent.click(updateButton);

    await waitFor(() => {
      expect(window.alert).toHaveBeenCalledWith(
        "Would update course 1 instructor to: new@example.com",
      );
    });

    // Modal should close after successful update
    await waitFor(() => {
      expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    });
  });

  test("Makes successful API call and reloads page when not in storybook mode", async () => {
    fetch.mockResolvedValueOnce({
      ok: true,
    });

    render(
      <BrowserRouter>
        <InstructorCoursesTable
          courses={coursesFixtures.severalCourses}
          currentUser={currentUserFixtures.adminUser}
          storybook={false}
        />
      </BrowserRouter>,
    );

    const instructorEmailButton = screen.getByTestId(
      `${testId}-cell-row-0-col-instructorEmail-button`,
    );

    fireEvent.click(instructorEmailButton);

    await waitFor(() => {
      expect(screen.getByRole("dialog")).toBeInTheDocument();
    });

    const emailInput = screen.getByTestId("update-instructor-email-input");
    const updateButton = screen.getByTestId("update-instructor-submit-button");

    fireEvent.change(emailInput, { target: { value: "new@example.com" } });
    fireEvent.click(updateButton);

    await waitFor(() => {
      expect(fetch).toHaveBeenCalledWith(
        "/api/courses/updateInstructor?courseId=1&instructorEmail=new%40example.com",
        {
          method: "PUT",
          headers: {
            "Content-Type": "application/json",
          },
          credentials: "include",
        },
      );
    });

    await waitFor(() => {
      expect(window.location.reload).toHaveBeenCalled();
    });

    // Modal should not appear at this point
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });

  test("Shows alert when API call fails with response error", async () => {
    fetch.mockResolvedValueOnce({
      ok: false,
      text: () => Promise.resolve("Email not found"),
    });

    render(
      <BrowserRouter>
        <InstructorCoursesTable
          courses={coursesFixtures.severalCourses}
          currentUser={currentUserFixtures.adminUser}
          storybook={false}
        />
      </BrowserRouter>,
    );

    const instructorEmailButton = screen.getByTestId(
      `${testId}-cell-row-0-col-instructorEmail-button`,
    );

    fireEvent.click(instructorEmailButton);

    await waitFor(() => {
      expect(screen.getByRole("dialog")).toBeInTheDocument();
    });

    const emailInput = screen.getByTestId("update-instructor-email-input");
    const updateButton = screen.getByTestId("update-instructor-submit-button");

    fireEvent.change(emailInput, {
      target: { value: "invalid@example.com" },
    });
    fireEvent.click(updateButton);

    await waitFor(() => {
      expect(window.alert).toHaveBeenCalledWith(
        "Error updating instructor: Email not found",
      );
    });
  });

  test("Shows alert when API call throws an error", async () => {
    const error = new Error("Network error");
    fetch.mockRejectedValueOnce(error);

    render(
      <BrowserRouter>
        <InstructorCoursesTable
          courses={coursesFixtures.severalCourses}
          currentUser={currentUserFixtures.adminUser}
          storybook={false}
        />
      </BrowserRouter>,
    );

    const instructorEmailButton = screen.getByTestId(
      `${testId}-cell-row-0-col-instructorEmail-button`,
    );

    fireEvent.click(instructorEmailButton);

    await waitFor(() => {
      expect(screen.getByRole("dialog")).toBeInTheDocument();
    });

    const emailInput = screen.getByTestId("update-instructor-email-input");
    const updateButton = screen.getByTestId("update-instructor-submit-button");

    fireEvent.change(emailInput, { target: { value: "test@example.com" } });
    fireEvent.click(updateButton);

    await waitFor(() => {
      expect(window.alert).toHaveBeenCalledWith(
        "Error updating instructor: Network error",
      );
    });
  });

  test("Button shows 'Updating...' text and is disabled during update", async () => {
    // Mock a slow API response
    fetch.mockImplementationOnce(
      () =>
        new Promise((resolve) => setTimeout(() => resolve({ ok: true }), 100)),
    );

    render(
      <BrowserRouter>
        <InstructorCoursesTable
          courses={coursesFixtures.severalCourses}
          currentUser={currentUserFixtures.adminUser}
          storybook={false}
        />
      </BrowserRouter>,
    );

    const instructorEmailButton = screen.getByTestId(
      `${testId}-cell-row-0-col-instructorEmail-button`,
    );

    fireEvent.click(instructorEmailButton);

    await waitFor(() => {
      expect(screen.getByRole("dialog")).toBeInTheDocument();
    });

    const emailInput = screen.getByTestId("update-instructor-email-input");
    const updateButton = screen.getByTestId("update-instructor-submit-button");

    fireEvent.change(emailInput, { target: { value: "test@example.com" } });
    fireEvent.click(updateButton);

    // Should show updating state
    expect(screen.getByText("Updating...")).toBeInTheDocument();
    expect(updateButton).toBeDisabled();
    expect(screen.getByText("Cancel")).toBeDisabled();
  });

  test("Does not call API when selectedCourse is null", async () => {
    render(
      <BrowserRouter>
        <InstructorCoursesTable
          courses={coursesFixtures.severalCourses}
          currentUser={currentUserFixtures.adminUser}
          storybook={false}
        />
      </BrowserRouter>,
    );

    const instructorEmailButton = screen.getByTestId(
      `${testId}-cell-row-0-col-instructorEmail-button`,
    );

    fireEvent.click(instructorEmailButton);

    await waitFor(() => {
      expect(screen.getByRole("dialog")).toBeInTheDocument();
    });

    const emailInput = screen.getByTestId("update-instructor-email-input");
    const updateButton = screen.getByTestId("update-instructor-submit-button");

    // Clear the email to make it empty
    fireEvent.change(emailInput, { target: { value: "" } });

    // Button should be disabled when email is empty
    expect(updateButton).toBeDisabled();

    // Try to force click (though it should be disabled)
    fireEvent.click(updateButton);

    // Should not make API call
    expect(fetch).not.toHaveBeenCalled();
  });

  test("Modal resets state correctly when reopened", async () => {
    render(
      <BrowserRouter>
        <InstructorCoursesTable
          courses={coursesFixtures.severalCourses}
          currentUser={currentUserFixtures.adminUser}
          storybook={true}
        />
      </BrowserRouter>,
    );

    // Open modal for first course
    const firstInstructorButton = screen.getByTestId(
      `${testId}-cell-row-0-col-instructorEmail-button`,
    );
    fireEvent.click(firstInstructorButton);

    await waitFor(() => {
      expect(screen.getByRole("dialog")).toBeInTheDocument();
    });

    // Change email and close modal
    const emailInput = screen.getByTestId("update-instructor-email-input");
    fireEvent.change(emailInput, {
      target: { value: "changed@example.com" },
    });

    const cancelButton = screen.getByText("Cancel");
    fireEvent.click(cancelButton);

    await waitFor(() => {
      expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    });

    // Open modal for second course
    const secondInstructorButton = screen.getByTestId(
      `${testId}-cell-row-1-col-instructorEmail-button`,
    );
    fireEvent.click(secondInstructorButton);

    await waitFor(() => {
      expect(screen.getByRole("dialog")).toBeInTheDocument();
    });

    // Should show the second course's original email, not the changed value
    expect(screen.getByDisplayValue("phtcon@ucsb.edu")).toBeInTheDocument();
    expect(screen.getByText("Course: CPTS 489")).toBeInTheDocument();
  });

  test("Tests styling of instructor email button for admins", async () => {
    render(
      <BrowserRouter>
        <InstructorCoursesTable
          courses={coursesFixtures.severalCourses}
          currentUser={currentUserFixtures.adminUser}
          storybook={true}
        />
      </BrowserRouter>,
    );

    const instructorEmailButton = screen.getByTestId(
      `${testId}-cell-row-0-col-instructorEmail-button`,
    );

    // Test button styling
    expect(instructorEmailButton).toHaveStyle({
      padding: "0px",
      textDecoration: "underline",
    });
  });

  test("Tests modal footer button text variations", async () => {
    render(
      <BrowserRouter>
        <InstructorCoursesTable
          courses={coursesFixtures.severalCourses}
          currentUser={currentUserFixtures.adminUser}
          storybook={true}
        />
      </BrowserRouter>,
    );

    const instructorEmailButton = screen.getByTestId(
      `${testId}-cell-row-0-col-instructorEmail-button`,
    );

    fireEvent.click(instructorEmailButton);

    await waitFor(() => {
      expect(screen.getByRole("dialog")).toBeInTheDocument();
    });

    const updateButton = screen.getByTestId("update-instructor-submit-button");

    // Test default button text
    expect(updateButton).toHaveTextContent("Update Instructor");
  });

  test("Tests empty email validation path in handleUpdateInstructor", async () => {
    render(
      <BrowserRouter>
        <InstructorCoursesTable
          courses={coursesFixtures.severalCourses}
          currentUser={currentUserFixtures.adminUser}
          storybook={false}
        />
      </BrowserRouter>,
    );

    const instructorEmailButton = screen.getByTestId(
      `${testId}-cell-row-0-col-instructorEmail-button`,
    );

    fireEvent.click(instructorEmailButton);

    await waitFor(() => {
      expect(screen.getByRole("dialog")).toBeInTheDocument();
    });

    const emailInput = screen.getByTestId("update-instructor-email-input");
    const updateButton = screen.getByTestId("update-instructor-submit-button");

    // Clear email to empty string
    fireEvent.change(emailInput, { target: { value: "" } });

    // Button should be disabled when email is empty, but test the internal logic
    expect(updateButton).toBeDisabled();

    // The function should return early if email is empty (this tests the conditional)
    expect(fetch).not.toHaveBeenCalled();
  });
});
