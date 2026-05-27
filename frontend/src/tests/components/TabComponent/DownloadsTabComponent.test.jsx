import { render, screen ,fireEvent} from "@testing-library/react";
import AxiosMockAdapter from "axios-mock-adapter";
import axios from "axios";
import {
  QueryClient,
  QueryClientProvider,
  useQuery,
} from "@tanstack/react-query";
import DownloadsTabComponent from "main/components/TabComponent/DownloadsTabComponent";
import { expect, vi } from "vitest";

const axiosMock = new AxiosMockAdapter(axios);
const queryClient = new QueryClient();
const testId = "InstructorCourseShowPage";
vi.mock("react-toastify", async (importOriginal) => {
  const mockToast = vi.fn();
  mockToast.error = vi.fn();
  return {
    ...(await importOriginal()),
    toast: mockToast,
  };
});

const mockedNavigate = vi.fn();
vi.mock("react-router", async (importOriginal) => ({
  ...(await importOriginal()),
  useNavigate: () => mockedNavigate,
}));

describe("DownloadsTabComponent Tests", () => {
  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    queryClient.clear();
    vi.resetAllMocks();
  });

  test("Download Button Renders", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <DownloadsTabComponent courseId={1} testIdPrefix={testId} />
      </QueryClientProvider>,
    );
    expect(
      screen.getByTestId(`${testId}-DownloadsTabComponent`),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Download Student CSV" }),
    ).toBeInTheDocument();
  });
  test("calls window.open with correct url", async () => {
    const openMock = vi.fn();
    window.open = openMock;

    render(
        <QueryClientProvider client={queryClient}>
            <DownloadsTabComponent courseId={42} testIdPrefix={testId} />
        </QueryClientProvider>,
    );
    const downloadButton = screen.getByTestId(`${testId}-download-student-csv-button`);
    fireEvent.click(downloadButton);
    expect(openMock).toHaveBeenCalledWith("/api/csv/rosterstudents?courseId=42", "_blank");
  });
});
