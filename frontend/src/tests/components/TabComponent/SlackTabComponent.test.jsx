import axios from "axios";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import AxiosMockAdapter from "axios-mock-adapter";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { expect, vi } from "vitest";
import SlackTabComponent from "main/components/TabComponent/SlackTabComponent";
import slackFixtures from "fixtures/slackFixtures";
import * as useBackendModule from "main/utils/useBackend";

const axiosMock = new AxiosMockAdapter(axios);
const useBackendSpy = vi.spyOn(useBackendModule, "useBackend");

const mockToast = vi.fn();
vi.mock("react-toastify", async (importOriginal) => {
  return {
    ...(await importOriginal()),
    toast: (x) => mockToast(x),
  };
});

function renderTab(
  slackTeamUrl = "https://ucsb-cs156-f26.slack.com/",
  showSectionChannels = undefined,
) {
  const client = new QueryClient();
  return render(
    <QueryClientProvider client={client}>
      <SlackTabComponent
        courseId={7}
        testIdPrefix="Test"
        slackTeamName="ucsb-cs156-f26"
        slackTeamUrl={slackTeamUrl}
        showSectionChannels={showSectionChannels}
      />
    </QueryClientProvider>,
  );
}

describe("SlackTabComponent tests", () => {
  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    useBackendSpy.mockClear();
    mockToast.mockClear();
    vi.spyOn(console, "error").mockImplementation(() => null);
  });

  test("shows link to the workspace and both tables", async () => {
    axiosMock
      .onGet("/api/courses/slack/users?courseId=7")
      .reply(200, slackFixtures.fourUsers);
    axiosMock
      .onGet("/api/courses/slack/missing?courseId=7")
      .reply(200, slackFixtures.threeMissingMembers);

    renderTab();

    expect(screen.getByTestId("Test-slack-tab-component")).toBeInTheDocument();
    const link = screen.getByTestId("Test-slack-workspace-link");
    expect(link).toHaveTextContent("ucsb-cs156-f26");
    expect(link).toHaveAttribute("href", "https://ucsb-cs156-f26.slack.com/");
    expect(link).toHaveAttribute("target", "_blank");
    expect(link).toHaveAttribute("rel", "noopener noreferrer");
    // there is a space between the label and the link
    expect(link.parentElement).toHaveTextContent(
      "Slack workspace: ucsb-cs156-f26",
    );
    expect(link.parentElement).toHaveClass("fs-5");

    const adminLink = screen.getByTestId("Test-slack-admin-link");
    expect(adminLink).toHaveTextContent("Admin");
    expect(adminLink).toHaveAttribute(
      "href",
      "https://ucsb-cs156-f26.slack.com/admin",
    );
    expect(adminLink).toHaveAttribute("target", "_blank");
    expect(adminLink).toHaveAttribute("rel", "noopener noreferrer");
    expect(adminLink).toHaveClass("ms-3");
    expect(adminLink.parentElement).toBe(link.parentElement);

    // before the data arrives
    expect(screen.getByTestId("Test-slack-users-heading")).toHaveTextContent(
      "Active Slack users (0)",
    );
    expect(screen.getByTestId("Test-slack-missing-heading")).toHaveTextContent(
      "Roster students and staff not active in Slack (0)",
    );

    expect(
      await screen.findByTestId("Test-slack-users-table-cell-row-0-col-email"),
    ).toHaveTextContent("phtcon@ucsb.edu");
    expect(
      await screen.findByTestId(
        "Test-slack-missing-table-cell-row-1-col-slackStatus",
      ),
    ).toHaveTextContent("Invited (has not signed in yet)");
    expect(screen.getByTestId("Test-slack-users-heading")).toHaveTextContent(
      "Active Slack users (4)",
    );
    expect(screen.getByTestId("Test-slack-missing-heading")).toHaveTextContent(
      "Roster students and staff not active in Slack (3)",
    );
    expect(screen.getByTestId("Test-slack-missing-heading")).toHaveClass(
      "mt-4",
    );
    expect(
      screen.queryByTestId("Test-slack-users-error"),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByTestId("Test-slack-missing-error"),
    ).not.toBeInTheDocument();

    expect(useBackendSpy).toHaveBeenCalledWith(
      ["/api/courses/slack/users?courseId=7"],
      { method: "GET", url: "/api/courses/slack/users?courseId=7" },
      [],
      true,
      { retry: false, refetchOnWindowFocus: false },
    );
    expect(useBackendSpy).toHaveBeenCalledWith(
      ["/api/courses/slack/missing?courseId=7"],
      { method: "GET", url: "/api/courses/slack/missing?courseId=7" },
      [],
      true,
      { retry: false, refetchOnWindowFocus: false },
    );
  });

  test("shows the message from the backend when Slack reports an error, without retrying", async () => {
    axiosMock.onGet("/api/courses/slack/users?courseId=7").reply(502, {
      ok: false,
      error: "missing_scope",
      message: "This Slack token is missing a required scope.",
    });
    axiosMock
      .onGet("/api/courses/slack/missing?courseId=7")
      .reply(200, slackFixtures.threeMissingMembers);

    renderTab();

    const error = await screen.findByTestId("Test-slack-users-error");
    expect(error).toHaveTextContent(
      "This Slack token is missing a required scope.",
    );
    expect(error).toHaveClass("alert-danger");
    expect(
      screen.queryByTestId("Test-slack-users-table"),
    ).not.toBeInTheDocument();
    expect(
      await screen.findByTestId(
        "Test-slack-missing-table-cell-row-0-col-email",
      ),
    ).toBeInTheDocument();
    expect(
      screen.queryByTestId("Test-slack-missing-error"),
    ).not.toBeInTheDocument();

    const usersRequests = axiosMock.history.get.filter((request) =>
      request.url.includes("/slack/users"),
    );
    expect(usersRequests.length).toBe(1);
    expect(mockToast).not.toHaveBeenCalled();
  });

  test("shows a generic message when there is no message from the backend", async () => {
    axiosMock
      .onGet("/api/courses/slack/users?courseId=7")
      .reply(200, slackFixtures.fourUsers);
    axiosMock.onGet("/api/courses/slack/missing?courseId=7").networkError();

    renderTab();

    const error = await screen.findByTestId("Test-slack-missing-error");
    expect(error).toHaveTextContent(
      "Error getting information from Slack: Error: Network Error",
    );
    expect(
      screen.queryByTestId("Test-slack-missing-table"),
    ).not.toBeInTheDocument();
    await waitFor(() =>
      expect(
        screen.getByTestId("Test-slack-users-table-cell-row-0-col-email"),
      ).toBeInTheDocument(),
    );
  });

  test("shows a generic message when the error response has no body", async () => {
    axiosMock.onGet("/api/courses/slack/users?courseId=7").reply(500);
    axiosMock.onGet("/api/courses/slack/missing?courseId=7").reply(200, []);

    renderTab();

    const error = await screen.findByTestId("Test-slack-users-error");
    expect(error).toHaveTextContent(
      "Error getting information from Slack: Error: Request failed with status code 500",
    );
  });

  test("has no Admin link when the workspace's own URL is not known", async () => {
    axiosMock.onGet("/api/courses/slack/users?courseId=7").reply(200, []);
    axiosMock.onGet("/api/courses/slack/missing?courseId=7").reply(200, []);

    renderTab("https://app.slack.com/client/T12345678");

    expect(screen.getByTestId("Test-slack-workspace-link")).toHaveAttribute(
      "href",
      "https://app.slack.com/client/T12345678",
    );
    expect(
      screen.queryByTestId("Test-slack-admin-link"),
    ).not.toBeInTheDocument();
    expect(screen.queryByText("Admin")).not.toBeInTheDocument();
  });

  test("has no Slack Section Channels card unless asked for", async () => {
    axiosMock.onGet("/api/courses/slack/users?courseId=7").reply(200, []);
    axiosMock.onGet("/api/courses/slack/missing?courseId=7").reply(200, []);

    renderTab();

    expect(
      screen.queryByTestId("Test-slack-section-channels-card"),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByText("Slack Section Channels"),
    ).not.toBeInTheDocument();
  });

  test("has the Slack Section Channels card at the bottom when asked for, which launches the job for this course", async () => {
    axiosMock.onGet("/api/courses/slack/users?courseId=7").reply(200, []);
    axiosMock.onGet("/api/courses/slack/missing?courseId=7").reply(200, []);
    axiosMock
      .onPost("/api/courses/slack/sectionChannels")
      .reply(200, { id: 17 });

    renderTab("https://ucsb-cs156-f26.slack.com/", true);

    const card = screen.getByTestId("Test-slack-section-channels-card");
    expect(card).toBeInTheDocument();

    fireEvent.click(screen.getByTestId("Test-slack-section-channels-submit"));
    await waitFor(() => expect(axiosMock.history.post.length).toBe(1));
    expect(axiosMock.history.post[0].params).toEqual({ courseId: 7 });
  });

  test("always has the Slack Team Channels card, which launches the job for this course", async () => {
    axiosMock.onGet("/api/courses/slack/users?courseId=7").reply(200, []);
    axiosMock.onGet("/api/courses/slack/missing?courseId=7").reply(200, []);
    axiosMock.onPost("/api/courses/slack/teamChannels").reply(200, { id: 18 });

    renderTab();

    const card = screen.getByTestId("Test-slack-team-channels-card");
    expect(
      screen.getByTestId("Test-slack-tab-component").lastElementChild,
    ).toBe(card);
    expect(
      screen.queryByTestId("Test-slack-section-channels-card"),
    ).not.toBeInTheDocument();

    fireEvent.click(screen.getByTestId("Test-slack-team-channels-submit"));
    await waitFor(() => expect(axiosMock.history.post.length).toBe(1));
    expect(axiosMock.history.post[0].url).toBe(
      "/api/courses/slack/teamChannels",
    );
    expect(axiosMock.history.post[0].params).toEqual({ courseId: 7 });
  });

  test("the Slack Team Channels card comes after the Slack Section Channels card", async () => {
    axiosMock.onGet("/api/courses/slack/users?courseId=7").reply(200, []);
    axiosMock.onGet("/api/courses/slack/missing?courseId=7").reply(200, []);

    renderTab("https://ucsb-cs156-f26.slack.com/", true);

    const tab = screen.getByTestId("Test-slack-tab-component");
    const section = screen.getByTestId("Test-slack-section-channels-card");
    const team = screen.getByTestId("Test-slack-team-channels-card");
    expect(section.nextElementSibling).toBe(team);
    expect(tab.lastElementChild).toBe(team);
  });
});
