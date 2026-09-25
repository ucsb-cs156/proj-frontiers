import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import { beforeEach, describe, expect, test, vi } from "vitest";
import { toast } from "react-toastify";

import NewAssignmentsTabComponent from "main/components/TabComponent/NewAssignmentsTabComponent";
import { newAssignmentsFixtures } from "fixtures/newAssignmentsFixtures";

vi.mock("react-toastify", async (importOriginal) => {
  const mockToast = vi.fn();
  return {
    ...(await importOriginal()),
    toast: mockToast,
  };
});

const axiosMock = new AxiosMockAdapter(axios);
let queryClient;
const testId = "InstructorCourseShowPage";
const tableId = `${testId}-new-assignments-table`;
const individualForm = "NewIndividualAssignmentForm";
const teamForm = "NewTeamAssignmentForm";

const assignmentsUrl = "/api/assignments";
const assignmentsGetHistory = () =>
  axiosMock.history.get.filter((request) => request.url === assignmentsUrl);

const renderTab = () =>
  render(
    <QueryClientProvider client={queryClient}>
      <NewAssignmentsTabComponent courseId={7} testIdPrefix={testId} />
    </QueryClientProvider>,
  );

const createModalTitle = (text) =>
  screen.findByText(text, { selector: ".modal-title" });

