import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import { beforeEach, describe, expect, test, vi } from "vitest";
import { toast } from "react-toastify";

import SectionsTabComponent from "main/components/TabComponent/SectionsTabComponent";
import { sectionsFixtures } from "fixtures/sectionsFixtures";

vi.mock("react-toastify", async (importOriginal) => {
  const mockToast = vi.fn();
  return {
    ...(await importOriginal()),
    toast: mockToast,
  };
});

const axiosMock = new AxiosMockAdapter(axios);
const queryClient = new QueryClient();
const testId = "InstructorCourseShowPage";

const sectionsUrl = "/api/courses/1/sections";
const sectionsGetHistory = () =>
  axiosMock.history.get.filter((request) => request.url === sectionsUrl);

const renderTab = () =>
  render(
    <QueryClientProvider client={queryClient}>
      <SectionsTabComponent courseId={1} testIdPrefix={testId} />
    </QueryClientProvider>,
  );

describe("SectionsTabComponent tests", () => {
  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    queryClient.clear();
    vi.resetAllMocks();
    // By default, Slack integration is disabled, so the Slack Channel Name
    // field/column should not appear. Individual tests can override this.
    axiosMock
      .onGet(/\/api\/course\/options.*/)
      .reply(200, { SLACK_INTEGRATION: false });
    axiosMock.onGet(/\/api\/courses\/slack\/info.*/).reply(200, {
      courseId: "1",
      slackBotToken: "",
      slackTeamId: "",
      slackTeamName: "",
    });
  });

  test("renders sections from the backend along with create button", async () => {
    axiosMock
      .onGet("/api/courses/1/sections")
      .reply(200, sectionsFixtures.threeSections);

    renderTab();

    expect(
      screen.getByTestId(`${testId}-sections-tab-component`),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId(`${testId}-create-section-button`),
    ).toHaveTextContent("Create Section");
    expect(screen.getByTestId(`${testId}-sections-table`)).toBeInTheDocument();

    expect(
      await screen.findByTestId(
        `${testId}-sections-table-cell-row-0-col-section`,
      ),
    ).toHaveTextContent("0100");
    expect(
      screen.getByTestId(`${testId}-sections-table-cell-row-2-col-label`),
    ).toHaveTextContent("Tue 11:00am");
    expect(
      screen.getByTestId(`${testId}-sections-table-cell-row-0-col-Edit-button`),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId(
        `${testId}-sections-table-cell-row-0-col-Delete-button`,
      ),
    ).toBeInTheDocument();

    expect(sectionsGetHistory().length).toBe(1);
    expect(sectionsGetHistory()[0].url).toBe("/api/courses/1/sections");
    expect(
      screen.queryByText("Create Section", { selector: ".modal-title" }),
    ).not.toBeInTheDocument();
  });

  test("create button opens modal, submitting sends POST and closes modal", async () => {
    axiosMock.onGet("/api/courses/1/sections").reply(200, []);
    axiosMock
      .onPost("/api/courses/1/sections")
      .reply(200, { id: 5, section: "0500", label: "Fri 3:00pm" });

    renderTab();

    fireEvent.click(screen.getByTestId(`${testId}-create-section-button`));

    expect(
      await screen.findByText("Create Section", { selector: ".modal-title" }),
    ).toBeInTheDocument();
    expect(screen.getByTestId(`${testId}-create-section-modal`)).toHaveClass(
      "modal-dialog modal-dialog-centered",
    );
    expect(screen.getByTestId("SectionsForm-section")).toHaveValue("");
    expect(screen.getByTestId("SectionsForm-submit")).toHaveTextContent(
      "Create",
    );

    fireEvent.change(screen.getByTestId("SectionsForm-section"), {
      target: { value: "0500" },
    });
    fireEvent.change(screen.getByTestId("SectionsForm-label"), {
      target: { value: "Fri 3:00pm" },
    });
    fireEvent.click(screen.getByTestId("SectionsForm-submit"));

    await waitFor(() => expect(axiosMock.history.post.length).toBe(1));
    expect(axiosMock.history.post[0].url).toBe("/api/courses/1/sections");
    expect(axiosMock.history.post[0].params).toEqual({
      section: "0500",
      label: "Fri 3:00pm",
    });

    await waitFor(() =>
      expect(toast).toHaveBeenCalledWith("Section successfully created."),
    );
    await waitFor(() =>
      expect(
        screen.queryByText("Create Section", { selector: ".modal-title" }),
      ).not.toBeInTheDocument(),
    );
    // the sections list is refetched after a successful create
    await waitFor(() => expect(sectionsGetHistory().length).toBe(2));
    expect(sectionsGetHistory()[1].url).toBe("/api/courses/1/sections");
  });

  test("editing a section through the table refetches the sections list", async () => {
    axiosMock
      .onGet("/api/courses/1/sections")
      .reply(200, sectionsFixtures.threeSections);
    axiosMock
      .onPut("/api/courses/1/sections/1")
      .reply(200, { id: 1, section: "0100", label: "Tue 9:30am" });

    renderTab();

    fireEvent.click(
      await screen.findByTestId(
        `${testId}-sections-table-cell-row-0-col-Edit-button`,
      ),
    );
    expect(
      await screen.findByTestId(`${testId}-sections-table-edit-modal-body`),
    ).toBeInTheDocument();
    fireEvent.change(screen.getByTestId("SectionsForm-label"), {
      target: { value: "Tue 9:30am" },
    });
    fireEvent.click(screen.getByTestId("SectionsForm-submit"));

    await waitFor(() => expect(axiosMock.history.put.length).toBe(1));
    expect(axiosMock.history.put[0].params).toEqual({
      section: "0100",
      label: "Tue 9:30am",
    });
    await waitFor(() => expect(sectionsGetHistory().length).toBe(2));
    expect(sectionsGetHistory()[1].url).toBe("/api/courses/1/sections");
  });

  test("deleting a section through the table refetches the sections list", async () => {
    axiosMock
      .onGet("/api/courses/1/sections")
      .reply(200, sectionsFixtures.threeSections);
    axiosMock
      .onDelete("/api/courses/1/sections/2")
      .reply(200, { message: "Section with id 2 deleted" });

    renderTab();

    fireEvent.click(
      await screen.findByTestId(
        `${testId}-sections-table-cell-row-1-col-Delete-button`,
      ),
    );
    expect(
      await screen.findByTestId("ConfirmationModal-base"),
    ).toBeInTheDocument();
    fireEvent.click(screen.getByText("Yes, I'd like to do this"));

    await waitFor(() => expect(axiosMock.history.delete.length).toBe(1));
    expect(axiosMock.history.delete[0].url).toBe("/api/courses/1/sections/2");
    await waitFor(() => expect(sectionsGetHistory().length).toBe(2));
    expect(sectionsGetHistory()[1].url).toBe("/api/courses/1/sections");
  });

  test("create modal can be closed without submitting", async () => {
    axiosMock.onGet("/api/courses/1/sections").reply(200, []);

    renderTab();

    fireEvent.click(screen.getByTestId(`${testId}-create-section-button`));
    expect(
      await screen.findByText("Create Section", { selector: ".modal-title" }),
    ).toBeInTheDocument();

    fireEvent.click(screen.getByLabelText("Close"));

    await waitFor(() =>
      expect(
        screen.queryByText("Create Section", { selector: ".modal-title" }),
      ).not.toBeInTheDocument(),
    );
    expect(axiosMock.history.post.length).toBe(0);
  });

  test("create shows duplicate message on 409 and keeps modal open", async () => {
    axiosMock
      .onGet("/api/courses/1/sections")
      .reply(200, sectionsFixtures.oneSection);
    axiosMock.onPost("/api/courses/1/sections").reply(409);

    renderTab();

    fireEvent.click(screen.getByTestId(`${testId}-create-section-button`));
    expect(
      await screen.findByText("Create Section", { selector: ".modal-title" }),
    ).toBeInTheDocument();

    fireEvent.change(screen.getByTestId("SectionsForm-section"), {
      target: { value: "0100" },
    });
    fireEvent.change(screen.getByTestId("SectionsForm-label"), {
      target: { value: "Duplicate" },
    });
    fireEvent.click(screen.getByTestId("SectionsForm-submit"));

    await waitFor(() => expect(axiosMock.history.post.length).toBe(1));
    await waitFor(() =>
      expect(toast).toHaveBeenCalledWith(
        "A section with that section value already exists for this course.",
      ),
    );
    expect(toast).toHaveBeenCalledTimes(1);
    expect(
      screen.getByText("Create Section", { selector: ".modal-title" }),
    ).toBeInTheDocument();
  });

  test("does not toast when the sections request fails", async () => {
    axiosMock.onGet("/api/courses/1/sections").reply(500);

    renderTab();

    await waitFor(() => expect(sectionsGetHistory().length).toBe(1));
    expect(toast).not.toHaveBeenCalled();
    expect(screen.getByTestId(`${testId}-sections-table`)).toBeInTheDocument();
  });

  test("does not show Slack Channel Name field/column when SLACK_INTEGRATION is disabled", async () => {
    axiosMock
      .onGet("/api/courses/1/sections")
      .reply(200, sectionsFixtures.threeSectionsWithSlackChannel);

    renderTab();

    await screen.findByTestId(
      `${testId}-sections-table-cell-row-0-col-section`,
    );
    expect(
      screen.queryByTestId(`${testId}-sections-table-header-slackChannelName`),
    ).not.toBeInTheDocument();

    fireEvent.click(screen.getByTestId(`${testId}-create-section-button`));
    await screen.findByText("Create Section", { selector: ".modal-title" });
    expect(
      screen.queryByTestId("SectionsForm-slackChannelName"),
    ).not.toBeInTheDocument();
  });

  test("does not show Slack Channel Name field/column when SLACK_INTEGRATION is disabled, even if stale cached Slack info has an active token", async () => {
    axiosMock
      .onGet("/api/courses/1/sections")
      .reply(200, sectionsFixtures.threeSectionsWithSlackChannel);
    // Simulate a stale cache entry (e.g. left over from when SLACK_INTEGRATION
    // was previously enabled and connected) to make sure the SLACK_INTEGRATION
    // check is not bypassed just because slackInfo happens to have a team id.
    queryClient.setQueryData([`/api/courses/slack/info?courseId=1`], {
      slackTeamId: "T12345",
      slackTeamName: "CS156 Workspace",
    });

    renderTab();

    await screen.findByTestId(
      `${testId}-sections-table-cell-row-0-col-section`,
    );
    expect(
      screen.queryByTestId(`${testId}-sections-table-header-slackChannelName`),
    ).not.toBeInTheDocument();
  });

  test("does not show Slack Channel Name field/column when SLACK_INTEGRATION is enabled but there is no active token", async () => {
    axiosMock.reset();
    axiosMock
      .onGet(/\/api\/course\/options.*/)
      .reply(200, { SLACK_INTEGRATION: true });
    axiosMock.onGet(/\/api\/courses\/slack\/info.*/).reply(200, {
      courseId: "1",
      slackBotToken: "",
      slackTeamId: "",
      slackTeamName: "",
    });
    axiosMock
      .onGet("/api/courses/1/sections")
      .reply(200, sectionsFixtures.threeSectionsWithSlackChannel);

    renderTab();

    await screen.findByTestId(
      `${testId}-sections-table-cell-row-0-col-section`,
    );
    expect(
      screen.queryByTestId(`${testId}-sections-table-header-slackChannelName`),
    ).not.toBeInTheDocument();
  });

  test("shows Slack Channel Name field/column when SLACK_INTEGRATION is enabled and there is an active token", async () => {
    axiosMock.reset();
    axiosMock
      .onGet(/\/api\/course\/options.*/)
      .reply(200, { SLACK_INTEGRATION: true });
    axiosMock.onGet(/\/api\/courses\/slack\/info.*/).reply(200, {
      courseId: "1",
      slackBotToken: "xoxb****xxxx",
      slackTeamId: "T12345",
      slackTeamName: "CS156 Workspace",
    });
    axiosMock
      .onGet("/api/courses/1/sections")
      .reply(200, sectionsFixtures.threeSectionsWithSlackChannel);
    axiosMock
      .onPost("/api/courses/1/sections")
      .reply(200, { id: 5, section: "0500", label: "Fri 3:00pm" });

    renderTab();

    expect(
      await screen.findByTestId(
        `${testId}-sections-table-header-slackChannelName`,
      ),
    ).toHaveTextContent("Slack Channel Name");
    expect(
      screen.getByTestId(
        `${testId}-sections-table-cell-row-0-col-slackChannelName`,
      ),
    ).toHaveTextContent("#cs156-0100");

    fireEvent.click(screen.getByTestId(`${testId}-create-section-button`));
    await screen.findByText("Create Section", { selector: ".modal-title" });
    expect(
      screen.getByTestId("SectionsForm-slackChannelName"),
    ).toBeInTheDocument();

    fireEvent.change(screen.getByTestId("SectionsForm-section"), {
      target: { value: "0500" },
    });
    fireEvent.change(screen.getByTestId("SectionsForm-label"), {
      target: { value: "Fri 3:00pm" },
    });
    fireEvent.change(screen.getByTestId("SectionsForm-slackChannelName"), {
      target: { value: "#cs156-0500" },
    });
    fireEvent.click(screen.getByTestId("SectionsForm-submit"));

    await waitFor(() => expect(axiosMock.history.post.length).toBe(1));
    expect(axiosMock.history.post[0].params).toEqual({
      section: "0500",
      label: "Fri 3:00pm",
      slackChannelName: "#cs156-0500",
    });
  });
});
