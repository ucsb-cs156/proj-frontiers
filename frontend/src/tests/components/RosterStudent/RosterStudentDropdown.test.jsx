import { render, screen, fireEvent } from "@testing-library/react";
import { rosterStudentFixtures } from "fixtures/rosterStudentFixtures";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter } from "react-router";
import RosterStudentDropdown from "main/components/RosterStudent/RosterStudentDropdown";

const queryClient = new QueryClient();
describe("RosterStudentForm tests", () => {
  beforeEach(() => {
    queryClient.clear();
  });
  test("that the dropdown renders correctly and handles selection", async () => {
    const mockRegister = vi.fn(() => ({
      name: "rosterStudentId",
      onChange: vi.fn(),
      onBlur: vi.fn(),
    }));

    render(
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          <RosterStudentDropdown
            rosterStudents={rosterStudentFixtures.studentsWithEachStatus}
            register={mockRegister}
          />
        </BrowserRouter>
      </QueryClientProvider>,
    );

    // Test that it renders with the default option
    expect(screen.getByText(/Select a student\./)).toBeInTheDocument();

    // Test that all student options are rendered
    expect(screen.getByText("Alice Brown")).toBeInTheDocument();
    expect(screen.getByText("Tom Hanks")).toBeInTheDocument();

    // Test that register was called with correct validation rules
    expect(mockRegister).toHaveBeenCalledWith("rosterStudentId", {
      required: "Please select a student",
    });

    // Test selection
    const dropdown = screen.getByTestId("RosterStudentDropdown");
    fireEvent.change(dropdown, { target: { value: "1" } });
    expect(dropdown.value).toBe("1");
  });
});