describe("NewAssignmentsTabComponent tests", () => {
  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    queryClient = new QueryClient();
    vi.resetAllMocks();
    axiosMock
      .onGet(assignmentsUrl)
      .reply(200, newAssignmentsFixtures.threeAssignments);
  });

  test("renders the two create buttons and the assignments from the backend", async () => {
    renderTab();

    expect(
      screen.getByTestId(`${testId}-new-assignments-tab-component`),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId(`${testId}-create-individual-assignment-button`),
    ).toHaveTextContent("Create Individual Assignment");
    expect(
      screen.getByTestId(`${testId}-create-team-assignment-button`),
    ).toHaveTextContent("Create Team Assignment");
    expect(screen.getByTestId(tableId)).toBeInTheDocument();

    expect(
      await screen.findByTestId(`${tableId}-cell-row-0-col-repoPrefix`),
    ).toHaveTextContent("lab01");
    expect(
      screen.getByTestId(`${tableId}-cell-row-2-col-teamRegex`),
    ).toHaveTextContent("s26-.*");
    expect(
      screen.getByTestId(`${tableId}-cell-row-0-col-Edit-button`),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId(`${tableId}-cell-row-0-col-Delete-button`),
    ).toBeInTheDocument();

    expect(assignmentsGetHistory().length).toBe(1);
    expect(assignmentsGetHistory()[0].params).toEqual({ courseId: 7 });
    expect(
      screen.queryByTestId(`${testId}-create-assignment-modal`),
    ).not.toBeInTheDocument();
  });

  test("Create Individual Assignment opens the individual form; submitting sends POST, toasts the job, closes the modal and refreshes", async () => {
    axiosMock
      .onPost("/api/assignments/post")
      .reply(200, newAssignmentsFixtures.savedWithJob);
    queryClient.setQueryData(["/api/jobs/course"], []);

    renderTab();
    await screen.findByTestId(`${tableId}-cell-row-0-col-repoPrefix`);

    fireEvent.click(
      screen.getByTestId(`${testId}-create-individual-assignment-button`),
    );

    expect(
      await createModalTitle("Create Individual Assignment"),
    ).toBeInTheDocument();
    expect(screen.getByTestId(`${testId}-create-assignment-modal`)).toHaveClass(
      "modal-dialog modal-dialog-centered",
    );
    expect(
      screen.getByTestId(`${testId}-create-assignment-modal-note`),
    ).toHaveTextContent("Saving starts a job that creates the repositories.");
    expect(screen.getByTestId(`${individualForm}-repoPrefix`)).toHaveValue("");
    expect(screen.getByTestId(`${individualForm}-submit`)).toHaveTextContent(
      "Create",
    );
    expect(screen.queryByTestId(teamForm)).not.toBeInTheDocument();

    fireEvent.change(screen.getByTestId(`${individualForm}-repoPrefix`), {
      target: { value: "lab04" },
    });
    fireEvent.change(screen.getByTestId(`${individualForm}-permission`), {
      target: { value: "WRITE" },
    });
    fireEvent.click(screen.getByTestId(`${individualForm}-isPrivate`));
    fireEvent.change(screen.getByTestId(`${individualForm}-createReposFor`), {
      target: { value: "STUDENTS_AND_STAFF" },
    });
    fireEvent.click(screen.getByTestId(`${individualForm}-submit`));

    await waitFor(() => expect(axiosMock.history.post.length).toBe(1));
    expect(axiosMock.history.post[0].url).toBe("/api/assignments/post");
    expect(axiosMock.history.post[0].params).toEqual({
      courseId: 7,
      asnType: "INDIVIDUAL",
      repoPrefix: "lab04",
      visibility: "PRIVATE",
      permission: "WRITE",
      createReposFor: "STUDENTS_AND_STAFF",
    });

    await waitFor(() =>
      expect(toast).toHaveBeenCalledWith(
        "Assignment created. Job 99 started to create its repositories; see the Jobs tab for its log.",
      ),
    );
    expect(toast).toHaveBeenCalledTimes(1);
    await waitFor(() =>
      expect(
        screen.queryByText("Create Individual Assignment", {
          selector: ".modal-title",
        }),
      ).not.toBeInTheDocument(),
    );
    // the list of assignments is refetched, and so is the list of jobs
    await waitFor(() => expect(assignmentsGetHistory().length).toBe(2));
    expect(queryClient.getQueryState(["/api/jobs/course"]).isInvalidated).toBe(
      true,
    );
  });

  test("Create Team Assignment opens the team form with the default team regex; submitting sends POST with the team regex", async () => {
    axiosMock
      .onPost("/api/assignments/post")
      .reply(200, newAssignmentsFixtures.savedWithJob);

    renderTab();

    fireEvent.click(
      screen.getByTestId(`${testId}-create-team-assignment-button`),
    );

    expect(
      await createModalTitle("Create Team Assignment"),
    ).toBeInTheDocument();
    expect(screen.getByTestId(`${teamForm}-teamRegex`)).toHaveValue(".*");
    expect(screen.queryByTestId(individualForm)).not.toBeInTheDocument();

    fireEvent.change(screen.getByTestId(`${teamForm}-repoPrefix`), {
      target: { value: "proj" },
    });
    fireEvent.click(screen.getByTestId(`${teamForm}-submit`));

    await waitFor(() => expect(axiosMock.history.post.length).toBe(1));
    expect(axiosMock.history.post[0].params).toEqual({
      courseId: 7,
      asnType: "TEAM",
      repoPrefix: "proj",
      visibility: "PUBLIC",
      permission: "MAINTAIN",
      teamRegex: ".*",
    });
    await waitFor(() => expect(toast).toHaveBeenCalledTimes(1));
  });

  test("the create modal can be closed, and reopened for the other kind of assignment", async () => {
    renderTab();

    fireEvent.click(
      screen.getByTestId(`${testId}-create-individual-assignment-button`),
    );
    expect(
      await createModalTitle("Create Individual Assignment"),
    ).toBeInTheDocument();

    fireEvent.click(screen.getByLabelText("Close"));
    await waitFor(() =>
      expect(
        screen.queryByText("Create Individual Assignment", {
          selector: ".modal-title",
        }),
      ).not.toBeInTheDocument(),
    );
    expect(axiosMock.history.post.length).toBe(0);
    expect(toast).not.toHaveBeenCalled();

    fireEvent.click(
      screen.getByTestId(`${testId}-create-team-assignment-button`),
    );
    expect(
      await createModalTitle("Create Team Assignment"),
    ).toBeInTheDocument();
  });

  test("a failed create shows the backend's message and keeps the modal open", async () => {
    axiosMock.onPost("/api/assignments/post").reply(400, {
      type: "NoLinkedOrganizationException",
      message:
        "No linked GitHub Organization to CS156. Please link a GitHub Organization first.",
    });

    renderTab();

    fireEvent.click(
      screen.getByTestId(`${testId}-create-individual-assignment-button`),
    );
    await createModalTitle("Create Individual Assignment");
    fireEvent.change(screen.getByTestId(`${individualForm}-repoPrefix`), {
      target: { value: "lab04" },
    });
    fireEvent.click(screen.getByTestId(`${individualForm}-submit`));

    await waitFor(() =>
      expect(toast).toHaveBeenCalledWith(
        "No linked GitHub Organization to CS156. Please link a GitHub Organization first.",
      ),
    );
    expect(toast).toHaveBeenCalledTimes(1);
    expect(
      screen.getByText("Create Individual Assignment", {
        selector: ".modal-title",
      }),
    ).toBeInTheDocument();
  });

  test("editing an assignment through the table refetches the list", async () => {
    axiosMock
      .onPut("/api/assignments/put")
      .reply(200, newAssignmentsFixtures.savedWithJob);
    queryClient.setQueryData(["/api/jobs/course"], []);

    renderTab();

    fireEvent.click(
      await screen.findByTestId(`${tableId}-cell-row-0-col-Edit-button`),
    );
    await screen.findByTestId(`${tableId}-edit-modal-body`);
    fireEvent.change(screen.getByTestId(`${individualForm}-repoPrefix`), {
      target: { value: "lab01-v2" },
    });
    fireEvent.click(screen.getByTestId(`${individualForm}-submit`));

    await waitFor(() => expect(axiosMock.history.put.length).toBe(1));
    await waitFor(() => expect(assignmentsGetHistory().length).toBe(2));
    expect(queryClient.getQueryState(["/api/jobs/course"]).isInvalidated).toBe(
      true,
    );
  });

  test("deleting an assignment through the table refetches the list, but not the jobs", async () => {
    axiosMock
      .onDelete("/api/assignments/2")
      .reply(200, { message: "Assignment with id 2 deleted" });
    queryClient.setQueryData(["/api/jobs/course"], []);

    renderTab();

    fireEvent.click(
      await screen.findByTestId(`${tableId}-cell-row-1-col-Delete-button`),
    );
    await screen.findByTestId("ConfirmationModal-base");
    fireEvent.click(screen.getByText("Yes, I'd like to do this"));

    await waitFor(() => expect(axiosMock.history.delete.length).toBe(1));
    await waitFor(() => expect(assignmentsGetHistory().length).toBe(2));
    expect(queryClient.getQueryState(["/api/jobs/course"]).isInvalidated).toBe(
      false,
    );
  });

  test("does not toast when the assignments request fails", async () => {
    axiosMock.reset();
    axiosMock.onGet(assignmentsUrl).reply(500);

    renderTab();

    await waitFor(() => expect(assignmentsGetHistory().length).toBe(1));
    expect(toast).not.toHaveBeenCalled();
    expect(screen.getByTestId(tableId)).toBeInTheDocument();
    expect(
      screen.queryByTestId(`${tableId}-cell-row-0-col-repoPrefix`),
    ).not.toBeInTheDocument();
  });
});
