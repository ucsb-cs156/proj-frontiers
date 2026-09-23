import axios from "axios";
import { fireEvent, render, waitFor, screen } from "@testing-library/react";
import SettingsTabComponent from "main/components/TabComponent/SettingsTabComponent";
import AxiosMockAdapter from "axios-mock-adapter";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { expect, vi } from "vitest";
import coursesFixtures from "fixtures/coursesFixtures";
import * as useBackendModule from "main/utils/useBackend";

const axiosMock = new AxiosMockAdapter(axios);
const mockToast = vi.fn();

const useBackendMutationSpy = vi.spyOn(useBackendModule, "useBackendMutation");

vi.mock("react-toastify", async (importOriginal) => {
  return {
    ...(await importOriginal()),
    toast: (x) => mockToast(x),
  };
});

describe("SettingsTabComponent tests", () => {
  let optionsState;

  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    optionsState = {
      ENABLE_CANVAS: false,
      TRANSLATE_SECTIONS: true,
      DOKKU_MANAGER: false,
      ENABLE_API_KEYS: false,
      SLACK_INTEGRATION: false,
    };
    axiosMock
      .onGet(/\/api\/course\/options.*/)
      .reply(() => [200, { ...optionsState }]);
    axiosMock.onGet(/\/api\/courses\/slack\/info.*/).reply(200, {
      courseId: "1",
      slackBotToken: "",
      slackTeamId: "",
      slackTeamName: "",
    });
    mockToast.mockClear();
    axiosMock.onPost("/api/course/options").reply((config) => {
      const url = new URL(config.url, "http://localhost");
      const option = config.params?.option ?? url.searchParams.get("option");
      const enabledParam =
        config.params?.enabled ?? url.searchParams.get("enabled");
      const enabled = enabledParam === true || enabledParam === "true";
      optionsState[option] = enabled;
      return [200, { [option]: enabled }];
    });
  });

  afterEach(() => {
    useBackendMutationSpy.mockClear();
  });

  test("Settings tab component hides canvas settings when ENABLE_CANVAS is false", async () => {
    axiosMock.onPut("/api/courses/updateCourseCanvasToken").reply(200);
    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <SettingsTabComponent
          courseId={coursesFixtures.severalCourses[0].id}
          testIdPrefix="CanvasApiForm"
          canEditCourseOptions={true}
        />
      </QueryClientProvider>,
    );

    expect(
      screen.queryByTestId("CanvasApiForm-canvasForm"),
    ).not.toBeInTheDocument();

    await screen.findByTestId("CourseOptionsForm-toggle-ENABLE_CANVAS");
    await waitFor(() =>
      expect(screen.getByLabelText("Enable Canvas")).toBeInTheDocument(),
    );

    expect(screen.queryByText("Connect Canvas")).not.toBeInTheDocument();
    expect(screen.queryByLabelText("Canvas Course ID")).not.toBeInTheDocument();
    expect(screen.queryByLabelText("Canvas API Token")).not.toBeInTheDocument();
    expect(
      screen.queryByTestId("CanvasApiForm-submit"),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByTestId("CanvasApiForm-canvasForm"),
    ).not.toBeInTheDocument();
    expect(screen.getByText("Course Options")).toBeInTheDocument();
    expect(screen.getByLabelText("Enable Canvas")).toBeInTheDocument();
    expect(screen.getByLabelText("Translate Sections")).toBeInTheDocument();
    expect(screen.getByLabelText("Dokku Manager")).toBeInTheDocument();
    expect(screen.getByLabelText("Enable Api Keys")).toBeInTheDocument();
    expect(screen.getByLabelText("Slack Integration")).toBeInTheDocument();
  });

  test("Call PUT for Canvas credentials properly", async () => {
    optionsState.ENABLE_CANVAS = true;
    axiosMock.onPut("/api/courses/updateCourseCanvasToken").reply(200);
    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <SettingsTabComponent
          courseId={coursesFixtures.severalCourses[0].id}
          canEditCourseOptions={true}
        />
      </QueryClientProvider>,
    );

    await screen.findByTestId("CanvasApiForm-submit");

    fireEvent.change(screen.getByLabelText("Canvas API Token"), {
      target: { value: "test-token" },
    });
    fireEvent.change(screen.getByLabelText("Canvas Course ID"), {
      target: { value: "test-id" },
    });
    fireEvent.click(screen.getByTestId("CanvasApiForm-submit"));
    await waitFor(() => expect(mockToast).toHaveBeenCalled());
    expect(mockToast).toBeCalledWith("Canvas credentials successfully added.");
    expect(axiosMock.history.put.length).toEqual(1);
    expect(axiosMock.history.put[0].params).toEqual({
      courseId: coursesFixtures.severalCourses[0].id,
      canvasApiToken: "test-token",
      canvasCourseId: "test-id",
    });
  });

  test("canvas settings appear after enabling Enable Canvas", async () => {
    axiosMock.onPut("/api/courses/updateCourseCanvasToken").reply(200);
    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <SettingsTabComponent
          courseId={coursesFixtures.severalCourses[0].id}
          canEditCourseOptions={true}
          testIdPrefix="CanvasApiForm"
        />
      </QueryClientProvider>,
    );

    const enableCanvasToggle = await screen.findByTestId(
      "CourseOptionsForm-toggle-ENABLE_CANVAS",
    );
    fireEvent.click(enableCanvasToggle);

    await waitFor(() =>
      expect(
        screen.getByTestId("CanvasApiForm-canvasForm"),
      ).toBeInTheDocument(),
    );
    expect(screen.getByText("Connect Canvas")).toBeInTheDocument();
    expect(screen.getByLabelText("Canvas Course ID")).toBeInTheDocument();
    expect(screen.getByLabelText("Canvas API Token")).toBeInTheDocument();
  });

  test("toggling non-canvas option does not show canvas settings", async () => {
    axiosMock.onPut("/api/courses/updateCourseCanvasToken").reply(200);
    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <SettingsTabComponent
          courseId={coursesFixtures.severalCourses[0].id}
          canEditCourseOptions={true}
          testIdPrefix="CanvasApiForm"
        />
      </QueryClientProvider>,
    );

    await screen.findByTestId("CourseOptionsForm-toggle-DOKKU_MANAGER");
    fireEvent.click(
      screen.getByTestId("CourseOptionsForm-toggle-DOKKU_MANAGER"),
    );

    expect(
      screen.queryByTestId("CanvasApiForm-canvasForm"),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByTestId("CanvasApiForm-slackForm"),
    ).not.toBeInTheDocument();
  });

  test("slack settings are hidden when SLACK_INTEGRATION is false", async () => {
    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <SettingsTabComponent
          courseId={coursesFixtures.severalCourses[0].id}
          canEditCourseOptions={true}
          testIdPrefix="Settings"
        />
      </QueryClientProvider>,
    );

    await screen.findByTestId("CourseOptionsForm-toggle-SLACK_INTEGRATION");
    expect(screen.queryByTestId("Settings-slackForm")).not.toBeInTheDocument();
    expect(screen.queryByLabelText("Slack Bot Token")).not.toBeInTheDocument();
  });

  test("slack settings appear after enabling Slack Integration, without canvas settings", async () => {
    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <SettingsTabComponent
          courseId={coursesFixtures.severalCourses[0].id}
          canEditCourseOptions={true}
          testIdPrefix="Settings"
        />
      </QueryClientProvider>,
    );

    fireEvent.click(
      await screen.findByTestId("CourseOptionsForm-toggle-SLACK_INTEGRATION"),
    );

    await waitFor(() =>
      expect(screen.getByTestId("Settings-slackForm")).toBeInTheDocument(),
    );
    expect(screen.getByText("Slack Integration Settings")).toBeInTheDocument();
    expect(screen.getByLabelText("Slack Bot Token")).toBeInTheDocument();
    expect(screen.queryByTestId("Settings-canvasForm")).not.toBeInTheDocument();
  });

  test("enabling canvas does not show slack settings", async () => {
    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <SettingsTabComponent
          courseId={coursesFixtures.severalCourses[0].id}
          canEditCourseOptions={true}
          testIdPrefix="Settings"
        />
      </QueryClientProvider>,
    );

    fireEvent.click(
      await screen.findByTestId("CourseOptionsForm-toggle-ENABLE_CANVAS"),
    );

    await waitFor(() =>
      expect(screen.getByTestId("Settings-canvasForm")).toBeInTheDocument(),
    );
    expect(screen.queryByTestId("Settings-slackForm")).not.toBeInTheDocument();
  });

  test("slack settings shown on load when SLACK_INTEGRATION is true, but not canvas", async () => {
    optionsState.SLACK_INTEGRATION = true;
    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <SettingsTabComponent
          courseId={coursesFixtures.severalCourses[0].id}
          canEditCourseOptions={true}
          testIdPrefix="Settings"
        />
      </QueryClientProvider>,
    );

    await screen.findByTestId("Settings-slackForm");
    expect(screen.queryByTestId("Settings-canvasForm")).not.toBeInTheDocument();
  });

  test("canvas settings shown on load when ENABLE_CANVAS is true, but not slack", async () => {
    optionsState.ENABLE_CANVAS = true;
    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <SettingsTabComponent
          courseId={coursesFixtures.severalCourses[0].id}
          canEditCourseOptions={true}
          testIdPrefix="Settings"
        />
      </QueryClientProvider>,
    );

    await screen.findByTestId("Settings-canvasForm");
    expect(screen.queryByTestId("Settings-slackForm")).not.toBeInTheDocument();
  });

  test("Slack token is POSTed in the request body and success is toasted", async () => {
    optionsState.SLACK_INTEGRATION = true;
    axiosMock.onPost("/api/courses/slack/token").reply(200, {
      ok: true,
      courseId: "1",
      slackBotToken: "xoxb******************ghij",
      slackTeamId: "T12345678",
      slackTeamName: "ucsb-cs156-f26",
    });
    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <SettingsTabComponent
          courseId={coursesFixtures.severalCourses[0].id}
          canEditCourseOptions={true}
          testIdPrefix="Settings"
        />
      </QueryClientProvider>,
    );

    await screen.findByTestId("Settings-slack-submit");
    fireEvent.change(screen.getByLabelText("Slack Bot Token"), {
      target: { value: "xoxb-1234567890-abcdefghij" },
    });
    fireEvent.click(screen.getByTestId("Settings-slack-submit"));

    await waitFor(() =>
      expect(mockToast).toBeCalledWith(
        "Slack token verified and saved for workspace ucsb-cs156-f26 (T12345678).",
      ),
    );
    expect(axiosMock.history.post.length).toEqual(1);
    expect(axiosMock.history.post[0].url).toEqual("/api/courses/slack/token");
    expect(axiosMock.history.post[0].params).toEqual({
      courseId: coursesFixtures.severalCourses[0].id,
    });
    expect(String(axiosMock.history.post[0].data)).toEqual(
      "slackBotToken=xoxb-1234567890-abcdefghij",
    );
    expect(
      screen.queryByTestId("Settings-slack-error"),
    ).not.toBeInTheDocument();
  });

  test("invalid Slack token shows message from backend, which clears on later success", async () => {
    optionsState.SLACK_INTEGRATION = true;
    axiosMock.onPost("/api/courses/slack/token").replyOnce(400, {
      ok: false,
      error: "invalid_auth",
      message: "Slack rejected this token as invalid.",
    });
    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <SettingsTabComponent
          courseId={coursesFixtures.severalCourses[0].id}
          canEditCourseOptions={true}
          testIdPrefix="Settings"
        />
      </QueryClientProvider>,
    );

    await screen.findByTestId("Settings-slack-submit");
    fireEvent.change(screen.getByLabelText("Slack Bot Token"), {
      target: { value: "xoxb-bad" },
    });
    fireEvent.click(screen.getByTestId("Settings-slack-submit"));

    const error = await screen.findByTestId("Settings-slack-error");
    expect(error).toHaveTextContent("Slack rejected this token as invalid.");
    expect(mockToast).not.toHaveBeenCalled();

    axiosMock.onPost("/api/courses/slack/token").reply(200, {
      ok: true,
      slackTeamId: "T12345678",
      slackTeamName: "ucsb-cs156-f26",
    });
    fireEvent.change(screen.getByLabelText("Slack Bot Token"), {
      target: { value: "xoxb-good" },
    });
    fireEvent.click(screen.getByTestId("Settings-slack-submit"));

    await waitFor(() =>
      expect(
        screen.queryByTestId("Settings-slack-error"),
      ).not.toBeInTheDocument(),
    );
  });

  test("Slack token error without a backend message shows a generic message", async () => {
    optionsState.SLACK_INTEGRATION = true;
    axiosMock.onPost("/api/courses/slack/token").networkError();
    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <SettingsTabComponent
          courseId={coursesFixtures.severalCourses[0].id}
          canEditCourseOptions={true}
          testIdPrefix="Settings"
        />
      </QueryClientProvider>,
    );

    await screen.findByTestId("Settings-slack-submit");
    fireEvent.change(screen.getByLabelText("Slack Bot Token"), {
      target: { value: "xoxb-bad" },
    });
    fireEvent.click(screen.getByTestId("Settings-slack-submit"));

    const error = await screen.findByTestId("Settings-slack-error");
    expect(error).toHaveTextContent(
      "Error saving Slack token: Error: Network Error",
    );
  });

  test("Slack token error response without a body shows a generic message", async () => {
    optionsState.SLACK_INTEGRATION = true;
    axiosMock.onPost("/api/courses/slack/token").reply(500);
    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <SettingsTabComponent
          courseId={coursesFixtures.severalCourses[0].id}
          canEditCourseOptions={true}
          testIdPrefix="Settings"
        />
      </QueryClientProvider>,
    );

    await screen.findByTestId("Settings-slack-submit");
    fireEvent.change(screen.getByLabelText("Slack Bot Token"), {
      target: { value: "xoxb-bad" },
    });
    fireEvent.click(screen.getByTestId("Settings-slack-submit"));

    const error = await screen.findByTestId("Settings-slack-error");
    expect(error).toHaveTextContent(
      "Error saving Slack token: Error: Request failed with status code 500",
    );
  });

  test("Slack useBackendMutation is called with correct cache query key", async () => {
    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <SettingsTabComponent
          courseId={coursesFixtures.severalCourses[0].id}
          canEditCourseOptions={true}
        />
      </QueryClientProvider>,
    );

    expect(useBackendMutationSpy).toHaveBeenCalledWith(
      expect.any(Function),
      { onSuccess: expect.any(Function), onError: expect.any(Function) },
      [
        `/api/courses/slack/info?courseId=${coursesFixtures.severalCourses[0].id}`,
      ],
    );
  });

  test("toggling an option sends POST with option payload", async () => {
    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <SettingsTabComponent
          courseId={coursesFixtures.severalCourses[0].id}
          canEditCourseOptions={true}
        />
      </QueryClientProvider>,
    );

    await screen.findByTestId("CourseOptionsForm-toggle-ENABLE_CANVAS");

    fireEvent.click(
      screen.getByTestId("CourseOptionsForm-toggle-ENABLE_CANVAS"),
    );

    await waitFor(() =>
      expect(axiosMock.history.post.length).toBeGreaterThan(0),
    );
    expect(axiosMock.history.post[0].params).toEqual({
      courseId: coursesFixtures.severalCourses[0].id,
      option: "ENABLE_CANVAS",
      enabled: true,
    });
  });

  test("course option toggles are disabled when user cannot edit", async () => {
    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <SettingsTabComponent
          courseId={coursesFixtures.severalCourses[0].id}
          canEditCourseOptions={false}
        />
      </QueryClientProvider>,
    );

    const toggle = await screen.findByTestId(
      "CourseOptionsForm-toggle-ENABLE_CANVAS",
    );
    expect(toggle).toBeDisabled();
  });
  test("useBackendMutation is called with correct cache query key", async () => {
    const client = new QueryClient();

    render(
      <QueryClientProvider client={client}>
        <SettingsTabComponent
          courseId={coursesFixtures.severalCourses[0].id}
          canEditCourseOptions={true}
        />
      </QueryClientProvider>,
    );

    expect(useBackendMutationSpy).toHaveBeenCalledWith(
      expect.any(Function),
      { onSuccess: expect.any(Function) },
      [
        `/api/courses/getCanvasInfo?courseId=${coursesFixtures.severalCourses[0].id}`,
      ],
    );
  });
});
