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
  beforeEach(() => {
    axiosMock.resetHistory();
    axiosMock.onGet(/\/api\/course\/options.*/).reply(200, {
      ENABLE_CANVAS: false,
      TRANSLATE_SECTIONS: true,
    });
    axiosMock.onPost("/api/course/options").reply(200, {
      ENABLE_CANVAS: true,
    });
  });

  afterEach(() => {
    useBackendMutationSpy.mockClear();
  });

  test("Settings tab component renders correctly", async () => {
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

    await screen.findByTestId("CanvasApiForm-submit");
    await waitFor(() =>
      expect(screen.getByLabelText("Enable Canvas")).toBeInTheDocument(),
    );

    expect(screen.getByText("Connect Canvas")).toBeInTheDocument();
    expect(screen.getByLabelText("Canvas Course ID")).toBeInTheDocument();
    expect(screen.getByLabelText("Canvas API Token")).toBeInTheDocument();
    expect(screen.getByTestId("CanvasApiForm-submit")).toBeInTheDocument();
    expect(screen.getByTestId("CanvasApiForm-canvasForm")).toBeInTheDocument();
    expect(screen.getByText("Course Options")).toBeInTheDocument();
    expect(screen.getByLabelText("Enable Canvas")).toBeInTheDocument();
    expect(screen.getByLabelText("Translate Sections")).toBeInTheDocument();
  });

  test("Call PUT for Canvas credentials properly", async () => {
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
