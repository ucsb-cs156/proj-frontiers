import axios from "axios";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import AxiosMockAdapter from "axios-mock-adapter";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { expect, vi } from "vitest";
import SlackSectionChannelsCard from "main/components/Slack/SlackSectionChannelsCard";
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
      <SlackSectionChannelsCard courseId={7} {...props} />
    </QueryClientProvider>,
  );
}

describe("SlackSectionChannelsCard tests", () => {
  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    mockToast.mockClear();
    useBackendMutationSpy.mockClear();
  });

  test("renders a card with a collapsed dropdown titled Slack Section Channels", async () => {
    renderCard();

    const card = screen.getByTestId(
      "SlackSectionChannelsCard-section-channels-card",
    );
    expect(card).toHaveClass("card", "mt-4");
    expect(card.querySelector(".accordion")).toHaveClass("accordion-flush");

    const header = screen.getByRole("button", {
      name: "Slack Section Channels",
    });
    expect(header).toHaveAttribute("aria-expanded", "false");
    fireEvent.click(header);
    await waitFor(() =>
      expect(header).toHaveAttribute("aria-expanded", "true"),
    );

    expect(
      screen.getByTestId(
        "SlackSectionChannelsCard-section-channels-description",
      ).textContent,
    ).toBe(
      "For each section on the Sections tab that has a Slack channel name, this creates a public Slack channel with that name (unless it already exists), and adds the roster students of that section who have an account in the Slack workspace. It then removes from each of those channels everyone who is not a roster student of that section, other than the course staff, the instructor, and bots. What was done is logged on the Jobs tab.",
    );
    expect(
      screen.getByTestId("SlackSectionChannelsCard-section-channels-submit"),
    ).toHaveTextContent("Set Up Section Slack Channels");

    expect(useBackendMutationSpy).toHaveBeenCalledWith(
      expect.any(Function),
      { onSuccess: expect.any(Function), onError: expect.any(Function) },
      ["/api/jobs/course"],
    );
    expect(axiosMock.history.post.length).toBe(0);
  });

  test("pressing the button launches the job and says where to find its log", async () => {
    axiosMock
      .onPost("/api/courses/slack/sectionChannels")
      .reply(200, { id: 17, status: "running" });

    renderCard({ testIdPrefix: "Custom" });
    expect(
      screen.getByTestId("Custom-section-channels-card"),
    ).toBeInTheDocument();
    fireEvent.click(screen.getByTestId("Custom-section-channels-submit"));

    await waitFor(() =>
      expect(mockToast).toHaveBeenCalledWith(
        "Job 17 to set up section Slack channels started; see the Jobs tab for its log.",
      ),
    );
    expect(mockToast).toHaveBeenCalledTimes(1);
    expect(axiosMock.history.post.length).toBe(1);
    expect(axiosMock.history.post[0].url).toBe(
      "/api/courses/slack/sectionChannels",
    );
    expect(axiosMock.history.post[0].params).toEqual({ courseId: 7 });
  });

  test("shows the message from the backend if the job cannot be launched", async () => {
    axiosMock.onPost("/api/courses/slack/sectionChannels").reply(400, {
      type: "IllegalArgumentException",
      message:
        "The course option TRANSLATE_SECTIONS must be enabled to set up section Slack channels.",
    });

    renderCard();
    fireEvent.click(
      screen.getByTestId("SlackSectionChannelsCard-section-channels-submit"),
    );

    await waitFor(() =>
      expect(mockToast).toHaveBeenCalledWith(
        "The course option TRANSLATE_SECTIONS must be enabled to set up section Slack channels.",
      ),
    );
    expect(mockToast).toHaveBeenCalledTimes(1);
  });

  test("shows a generic message if there is no message from the backend", async () => {
    axiosMock.onPost("/api/courses/slack/sectionChannels").networkError();

    renderCard();
    fireEvent.click(
      screen.getByTestId("SlackSectionChannelsCard-section-channels-submit"),
    );

    await waitFor(() =>
      expect(mockToast).toHaveBeenCalledWith(
        "Error starting job to set up section Slack channels: Error: Network Error",
      ),
    );
  });

  test("shows a generic message if the error response has no body", async () => {
    axiosMock.onPost("/api/courses/slack/sectionChannels").reply(500);

    renderCard();
    fireEvent.click(
      screen.getByTestId("SlackSectionChannelsCard-section-channels-submit"),
    );

    await waitFor(() =>
      expect(mockToast).toHaveBeenCalledWith(
        "Error starting job to set up section Slack channels: Error: Request failed with status code 500",
      ),
    );
  });
});
