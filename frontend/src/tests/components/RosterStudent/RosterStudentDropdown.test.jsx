import { render, screen } from "@testing-library/react";
import { rosterStudentFixtures } from "fixtures/rosterStudentFixtures";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import RosterStudentDropdown from "main/components/RosterStudent/RosterStudentDropdown";

const firstStudent = rosterStudentFixtures.studentsWithEachStatus[0];
const expectedFullName = `${firstStudent.firstName} ${firstStudent.lastName}`;

// Mock Typeahead
vi.mock("react-bootstrap-typeahead", () => ({
  Typeahead: (props) => {
    const {
      onChange,
      onInputChange,
      inputProps,
      placeholder,
      options,
      selected,
    } = props;

    expect(options.length).toBeGreaterThan(0);
    expect(options[0]).toHaveProperty("id");
    expect(options[0].fullName).toBe(expectedFullName);

    if (options.length > 0) {
      expect(selected.length).toBe(1);
      expect(selected[0].id).toBe(1);
    }

    onChange([{ id: 1, fullName: "Test Student" }]);
    onChange([]);

    if (onInputChange) {
      onInputChange("typed text");
    }

    // Render a simple input
    return (
      <input
        data-testid={inputProps["data-testid"]}
        placeholder={placeholder}
        aria-label={inputProps["aria-label"]}
        className={inputProps.className}
      />
    );
  },
}));

const queryClient = new QueryClient();
describe("RosterStudentForm tests", () => {
  beforeEach(() => {
    queryClient.clear();
  });
  test("renders invalid state and calls onChange correctly", () => {
    const mockOnChange = vi.fn();

    render(
      <RosterStudentDropdown
        rosterStudents={rosterStudentFixtures.studentsWithEachStatus}
        value={1}
        onChange={mockOnChange}
        isInvalid={true}
      />,
    );

    expect(mockOnChange).toHaveBeenCalledWith(1);
    expect(mockOnChange).toHaveBeenCalledWith("");
    expect(mockOnChange).toHaveBeenCalledWith("typed text");

    expect(
      screen.getByPlaceholderText("Select a student."),
    ).toBeInTheDocument();

    const input = screen.getByTestId("RosterStudentDropdown");
    expect(input.className).toBe("form-control is-invalid");
  });

  test("renders valid state with normal className", () => {
    const mockOnChange = vi.fn();

    render(
      <RosterStudentDropdown
        rosterStudents={rosterStudentFixtures.studentsWithEachStatus}
        value={1}
        onChange={mockOnChange}
        isInvalid={false}
      />,
    );

    const input = screen.getByTestId("RosterStudentDropdown");
    expect(input.className).toBe("form-control");
  });
});
