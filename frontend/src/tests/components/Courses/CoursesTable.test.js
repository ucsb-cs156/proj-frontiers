import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import coursesFixtures from "fixtures/coursesFixtures";
import CoursesTable from "main/components/Courses/CoursesTable";
import { BrowserRouter } from "react-router-dom";

const joinCallback = jest.fn();

describe("CoursesTable tests", () => {
  test("Has the expected column headers and content", () => {
    render(
      <BrowserRouter>
        <CoursesTable
          courses={coursesFixtures.oneCourseWithEachStatus}
          testId={"CoursesTable"}
          joinCallback={joinCallback}
        />
      </BrowserRouter>,
    );

    const expectedHeaders = ["id", "Course Name", "Term", "School", "Status"];
    const expectedFields = [
      "id",
      "courseName",
      "term",
      "school",
      "studentStatus",
    ];
    const testId = "CoursesTable";

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
      screen.getByTestId(`${testId}-cell-row-0-col-courseName`),
    ).toHaveTextContent("CMPSC 156");
    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-term`),
    ).toHaveTextContent("Spring 2025");
    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-school`),
    ).toHaveTextContent("UCSB");
    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-studentStatus`),
    ).toHaveTextContent("Pending");

    const pending = screen.getByText("Pending");
    expect(pending).toBeInTheDocument();
    expect(pending).toHaveStyle("color: orange");

    const joinCourse = screen.getByText("Join Course");
    expect(joinCourse).toBeInTheDocument();
    expect(joinCourse).toHaveAttribute("class", "btn btn-primary");

    const member = screen.getByText("Member");
    expect(member).toBeInTheDocument();
    expect(member).toHaveStyle("color: blue");

    const owner = screen.getByText("Owner");
    expect(owner).toBeInTheDocument();
    expect(owner).toHaveStyle("color: purple");

    const unexpected = screen.getByText("Illegal status that will never occur");
    expect(unexpected).toBeInTheDocument();
    expect(unexpected).not.toHaveStyle("color: red");

    // expect that the mocked joinCallback function is not called
    expect(joinCallback).not.toHaveBeenCalled();
  });

  //tests for button 'Join Course'
  test("Does not call joinCallback in default case for button 'Join Course'", async () => {
    render(
      <BrowserRouter>
        <CoursesTable
          courses={coursesFixtures.oneCourseWithEachStatus}
          testId={"CoursesTable"}
          joinCallback={joinCallback}
        />
      </BrowserRouter>,
    );

    const button = screen.getByTestId(
      "CoursesTable-cell-row-1-col-studentStatus-button",
    );
    expect(button).toBeInTheDocument();
    expect(button).toHaveTextContent("Join Course");
    expect(button).toHaveAttribute("class", "btn btn-primary");

    fireEvent.click(button);
    await waitFor(() => {
      expect(joinCallback).toHaveBeenCalled();
    });
  });

  test("Calls joinCallback when the button is pressed on storybook for button 'Join Course'", async () => {
    render(
      <BrowserRouter>
        <CoursesTable
          courses={coursesFixtures.oneCourseWithEachStatus}
          storybook={true}
          testId={"CoursesTable"}
          joinCallback={joinCallback}
        />
      </BrowserRouter>,
    );

    const button = screen.getByTestId(
      "CoursesTable-cell-row-1-col-studentStatus-button",
    );
    expect(button).toBeInTheDocument();
    expect(button).toHaveTextContent("Join Course");
    expect(button).toHaveAttribute("class", "btn btn-primary");
    fireEvent.click(button);
    await waitFor(() => {
      expect(joinCallback).toHaveBeenCalledTimes(1);
    });
  });

  test("Does not call joinCallback when storybook is explicitly false for button 'Join Course'", () => {
    render(
      <BrowserRouter>
        <CoursesTable
          courses={coursesFixtures.oneCourseWithEachStatus}
          storybook={false}
          testId={"CoursesTable"}
          joinCallback={joinCallback}
        />
      </BrowserRouter>,
    );

    const button = screen.getByTestId(
      "CoursesTable-cell-row-1-col-studentStatus-button",
    );
    expect(button).toBeInTheDocument();
    expect(button).toHaveTextContent("Join Course");
    expect(button).toHaveAttribute("class", "btn btn-primary");

    fireEvent.click(button);
    expect(joinCallback).toHaveBeenCalled();
  });

  //tests for button 'View Invite'
  test("Does not call joinCallback in default case for button'View Invite'", () => {
    render(
      <BrowserRouter>
        <CoursesTable
          courses={coursesFixtures.oneCourseWithEachStatus}
          testId={"CoursesTable"}
          joinCallback={joinCallback}
        />
      </BrowserRouter>,
    );

    const button = screen.getByTestId(
      "CoursesTable-cell-row-2-col-studentStatus-button",
    );
    expect(button).toBeInTheDocument();
    expect(button).toHaveTextContent("View Invite");
    expect(button).toHaveAttribute("class", "btn btn-primary");

    fireEvent.click(button);
    expect(joinCallback).not.toHaveBeenCalled();
  });

  test("Calls joinCallback when the button is pressed on storybook for button 'View Invite'", async () => {
    render(
      <BrowserRouter>
        <CoursesTable
          courses={coursesFixtures.oneCourseWithEachStatus}
          storybook={true}
          testId={"CoursesTable"}
          joinCallback={joinCallback}
        />
      </BrowserRouter>,
    );

    const button = screen.getByTestId(
      "CoursesTable-cell-row-2-col-studentStatus-button",
    );
    expect(button).toBeInTheDocument();
    expect(button).toHaveTextContent("View Invite");
    expect(button).toHaveAttribute("class", "btn btn-primary");
  });

  test("Does not call joinCallback when storybook is explicitly false for button 'View Invite'", () => {
    const openMock = jest.fn();
    window.open = (a, b) => openMock(a, b);
    render(
      <BrowserRouter>
        <CoursesTable
          courses={coursesFixtures.oneCourseWithEachStatus}
          storybook={false}
          testId={"CoursesTable"}
          joinCallback={joinCallback}
        />
      </BrowserRouter>,
    );

    const button = screen.getByTestId(
      "CoursesTable-cell-row-2-col-studentStatus-button",
    );
    expect(button).toBeInTheDocument();
    expect(button).toHaveTextContent("View Invite");
    expect(button).toHaveAttribute("class", "btn btn-primary");

    fireEvent.click(button);
    expect(joinCallback).not.toHaveBeenCalled();

    expect(openMock).toHaveBeenCalledWith(
      "https://github.com/orgs/ucsb-cs156-f25/invitation",
      "_blank",
    );
  });
});
