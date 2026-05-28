import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import DeleteRepoForm from "main/components/Assignments/DeleteRepoForm";
import { vi } from "vitest";

const mockSubmit = vi.fn();

beforeEach(() => {
  mockSubmit.mockClear();
});

test("No submit call on empty prefix", async () => {
  render(<DeleteRepoForm submitAction={mockSubmit} />);

  await screen.findByText("Delete Empty Matching Repos");
  fireEvent.click(screen.getByTestId("DeleteRepoForm-submit"));

  expect(mockSubmit).not.toHaveBeenCalled();

  await screen.findByText("Repository Prefix is required.");
  expect(screen.getByTestId("DeleteRepoForm-repoPrefix")).toBeInTheDocument();
});

test("Submit call on successful data", async () => {
  render(<DeleteRepoForm submitAction={mockSubmit} />);

  await screen.findByText("Delete Empty Matching Repos");

  fireEvent.change(screen.getByTestId("DeleteRepoForm-repoPrefix"), {
    target: { value: "test-prefix" },
  });

  fireEvent.click(screen.getByTestId("DeleteRepoForm-submit"));

  await waitFor(() => expect(mockSubmit).toHaveBeenCalled());

  expect(
    screen.queryByText("Repository Prefix is required."),
  ).not.toBeInTheDocument();

  // Verify the submit action was passed the correct form data object
  const firstCallArg = mockSubmit.mock.calls[0][0];
  expect(firstCallArg.repoPrefix).toBe("test-prefix");
});
