import axios from "axios";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import AxiosMockAdapter from "axios-mock-adapter";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { expect, vi } from "vitest";
import SlackTokenForm from "main/components/Settings/SlackTokenForm";
import SlackCourseSettings from "main/components/Settings/SlackCourseSettings";
import * as useBackendModule from "main/utils/useBackend";

const axiosMock = new AxiosMockAdapter(axios);
const useBackendSpy = vi.spyOn(useBackendModule, "useBackend");

const connectedInfo = {
  courseId: "7",
  slackBotToken: "xoxb******************ghij",
  slackTeamId: "T12345678",
  slackTeamName: "ucsb-cs156-f26",
};

const notConnectedInfo = {
  courseId: "7",
  slackBotToken: "",
  slackTeamId: "",
  slackTeamName: "",
};

function renderWithClient(ui) {
  const client = new QueryClient();
  return render(
    <QueryClientProvider client={client}>{ui}</QueryClientProvider>,
  );
}

describe("SlackTokenForm tests", () => {
  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
  });

  test("renders correctly when no token has been stored", async () => {
    axiosMock
      .onGet(/\/api\/courses\/slack\/info.*/)
      .reply(200, notConnectedInfo);
    renderWithClient(<SlackTokenForm submitAction={vi.fn()} courseId={7} />);

    await waitFor(() => expect(axiosMock.history.get.length).toBe(1));
    expect(axiosMock.history.get[0].url).toBe(
      "/api/courses/slack/info?courseId=7",
    );
    expect(useBackendSpy).toHaveBeenCalledWith(
      ["/api/courses/slack/info?courseId=7"],
      { method: "GET", url: "/api/courses/slack/info?courseId=7" },
      {},
    );

    expect(screen.getByLabelText("Slack Bot Token")).toBeInTheDocument();
    expect(screen.getByTestId("SlackTokenForm-slackBotToken")).toHaveAttribute(
      "placeholder",
      "Token not set yet.",
    );
    expect(screen.getByTestId("SlackTokenForm-slackBotToken")).toHaveAttribute(
      "type",
      "password",
    );
    expect(screen.getByTestId("SlackTokenForm-slackBotToken")).toHaveAttribute(
      "autocomplete",
      "new-password",
    );
    expect(screen.getByTestId("SlackTokenForm-workspace")).toHaveTextContent(
      "Not connected to a Slack workspace yet.",
    );
    expect(screen.getByTestId("SlackTokenForm-submit")).toHaveTextContent(
      "Verify and Save Token",
    );
    expect(
      screen.queryByTestId("SlackTokenForm-error"),
    ).not.toBeInTheDocument();
  });

  test("shows masked token and workspace when a token has been stored", async () => {
    axiosMock.onGet(/\/api\/courses\/slack\/info.*/).reply(200, connectedInfo);
    renderWithClient(
      <SlackTokenForm
        submitAction={vi.fn()}
        courseId={7}
        buttonLabel="Update"
        testIdPrefix="Custom"
      />,
    );

    await waitFor(() =>
      expect(screen.getByTestId("Custom-slackBotToken")).toHaveAttribute(
        "placeholder",
        "Current Token: xoxb******************ghij",
      ),
    );
    expect(screen.getByTestId("Custom-workspace")).toHaveTextContent(
      "Connected to Slack workspace: ucsb-cs156-f26 (T12345678)",
    );
    expect(screen.getByTestId("Custom-submit")).toHaveTextContent("Update");
  });

  test("submits trimmed token and resets the field", async () => {
    axiosMock
      .onGet(/\/api\/courses\/slack\/info.*/)
      .reply(200, notConnectedInfo);
    const submitAction = vi.fn();
    renderWithClient(
      <SlackTokenForm submitAction={submitAction} courseId={7} />,
    );

    const input = screen.getByLabelText("Slack Bot Token");
    fireEvent.change(input, { target: { value: "  xoxb-test-token  " } });
    fireEvent.click(screen.getByTestId("SlackTokenForm-submit"));

    await waitFor(() => expect(submitAction).toHaveBeenCalledTimes(1));
    expect(submitAction).toHaveBeenCalledWith({
      slackBotToken: "xoxb-test-token",
    });
    await waitFor(() => expect(input).toHaveValue(""));
  });

  test("does not submit a blank token", async () => {
    axiosMock
      .onGet(/\/api\/courses\/slack\/info.*/)
      .reply(200, notConnectedInfo);
    const submitAction = vi.fn();
    renderWithClient(
      <SlackTokenForm submitAction={submitAction} courseId={7} />,
    );

    fireEvent.change(screen.getByLabelText("Slack Bot Token"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("SlackTokenForm-submit"));

    await screen.findByText("Slack Bot Token is required.");
    expect(screen.getByLabelText("Slack Bot Token")).toHaveClass("is-invalid");
    expect(submitAction).not.toHaveBeenCalled();
  });

  test("field is not marked invalid before submitting", async () => {
    axiosMock
      .onGet(/\/api\/courses\/slack\/info.*/)
      .reply(200, notConnectedInfo);
    renderWithClient(<SlackTokenForm submitAction={vi.fn()} courseId={7} />);
    expect(screen.getByLabelText("Slack Bot Token")).not.toHaveClass(
      "is-invalid",
    );
  });

  test("shows error message passed in from parent", async () => {
    axiosMock
      .onGet(/\/api\/courses\/slack\/info.*/)
      .reply(200, notConnectedInfo);
    renderWithClient(
      <SlackTokenForm
        submitAction={vi.fn()}
        courseId={7}
        errorMessage="Slack rejected this token as invalid."
      />,
    );
    expect(screen.getByTestId("SlackTokenForm-error")).toHaveTextContent(
      "Slack rejected this token as invalid.",
    );
    expect(screen.getByTestId("SlackTokenForm-error")).toHaveClass(
      "text-danger",
    );
  });
});

describe("SlackCourseSettings tests", () => {
  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    axiosMock.onGet(/\/api\/courses\/slack\/info.*/).reply(200, connectedInfo);
  });

  test("renders card with default test id prefix", async () => {
    const submitAction = vi.fn();
    renderWithClient(
      <SlackCourseSettings
        courseId={7}
        submitAction={submitAction}
        errorMessage="Some error"
      />,
    );

    expect(
      screen.getByTestId("SlackCourseSettings-slackForm"),
    ).toBeInTheDocument();
    expect(screen.getByText("Slack Integration Settings")).toBeInTheDocument();
    expect(
      screen.getByTestId("SlackCourseSettings-slack-error"),
    ).toHaveTextContent("Some error");
    await waitFor(() => expect(axiosMock.history.get.length).toBe(1));
    expect(axiosMock.history.get[0].url).toBe(
      "/api/courses/slack/info?courseId=7",
    );

    fireEvent.change(screen.getByLabelText("Slack Bot Token"), {
      target: { value: "xoxb-abc" },
    });
    fireEvent.click(screen.getByTestId("SlackCourseSettings-slack-submit"));
    await waitFor(() =>
      expect(submitAction).toHaveBeenCalledWith({ slackBotToken: "xoxb-abc" }),
    );
  });

  test("uses custom test id prefix", async () => {
    renderWithClient(
      <SlackCourseSettings
        courseId={7}
        submitAction={vi.fn()}
        testIdPrefix="Custom"
      />,
    );
    expect(screen.getByTestId("Custom-slackForm")).toBeInTheDocument();
    expect(screen.getByTestId("Custom-slack-submit")).toBeInTheDocument();
  });
});
