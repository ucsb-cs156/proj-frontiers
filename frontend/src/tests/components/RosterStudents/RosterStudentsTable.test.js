import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "react-query";
import rosterStudentsFixtures from "fixtures/rosterStudentsFixtures";
import RosterStudentsTable from "main/components/RosterStudents/RosterStudentsTable";
import { MemoryRouter } from "react-router-dom";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";

const mockedNavigate = jest.fn();

jest.mock("react-router-dom", () => ({
  ...jest.requireActual("react-router-dom"),
  useNavigate: () => mockedNavigate,
}));

describe("RosterStudentsTable tests", () => {
  const queryClient = new QueryClient();

  test("Has the expected column headers and content", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <RosterStudentsTable
            rosterStudents={rosterStudentsFixtures.threeRosterStudents}
          />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const expectedHeaders = [
      "id",
      "Student ID",
      "First Name",
      "Last Name",
      "Email",
      "Roster Status",
      "Github Org Status",
      "Github ID",
      "Github Username",
    ];
    const expectedFields = [
      "id",
      "studentId",
      "firstName",
      "lastName",
      "email",
      "rosterStatus",
      "orgStatus",
      "githubId",
      "githubLogin",
    ];
    const testId = "RosterStudentsTable";

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
      screen.getByTestId(`${testId}-cell-row-0-col-studentId`),
    ).toHaveTextContent("1234X67");
    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-firstName`),
    ).toHaveTextContent("Phill");
    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-lastName`),
    ).toHaveTextContent("Conrad");
    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-email`),
    ).toHaveTextContent("phtcon@ucsb.edu");
    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-rosterStatus`),
    ).toHaveTextContent("MANUAL");
    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-orgStatus`),
    ).toHaveTextContent("NONE");
    // github ID and Login are empty when null
    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-githubId`),
    ).toHaveTextContent("");
    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-githubLogin`),
    ).toHaveTextContent("");
  });

  test("Does NOT show buttons by default", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <RosterStudentsTable
            rosterStudents={rosterStudentsFixtures.threeRosterStudents}
          />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const editButton = screen.queryByTestId(
      "RosterStudentsTable-cell-row-0-col-Edit-button",
    );
    expect(editButton).not.toBeInTheDocument();

    const deleteButton = screen.queryByTestId(
      "RosterStudentsTable-cell-row-0-col-Delete-button",
    );
    expect(deleteButton).not.toBeInTheDocument();
  });

  test("Edit button appears and navigates to edit page", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <RosterStudentsTable
            rosterStudents={rosterStudentsFixtures.threeRosterStudents}
            showButtons={true}
          />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const editButton = screen.getByTestId(
      "RosterStudentsTable-cell-row-0-col-Edit-button",
    );
    expect(editButton).toBeInTheDocument();
    expect(editButton).toHaveTextContent("Edit");
    expect(editButton).toHaveAttribute("class", "btn btn-primary");

    fireEvent.click(editButton);

    await waitFor(() =>
      expect(mockedNavigate).toHaveBeenCalledWith("/rosterstudents/edit/1"),
    );
  });

  test("Delete button appears and calls delete callback", async () => {

    const axiosMock = new AxiosMockAdapter(axios);
    axiosMock
      .onDelete("/api/rosterstudents")
      .reply(200, { message: "Roster Student deleted" });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <RosterStudentsTable
            rosterStudents={rosterStudentsFixtures.threeRosterStudents}
            showButtons={true}
          />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const deleteButton = screen.getByTestId(
      "RosterStudentsTable-cell-row-0-col-Delete-button",
    );
    expect(deleteButton).toBeInTheDocument();
    expect(deleteButton).toHaveTextContent("Delete");
    expect(deleteButton).toHaveAttribute("class", "btn btn-danger");

    
    fireEvent.click(deleteButton);

    await waitFor(() => expect(axiosMock.history.delete.length).toBe(1));
    expect(axiosMock.history.delete[0].params).toEqual({ id: 1 });
  });
});
