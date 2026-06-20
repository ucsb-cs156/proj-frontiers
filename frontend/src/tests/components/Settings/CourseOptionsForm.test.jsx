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
      });
    axiosMock.onPost("/api/course/options").reply(200, { ENABLE_CANVAS: true });
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
});
