import { fireEvent, render, waitFor, screen } from "@testing-library/react";
import { rosterStudentFixtures } from "fixtures/rosterStudentFixtures";
import RosterStudentTable, {
  cellToAxiosParamsDelete,
} from "main/components/RosterStudent/RosterStudentTable";
import { QueryClient, QueryClientProvider } from "react-query";
import { MemoryRouter } from "react-router-dom";
import { currentUserFixtures } from "fixtures/currentUserFixtures";

const mockedNavigate = jest.fn();
const mockedMutate = jest.fn();

jest.mock("react-router-dom", () => ({
  ...jest.requireActual("react-router-dom"),
  useNavigate: () => mockedNavigate,
}));

jest.mock("main/utils/useBackend", () => ({
  ...jest.requireActual("main/utils/useBackend"),
  useBackendMutation: () => ({
    mutate: mockedMutate,
  }),
}));

describe("RosterStudentTable tests", () => {
  const queryClient = new QueryClient();

  test("renders without crashing for empty table", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <RosterStudentTable rosterStudents={[]} />
        </MemoryRouter>
      </QueryClientProvider>,
    );
  });

  test("renders without crashing for three roster students", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <RosterStudentTable
            rosterStudents={rosterStudentFixtures.threeRosterStudents}
          />
        </MemoryRouter>
      </QueryClientProvider>,
    );
  });

  test("Has the expected column headers and content", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <RosterStudentTable
            rosterStudents={rosterStudentFixtures.threeRosterStudents}
          />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const expectedHeaders = [
      "id",
      "Enrollment Code",
      "Student ID",
      "First Name",
      "Last Name",
      "Email",
    ];
    const expectedFields = [
      "id",
      "enrollmentCode",
      "studentId",
      "firstName",
      "lastName",
      "email",
    ];
    const testId = "RosterStudentTable";

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
      screen.getByTestId(`${testId}-cell-row-0-col-enrollmentCode`),
    ).toHaveTextContent("12345");
    expect(screen.getByTestId(`${testId}-cell-row-1-col-id`)).toHaveTextContent(
      "2",
    );
    expect(
      screen.getByTestId(`${testId}-cell-row-1-col-enrollmentCode`),
    ).toHaveTextContent("23456");
  });

  test("Edit button navigates to the edit page for admin user", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <RosterStudentTable
            rosterStudents={rosterStudentFixtures.threeRosterStudents}
            currentUser={currentUserFixtures.adminUser}
          />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(
      await screen.findByTestId("RosterStudentTable-cell-row-0-col-id"),
    ).toHaveTextContent("1");

    const editButton = screen.getByTestId(
      "RosterStudentTable-cell-row-0-col-Edit-button",
    );
    expect(editButton).toBeInTheDocument();

    // Test the Edit button has the primary style
    expect(editButton).toHaveClass("btn-primary");

    fireEvent.click(editButton);

    await waitFor(() =>
      expect(mockedNavigate).toHaveBeenCalledWith("/rosterstudent/edit/1"),
    );
  });

  test("Delete button calls delete callback for admin user", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <RosterStudentTable
            rosterStudents={rosterStudentFixtures.threeRosterStudents}
            currentUser={currentUserFixtures.adminUser}
          />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(
      await screen.findByTestId("RosterStudentTable-cell-row-0-col-id"),
    ).toHaveTextContent("1");

    const deleteButton = screen.getByTestId(
      "RosterStudentTable-cell-row-0-col-Delete-button",
    );
    expect(deleteButton).toBeInTheDocument();

    // Test the Delete button has the danger style
    expect(deleteButton).toHaveClass("btn-danger");

    fireEvent.click(deleteButton);

    // Check that the mutate function was called
    await waitFor(() => expect(mockedMutate).toHaveBeenCalled());
  });

  test("Edit and Delete buttons are not rendered for non-admin user", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <RosterStudentTable
            rosterStudents={rosterStudentFixtures.threeRosterStudents}
            currentUser={currentUserFixtures.userOnly}
          />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(
      await screen.findByTestId("RosterStudentTable-cell-row-0-col-id"),
    ).toHaveTextContent("1");

    expect(
      screen.queryByTestId("RosterStudentTable-cell-row-0-col-Edit-button"),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByTestId("RosterStudentTable-cell-row-0-col-Delete-button"),
    ).not.toBeInTheDocument();
  });

  // Test the cellToAxiosParamsDelete function directly
  test("cellToAxiosParamsDelete returns the correct parameters", () => {
    // Create a mock cell object that matches the structure expected by the function
    const cell = {
      row: {
        values: {
          id: 5,
        },
      },
    };

    // Call the function and check its output
    const result = cellToAxiosParamsDelete(cell);
    expect(result).toEqual({
      url: "/api/rosterstudents",
      method: "DELETE",
      params: {
        id: 5,
      },
    });
  });
});
