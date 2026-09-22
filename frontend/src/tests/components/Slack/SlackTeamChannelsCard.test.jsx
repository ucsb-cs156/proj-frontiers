import axios from "axios";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import AxiosMockAdapter from "axios-mock-adapter";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { expect, vi } from "vitest";
import SlackTeamChannelsCard from "main/components/Slack/SlackTeamChannelsCard";
import * as useBackendModule from "main/utils/useBackend";

const axiosMock = new AxiosMockAdapter(axios);
const useBackendMutationSpy = vi.spyOn(useBackendModule, "useBackendMutation");

const mockToast = vi.fn();
vi.mock("react-toastify", async (importOriginal) => {
  return {
    ...(await importOriginal()),
    toast: (x) => mockToast(x),
  };
});

function renderCard(props = {}) {
  const client = new QueryClient();
  return render(
    <QueryClientProvider client={client}>
      <SlackTeamChannelsCard courseId={7} {...props} />
    </QueryClientProvider>,
  );
}

describe("SlackTeamChannelsCard tests", () => {
  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    mockToast.mockClear();
    useBackendMutationSpy.mockClear();
  });

  test("renders a card with a collapsed dropdown titled Slack Team Channels", async () => {
    renderCard();

    const card = screen.getByTestId("SlackTeamChannelsCard-team-channels-card");
    expect(card).toHaveClass("card", "mt-4");
    expect(card.querySelector(".accordion")).toHaveClass("accordion-flush");

    const header = screen.getByRole("button", {
      name: "Slack Team Channels",
    });
    expect(header).toHaveAttribute("aria-expanded", "false");
    fireEvent.click(header);
    await waitFor(() =>
      expect(header).toHaveAttribute("aria-expanded", "true"),
    );

    expect(
      screen.getByTestId("SlackTeamChannelsCard-team-channels-description")
        .textContent,
    ).toBe(
      "For each team on the Teams tab, this creates a public Slack channel named team- followed by the team name (lowercased, with spaces and other special characters replaced by hyphens), unless it already exists. It adds the members of the team who have an account in the Slack workspace, and the instructor, to the channel. It then removes from each channel everyone who is not on that team, other than the instructor, the course staff, and bots. If two teams would get the same channel name, nothing is done and the job reports which teams. What was done is logged on the Jobs tab.",
    );
    expect(
      screen.getByTestId("SlackTeamChannelsCard-team-channels-submit"),
    ).toHaveTextContent("Set Up Team Slack Channels");

    expect(useBackendMutationSpy).toHaveBeenCalledWith(
      expect.any(Function),
      { onSuccess: expect.any(Function), onError: expect.any(Function) },
      ["/api/jobs/course"],
    );
    expect(axiosMock.history.post.length).toBe(0);
  });

  test("pressing the button launches the job and says where to find its log", async () => {
    axiosMock
      .onPost("/api/courses/slack/teamChannels")
      .reply(200, { id: 17, status: "running" });

    renderCard({ testIdPrefix: "Custom" });
    expect(screen.getByTestId("Custom-team-channels-card")).toBeInTheDocument();
    fireEvent.click(screen.getByTestId("Custom-team-channels-submit"));

    await waitFor(() =>
      expect(mockToast).toHaveBeenCalledWith(
        "Job 17 to set up team Slack channels started; see the Jobs tab for its log.",
      ),
    );
    expect(mockToast).toHaveBeenCalledTimes(1);
    expect(axiosMock.history.post.length).toBe(1);
    expect(axiosMock.history.post[0].url).toBe(
      "/api/courses/slack/teamChannels",
    );
    expect(axiosMock.history.post[0].params).toEqual({ courseId: 7 });
  });

  test("shows the message from the backend if the job cannot be launched", async () => {
    axiosMock.onPost("/api/courses/slack/teamChannels").reply(400, {
      type: "IllegalArgumentException",
      message:
        "The course option SLACK_INTEGRATION must be enabled to set up team Slack channels.",
    });

    renderCard();
    fireEvent.click(
      screen.getByTestId("SlackTeamChannelsCard-team-channels-submit"),
    );

    await waitFor(() =>
      expect(mockToast).toHaveBeenCalledWith(
        "The course option SLACK_INTEGRATION must be enabled to set up team Slack channels.",
      ),
    );
    expect(mockToast).toHaveBeenCalledTimes(1);
  });

  test("shows a generic message if there is no message from the backend", async () => {
    axiosMock.onPost("/api/courses/slack/teamChannels").networkError();

    renderCard();
    fireEvent.click(
      screen.getByTestId("SlackTeamChannelsCard-team-channels-submit"),
    );

    await waitFor(() =>
      expect(mockToast).toHaveBeenCalledWith(
        "Error starting job to set up team Slack channels: Error: Network Error",
      ),
    );
  });

  test("shows a generic message if the error response has no body", async () => {
    axiosMock.onPost("/api/courses/slack/teamChannels").reply(500);

    renderCard();
    fireEvent.click(
      screen.getByTestId("SlackTeamChannelsCard-team-channels-submit"),
    );

    await waitFor(() =>
      expect(mockToast).toHaveBeenCalledWith(
        "Error starting job to set up team Slack channels: Error: Request failed with status code 500",
      ),
    );
  });
});
