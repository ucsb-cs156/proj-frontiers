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
    axiosMock.resetHistory();
    optionsState = {
      ENABLE_CANVAS: false,
      TRANSLATE_SECTIONS: true,
      DOKKU_MANAGER: false,
      ENABLE_API_KEYS: false,
    };
    axiosMock
      .onGet(/\/api\/course\/options.*/)
      .reply(() => [200, { ...optionsState }]);
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
