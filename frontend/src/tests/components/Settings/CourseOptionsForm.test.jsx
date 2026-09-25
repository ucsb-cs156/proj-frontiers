import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import CourseOptionsForm from "main/components/Settings/CourseOptionsForm";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import { expect, vi } from "vitest";

const axiosMock = new AxiosMockAdapter(axios);
const mockToast = vi.fn();

vi.mock("react-toastify", async (importOriginal) => {
  return {
    ...(await importOriginal()),
    toast: (x) => mockToast(x),
  };
});

describe("CourseOptionsForm tests", () => {
  beforeEach(() => {
    axiosMock.reset();
    mockToast.mockClear();
    // Mock the GET request to fetch course options for courseId=1
    axiosMock
      .onGet("/api/course/options", { params: { courseId: 1 } })
      .reply(200, {
        ENABLE_CANVAS: false,
        TRANSLATE_SECTIONS: true,
        DOKKU_MANAGER: false,
        ENABLE_API_KEYS: false,
        SLACK_INTEGRATION: false,
        NEW_ASSIGNMENT_FEATURES: false,
      });
    axiosMock.onPost("/api/course/options").reply((config) => [
      200,
      {
        [config.params.option]: config.params.enabled,
      },
    ]);
  });

  test("Course options form renders correctly", async () => {
    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <CourseOptionsForm courseId={1} canEdit={true} />
      </QueryClientProvider>,
    );

    await screen.findByText("Course Options");
    await screen.findByLabelText("Enable Canvas");
    expect(screen.getByLabelText("Enable Canvas")).toBeInTheDocument();
    expect(screen.getByLabelText("Translate Sections")).toBeInTheDocument();
    expect(screen.getByLabelText("Dokku Manager")).toBeInTheDocument();
    expect(screen.getByLabelText("Enable Api Keys")).toBeInTheDocument();
    expect(screen.getByLabelText("Slack Integration")).toBeInTheDocument();
    expect(
      screen.getByLabelText("New Assignment Features"),
    ).toBeInTheDocument();

    const toggle = screen.getByTestId("CourseOptionsForm-toggle-ENABLE_CANVAS");
    fireEvent.click(toggle);

    await waitFor(() => expect(axiosMock.history.post.length).toBe(1));
    expect(axiosMock.history.post[0].params).toEqual({
      courseId: 1,
      option: "ENABLE_CANVAS",
      enabled: true,
    });
  });

  test("GET request is made with method GET and correct courseId param", async () => {
    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <CourseOptionsForm courseId={1} canEdit={true} />
      </QueryClientProvider>,
    );

    await screen.findByLabelText("Enable Canvas");
    expect(axiosMock.history.get.length).toBeGreaterThan(0);
    expect(axiosMock.history.get[0].params).toEqual({ courseId: 1 });
  });

  test("Slack Integration toggle sends the correct option payload", async () => {
    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <CourseOptionsForm courseId={1} canEdit={true} />
      </QueryClientProvider>,
    );

    await screen.findByLabelText("Slack Integration");
    const toggle = screen.getByTestId(
      "CourseOptionsForm-toggle-SLACK_INTEGRATION",
    );
    fireEvent.click(toggle);

    await waitFor(() => expect(axiosMock.history.post.length).toBe(1));
    expect(axiosMock.history.post[0].params).toEqual({
      courseId: 1,
      option: "SLACK_INTEGRATION",
      enabled: true,
    });
  });

  test("Toast shows correct message after successful toggle", async () => {
    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <CourseOptionsForm courseId={1} canEdit={true} />
      </QueryClientProvider>,
    );

    await screen.findByLabelText("Enable Canvas");
    const toggle = screen.getByTestId("CourseOptionsForm-toggle-ENABLE_CANVAS");
    fireEvent.click(toggle);

    await waitFor(() =>
      expect(mockToast).toHaveBeenCalledWith("Enable Canvas set to true"),
    );
  });

  test("GET is re-fetched after mutation to reflect updated data", async () => {
    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <CourseOptionsForm courseId={1} canEdit={true} />
      </QueryClientProvider>,
    );

    await screen.findByLabelText("Enable Canvas");
    const initialGetCount = axiosMock.history.get.length;

    const toggle = screen.getByTestId("CourseOptionsForm-toggle-ENABLE_CANVAS");
    fireEvent.click(toggle);

    await waitFor(() =>
      expect(axiosMock.history.get.length).toBeGreaterThan(initialGetCount),
    );
  });

  test("Toast shows error when course options cannot be loaded", async () => {
    axiosMock.reset();
    axiosMock.onGet("/api/course/options").reply(500);

    const client = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });
    render(
      <QueryClientProvider client={client}>
        <CourseOptionsForm courseId={1} canEdit={true} />
      </QueryClientProvider>,
    );

    await waitFor(() => expect(axiosMock.history.get.length).toBe(1));
    await waitFor(() =>
      expect(mockToast).toHaveBeenCalledWith(
        "Error communicating with backend via GET on /api/course/options",
      ),
    );
  });
});
