import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import RosterStudentsTable from "main/components/RosterStudents/RosterStudentsTable";
import rosterStudentsFixtures from "fixtures/RosterStudentsFixtures";
import { BrowserRouter } from "react-router-dom";

window.alert = jest.fn();

describe("RosterStudentsTable tests", () => {
  const testId = "RosterStudentsTable";

  test("renders correct column headers", () => {
    const expectedHeaders = [
      "id",
      "First Name",
      "Last Name",
      "Email",
      "GitHub Username",
      "GitHub ID",
    ];

    render(
      <BrowserRouter>
        <RosterStudentsTable
          rosterStudents={rosterStudentsFixtures.threeRosterStudents}
        />
      </BrowserRouter>,
    );

    expectedHeaders.forEach((headerText) => {
      expect(screen.getByText(headerText)).toBeInTheDocument();
    });
  });

  test("renders expected field data for all rows", () => {
    render(
      <BrowserRouter>
        <RosterStudentsTable
          rosterStudents={rosterStudentsFixtures.threeRosterStudents}
        />
      </BrowserRouter>,
    );

    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-firstName`),
    ).toHaveTextContent("Shuang");
    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-lastName`),
    ).toHaveTextContent("Li");
    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-email`),
    ).toHaveTextContent("shuang@ucsb.edu");
    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-githubLogin`),
    ).toHaveTextContent("aliceGH");
    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-githubId`),
    ).toHaveTextContent("123456");

    expect(
      screen.getByTestId(`${testId}-cell-row-1-col-lastName`),
    ).toHaveTextContent("Green");
    expect(
      screen.getByTestId(`${testId}-cell-row-2-col-email`),
    ).toHaveTextContent("wendy@ucsb.edu");
  });

  test("does NOT show Edit/Delete buttons when showButtons is false explicitly", () => {
    render(
      <BrowserRouter>
        <RosterStudentsTable
          rosterStudents={rosterStudentsFixtures.threeRosterStudents}
          showButtons={false}
        />
      </BrowserRouter>,
    );

    expect(
      screen.queryByTestId(`${testId}-cell-row-0-col-Edit-button`),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByTestId(`${testId}-cell-row-0-col-Delete-button`),
    ).not.toBeInTheDocument();
  });

  test("does NOT show Edit/Delete buttons when showButtons is omitted", () => {
    render(
      <BrowserRouter>
        <RosterStudentsTable
          rosterStudents={rosterStudentsFixtures.threeRosterStudents}
        />
      </BrowserRouter>,
    );

    expect(
      screen.queryByTestId(`${testId}-cell-row-0-col-Edit-button`),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByTestId(`${testId}-cell-row-0-col-Delete-button`),
    ).not.toBeInTheDocument();
  });

  test("shows Edit/Delete buttons and triggers alerts in storybook mode", async () => {
    render(
      <BrowserRouter>
        <RosterStudentsTable
          rosterStudents={rosterStudentsFixtures.threeRosterStudents}
          showButtons={true}
          storybook={true}
        />
      </BrowserRouter>,
    );

    const editBtn = screen.getByTestId(`${testId}-cell-row-0-col-Edit-button`);
    const deleteBtn = screen.getByTestId(
      `${testId}-cell-row-0-col-Delete-button`,
    );

    fireEvent.click(editBtn);
    fireEvent.click(deleteBtn);

    await waitFor(() => {
      expect(window.alert).toHaveBeenCalledTimes(2);
    });
  });

  test("Edit/Delete use console.log when not in storybook", () => {
    const logSpy = jest.spyOn(console, "log").mockImplementation(() => {});

    render(
      <BrowserRouter>
        <RosterStudentsTable
          rosterStudents={rosterStudentsFixtures.threeRosterStudents}
          showButtons={true}
          storybook={false}
        />
      </BrowserRouter>,
    );

    fireEvent.click(screen.getByTestId(`${testId}-cell-row-0-col-Edit-button`));
    fireEvent.click(
      screen.getByTestId(`${testId}-cell-row-0-col-Delete-button`),
    );

    expect(logSpy).toHaveBeenCalledWith("Edit clicked for row with id: 1");
    expect(logSpy).toHaveBeenCalledWith("Delete clicked for row with id: 1");

    logSpy.mockRestore();
  });

  test("edit/delete fallback when storybook is omitted (default false)", () => {
    const logSpy = jest.spyOn(console, "log").mockImplementation(() => {});

    render(
      <BrowserRouter>
        <RosterStudentsTable
          rosterStudents={rosterStudentsFixtures.threeRosterStudents}
          showButtons={true}
        />
      </BrowserRouter>,
    );

    fireEvent.click(screen.getByTestId(`${testId}-cell-row-0-col-Edit-button`));
    fireEvent.click(
      screen.getByTestId(`${testId}-cell-row-0-col-Delete-button`),
    );

    expect(logSpy).toHaveBeenCalledWith("Edit clicked for row with id: 1");
    expect(logSpy).toHaveBeenCalledWith("Delete clicked for row with id: 1");

    logSpy.mockRestore();
  });

  test("Edit/Delete buttons have correct text and class", () => {
    render(
      <BrowserRouter>
        <RosterStudentsTable
          rosterStudents={rosterStudentsFixtures.threeRosterStudents}
          showButtons={true}
        />
      </BrowserRouter>,
    );

    const editBtn = screen.getByTestId(`${testId}-cell-row-0-col-Edit-button`);
    const deleteBtn = screen.getByTestId(
      `${testId}-cell-row-0-col-Delete-button`,
    );

    expect(editBtn).toHaveTextContent("Edit");
    expect(editBtn).toHaveClass("btn-primary");
    expect(deleteBtn).toHaveTextContent("Delete");
    expect(deleteBtn).toHaveClass("btn-danger");
  });
});
