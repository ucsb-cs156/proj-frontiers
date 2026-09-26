import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, test, vi } from "vitest";
import NewTeamAssignmentForm from "main/components/NewAssignments/NewTeamAssignmentForm";

const testId = "NewTeamAssignmentForm";

describe("NewTeamAssignmentForm tests", () => {
  test("renders with the defaults when there are no initialContents", () => {
    render(<NewTeamAssignmentForm submitAction={vi.fn()} />);

    expect(screen.getByText("Team Repository Prefix")).toBeInTheDocument();
    expect(screen.getByLabelText("Private Repositories?")).toBe(
      screen.getByTestId(`${testId}-isPrivate`),
    );
    expect(screen.getByText("Team Permissions")).toBeInTheDocument();
    expect(screen.getByText("Team Regex")).toBeInTheDocument();

    expect(screen.getByTestId(`${testId}-repoPrefix`)).toHaveValue("");
    expect(screen.getByTestId(`${testId}-isPrivate`)).not.toBeChecked();
    expect(screen.getByTestId(`${testId}-permission`)).toHaveValue("MAINTAIN");
    expect(screen.getByTestId(`${testId}-teamRegex`)).toHaveValue(".*");
    expect(screen.getByTestId(`${testId}-submit`)).toHaveTextContent("Create");
    expect(
      screen.queryByTestId(`${testId}-createReposFor`),
    ).not.toBeInTheDocument();
  });

  test("has a switch to require signed commits, off by default, that says what it does", () => {
    render(<NewTeamAssignmentForm submitAction={vi.fn()} />);

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
      <NewTeamAssignmentForm
        submitAction={vi.fn()}
        initialContents={{
          repoPrefix: "proj",
          teamRegex: ".*",
          requireSignedCommit: true,
        }}
      />,
    );

    expect(screen.getByTestId(`${testId}-requireSignedCommit`)).toBeChecked();
  });

  test("shows initialContents and a custom button label", () => {
    render(
      <NewTeamAssignmentForm
        submitAction={vi.fn()}
        initialContents={{
          repoPrefix: "proj",
          isPrivate: true,
          permission: "WRITE",
          teamRegex: "s26-.*",
        }}
        buttonLabel="Update"
      />,
    );

    expect(screen.getByTestId(`${testId}-repoPrefix`)).toHaveValue("proj");
    expect(screen.getByTestId(`${testId}-isPrivate`)).toBeChecked();
    expect(screen.getByTestId(`${testId}-permission`)).toHaveValue("WRITE");
    expect(screen.getByTestId(`${testId}-teamRegex`)).toHaveValue("s26-.*");
    expect(screen.getByTestId(`${testId}-submit`)).toHaveTextContent("Update");
  });

  test("the info icon explains the team regex", async () => {
    const user = userEvent.setup();
    const { container } = render(
      <NewTeamAssignmentForm submitAction={vi.fn()} />,
    );

    await user.hover(container.querySelector("svg"));

    const tooltip = await screen.findByRole("tooltip");
    expect(tooltip).toHaveTextContent(
      "Only teams whose names match this regular expression will have repositories created. The default, .*, matches every team.",
    );
  });

  test("requires a prefix and a team regex, and does not submit without them", async () => {
    const submitAction = vi.fn();
    render(<NewTeamAssignmentForm submitAction={submitAction} />);

    fireEvent.change(screen.getByTestId(`${testId}-teamRegex`), {
      target: { value: "  " },
    });
    fireEvent.click(screen.getByTestId(`${testId}-submit`));

    await screen.findByText("Team Repository Prefix is required.");
    expect(screen.getByText("Team Regex is required.")).toBeInTheDocument();
    expect(screen.getByTestId(`${testId}-repoPrefix`)).toHaveClass(
      "is-invalid",
    );
    expect(screen.getByTestId(`${testId}-teamRegex`)).toHaveClass("is-invalid");
    expect(submitAction).not.toHaveBeenCalled();

    // a prefix of only spaces is not accepted, even when everything else is fine
    fireEvent.change(screen.getByTestId(`${testId}-teamRegex`), {
      target: { value: ".*" },
    });
    fireEvent.change(screen.getByTestId(`${testId}-repoPrefix`), {
      target: { value: " " },
    });
    fireEvent.click(screen.getByTestId(`${testId}-submit`));
    await waitFor(() =>
      expect(
        screen.queryByText("Team Regex is required."),
      ).not.toBeInTheDocument(),
    );
    expect(
      screen.getByText("Team Repository Prefix is required."),
    ).toBeInTheDocument();
    expect(submitAction).not.toHaveBeenCalled();
  });

  test("submits the defaults with the entered prefix", async () => {
    const submitAction = vi.fn();
    render(<NewTeamAssignmentForm submitAction={submitAction} />);

    fireEvent.change(screen.getByTestId(`${testId}-repoPrefix`), {
      target: { value: "proj" },
    });
    fireEvent.click(screen.getByTestId(`${testId}-submit`));

    await waitFor(() => expect(submitAction).toHaveBeenCalledTimes(1));
    expect(submitAction.mock.calls[0][0]).toEqual({
      repoPrefix: "proj",
      isPrivate: false,
      permission: "MAINTAIN",
      requireSignedCommit: false,
      teamRegex: ".*",
    });
    expect(
      screen.queryByText("Team Repository Prefix is required."),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByText("Team Regex is required."),
    ).not.toBeInTheDocument();
    expect(screen.getByTestId(`${testId}-repoPrefix`)).not.toHaveClass(
      "is-invalid",
    );
    expect(screen.getByTestId(`${testId}-teamRegex`)).not.toHaveClass(
      "is-invalid",
    );
  });

  test("submits the changed choices", async () => {
    const submitAction = vi.fn();
    render(<NewTeamAssignmentForm submitAction={submitAction} />);

    fireEvent.change(screen.getByTestId(`${testId}-repoPrefix`), {
      target: { value: "proj2" },
    });
    fireEvent.click(screen.getByTestId(`${testId}-isPrivate`));
    fireEvent.click(screen.getByTestId(`${testId}-requireSignedCommit`));
    fireEvent.change(screen.getByTestId(`${testId}-permission`), {
      target: { value: "READ" },
    });
    fireEvent.change(screen.getByTestId(`${testId}-teamRegex`), {
      target: { value: "s26-0[1-3]" },
    });
    fireEvent.click(screen.getByTestId(`${testId}-submit`));

    await waitFor(() => expect(submitAction).toHaveBeenCalledTimes(1));
    expect(submitAction.mock.calls[0][0]).toEqual({
      repoPrefix: "proj2",
      isPrivate: true,
      permission: "READ",
      requireSignedCommit: true,
      teamRegex: "s26-0[1-3]",
    });
  });
});
