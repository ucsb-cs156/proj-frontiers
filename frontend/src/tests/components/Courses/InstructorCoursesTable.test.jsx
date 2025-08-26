import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import coursesFixtures from "fixtures/coursesFixtures";
import { currentUserFixtures } from "fixtures/currentUserFixtures";
import InstructorCoursesTable from "main/components/Courses/InstructorCoursesTable";
import { BrowserRouter, MemoryRouter } from "react-router";
import AxiosMockAdapter from "axios-mock-adapter";
import axios from "axios";
import { vi } from "vitest";

window.alert = vi.fn();

const mockToast = vi.fn();
vi.mock("react-toastify", async (importOriginal) => {
  return {
    ...(await importOriginal()),
    toast: (x) => mockToast(x),
  };
});

const queryClient = new QueryClient();
const testId = "InstructorCoursesTable";
let axiosMock;
describe("InstructorCoursesTable tests", () => {
  describe("InstructorCoursesTable basic tests", () => {
    const originalLocation = window.location;

    beforeEach(() => {
      // Remove window.location and mock it
      delete window.location;
      window.location = { href: "", reload: vi.fn() }; // Add reload mock
      // Reset mocks
      window.alert.mockClear();
    });

    afterEach(() => {
      // Restore original window.location
      window.location = originalLocation;
    });

    test("Has the expected column headers and content for instructor user", async () => {
      render(
        <QueryClientProvider client={queryClient}>
          <BrowserRouter>
            <InstructorCoursesTable
              courses={coursesFixtures.severalCourses}
              currentUser={currentUserFixtures.instructorUser}
              storybook={true}
            />
          </BrowserRouter>
        </QueryClientProvider>,
      );

      const expectedHeaders = [
        "id",
        "Course Name",
        "Term",
        "School",
        "Edit",
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
        if (headerText === "Edit") {
          const header = screen.getByTestId(
            "InstructorCoursesTable-header-edit-sort-header",
          );
          expect(header).toBeInTheDocument();
          expect(header).toHaveTextContent("Edit");
        } else {
          const header = screen.getByText(headerText);
          expect(header).toBeInTheDocument();
        }
      });

      expectedFields.forEach((field) => {
        const header = screen.getByTestId(`${testId}-cell-row-0-col-${field}`);
        expect(header).toBeInTheDocument();
      });

      expect(screen.getByText("GitHub Org")).toBeInTheDocument();

      expect(
        screen.getByTestId(`${testId}-cell-row-0-col-id`),
      ).toHaveTextContent("1");
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

      // Check that Edit buttons are present for courses the instructor can edit
      const editButton0 = screen.getByTestId(
        `${testId}-cell-row-0-col-edit-button`,
      );
      expect(editButton0).toBeInTheDocument();
      expect(editButton0).toHaveTextContent("Edit");

      const editButton2 = screen.getByTestId(
        `${testId}-cell-row-2-col-edit-button`,
      );
      expect(editButton2).toBeInTheDocument();
      expect(editButton2).toHaveTextContent("Edit");

      // Check that instructor cannot edit course they don't own
      const noEditPermission = screen.getByTestId(
        `${testId}-cell-row-1-col-edit-no-permission`,
      );
      expect(noEditPermission).toBeInTheDocument();
      expect(noEditPermission).toBeEmptyDOMElement();
    });

    test("Has the expected column headers and content for admin user", async () => {
      render(
        <QueryClientProvider client={new QueryClient()}>
          <BrowserRouter>
            <InstructorCoursesTable
              courses={coursesFixtures.severalCourses}
              currentUser={currentUserFixtures.adminUser}
              storybook={true}
            />
          </BrowserRouter>
        </QueryClientProvider>,
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

      // Check that admin can edit all courses
      const editButton0 = screen.getByTestId(
        `${testId}-cell-row-0-col-edit-button`,
      );
      expect(editButton0).toBeInTheDocument();
      expect(editButton0).toHaveTextContent("Edit");

      const editButton1 = screen.getByTestId(
        `${testId}-cell-row-1-col-edit-button`,
      );
      expect(editButton1).toBeInTheDocument();
      expect(editButton1).toHaveTextContent("Edit");

      const editButton2 = screen.getByTestId(
        `${testId}-cell-row-2-col-edit-button`,
      );
      expect(editButton2).toBeInTheDocument();
      expect(editButton2).toHaveTextContent("Edit");
    });

    test("Calls window.alert when the button is pressed on storybook", async () => {
      render(
        <QueryClientProvider client={queryClient}>
          <BrowserRouter>
            <InstructorCoursesTable
              courses={coursesFixtures.severalCourses}
              currentUser={currentUserFixtures.instructorUser}
              storybook={true}
            />
          </BrowserRouter>
        </QueryClientProvider>,
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
        <QueryClientProvider client={queryClient}>
          <MemoryRouter initialEntries={["/instructor/courses"]}>
            <InstructorCoursesTable
              courses={coursesFixtures.severalCourses}
              currentUser={currentUserFixtures.instructorUser}
              storybook={false}
            />
          </MemoryRouter>
        </QueryClientProvider>,
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
      expect(sessionStorage.getItem("redirect")).toBe("/instructor/courses");
      sessionStorage.clear();
    });
    test("Tests for GitHub link", async () => {
      render(
        <QueryClientProvider client={queryClient}>
          <BrowserRouter>
            <InstructorCoursesTable
              courses={coursesFixtures.severalCourses}
              currentUser={currentUserFixtures.instructorUser}
              storybook={false}
            />
          </BrowserRouter>
        </QueryClientProvider>,
      );

      const githubLink = screen.getByTestId(
        `CoursesTable-cell-row-0-col-orgName-github-settings-link`,
      );
      expect(githubLink).toBeInTheDocument();
      expect(githubLink).toHaveAttribute(
        "href",
        "https://github.com/organizations/ucsb-cs156-s25/settings/installations/123456",
      );
    });
    test("tests for GitHub Settings icon", async () => {
      render(
        <QueryClientProvider client={queryClient}>
          <BrowserRouter>
            <InstructorCoursesTable
              courses={coursesFixtures.severalCourses}
              currentUser={currentUserFixtures.instructorUser}
              storybook={false}
            />
          </BrowserRouter>
        </QueryClientProvider>,
      );

      const githubSettingsIcon = screen.getByTestId(
        `CoursesTable-cell-row-0-col-orgName-gear-github-icon`,
      );
      expect(githubSettingsIcon).toBeInTheDocument();
      expect(githubSettingsIcon).toHaveStyle({
        display: "absolute",
        alignItems: "inline-block",
      });

      const githubIcon = screen.getByTestId(
        "CoursesTable-cell-row-0-col-orgName-gear-github-icon-github-icon",
      );
      expect(githubIcon).toBeInTheDocument();
      expect(githubIcon).toHaveAttribute("width", "24");
      expect(githubIcon).toHaveAttribute("height", "24");
      expect(githubIcon).toHaveAttribute("color", "black");

      const settingsIcon = screen.getByTestId(
        "CoursesTable-cell-row-0-col-orgName-gear-github-icon-settings-icon",
      );
      expect(settingsIcon).toBeInTheDocument();
      expect(settingsIcon).toHaveAttribute("width", "16");
      expect(settingsIcon).toHaveAttribute("height", "16");
      expect(settingsIcon).toHaveAttribute("color", "blue");
      expect(settingsIcon).toHaveStyle({ position: "relative" });
      expect(settingsIcon).toHaveStyle({ top: "0px" });
      expect(settingsIcon).toHaveStyle({ left: "0px" });
      expect(settingsIcon).toHaveStyle({ transform: "translate(-15%, 60%)" });
    });
    test("Tests that when storybook is false by default all works as expected", async () => {
      render(
        <QueryClientProvider client={queryClient}>
          <BrowserRouter>
            <InstructorCoursesTable
              courses={coursesFixtures.severalCourses}
              currentUser={currentUserFixtures.instructorUser}
            />
          </BrowserRouter>
        </QueryClientProvider>,
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
        <QueryClientProvider client={queryClient}>
          <BrowserRouter>
            <InstructorCoursesTable
              courses={coursesFixtures.severalCourses}
              currentUser={currentUserFixtures.instructorUser}
              storybook={false}
            />
          </BrowserRouter>
        </QueryClientProvider>,
      );

      fireEvent.mouseOver(screen.getByText("CPTS 489"));

      const tooltip = await screen.findByRole("tooltip");
      expect(tooltip).toHaveAttribute("id", "tooltip-coursename-1");
    });
    test("expect the correct tooltip ID for the orgName tooltips", async () => {
      render(
        <QueryClientProvider client={queryClient}>
          <BrowserRouter>
            <InstructorCoursesTable
              courses={coursesFixtures.severalCourses}
              currentUser={currentUserFixtures.instructorUser}
              storybook={false}
            />
          </BrowserRouter>
        </QueryClientProvider>,
      );

      fireEvent.mouseOver(screen.getByText("wsu-cpts489-fa20"));

      const tooltip = await screen.findByRole("tooltip");
      expect(tooltip).toHaveAttribute("id", "tooltip-orgname-1");
    });
    test("expect the correct tooltip ID for the github icon (that redirects to github installation settings)", async () => {
      render(
        <QueryClientProvider client={queryClient}>
          <BrowserRouter>
            <InstructorCoursesTable
              courses={coursesFixtures.severalCourses}
              currentUser={currentUserFixtures.instructorUser}
              storybook={false}
            />
          </BrowserRouter>
        </QueryClientProvider>,
      );

      const githubIcon = screen.getByTestId(
        `CoursesTable-cell-row-0-col-orgName-gear-github-icon`,
      );

      fireEvent.mouseOver(githubIcon);

      const tooltip = await screen.findByRole("tooltip");
      expect(tooltip).toHaveAttribute("id", "tooltip-geargithubicon-0");
    });
    test("the correct tooltip renders for courseName", async () => {
      render(
        <QueryClientProvider client={queryClient}>
          <BrowserRouter>
            <InstructorCoursesTable
              courses={coursesFixtures.severalCourses}
              currentUser={currentUserFixtures.instructorUser}
              storybook={false}
            />
          </BrowserRouter>
        </QueryClientProvider>,
      );

      fireEvent.mouseOver(screen.getByText("CPTS 489"));

      await waitFor(() => {
        expect(screen.getByText("View course details")).toBeInTheDocument();
      });
    });
    test("the correct tooltip renders for orgName when a GitHub organization exists for the course", async () => {
      render(
        <QueryClientProvider client={queryClient}>
          <BrowserRouter>
            <InstructorCoursesTable
              courses={coursesFixtures.severalCourses}
              currentUser={currentUserFixtures.instructorUser}
              storybook={false}
            />
          </BrowserRouter>
        </QueryClientProvider>,
      );
      fireEvent.mouseOver(screen.getByText("wsu-cpts489-fa20"));

      await waitFor(() => {
        expect(
          screen.getByText("View organization associated with CPTS 489."),
        ).toBeInTheDocument();
      });
    });
    test("the correct tooltip renders for orgName when a GitHub organization does NOT exist for the course", async () => {
      render(
        <QueryClientProvider client={queryClient}>
          <BrowserRouter>
            <InstructorCoursesTable
              courses={coursesFixtures.severalCourses}
              currentUser={currentUserFixtures.instructorUser}
              storybook={false}
            />
          </BrowserRouter>
        </QueryClientProvider>,
      );

      fireEvent.mouseOver(screen.getByText("Install GitHub App"));

      await waitFor(() => {
        expect(
          screen.getByText("Click to install the GitHub app for CMPSC 156"),
        ).toBeInTheDocument();
      });
    });
  });

  describe("InstructorCoursesTable update instructor modal tests", () => {
    let invalidateQueriesSpy;

    beforeEach(() => {
      axiosMock = new AxiosMockAdapter(axios);
      axiosMock.reset();
      axiosMock.resetHistory();
      mockToast.mockClear();
      invalidateQueriesSpy = vi.spyOn(queryClient, "invalidateQueries");
    });

    afterEach(() => {
      invalidateQueriesSpy.mockRestore();
    });

    test("Tests instructor email is clickable for admin users when enableInstructorUpdate selected", async () => {
      render(
        <QueryClientProvider client={queryClient}>
          <BrowserRouter>
            <InstructorCoursesTable
              courses={coursesFixtures.severalCourses}
              currentUser={currentUserFixtures.adminUser}
              storybook={true}
              enableInstructorUpdate={true}
            />
          </BrowserRouter>
        </QueryClientProvider>,
      );

      const instructorEmailButton = screen.getByTestId(
        `${testId}-cell-row-0-col-instructorEmail-button`,
      );
      expect(instructorEmailButton).toBeInTheDocument();
      expect(instructorEmailButton).toHaveTextContent("diba@ucsb.edu");
      expect(instructorEmailButton).toHaveClass("btn-link");
    });

    test("Tests instructor email is plain text for admin users when enableInstructorUpdate not selected", async () => {
      render(
        <QueryClientProvider client={queryClient}>
          <BrowserRouter>
            <InstructorCoursesTable
              courses={coursesFixtures.severalCourses}
              currentUser={currentUserFixtures.adminUser}
              storybook={true}
            />
          </BrowserRouter>
        </QueryClientProvider>,
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

    test("Tests instructor email is plain text for non-admin users", async () => {
      render(
        <QueryClientProvider client={queryClient}>
          <BrowserRouter>
            <InstructorCoursesTable
              courses={coursesFixtures.severalCourses}
              currentUser={currentUserFixtures.instructorUser}
              storybook={true}
            />
          </BrowserRouter>
        </QueryClientProvider>,
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
        <QueryClientProvider client={queryClient}>
          <BrowserRouter>
            <InstructorCoursesTable
              courses={coursesFixtures.severalCourses}
              currentUser={currentUserFixtures.adminUser}
              enableInstructorUpdate={true}
              storybook={true}
            />
          </BrowserRouter>
        </QueryClientProvider>,
      );

      const instructorEmailButton = screen.getByTestId(
        `${testId}-cell-row-0-col-instructorEmail-button`,
      );

      fireEvent.click(instructorEmailButton);

      await waitFor(() => {
        expect(screen.getByRole("dialog")).toBeInTheDocument();
      });

      const modal = screen.getByTestId(`${testId}-modal`);
      expect(modal).toBeInTheDocument();
      expect(modal).toHaveClass("modal-dialog modal-dialog-centered");
    });

    test("Modal closes when close button (X) is clicked", async () => {
      render(
        <QueryClientProvider client={queryClient}>
          <BrowserRouter>
            <InstructorCoursesTable
              courses={coursesFixtures.severalCourses}
              currentUser={currentUserFixtures.adminUser}
              enableInstructorUpdate={true}
              storybook={true}
            />
          </BrowserRouter>
        </QueryClientProvider>,
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

    test("Error message if email is empty", async () => {
      render(
        <QueryClientProvider client={queryClient}>
          <BrowserRouter>
            <InstructorCoursesTable
              courses={coursesFixtures.severalCourses}
              currentUser={currentUserFixtures.adminUser}
              enableInstructorUpdate={true}
              storybook={true}
            />
          </BrowserRouter>
        </QueryClientProvider>,
      );

      const instructorEmailButton = screen.getByTestId(
        `${testId}-cell-row-0-col-instructorEmail-button`,
      );

      fireEvent.click(instructorEmailButton);

      await waitFor(() => {
        expect(screen.getByRole("dialog")).toBeInTheDocument();
      });

      const emailInput = screen.getByTestId("update-instructor-email-input");
      const updateButton = screen.getByTestId(
        "update-instructor-submit-button",
      );

      expect(emailInput).toBeInTheDocument();
      expect(updateButton).toBeInTheDocument();

      // Clear the email input
      fireEvent.change(emailInput, { target: { value: "" } });

      fireEvent.click(updateButton);

      await waitFor(() => {
        expect(
          screen.getByText(/Instructor email is required/),
        ).toBeInTheDocument();
      });
    });

    test("Email input field updates when user types", async () => {
      render(
        <QueryClientProvider client={queryClient}>
          <BrowserRouter>
            <InstructorCoursesTable
              courses={coursesFixtures.severalCourses}
              currentUser={currentUserFixtures.adminUser}
              enableInstructorUpdate={true}
              storybook={true}
            />
          </BrowserRouter>
        </QueryClientProvider>,
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

    test("Makes successful API call and dismisses modal", async () => {
      axiosMock.onPut("/api/courses/updateInstructor").reply(200, {});

      render(
        <QueryClientProvider client={queryClient}>
          <BrowserRouter>
            <InstructorCoursesTable
              courses={coursesFixtures.severalCourses}
              currentUser={currentUserFixtures.adminUser}
              enableInstructorUpdate={true}
              storybook={false}
            />
          </BrowserRouter>
        </QueryClientProvider>,
      );

      const instructorEmailButton = screen.getByTestId(
        `${testId}-cell-row-0-col-instructorEmail-button`,
      );
      const githubIcon = screen.getByTestId(
        `CoursesTable-cell-row-0-col-orgName-gear-github-icon`,
      );
      expect(githubIcon).toBeInTheDocument();

      fireEvent.click(instructorEmailButton);

      await waitFor(() => {
        expect(screen.getByRole("dialog")).toBeInTheDocument();
      });

      const emailInput = screen.getByTestId("update-instructor-email-input");
      const updateButton = screen.getByTestId(
        "update-instructor-submit-button",
      );

      fireEvent.change(emailInput, { target: { value: "new@example.com" } });
      fireEvent.click(updateButton);

      await waitFor(() => {
        expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
      });

      await waitFor(() => expect(axiosMock.history.put.length).toBe(1));
      expect(axiosMock.history.put[0].params).toEqual({
        courseId: 1,
        instructorEmail: "new@example.com",
      });
    });

    test("Shows alert when API call fails with 400 error plus message", async () => {
      axiosMock.onPut("/api/courses/updateInstructor").reply(400, {
        message: "Email must belong to either an instructor or admin",
        type: "IllegalArgumentException",
      });

      render(
        <QueryClientProvider client={queryClient}>
          <BrowserRouter>
            <InstructorCoursesTable
              courses={coursesFixtures.severalCourses}
              currentUser={currentUserFixtures.adminUser}
              enableInstructorUpdate={true}
              storybook={false}
            />
          </BrowserRouter>
        </QueryClientProvider>,
      );

      const instructorEmailButton = screen.getByTestId(
        `${testId}-cell-row-0-col-instructorEmail-button`,
      );

      fireEvent.click(instructorEmailButton);

      await waitFor(() => {
        expect(screen.getByRole("dialog")).toBeInTheDocument();
      });

      const emailInput = screen.getByTestId("update-instructor-email-input");
      const updateButton = screen.getByTestId(
        "update-instructor-submit-button",
      );

      fireEvent.change(emailInput, {
        target: { value: "invalid@example.com" },
      });
      fireEvent.click(updateButton);

      await waitFor(() => {
        expect(mockToast).toHaveBeenCalledWith(
          "Was not able to update instructor:\nEmail must belong to either an instructor or admin",
        );
      });
    });

    test("Shows alert when API call fails with 400 error without message", async () => {
      axiosMock.onPut("/api/courses/updateInstructor").reply(400, {});

      render(
        <QueryClientProvider client={queryClient}>
          <BrowserRouter>
            <InstructorCoursesTable
              courses={coursesFixtures.severalCourses}
              currentUser={currentUserFixtures.adminUser}
              enableInstructorUpdate={true}
              storybook={false}
            />
          </BrowserRouter>
        </QueryClientProvider>,
      );

      const instructorEmailButton = screen.getByTestId(
        `${testId}-cell-row-0-col-instructorEmail-button`,
      );

      fireEvent.click(instructorEmailButton);

      await waitFor(() => {
        expect(screen.getByRole("dialog")).toBeInTheDocument();
      });

      const emailInput = screen.getByTestId("update-instructor-email-input");
      const updateButton = screen.getByTestId(
        "update-instructor-submit-button",
      );

      fireEvent.change(emailInput, {
        target: { value: "invalid@example.com" },
      });
      fireEvent.click(updateButton);

      await waitFor(() => {
        expect(mockToast).toHaveBeenCalledWith(
          "Was not able to update instructor:\nRequest failed with status code 400",
        );
      });
    });

    test("Modal resets state correctly when reopened", async () => {
      render(
        <QueryClientProvider client={queryClient}>
          <BrowserRouter>
            <InstructorCoursesTable
              courses={coursesFixtures.severalCourses}
              currentUser={currentUserFixtures.adminUser}
              enableInstructorUpdate={true}
              storybook={true}
            />
          </BrowserRouter>
        </QueryClientProvider>,
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

      const closeButton = screen.getByRole("button", { name: /close/i });
      fireEvent.click(closeButton);

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
      expect(screen.getByText(/CPTS 489/)).toBeInTheDocument();
    });

    test("Tests styling of instructor email button for admins", async () => {
      render(
        <QueryClientProvider client={queryClient}>
          <BrowserRouter>
            <InstructorCoursesTable
              courses={coursesFixtures.severalCourses}
              currentUser={currentUserFixtures.adminUser}
              enableInstructorUpdate={true}
              storybook={true}
            />
          </BrowserRouter>
        </QueryClientProvider>,
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
        <QueryClientProvider client={queryClient}>
          <BrowserRouter>
            <InstructorCoursesTable
              courses={coursesFixtures.severalCourses}
              currentUser={currentUserFixtures.adminUser}
              enableInstructorUpdate={true}
              storybook={true}
            />
          </BrowserRouter>
        </QueryClientProvider>,
      );

      const instructorEmailButton = screen.getByTestId(
        `${testId}-cell-row-0-col-instructorEmail-button`,
      );

      fireEvent.click(instructorEmailButton);

      await waitFor(() => {
        expect(screen.getByRole("dialog")).toBeInTheDocument();
      });

      const updateButton = screen.getByTestId(
        "update-instructor-submit-button",
      );

      // Test default button text
      expect(updateButton).toHaveTextContent("Update Instructor");
    });

    test("Edit course modal opens and closes properly", async () => {
      axiosMock = new AxiosMockAdapter(axios);
      axiosMock
        .onPut("/api/courses")
        .reply(200, coursesFixtures.severalCourses[0]);

      render(
        <QueryClientProvider client={queryClient}>
          <MemoryRouter>
            <InstructorCoursesTable
              courses={coursesFixtures.severalCourses}
              currentUser={currentUserFixtures.instructorUser}
              testId={testId}
            />
          </MemoryRouter>
        </QueryClientProvider>,
      );

      // Verify modal is not initially open
      expect(screen.queryByTestId("CourseModal-base")).not.toBeInTheDocument();

      // Click the edit button
      const editButton = screen.getByTestId(
        `${testId}-cell-row-0-col-edit-button`,
      );
      fireEvent.click(editButton);

      // Check that modal appears with correct title
      await waitFor(() => {
        expect(screen.getByTestId("CourseModal-base")).toBeInTheDocument();
        expect(screen.getByText("Edit Course")).toBeInTheDocument();
        expect(screen.getByText("Update")).toBeInTheDocument();
      });

      // Close modal using close button
      const closeButton = screen.getByTestId("CourseModal-closeButton");
      fireEvent.click(closeButton);

      await waitFor(() => {
        expect(
          screen.queryByTestId("CourseModal-base"),
        ).not.toBeInTheDocument();
      });
    });

    test("Makes successful course update API call and shows success toast", async () => {
      axiosMock = new AxiosMockAdapter(axios);
      axiosMock.reset();
      axiosMock.resetHistory();
      axiosMock.onPut("/api/courses").reply(200, {});

      render(
        <QueryClientProvider client={new QueryClient()}>
          <MemoryRouter>
            <InstructorCoursesTable
              courses={coursesFixtures.severalCourses}
              currentUser={currentUserFixtures.instructorUser}
              testId={testId}
            />
          </MemoryRouter>
        </QueryClientProvider>,
      );

      // Click the edit button
      const editButton = screen.getByTestId(
        `${testId}-cell-row-0-col-edit-button`,
      );
      fireEvent.click(editButton);

      // Wait for modal to appear
      await waitFor(() => {
        expect(screen.getByTestId("CourseModal-base")).toBeInTheDocument();
      });

      // Fill out the form using testIds
      const courseNameInput = screen.getByTestId("CourseModal-courseName");
      const termInput = screen.getByTestId("CourseModal-term");
      const schoolInput = screen.getByTestId("CourseModal-school");

      fireEvent.change(courseNameInput, {
        target: { value: "Updated Course" },
      });
      fireEvent.change(termInput, { target: { value: "Fall 2025" } });
      fireEvent.change(schoolInput, { target: { value: "Updated School" } });

      // Click the Update button
      const updateButton = screen.getByTestId("CourseModal-submit");
      fireEvent.click(updateButton);

      // Verify API call was made with correct parameters
      await waitFor(() => expect(axiosMock.history.put.length).toBe(1));
      expect(axiosMock.history.put[0].params).toEqual({
        courseId: 1,
        courseName: "Updated Course",
        term: "Fall 2025",
        school: "Updated School",
      });

      // Verify success toast was shown
      await waitFor(() => {
        expect(mockToast).toHaveBeenCalledWith("Course updated successfully");
      });

      // Verify modal is closed
      await waitFor(() => {
        expect(
          screen.queryByTestId("CourseModal-base"),
        ).not.toBeInTheDocument();
      });
    });

    test("Shows error toast when course update API call fails with message", async () => {
      axiosMock = new AxiosMockAdapter(axios);
      axiosMock.reset();
      axiosMock.resetHistory();
      axiosMock.onPut("/api/courses").reply(400, {
        message: "Course name already exists",
        type: "ValidationException",
      });

      render(
        <QueryClientProvider client={new QueryClient()}>
          <MemoryRouter>
            <InstructorCoursesTable
              courses={coursesFixtures.severalCourses}
              currentUser={currentUserFixtures.instructorUser}
              testId={testId}
            />
          </MemoryRouter>
        </QueryClientProvider>,
      );

      // Click the edit button
      const editButton = screen.getByTestId(
        `${testId}-cell-row-0-col-edit-button`,
      );
      fireEvent.click(editButton);

      // Wait for modal to appear
      await waitFor(() => {
        expect(screen.getByTestId("CourseModal-base")).toBeInTheDocument();
      });

      // Fill out all required form fields
      const courseNameInput = screen.getByTestId("CourseModal-courseName");
      const termInput = screen.getByTestId("CourseModal-term");
      const schoolInput = screen.getByTestId("CourseModal-school");

      fireEvent.change(courseNameInput, {
        target: { value: "Invalid Course" },
      });
      fireEvent.change(termInput, { target: { value: "Spring 2025" } });
      fireEvent.change(schoolInput, { target: { value: "UCSB" } });

      // Click the Update button
      const updateButton = screen.getByTestId("CourseModal-submit");
      fireEvent.click(updateButton);

      // Verify error toast was shown with message
      await waitFor(() => {
        expect(mockToast).toHaveBeenCalledWith(
          "Was not able to update course:\nCourse name already exists",
        );
      });
    });

    test("Shows error toast when course update API call fails without message", async () => {
      axiosMock = new AxiosMockAdapter(axios);
      axiosMock.reset();
      axiosMock.resetHistory();
      axiosMock.onPut("/api/courses").reply(400, {});

      render(
        <QueryClientProvider client={new QueryClient()}>
          <MemoryRouter>
            <InstructorCoursesTable
              courses={coursesFixtures.severalCourses}
              currentUser={currentUserFixtures.instructorUser}
              testId={testId}
            />
          </MemoryRouter>
        </QueryClientProvider>,
      );

      // Click the edit button
      const editButton = screen.getByTestId(
        `${testId}-cell-row-0-col-edit-button`,
      );
      fireEvent.click(editButton);

      // Wait for modal to appear
      await waitFor(() => {
        expect(screen.getByTestId("CourseModal-base")).toBeInTheDocument();
      });

      // Fill out all required form fields
      const courseNameInput = screen.getByTestId("CourseModal-courseName");
      const termInput = screen.getByTestId("CourseModal-term");
      const schoolInput = screen.getByTestId("CourseModal-school");

      fireEvent.change(courseNameInput, {
        target: { value: "Invalid Course" },
      });
      fireEvent.change(termInput, { target: { value: "Spring 2025" } });
      fireEvent.change(schoolInput, { target: { value: "UCSB" } });

      // Click the Update button
      const updateButton = screen.getByTestId("CourseModal-submit");
      fireEvent.click(updateButton);

      // Verify error toast was shown with generic message
      await waitFor(() => {
        expect(mockToast).toHaveBeenCalledWith(
          "Was not able to update course:\nRequest failed with status code 400",
        );
      });
    });

    test("Instructor update mutation uses correct cache keys for invalidation", async () => {
      axiosMock.onPut("/api/courses/updateInstructor").reply(200, {});

      render(
        <QueryClientProvider client={queryClient}>
          <BrowserRouter>
            <InstructorCoursesTable
              courses={coursesFixtures.severalCourses}
              currentUser={currentUserFixtures.adminUser}
              enableInstructorUpdate={true}
              storybook={false}
            />
          </BrowserRouter>
        </QueryClientProvider>,
      );

      const instructorEmailButton = screen.getByTestId(
        `${testId}-cell-row-0-col-instructorEmail-button`,
      );

      fireEvent.click(instructorEmailButton);

      await waitFor(() => {
        expect(screen.getByRole("dialog")).toBeInTheDocument();
      });

      const emailInput = screen.getByTestId("update-instructor-email-input");
      const updateButton = screen.getByTestId(
        "update-instructor-submit-button",
      );

      fireEvent.change(emailInput, { target: { value: "new@example.com" } });
      fireEvent.click(updateButton);

      await waitFor(() => {
        expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
      });

      await waitFor(() => expect(axiosMock.history.put.length).toBe(1));

      // Verify that invalidateQueries was called with all expected cache keys
      expect(invalidateQueriesSpy).toHaveBeenCalledTimes(3);
      expect(invalidateQueriesSpy).toHaveBeenCalledWith({
        queryKey: ["/api/courses"],
      });
      expect(invalidateQueriesSpy).toHaveBeenCalledWith({
        queryKey: ["/api/courses/allForAdmins"],
      });
      expect(invalidateQueriesSpy).toHaveBeenCalledWith({
        queryKey: ["/api/courses/allForInstructors"],
      });
    });

    test("Course update mutation uses correct cache keys for invalidation", async () => {
      axiosMock = new AxiosMockAdapter(axios);
      axiosMock.reset();
      axiosMock.resetHistory();
      axiosMock.onPut("/api/courses").reply(200, {});

      render(
        <QueryClientProvider client={queryClient}>
          <MemoryRouter>
            <InstructorCoursesTable
              courses={coursesFixtures.severalCourses}
              currentUser={currentUserFixtures.instructorUser}
              testId={testId}
            />
          </MemoryRouter>
        </QueryClientProvider>,
      );

      // Click the edit button
      const editButton = screen.getByTestId(
        `${testId}-cell-row-0-col-edit-button`,
      );
      fireEvent.click(editButton);

      // Wait for modal to appear
      await waitFor(() => {
        expect(screen.getByTestId("CourseModal-base")).toBeInTheDocument();
      });

      // Fill out the form using testIds
      const courseNameInput = screen.getByTestId("CourseModal-courseName");
      const termInput = screen.getByTestId("CourseModal-term");
      const schoolInput = screen.getByTestId("CourseModal-school");

      fireEvent.change(courseNameInput, {
        target: { value: "Updated Course" },
      });
      fireEvent.change(termInput, { target: { value: "Fall 2025" } });
      fireEvent.change(schoolInput, { target: { value: "Updated School" } });

      // Click the Update button
      const updateButton = screen.getByTestId("CourseModal-submit");
      fireEvent.click(updateButton);

      // Verify API call was made
      await waitFor(() => expect(axiosMock.history.put.length).toBe(1));

      // Verify that invalidateQueries was called with all expected cache keys
      expect(invalidateQueriesSpy).toHaveBeenCalledTimes(3);
      expect(invalidateQueriesSpy).toHaveBeenCalledWith({
        queryKey: ["/api/courses"],
      });
      expect(invalidateQueriesSpy).toHaveBeenCalledWith({
        queryKey: ["/api/courses/allForAdmins"],
      });
      expect(invalidateQueriesSpy).toHaveBeenCalledWith({
        queryKey: ["/api/courses/allForInstructors"],
      });
    });
  });
});
