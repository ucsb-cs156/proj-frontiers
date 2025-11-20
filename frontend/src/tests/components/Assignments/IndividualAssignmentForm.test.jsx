import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import IndividualAssignmentForm from "main/components/Assignments/IndividualAssignmentForm";
import { vi } from "vitest";

const mockSubmit = vi.fn();

beforeEach(() => {
  mockSubmit.mockClear();
});

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
  expect(
    screen.getByTestId("IndividualAssignmentForm-creationOption"),
  ).toHaveValue("STUDENTS_ONLY");
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

test("Submit passes selected creation option", async () => {
  render(<IndividualAssignmentForm submitAction={mockSubmit} />);

  await screen.findByText("Create");

  fireEvent.change(screen.getByLabelText("Repository Prefix"), {
    target: { value: "test-creation-option" },
  });
  fireEvent.change(
    screen.getByTestId("IndividualAssignmentForm-creationOption"),
    { target: { value: "STUDENTS_AND_STAFF" } },
  );
  fireEvent.click(screen.getByTestId("IndividualAssignmentForm-submit"));
  await waitFor(() => expect(mockSubmit).toHaveBeenCalled());
  const firstCallArg = mockSubmit.mock.calls[0][0];
  expect(firstCallArg.creationOption).toBe("STUDENTS_AND_STAFF");
});
