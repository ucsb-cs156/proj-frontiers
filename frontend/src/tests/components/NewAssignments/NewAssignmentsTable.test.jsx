import {
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import { beforeEach, describe, expect, test, vi } from "vitest";
import { toast } from "react-toastify";

import NewAssignmentsTable from "main/components/NewAssignments/NewAssignmentsTable";
import { newAssignmentsFixtures } from "fixtures/newAssignmentsFixtures";

vi.mock("react-toastify", async (importOriginal) => {
  const mockToast = vi.fn();
  return {
    ...(await importOriginal()),
    toast: mockToast,
  };
});

const axiosMock = new AxiosMockAdapter(axios);
const queryClient = new QueryClient();
const testId = "NewAssignmentsTable";
const individualForm = "NewIndividualAssignmentForm";
const teamForm = "NewTeamAssignmentForm";

const renderTable = (props = {}) =>
  render(
    <QueryClientProvider client={queryClient}>
      <NewAssignmentsTable
        assignments={newAssignmentsFixtures.threeAssignments}
        courseId={1}
        {...props}
      />
    </QueryClientProvider>,
  );

const cell = (row, col) =>
  screen.getByTestId(`${testId}-cell-row-${row}-col-${col}`);

describe("NewAssignmentsTable tests", () => {
  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    queryClient.clear();
    vi.resetAllMocks();
  });

  test("renders headers, rows with readable labels, and buttons", () => {
    renderTable();

    expect(screen.getByTestId(testId)).toBeInTheDocument();
    const headers = {
      repoPrefix: "Repository Prefix",
      asnType: "Type",
      visibility: "Visibility",
      permission: "Permission",
      requireSignedCommit: "Signed Commits",
      createReposFor: "Repositories For",
      teamRegex: "Team Regex",
      lastJobId: "Job Log",
      Refresh: "Refresh",
      Edit: "Edit",
      Delete: "Delete",
    };
    Object.entries(headers).forEach(([column, text]) => {
      expect(
        screen.getByTestId(`${testId}-header-${column}`),
      ).toHaveTextContent(text);
    });

    expect(cell(0, "repoPrefix")).toHaveTextContent("lab01");
    expect(cell(0, "asnType")).toHaveTextContent("Individual");
    expect(cell(0, "visibility")).toHaveTextContent("Public");
    expect(cell(0, "permission")).toHaveTextContent("Maintain");
    expect(cell(0, "createReposFor")).toHaveTextContent("Students Only");
    expect(cell(0, "teamRegex")).toHaveTextContent("");

    expect(cell(1, "visibility")).toHaveTextContent("Private");
    expect(cell(1, "permission")).toHaveTextContent("Read");
    expect(cell(1, "createReposFor")).toHaveTextContent("Students and Staff");

    expect(cell(2, "repoPrefix")).toHaveTextContent("proj-team");
    expect(cell(2, "asnType")).toHaveTextContent("Team");
    expect(cell(2, "permission")).toHaveTextContent("Write");
    expect(cell(2, "createReposFor")).toHaveTextContent("");
    expect(cell(2, "teamRegex")).toHaveTextContent("s26-.*");

    // a checkmark for the assignments that require signed commits, nothing for the others
    expect(cell(0, "requireSignedCommit")).toHaveTextContent("");
    expect(cell(1, "requireSignedCommit")).toHaveTextContent("✅");
    expect(cell(2, "requireSignedCommit")).toHaveTextContent("✅");
    const checkmark = within(cell(1, "requireSignedCommit")).getByRole("img");
    expect(checkmark).toHaveAccessibleName("Signed commits are required");
    expect(checkmark).toHaveAttribute("title", "Signed commits are required");
    expect(
      within(cell(0, "requireSignedCommit")).queryByRole("img"),
    ).not.toBeInTheDocument();
    expect(cell(0, "requireSignedCommit")).not.toHaveTextContent("false");
    expect(cell(1, "requireSignedCommit")).not.toHaveTextContent("true");

    expect(cell(0, "lastJobId")).toHaveTextContent("12");
    expect(cell(1, "lastJobId")).toHaveTextContent("");
    expect(cell(2, "lastJobId")).toHaveTextContent("15");

    const refresh = screen.getByTestId(
      `${testId}-cell-row-0-col-Refresh-button`,
    );
    expect(refresh).toHaveTextContent("Refresh");
    expect(refresh).toHaveClass("btn-success");

    const edit = screen.getByTestId(`${testId}-cell-row-0-col-Edit-button`);
    expect(edit).toHaveTextContent("Edit");
    expect(edit).toHaveClass("btn-primary");
    const del = screen.getByTestId(`${testId}-cell-row-0-col-Delete-button`);
    expect(del).toHaveTextContent("Delete");
    expect(del).toHaveClass("btn-danger");

    expect(
      screen.queryByTestId(`${testId}-edit-modal-body`),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByTestId("ConfirmationModal-base"),
    ).not.toBeInTheDocument();
  });

  test("renders an empty table, and uses a custom testIdPrefix", () => {
    renderTable({ assignments: [], testIdPrefix: "Custom" });
    expect(screen.getByTestId("Custom")).toBeInTheDocument();
    expect(screen.getByTestId("Custom-header-repoPrefix")).toBeInTheDocument();
    expect(
      screen.queryByTestId("Custom-cell-row-0-col-repoPrefix"),
    ).not.toBeInTheDocument();
  });

  test("the last job is a link to the log of the job, for the assignments that have one", () => {
    renderTable();

    const first = screen.getByTestId(`${testId}-cell-row-0-col-lastJobId-link`);
    expect(first.tagName).toBe("A");
    expect(first).toHaveTextContent("12");
    // a real address, for opening the whole log in its own page
    expect(first).toHaveAttribute("href", "/instructor/courses/1/jobs/12/logs");
    expect(
      screen.getByTestId(`${testId}-cell-row-2-col-lastJobId-link`),
    ).toHaveAttribute("href", "/instructor/courses/1/jobs/15/logs");

    // an assignment that has not had a job has nothing to link to
    expect(
      screen.queryByTestId(`${testId}-cell-row-1-col-lastJobId-link`),
    ).not.toBeInTheDocument();
  });

  test("clicking the last job shows its log in a modal, following the job, and the modal can be closed", async () => {
    axiosMock.onGet("/api/jobs/course/logs/tail").reply(200, {
      status: "complete",
      lines: [
        { id: 1, jobId: 12, message: "Creating lab01-cgaucho" },
        { id: 2, jobId: 12, message: "Done" },
      ],
    });

    renderTable();
    expect(screen.queryByText("Job 12 Log")).not.toBeInTheDocument();

    // the click is handled here, not by the browser going to the log's page
    const notPrevented = fireEvent.click(
      screen.getByTestId(`${testId}-cell-row-0-col-lastJobId-link`),
    );
    expect(notPrevented).toBe(false);

    expect(await screen.findByText("Job 12 Log")).toBeInTheDocument();
    expect(screen.getByTestId(`${testId}-job-log-modal`)).toBeInTheDocument();
    expect(
      screen.getByTestId(`${testId}-job-log-modal-tail`),
    ).toBeInTheDocument();
    expect(
      await screen.findByText(/Creating lab01-cgaucho/, { selector: "pre" }),
    ).toHaveTextContent("Creating lab01-cgaucho Done");
    expect(axiosMock.history.get.length).toBe(1);
    expect(axiosMock.history.get[0].url).toBe("/api/jobs/course/logs/tail");
    expect(axiosMock.history.get[0].params).toEqual({
      courseId: 1,
      jobId: 12,
      afterId: 0,
    });

    fireEvent.click(screen.getByLabelText("Close"));
    await waitFor(() =>
      expect(screen.queryByText("Job 12 Log")).not.toBeInTheDocument(),
    );
  });

  test("clicking the last job of another assignment shows the log of that job", async () => {
    axiosMock.onGet("/api/jobs/course/logs/tail").reply(200, {
      status: "complete",
      lines: [{ id: 1, jobId: 15, message: "team job" }],
    });

    renderTable();

    fireEvent.click(
      screen.getByTestId(`${testId}-cell-row-2-col-lastJobId-link`),
    );

    expect(await screen.findByText("Job 15 Log")).toBeInTheDocument();
    expect(axiosMock.history.get[0].params.jobId).toBe(15);
  });

  test("Refresh starts the job of the assignment, and says which job", async () => {
    axiosMock.onPost("/api/assignments/launch").reply(200, {
      assignment: {
        ...newAssignmentsFixtures.threeAssignments[1],
        lastJobId: 99,
      },
      job: { id: 99, status: "processing" },
    });

    renderTable();

    fireEvent.click(
      screen.getByTestId(`${testId}-cell-row-1-col-Refresh-button`),
    );

    await waitFor(() => expect(axiosMock.history.post.length).toBe(1));
    expect(axiosMock.history.post[0].url).toBe("/api/assignments/launch");
    expect(axiosMock.history.post[0].params).toEqual({
      courseId: 1,
      assignmentId: 2,
    });
    await waitFor(() =>
      expect(toast).toHaveBeenCalledWith(
        "Job 99 started to create the repositories of this assignment; click the job number in the table to watch its log.",
      ),
    );
    expect(toast).toHaveBeenCalledTimes(1);
  });

  test("Refresh on another row starts the job of that assignment", async () => {
    axiosMock.onPost("/api/assignments/launch").reply(200, {
      assignment: newAssignmentsFixtures.threeAssignments[2],
      job: { id: 100, status: "processing" },
    });

    renderTable();

    fireEvent.click(
      screen.getByTestId(`${testId}-cell-row-2-col-Refresh-button`),
    );

    await waitFor(() => expect(axiosMock.history.post.length).toBe(1));
    expect(axiosMock.history.post[0].params).toEqual({
      courseId: 1,
      assignmentId: 3,
    });
  });

  test("a failed Refresh shows the backend's message", async () => {
    axiosMock.onPost("/api/assignments/launch").reply(400, {
      type: "NoLinkedOrganizationException",
      message:
        "No linked GitHub Organization to CS156. Please link a GitHub Organization first.",
    });

    renderTable();

    fireEvent.click(
      screen.getByTestId(`${testId}-cell-row-0-col-Refresh-button`),
    );

    await waitFor(() =>
      expect(toast).toHaveBeenCalledWith(
        "No linked GitHub Organization to CS156. Please link a GitHub Organization first.",
      ),
    );
    expect(toast).toHaveBeenCalledTimes(1);
  });

  test("editing an individual assignment opens the individual form, prefilled, without a way to change the type", async () => {
    renderTable();

    fireEvent.click(screen.getByTestId(`${testId}-cell-row-1-col-Edit-button`));

    expect(
      await screen.findByTestId(`${testId}-edit-modal-body`),
    ).toBeInTheDocument();
    expect(screen.getByTestId(`${testId}-edit-modal`)).toHaveClass(
      "modal-dialog modal-dialog-centered",
    );
    expect(screen.getByText("Edit Individual Assignment")).toBeInTheDocument();
    expect(screen.getByTestId(`${testId}-edit-modal-note`)).toHaveTextContent(
      "The type of an assignment cannot be changed; to change it, delete the assignment and create a new one. Saving starts a job that creates the repositories.",
    );
    expect(screen.getByTestId(`${individualForm}-repoPrefix`)).toHaveValue(
      "lab02",
    );
    expect(screen.getByTestId(`${individualForm}-isPrivate`)).toBeChecked();
    expect(screen.getByTestId(`${individualForm}-permission`)).toHaveValue(
      "READ",
    );
    expect(screen.getByTestId(`${individualForm}-createReposFor`)).toHaveValue(
      "STUDENTS_AND_STAFF",
    );
    expect(screen.getByTestId(`${individualForm}-submit`)).toHaveTextContent(
      "Update",
    );
    expect(screen.queryByTestId(`${teamForm}`)).not.toBeInTheDocument();
    expect(screen.queryByLabelText(/type/i)).not.toBeInTheDocument();
  });

  test("submitting the individual edit sends PUT without the type, toasts the job, and closes the modal", async () => {
    axiosMock
      .onPut("/api/assignments/put")
      .reply(200, newAssignmentsFixtures.savedWithJob);

    renderTable();

    fireEvent.click(screen.getByTestId(`${testId}-cell-row-1-col-Edit-button`));
    await screen.findByTestId(`${testId}-edit-modal-body`);
    fireEvent.change(screen.getByTestId(`${individualForm}-repoPrefix`), {
      target: { value: "lab02-v2" },
    });
    fireEvent.click(screen.getByTestId(`${individualForm}-isPrivate`));
    fireEvent.change(screen.getByTestId(`${individualForm}-createReposFor`), {
      target: { value: "STAFF_ONLY" },
    });
    fireEvent.click(screen.getByTestId(`${individualForm}-submit`));

    await waitFor(() => expect(axiosMock.history.put.length).toBe(1));
    expect(axiosMock.history.put[0].url).toBe("/api/assignments/put");
    expect(axiosMock.history.put[0].params).toEqual({
      courseId: 1,
      assignmentId: 2,
      repoPrefix: "lab02-v2",
      visibility: "PUBLIC",
      permission: "READ",
      // lab02 requires signed commits, and the edit did not change that
      requireSignedCommit: true,
      createReposFor: "STAFF_ONLY",
    });

    await waitFor(() =>
      expect(toast).toHaveBeenCalledWith(
        "Assignment updated. Job 99 started to create its repositories; click the job number in the table to watch its log.",
      ),
    );
    expect(toast).toHaveBeenCalledTimes(1);
    await waitFor(() =>
      expect(
        screen.queryByTestId(`${testId}-edit-modal-body`),
      ).not.toBeInTheDocument(),
    );
  });

  test("editing a team assignment opens the team form, and submitting sends the team regex", async () => {
    axiosMock
      .onPut("/api/assignments/put")
      .reply(200, newAssignmentsFixtures.savedWithJob);

    renderTable();

    fireEvent.click(screen.getByTestId(`${testId}-cell-row-2-col-Edit-button`));
    await screen.findByTestId(`${testId}-edit-modal-body`);
    expect(screen.getByText("Edit Team Assignment")).toBeInTheDocument();
    expect(screen.getByTestId(`${teamForm}-repoPrefix`)).toHaveValue(
      "proj-team",
    );
    expect(screen.getByTestId(`${teamForm}-isPrivate`)).toBeChecked();
    expect(screen.getByTestId(`${teamForm}-permission`)).toHaveValue("WRITE");
    expect(screen.getByTestId(`${teamForm}-teamRegex`)).toHaveValue("s26-.*");
    expect(screen.queryByTestId(`${individualForm}`)).not.toBeInTheDocument();

    fireEvent.change(screen.getByTestId(`${teamForm}-teamRegex`), {
      target: { value: "s26-0[1-3]" },
    });
    fireEvent.click(screen.getByTestId(`${teamForm}-submit`));

    await waitFor(() => expect(axiosMock.history.put.length).toBe(1));
    expect(axiosMock.history.put[0].params).toEqual({
      courseId: 1,
      assignmentId: 3,
      repoPrefix: "proj-team",
      visibility: "PRIVATE",
      permission: "WRITE",
      requireSignedCommit: true,
      teamRegex: "s26-0[1-3]",
    });
  });

  test("the edit modal shows whether signed commits are required, and the switch can clear it", async () => {
    axiosMock
      .onPut("/api/assignments/put")
      .reply(200, newAssignmentsFixtures.savedWithJob);

    renderTable();

    // lab01 does not require them: the switch is off, and turning it on is sent
    fireEvent.click(screen.getByTestId(`${testId}-cell-row-0-col-Edit-button`));
    await screen.findByTestId(`${testId}-edit-modal-body`);
    expect(
      screen.getByTestId(`${individualForm}-requireSignedCommit`),
    ).not.toBeChecked();
    fireEvent.click(
      screen.getByTestId(`${individualForm}-requireSignedCommit`),
    );
    fireEvent.click(screen.getByTestId(`${individualForm}-submit`));
    await waitFor(() => expect(axiosMock.history.put.length).toBe(1));
    expect(axiosMock.history.put[0].params.requireSignedCommit).toBe(true);
    await waitFor(() =>
      expect(
        screen.queryByTestId(`${testId}-edit-modal-body`),
      ).not.toBeInTheDocument(),
    );

    // the team assignment requires them: the switch is on, and turning it off is sent
    fireEvent.click(screen.getByTestId(`${testId}-cell-row-2-col-Edit-button`));
    await screen.findByTestId(`${testId}-edit-modal-body`);
    expect(screen.getByTestId(`${teamForm}-requireSignedCommit`)).toBeChecked();
    fireEvent.click(screen.getByTestId(`${teamForm}-requireSignedCommit`));
    fireEvent.click(screen.getByTestId(`${teamForm}-submit`));
    await waitFor(() => expect(axiosMock.history.put.length).toBe(2));
    expect(axiosMock.history.put[1].params.requireSignedCommit).toBe(false);
  });

  test("the edit modal can be closed without submitting", async () => {
    renderTable();

    fireEvent.click(screen.getByTestId(`${testId}-cell-row-0-col-Edit-button`));
    expect(
      await screen.findByTestId(`${testId}-edit-modal-body`),
    ).toBeInTheDocument();

    fireEvent.click(screen.getByLabelText("Close"));

    await waitFor(() =>
      expect(
        screen.queryByTestId(`${testId}-edit-modal-body`),
      ).not.toBeInTheDocument(),
    );
    expect(axiosMock.history.put.length).toBe(0);
    expect(toast).not.toHaveBeenCalled();
  });

  test("a failed edit shows the backend's message and keeps the modal open", async () => {
    axiosMock
      .onPut("/api/assignments/put")
      .reply(400, { message: "teamRegex is not a valid regular expression" });

    renderTable();

    fireEvent.click(screen.getByTestId(`${testId}-cell-row-2-col-Edit-button`));
    await screen.findByTestId(`${testId}-edit-modal-body`);
    fireEvent.change(screen.getByTestId(`${teamForm}-teamRegex`), {
      target: { value: "(" },
    });
    fireEvent.click(screen.getByTestId(`${teamForm}-submit`));

    await waitFor(() =>
      expect(toast).toHaveBeenCalledWith(
        "teamRegex is not a valid regular expression",
      ),
    );
    expect(toast).toHaveBeenCalledTimes(1);
    expect(screen.getByTestId(`${testId}-edit-modal-body`)).toBeInTheDocument();
  });

  test("delete asks for confirmation, then sends DELETE and toasts", async () => {
    axiosMock
      .onDelete("/api/assignments/3")
      .reply(200, { message: "Assignment with id 3 deleted" });

    renderTable();

    fireEvent.click(
      screen.getByTestId(`${testId}-cell-row-2-col-Delete-button`),
    );

    expect(
      await screen.findByTestId("ConfirmationModal-base"),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId(`${testId}-delete-confirmation-message`),
    ).toHaveTextContent(
      "Are you sure you want to delete the assignment proj-team? Repositories that have already been created are not deleted.",
    );

    fireEvent.click(screen.getByText("Yes, I'd like to do this"));

    await waitFor(() => expect(axiosMock.history.delete.length).toBe(1));
    expect(axiosMock.history.delete[0].url).toBe("/api/assignments/3");
    expect(axiosMock.history.delete[0].params).toEqual({ courseId: 1 });
    await waitFor(() =>
      expect(toast).toHaveBeenCalledWith("Assignment deleted successfully."),
    );
    expect(toast).toHaveBeenCalledTimes(1);
    await waitFor(() =>
      expect(
        screen.queryByTestId("ConfirmationModal-base"),
      ).not.toBeInTheDocument(),
    );
  });

  test("delete confirmation can be declined", async () => {
    renderTable();

    fireEvent.click(
      screen.getByTestId(`${testId}-cell-row-0-col-Delete-button`),
    );
    expect(
      await screen.findByTestId("ConfirmationModal-base"),
    ).toBeInTheDocument();

    fireEvent.click(screen.getByText("No, take me back"));

    await waitFor(() =>
      expect(
        screen.queryByTestId("ConfirmationModal-base"),
      ).not.toBeInTheDocument(),
    );
    expect(axiosMock.history.delete.length).toBe(0);
    expect(toast).not.toHaveBeenCalled();
  });

  test("a failed delete shows the backend's message", async () => {
    axiosMock
      .onDelete("/api/assignments/1")
      .reply(404, { message: "Assignment with id 1 not found" });

    renderTable();

    fireEvent.click(
      screen.getByTestId(`${testId}-cell-row-0-col-Delete-button`),
    );
    await screen.findByTestId("ConfirmationModal-base");
    fireEvent.click(screen.getByText("Yes, I'd like to do this"));

    await waitFor(() =>
      expect(toast).toHaveBeenCalledWith("Assignment with id 1 not found"),
    );
    expect(toast).toHaveBeenCalledTimes(1);
  });
});
