import axios from "axios";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import AxiosMockAdapter from "axios-mock-adapter";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { expect, vi } from "vitest";
import SlackPrivateChannelsCard from "main/components/Slack/SlackPrivateChannelsCard";
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
      <SlackPrivateChannelsCard courseId={7} {...props} />
    </QueryClientProvider>,
  );
}

describe("SlackPrivateChannelsCard tests", () => {
  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    mockToast.mockClear();
    useBackendMutationSpy.mockClear();
  });

  test("renders a card with a collapsed dropdown titled Slack Private Channels for Each Student plus Staff", async () => {
    renderCard();

    const card = screen.getByTestId(
      "SlackPrivateChannelsCard-private-channels-card",
    );
    expect(card).toHaveClass("card", "mt-4");
    expect(card.querySelector(".accordion")).toHaveClass("accordion-flush");

    const header = screen.getByRole("button", {
      name: "Slack Private Channels for Each Student plus Staff",
    });
    expect(header).toHaveAttribute("aria-expanded", "false");
    fireEvent.click(header);
    await waitFor(() =>
      expect(header).toHaveAttribute("aria-expanded", "true"),
    );

    expect(
      screen.getByTestId(
        "SlackPrivateChannelsCard-private-channels-description",
      ).textContent,
    ).toBe(
      "For each student on the roster who has an account in the Slack workspace, this creates a private Slack channel named private-first-last, and adds the student, the instructor and all of the course staff to it. The first and last name are taken from Slack when the student has entered them there, and otherwise from the roster; only the first word of the first name is used. A student's channel is recognized by its members, not by its name, so running this again does not create duplicates: it creates channels for new students, adds new staff members to the existing channels, and renames a channel if the name of its student has changed. Nobody is ever removed from a channel. What was done is logged on the Jobs tab.",
    );
    expect(
      screen.getByTestId("SlackPrivateChannelsCard-private-channels-submit"),
    ).toHaveTextContent("Create Private Channels for Each Student plus Staff");

    expect(useBackendMutationSpy).toHaveBeenCalledWith(
      expect.any(Function),
      { onSuccess: expect.any(Function), onError: expect.any(Function) },
      ["/api/jobs/course"],
    );
    expect(axiosMock.history.post.length).toBe(0);
  });

  test("pressing the button launches the job and says where to find its log", async () => {
    axiosMock
      .onPost("/api/courses/slack/privateChannels")
      .reply(200, { id: 17, status: "running" });

    renderCard({ testIdPrefix: "Custom" });
    expect(
      screen.getByTestId("Custom-private-channels-card"),
    ).toBeInTheDocument();
    fireEvent.click(screen.getByTestId("Custom-private-channels-submit"));

    await waitFor(() =>
      expect(mockToast).toHaveBeenCalledWith(
        "Job 17 to create private Slack channels started; see the Jobs tab for its log.",
      ),
    );
    expect(mockToast).toHaveBeenCalledTimes(1);
    expect(axiosMock.history.post.length).toBe(1);
    expect(axiosMock.history.post[0].url).toBe(
      "/api/courses/slack/privateChannels",
    );
    expect(axiosMock.history.post[0].params).toEqual({ courseId: 7 });
  });

  test("shows the message from the backend if the job cannot be launched", async () => {
    axiosMock.onPost("/api/courses/slack/privateChannels").reply(400, {
      type: "IllegalArgumentException",
      message:
        "The course option SLACK_INTEGRATION must be enabled to set up private Slack channels.",
    });

    renderCard();
    fireEvent.click(
      screen.getByTestId("SlackPrivateChannelsCard-private-channels-submit"),
    );

    await waitFor(() =>
      expect(mockToast).toHaveBeenCalledWith(
        "The course option SLACK_INTEGRATION must be enabled to set up private Slack channels.",
      ),
    );
    expect(mockToast).toHaveBeenCalledTimes(1);
  });

  test("shows a generic message if there is no message from the backend", async () => {
    axiosMock.onPost("/api/courses/slack/privateChannels").networkError();

    renderCard();
    fireEvent.click(
      screen.getByTestId("SlackPrivateChannelsCard-private-channels-submit"),
    );

    await waitFor(() =>
      expect(mockToast).toHaveBeenCalledWith(
        "Error starting job to create private Slack channels: Error: Network Error",
      ),
    );
  });

  test("shows a generic message if the error response has no body", async () => {
    axiosMock.onPost("/api/courses/slack/privateChannels").reply(500);

    renderCard();
    fireEvent.click(
      screen.getByTestId("SlackPrivateChannelsCard-private-channels-submit"),
    );

    await waitFor(() =>
      expect(mockToast).toHaveBeenCalledWith(
        "Error starting job to create private Slack channels: Error: Request failed with status code 500",
      ),
    );
  });
});
