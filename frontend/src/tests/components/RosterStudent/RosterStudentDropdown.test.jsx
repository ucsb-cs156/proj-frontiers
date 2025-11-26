import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { rosterStudentFixtures } from "fixtures/rosterStudentFixtures";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter } from "react-router";
import RosterStudentDropdown from "main/components/RosterStudent/RosterStudentDropdown";

const queryClient = new QueryClient();
describe("RosterStudentDropdown tests", () => {
  beforeEach(() => {
    queryClient.clear();
  });
  test("that the dropdown renders correctly and handles selection", async () => {
    const mockSetValue = vi.fn();

    render(
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          <RosterStudentDropdown
            rosterStudents={rosterStudentFixtures.studentsWithEachStatus}
            setValue={mockSetValue}
          />
        </BrowserRouter>
      </QueryClientProvider>,
    );

    // Test that it renders with the placeholder
    expect(
      screen.getByPlaceholderText(/Select a student.../),
    ).toBeInTheDocument();

    // Test that the dropdown input is rendered
    const dropdown = screen.getByTestId("RosterStudentDropdown");
    expect(dropdown).toBeInTheDocument();

    // Test typing and selecting an option
    fireEvent.change(dropdown, { target: { value: "Alice" } });

    // Wait for the option to appear and click it
    await waitFor(() => {
      expect(
        screen.getByRole("option", { name: "Alice Brown" }),
      ).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole("option", { name: "Alice Brown" }));

    // Test that setValue was called with correct parameters
    expect(mockSetValue).toHaveBeenCalledWith("rosterStudentId", 1, {
      shouldValidate: true,
    });
  });

  test("that clearing the selection sets rosterStudentId to empty string", async () => {
    const mockSetValue = vi.fn();

    render(
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          <RosterStudentDropdown
            rosterStudents={rosterStudentFixtures.studentsWithEachStatus}
            setValue={mockSetValue}
          />
        </BrowserRouter>
      </QueryClientProvider>,
    );

    const dropdown = screen.getByTestId("RosterStudentDropdown");

    // First select an option
    fireEvent.change(dropdown, { target: { value: "Alice" } });

    await waitFor(() => {
      expect(
        screen.getByRole("option", { name: "Alice Brown" }),
      ).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole("option", { name: "Alice Brown" }));

    expect(mockSetValue).toHaveBeenCalledWith("rosterStudentId", 1, {
      shouldValidate: true,
    });

    // Clear the selection by focusing and clearing the input
    fireEvent.focus(dropdown);
    fireEvent.change(dropdown, { target: { value: "" } });

    // Blur to trigger the onChange with empty selection
    fireEvent.blur(dropdown);

    // Test that setValue was called with empty string when cleared
    await waitFor(() => {
      expect(mockSetValue).toHaveBeenCalledWith("rosterStudentId", "", {
        shouldValidate: true,
      });
    });
  });
});
