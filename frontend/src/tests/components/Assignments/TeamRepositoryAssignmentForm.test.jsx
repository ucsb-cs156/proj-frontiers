import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import TeamRepositoryAssignmentForm from "main/components/Assignments/TeamRepositoryAssignmentForm";
import { vi } from "vitest";

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

test("Team regex field renders with tooltip", async () => {
  render(<TeamRepositoryAssignmentForm submitAction={mockSubmit} />);

  await screen.findByText("Create");
  expect(screen.getByLabelText("Team Regex")).toBeInTheDocument();

  fireEvent.mouseOver(
    screen.getByTestId("TeamRepositoryAssignmentForm-teamRegex-tooltip"),
  );
  await screen.findByText(
    "Only create repositories for teams whose names match this regular expression. Leave blank to create repositories for all teams.",
  );
});

test("Submit passes team regex", async () => {
  render(<TeamRepositoryAssignmentForm submitAction={mockSubmit} />);

  await screen.findByText("Create");
  fireEvent.change(screen.getByLabelText("Team Repository Prefix"), {
    target: { value: "test" },
  });
  fireEvent.change(screen.getByLabelText("Team Regex"), {
    target: { value: "^team0[12]$" },
  });
  fireEvent.click(screen.getByTestId("TeamRepositoryAssignmentForm-submit"));
  await waitFor(() => expect(mockSubmit).toHaveBeenCalled());
  const firstCallArg = mockSubmit.mock.calls[0][0];
  expect(firstCallArg.teamRegex).toBe("^team0[12]$");
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
