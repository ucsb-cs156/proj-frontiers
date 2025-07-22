import {
  render,
  screen,
  waitFor,
  fireEvent,
  within,
} from "@testing-library/react";
import coursesFixtures from "fixtures/coursesFixtures";
import CoursesTable from "main/components/Courses/CoursesTable";
import { BrowserRouter } from "react-router-dom";

const joinCallback = jest.fn();
const isLoading = jest.fn(() => false);

describe("CoursesTable tests", () => {
  test("Has the expected column headers and content", () => {
    render(
      <BrowserRouter>
        <CoursesTable
          courses={coursesFixtures.oneCourseWithEachStatus}
          testId={"CoursesTable"}
          joinCallback={joinCallback}
          isLoading={isLoading}
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

  test("the loading render", async () => {
    const loadingMocks = [
      {
        id: 1,
        courseName: "CMPSC 156",
        term: "Spring 2025",
        school: "UCSB",
        orgName: "ucsb-cs156-s25",
        studentStatus: "JOINCOURSE",
      },
      {
        id: 2,
        courseName: "CPTS 489",
        term: "Fall 2020",
        school: "WSU",
        orgName: "wsu-cpts489-f20",
        studentStatus: "JOINCOURSE",
      },
    ];

    const determineLoading = (cell) => {
      return cell.row.index === 1;
    };

    render(
      <BrowserRouter>
        <CoursesTable
          courses={loadingMocks}
          testId={"CoursesTable"}
          joinCallback={joinCallback}
          isLoading={determineLoading}
        />
      </BrowserRouter>,
    );

    const button = screen.getByText("Join Course");
    expect(button).toHaveAttribute("class", "btn btn-primary");
    const loadingButton = screen.getByText("Joining...");
    expect(within(loadingButton).getByRole("status")).toHaveClass(
      "spinner-grow",
      "spinner-grow-sm",
    );
  });
  //tests for button 'Join Course'
  test("Calls joinCallback when the button is pressed", async () => {
    render(
      <BrowserRouter>
        <CoursesTable
          courses={coursesFixtures.oneCourseWithEachStatus}
          testId={"CoursesTable"}
          joinCallback={joinCallback}
          isLoading={isLoading}
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
  //tests for button 'View Invite'
  test("Does not call joinCallback in default case for button'View Invite'", () => {
    render(
      <BrowserRouter>
        <CoursesTable
          courses={coursesFixtures.oneCourseWithEachStatus}
          testId={"CoursesTable"}
          joinCallback={joinCallback}
          isLoading={isLoading}
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
  test("Does not call joinCallback when storybook is explicitly false for button 'View Invite'", () => {
    const openMock = jest.fn();
    window.open = (a, b) => openMock(a, b);
    render(
      <BrowserRouter>
        <CoursesTable
          courses={coursesFixtures.oneCourseWithEachStatus}
          testId={"CoursesTable"}
          joinCallback={joinCallback}
          isLoading={isLoading}
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
  test("tooltips for PENDING status", async () => {
    render(
      <BrowserRouter>
        <CoursesTable
          courses={coursesFixtures.oneCourseWithEachStatus}
          testId={"CoursesTable"}
          joinCallback={joinCallback}
          isLoading={isLoading}
        />
      </BrowserRouter>,
    );

    fireEvent.mouseOver(screen.getByText("Pending"));

    await waitFor(() => {
      expect(
        screen.getByText(
          "This course has not been completely set up by your instructor yet.",
        ),
      ).toBeInTheDocument();
    });
  });
  test("tooltips for JOINCOURSE status", async () => {
    render(
      <BrowserRouter>
        <CoursesTable
          courses={coursesFixtures.oneCourseWithEachStatus}
          testId={"CoursesTable"}
          joinCallback={joinCallback}
          isLoading={isLoading}
        />
      </BrowserRouter>,
    );

    fireEvent.mouseOver(screen.getByText("Join Course"));

    await waitFor(() => {
      expect(
        screen.getByText(
          "Clicking this button will generate an invitation to the GitHub organization associated with this course.",
        ),
      ).toBeInTheDocument();
    });
  });
  test("tooltips for INVITED status", async () => {
    render(
      <BrowserRouter>
        <CoursesTable
          courses={coursesFixtures.oneCourseWithEachStatus}
          testId={"CoursesTable"}
          joinCallback={joinCallback}
          isLoading={isLoading}
        />
      </BrowserRouter>,
    );

    fireEvent.mouseOver(screen.getByText("View Invite"));
    await waitFor(() => {
      expect(
        screen.getByText(
          "You have been invited to the GitHub organization associated with this course, but you still need to accept or decline the invitation. Please accept it if you plan to stay enrolled, and decline only if you plan to withdraw from the course.",
        ),
      ).toBeInTheDocument();
    });
  });
  test("tooltips for OWNER status", async () => {
    render(
      <BrowserRouter>
        <CoursesTable
          courses={coursesFixtures.oneCourseWithEachStatus}
          testId={"CoursesTable"}
          joinCallback={joinCallback}
          isLoading={isLoading}
        />
      </BrowserRouter>,
    );

    fireEvent.mouseOver(screen.getByText("Owner"));
    await waitFor(() => {
      expect(
        screen.getByText(
          "You are an owner of the GitHub organization associated with this course.",
        ),
      ).toBeInTheDocument();
    });
  });
  test("tooltips for MEMBER status", async () => {
    render(
      <BrowserRouter>
        <CoursesTable
          courses={coursesFixtures.oneCourseWithEachStatus}
          testId={"CoursesTable"}
          joinCallback={joinCallback}
          isLoading={isLoading}
        />
      </BrowserRouter>,
    );

    fireEvent.mouseOver(screen.getByText("Member"));

    await waitFor(() => {
      expect(
        screen.getByText(
          "You are a member of the GitHub organization associated with this course.",
        ),
      ).toBeInTheDocument();
    });
  });
  test("tooltips for an illegal status", async () => {
    render(
      <BrowserRouter>
        <CoursesTable
          courses={coursesFixtures.oneCourseWithEachStatus}
          testId={"CoursesTable"}
          joinCallback={joinCallback}
          isLoading={isLoading}
        />
      </BrowserRouter>,
    );

    fireEvent.mouseOver(
      screen.getByText("Illegal status that will never occur"),
    );

    await waitFor(() => {
      expect(
        screen.getByText("Tooltip for illegal status that will never occur"),
      ).toBeInTheDocument();
    });
  });
  test("expect the correct tooltip ID", async () => {
    render(
      <BrowserRouter>
        <CoursesTable
          courses={coursesFixtures.oneCourseWithEachStatus}
          testId={"CoursesTable"}
          joinCallback={joinCallback}
          isLoading={isLoading}
        />
      </BrowserRouter>,
    );

    fireEvent.mouseOver(screen.getByText("Member"));

    const tooltip = await screen.findByRole("tooltip");
    expect(tooltip).toHaveAttribute("id", "member-tooltip");
  });
});
