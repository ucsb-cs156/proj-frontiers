import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import IndividualAssignmentForm from "main/components/Assignments/IndividualAssignmentForm";
import { vi } from "vitest";

const mockSubmit = vi.fn();

test("No submit call on empty prefix", async () => {
  render(<IndividualAssignmentForm submitAction={mockSubmit} />);
  await screen.findByText("Create");
  fireEvent.click(screen.getByText("Create"));
  expect(mockSubmit).not.toHaveBeenCalled();
  await screen.findByText("Repository Prefix is required.");
  expect(
    screen.getByTestId("IndividualAssignmentForm-repoPrefix"),
  ).toBeInTheDocument();
  expect(
    screen.getByTestId("IndividualAssignmentForm-assignmentPrivacy"),
  ).toBeInTheDocument();
  expect(
    screen.getByTestId("IndividualAssignmentForm-permissions"),
  ).toHaveValue("MAINTAIN");
});

test("Submit call on successful data", async () => {
  render(<IndividualAssignmentForm submitAction={mockSubmit} />);

  await screen.findByText("Create");
  fireEvent.change(screen.getByLabelText("Repository Prefix"), {
    target: { value: "test" },
  });
  fireEvent.click(screen.getByTestId("IndividualAssignmentForm-submit"));
  await waitFor(() => expect(mockSubmit).toHaveBeenCalled());
  expect(
    screen.queryByText("Repository Prefix is required."),
  ).not.toBeInTheDocument();
});
