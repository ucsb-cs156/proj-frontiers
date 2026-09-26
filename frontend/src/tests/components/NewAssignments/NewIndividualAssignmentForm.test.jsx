import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, test, vi } from "vitest";
import NewIndividualAssignmentForm from "main/components/NewAssignments/NewIndividualAssignmentForm";

const testId = "NewIndividualAssignmentForm";

const optionsOf = (element) =>
  Array.from(element.querySelectorAll("option")).map((o) => [
    o.value,
    o.textContent,
  ]);

describe("NewIndividualAssignmentForm tests", () => {
  test("renders with the defaults when there are no initialContents", () => {
    render(<NewIndividualAssignmentForm submitAction={vi.fn()} />);

    expect(screen.getByText("Repository Prefix")).toBeInTheDocument();
    expect(screen.getByLabelText("Private Repositories?")).toBe(
      screen.getByTestId(`${testId}-isPrivate`),
    );
    expect(screen.getByText("Student Permissions")).toBeInTheDocument();
    expect(screen.getByText("Create Repositories for")).toBeInTheDocument();

    expect(screen.getByTestId(`${testId}-repoPrefix`)).toHaveValue("");
    expect(screen.getByTestId(`${testId}-isPrivate`)).not.toBeChecked();
    expect(screen.getByTestId(`${testId}-permission`)).toHaveValue("MAINTAIN");
    expect(screen.getByTestId(`${testId}-createReposFor`)).toHaveValue(
      "STUDENTS_ONLY",
    );
    expect(screen.getByTestId(`${testId}-submit`)).toHaveTextContent("Create");
  });

  test("has a switch to require signed commits, off by default, that says what it does", () => {
    render(<NewIndividualAssignmentForm submitAction={vi.fn()} />);

    expect(screen.getByLabelText("Require Signed Commits?")).toBe(
      screen.getByTestId(`${testId}-requireSignedCommit`),
    );
    expect(
      screen.getByTestId(`${testId}-requireSignedCommit`),
    ).not.toBeChecked();
    expect(
      screen.getByTestId(`${testId}-requireSignedCommit-help`),
    ).toHaveTextContent(
      "Repositories get a ruleset that requires signed commits on every branch except gh-pages. When this is off, that ruleset is removed from them.",
    );
  });

  test("shows that signed commits are required when initialContents say so", () => {
    render(
      <NewIndividualAssignmentForm
        submitAction={vi.fn()}
        initialContents={{ repoPrefix: "lab02", requireSignedCommit: true }}
      />,
    );

    expect(screen.getByTestId(`${testId}-requireSignedCommit`)).toBeChecked();
  });

  test("offers the same options as the individual assignment form of the Assignments tab", () => {
    render(<NewIndividualAssignmentForm submitAction={vi.fn()} />);

    expect(optionsOf(screen.getByTestId(`${testId}-permission`))).toEqual([
      ["READ", "Read"],
      ["WRITE", "Write"],
      ["MAINTAIN", "Maintain"],
      ["ADMIN", "Admin"],
    ]);
    expect(optionsOf(screen.getByTestId(`${testId}-createReposFor`))).toEqual([
      ["STUDENTS_ONLY", "Students Only"],
      ["STAFF_ONLY", "Staff Only"],
      ["STUDENTS_AND_STAFF", "Students and Staff"],
    ]);
  });

  test("shows initialContents and a custom button label", () => {
    render(
      <NewIndividualAssignmentForm
        submitAction={vi.fn()}
        initialContents={{
          repoPrefix: "lab02",
          isPrivate: true,
          permission: "READ",
          createReposFor: "STUDENTS_AND_STAFF",
        }}
        buttonLabel="Update"
      />,
    );

    expect(screen.getByTestId(`${testId}-repoPrefix`)).toHaveValue("lab02");
    expect(screen.getByTestId(`${testId}-isPrivate`)).toBeChecked();
    expect(screen.getByTestId(`${testId}-permission`)).toHaveValue("READ");
    expect(screen.getByTestId(`${testId}-createReposFor`)).toHaveValue(
      "STUDENTS_AND_STAFF",
    );
    expect(screen.getByTestId(`${testId}-submit`)).toHaveTextContent("Update");
  });

  test("requires a repository prefix, and does not submit without one", async () => {
    const submitAction = vi.fn();
    render(<NewIndividualAssignmentForm submitAction={submitAction} />);

    fireEvent.click(screen.getByTestId(`${testId}-submit`));
    await screen.findByText("Repository Prefix is required.");
    expect(screen.getByTestId(`${testId}-repoPrefix`)).toHaveClass(
      "is-invalid",
    );
    expect(submitAction).not.toHaveBeenCalled();

    fireEvent.change(screen.getByTestId(`${testId}-repoPrefix`), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId(`${testId}-submit`));
    await screen.findByText("Repository Prefix is required.");
    expect(submitAction).not.toHaveBeenCalled();
  });

  test("submits the default choices with the entered prefix", async () => {
    const submitAction = vi.fn();
    render(<NewIndividualAssignmentForm submitAction={submitAction} />);

    fireEvent.change(screen.getByTestId(`${testId}-repoPrefix`), {
      target: { value: "lab03" },
    });
    fireEvent.click(screen.getByTestId(`${testId}-submit`));

    await waitFor(() => expect(submitAction).toHaveBeenCalledTimes(1));
    expect(submitAction.mock.calls[0][0]).toEqual({
      repoPrefix: "lab03",
      isPrivate: false,
      permission: "MAINTAIN",
      requireSignedCommit: false,
      createReposFor: "STUDENTS_ONLY",
    });
    expect(
      screen.queryByText("Repository Prefix is required."),
    ).not.toBeInTheDocument();
    expect(screen.getByTestId(`${testId}-repoPrefix`)).not.toHaveClass(
      "is-invalid",
    );
  });

  test("submits the changed choices", async () => {
    const submitAction = vi.fn();
    render(<NewIndividualAssignmentForm submitAction={submitAction} />);

    fireEvent.change(screen.getByTestId(`${testId}-repoPrefix`), {
      target: { value: "lab04" },
    });
    fireEvent.click(screen.getByTestId(`${testId}-isPrivate`));
    fireEvent.click(screen.getByTestId(`${testId}-requireSignedCommit`));
    fireEvent.change(screen.getByTestId(`${testId}-permission`), {
      target: { value: "ADMIN" },
    });
    fireEvent.change(screen.getByTestId(`${testId}-createReposFor`), {
      target: { value: "STAFF_ONLY" },
    });
    fireEvent.click(screen.getByTestId(`${testId}-submit`));

    await waitFor(() => expect(submitAction).toHaveBeenCalledTimes(1));
    expect(submitAction.mock.calls[0][0]).toEqual({
      repoPrefix: "lab04",
      isPrivate: true,
      permission: "ADMIN",
      requireSignedCommit: true,
      createReposFor: "STAFF_ONLY",
    });
  });
});
