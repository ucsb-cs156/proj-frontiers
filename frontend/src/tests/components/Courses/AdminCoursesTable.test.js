import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import coursesFixtures from "fixtures/coursesFixtures";
import AdminCoursesTable from "main/components/Courses/AdminCoursesTable";
import { BrowserRouter } from "react-router-dom";

window.alert = jest.fn();

describe("AdminCoursesTable tests", () => {
  const originalLocation = window.location;

  beforeEach(() => {
    // Remove window.location and mock it
    delete window.location;
    window.location = { href: "" }; // Minimal mock
  });

  afterEach(() => {
    // Restore original window.location
    window.location = originalLocation;
  });

  test("Has the expected column headers and content", () => {
    render(
      <BrowserRouter>
        <AdminCoursesTable courses={coursesFixtures.threeCourses} />
      </BrowserRouter>,
    );

    const expectedHeaders = [
      "id",
      "Installation Id",
      "Org Name",
      "Course Name",
      "Term",
      "School",
    ];
    const expectedFields = [
      "id",
      "installationId",
      "orgName",
      "courseName",
      "term",
      "school",
    ];
    const testId = "AdminCoursesTable";

    expectedHeaders.forEach((headerText) => {
      const header = screen.getByText(headerText);
      expect(header).toBeInTheDocument();
    });

    expectedFields.forEach((field) => {
      const header = screen.getByTestId(`${testId}-cell-row-0-col-${field}`);
      expect(header).toBeInTheDocument();
    });

    expect(screen.getByTestId(`${testId}-cell-row-0-col-id`)).toHaveTextContent(
      "1",
    );
    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-installationId`),
    ).toHaveTextContent("123456");
    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-orgName`),
    ).toHaveTextContent("ucsb-cs156-s25");
    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-courseName`),
    ).toHaveTextContent("CMPSC 156");
    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-term`),
    ).toHaveTextContent("Spring 2025");
    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-school`),
    ).toHaveTextContent("UCSB");

    // expect that the mocked window.alert function is not called
    expect(window.alert).not.toHaveBeenCalled();
  });

  test("Calls the navigate callback when the button is pressed", () => {
    render(
      <BrowserRouter>
        <AdminCoursesTable
          courses={coursesFixtures.threeCourses}
          showInstallButton={true}
        />
      </BrowserRouter>,
    );

    const button = screen.getByTestId(
      "AdminCoursesTable-cell-row-0-col-Install Github App-button",
    );
    expect(button).toBeInTheDocument();
    expect(button).toHaveTextContent("Install Github App");
    expect(button).toHaveAttribute("class", "btn btn-primary");

    fireEvent.click(button);
    expect(window.alert).not.toHaveBeenCalled();
  });

  test("Calls window.alert when the button is pressed on storybook", async () => {
    render(
      <BrowserRouter>
        <AdminCoursesTable
          courses={coursesFixtures.threeCourses}
          showInstallButton={true}
          storybook={true}
        />
      </BrowserRouter>,
    );

    const button = screen.getByTestId(
      "AdminCoursesTable-cell-row-0-col-Install Github App-button",
    );
    expect(button).toBeInTheDocument();
    expect(button).toHaveTextContent("Install Github App");
    expect(button).toHaveAttribute("class", "btn btn-primary");
    fireEvent.click(button);
    await waitFor(() => {
      expect(window.alert).toHaveBeenCalledTimes(1);
    });
    expect(window.alert).toHaveBeenCalledWith(
      "would have navigated to: /api/courses/redirect?courseId=1",
    );
  });

  test("Tests that the default is to NOT show the buttons for installation", () => {
    render(
      <BrowserRouter>
        <AdminCoursesTable courses={coursesFixtures.threeCourses} />
      </BrowserRouter>,
    );

    // Check that the button is NOT in the document
    const button = screen.queryByTestId(
      "AdminCoursesTable-cell-row-0-col-Install Github App-button",
    );
    expect(button).not.toBeInTheDocument();
    // expect that the mocked window.alert function is not called
    expect(window.alert).not.toHaveBeenCalled();
  });

  test("Tests that we don't see the buttons when we specify false", () => {
    render(
      <BrowserRouter>
        <AdminCoursesTable
          courses={coursesFixtures.threeCourses}
          showInstallButton={false}
        />
      </BrowserRouter>,
    );

    // Check that the button is NOT in the document
    const button = screen.queryByTestId(
      "AdminCoursesTable-cell-row-0-col-Install Github App-button",
    );
    expect(button).not.toBeInTheDocument();
    // expect that the mocked window.alert function is not called
    expect(window.alert).not.toHaveBeenCalled();
  });

  test("Tests that storybook is explictly false all still works as expected", async () => {
    render(
      <BrowserRouter>
        <AdminCoursesTable
          courses={coursesFixtures.threeCourses}
          showInstallButton={true}
          storybook={false}
        />
      </BrowserRouter>,
    );

    // Check that the button is NOT in the document
    const button = screen.getByTestId(
      "AdminCoursesTable-cell-row-0-col-Install Github App-button",
    );
    expect(button).toBeInTheDocument();
    expect(button).toHaveTextContent("Install Github App");
    expect(button).toHaveAttribute("class", "btn btn-primary");
    fireEvent.click(button);

    await waitFor(() => {
      expect(window.alert).not.toHaveBeenCalled();
    });

    expect(window.location.href).toBe("/api/courses/redirect?courseId=1");
  });
});
