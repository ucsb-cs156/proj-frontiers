import { fireEvent, render, screen } from "@testing-library/react";
import SlackUsersTable from "main/components/Slack/SlackUsersTable";
import SlackMissingMembersTable from "main/components/Slack/SlackMissingMembersTable";
import slackFixtures from "fixtures/slackFixtures";

// Values of a column in the order displayed. (Test ids of cells use the index of
// the row in the original data, so they do not change when the table is sorted.)
const columnValues = (testId, column) =>
  screen
    .getAllByTestId(new RegExp(`^${testId}-cell-row-\\d+-col-${column}$`))
    .map((cell) => cell.textContent);

describe("SlackUsersTable tests", () => {
  test("renders headers and content", () => {
    render(<SlackUsersTable users={slackFixtures.fourUsers} />);

    const headers = {
      realName: "Name",
      displayName: "Display Name",
      name: "Slack Username",
      email: "Email",
      courseRole: "Course Role",
    };
    Object.entries(headers).forEach(([id, text]) => {
      expect(
        screen.getByTestId(`SlackUsersTable-header-${id}`),
      ).toHaveTextContent(text);
    });

    const cell = (col) =>
      screen.getByTestId(`SlackUsersTable-cell-row-1-col-${col}`);
    expect(cell("realName")).toHaveTextContent("Chris Gaucho");
    expect(cell("displayName")).toHaveTextContent("chris");
    expect(cell("name")).toHaveTextContent("cgaucho");
    expect(cell("email")).toHaveTextContent("cgaucho@ucsb.edu");
    expect(cell("courseRole")).toHaveTextContent("Student");
    expect(columnValues("SlackUsersTable", "courseRole")).toEqual([
      "Instructor",
      "Student",
      "Staff",
      "None",
    ]);
  });

  test("is sortable by course role", () => {
    render(
      <SlackUsersTable users={slackFixtures.fourUsers} testIdPrefix="Custom" />,
    );

    const sortHeader = screen.getByTestId(
      "Custom-header-courseRole-sort-header",
    );
    fireEvent.click(sortHeader);
    expect(columnValues("Custom", "realName")).toEqual([
      "Phill Conrad",
      "Some Visitor",
      "Lauren Del Playa",
      "Chris Gaucho",
    ]);

    fireEvent.click(sortHeader);
    expect(columnValues("Custom", "realName")).toEqual([
      "Chris Gaucho",
      "Lauren Del Playa",
      "Some Visitor",
      "Phill Conrad",
    ]);
  });
});

describe("SlackMissingMembersTable tests", () => {
  test("renders headers and content", () => {
    render(
      <SlackMissingMembersTable members={slackFixtures.threeMissingMembers} />,
    );

    const headers = {
      courseRole: "Course Role",
      firstName: "First Name",
      lastName: "Last Name",
      email: "Email",
      slackStatus: "Slack Status",
    };
    Object.entries(headers).forEach(([id, text]) => {
      expect(
        screen.getByTestId(`SlackMissingMembersTable-header-${id}`),
      ).toHaveTextContent(text);
    });

    const cell = (col) =>
      screen.getByTestId(`SlackMissingMembersTable-cell-row-1-col-${col}`);
    expect(cell("courseRole")).toHaveTextContent("Student");
    expect(cell("firstName")).toHaveTextContent("Taylor");
    expect(cell("lastName")).toHaveTextContent("Trigo");
    expect(cell("email")).toHaveTextContent("ttrigo@ucsb.edu");
    expect(columnValues("SlackMissingMembersTable", "courseRole")).toEqual([
      "Staff",
      "Student",
      "Student",
    ]);
    expect(columnValues("SlackMissingMembersTable", "slackStatus")).toEqual([
      "Not in Slack",
      "Invited (has not signed in yet)",
      "Account deactivated",
    ]);
  });

  test("uses custom test id prefix", () => {
    render(
      <SlackMissingMembersTable
        members={slackFixtures.threeMissingMembers}
        testIdPrefix="Custom"
      />,
    );
    expect(screen.getByTestId("Custom-cell-row-0-col-email")).toHaveTextContent(
      "ssabado@ucsb.edu",
    );
  });
});
