import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import TeamRepositoryAssignmentForm from "main/components/Assignments/TeamRepositoryAssignmentForm";
import { vi } from "vitest";
import userEvent from "@testing-library/user-event";

const mockSubmit = vi.fn();

beforeEach(() => {
  mockSubmit.mockClear();
});

test("No submit call on empty prefix", async () => {
  render(<TeamRepositoryAssignmentForm submitAction={mockSubmit} />);
  await screen.findByText("Create");
  fireEvent.click(screen.getByText("Create"));
  expect(mockSubmit).not.toHaveBeenCalled();
  await screen.findByText("Team Repository Prefix is required.");
  expect(
    screen.getByTestId("TeamRepositoryAssignmentForm-repoPrefix"),
  ).toBeInTheDocument();
  expect(
    screen.getByTestId("TeamRepositoryAssignmentForm-assignmentPrivacy"),
  ).toBeInTheDocument();
  expect(
    screen.getByTestId("TeamRepositoryAssignmentForm-permissions"),
  ).toHaveValue("MAINTAIN");
});

test("Submit call on successful data", async () => {
  render(<TeamRepositoryAssignmentForm submitAction={mockSubmit} />);

  await screen.findByText("Create");
  fireEvent.change(screen.getByLabelText("Team Repository Prefix"), {
    target: { value: "test" },
  });
  fireEvent.click(screen.getByTestId("TeamRepositoryAssignmentForm-submit"));
  await waitFor(() => expect(mockSubmit).toHaveBeenCalled());
  expect(
    screen.queryByText("Team Repository Prefix is required."),
  ).not.toBeInTheDocument();
});

test("Submit passes selected assignment privacy", async () => {
  render(<TeamRepositoryAssignmentForm submitAction={mockSubmit} />);

  await screen.findByText("Create");

  fireEvent.change(screen.getByLabelText("Team Repository Prefix"), {
    target: { value: "test-creation-option" },
  });
  fireEvent.change(
    screen.getByTestId("TeamRepositoryAssignmentForm-assignmentPrivacy"),
    { target: { value: "false" } },
  );
  fireEvent.click(screen.getByTestId("TeamRepositoryAssignmentForm-submit"));
  await waitFor(() => expect(mockSubmit).toHaveBeenCalled());
  const firstCallArg = mockSubmit.mock.calls[0][0];
  expect(firstCallArg.assignmentPrivacy).toBe(false);
});

test("Info icon displays tooltip and opens help page on click", async () => {
  const user = userEvent.setup();

  render(<TeamRepositoryAssignmentForm submitAction={mockSubmit} />);

  await waitFor(() => {
    expect(
      screen.getByTestId(`testid-teamRegex-info-icon`),
    ).toBeInTheDocument();
  });

  const infoIcon = screen.getByTestId(`testid-teamRegex-info-icon`);
  expect(infoIcon).toBeInTheDocument();

  await user.hover(infoIcon);

  expect(
    screen.getByText(/For team names which contain this regex/i),
  ).toBeInTheDocument();
  expect(screen.getByText(/Ex:/i)).toBeInTheDocument();
  expect(
    screen.getByText(
      /"s26-0\[1-2\]" will create \[prefix\]-s26-01 and \[prefix\]-s26-02/i,
    ),
  ).toBeInTheDocument();
  expect(
    screen.getByText(/"s26" will create \[prefix\]-s26-01/i),
  ).toBeInTheDocument();
});
