import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router";
import { vi } from "vitest";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import JobsTable from "main/components/Jobs/JobsTable";
import { formatTime } from "main/utils/dateUtils";

vi.mock("main/utils/dateUtils", () => ({
  formatTime: vi.fn(),
}));

const axiosMock = new AxiosMockAdapter(axios);
const mockToast = vi.fn();
vi.mock("react-toastify", async (importOriginal) => {
  return {
    ...(await importOriginal()),
    toast: (x) => mockToast(x),
  };
});

describe("JobsTable tests", () => {
  const queryClient = new QueryClient();

  beforeEach(() => {
    formatTime.mockReset();
    axiosMock.reset();
    axiosMock.resetHistory();
    mockToast.mockReset();
  });

  test("renders without crashing for empty table", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <JobsTable jobs={[]} />
        </MemoryRouter>
      </QueryClientProvider>,
    );
  });

  test("renders correctly with jobs data", () => {
    // Mock the formatTime function to return predictable values
    formatTime
      .mockReturnValueOnce("2023-01-01 10:00:00") // for createdAt
      .mockReturnValueOnce("2023-01-01 10:05:00"); // for updatedAt

    const jobsFixture = [
      {
        id: 1,
        jobName: "Test Job",
        createdByEmail: "user1@example.com",
        scopeType: "course",
        scopeId: 101,
        createdAt: "2023-01-01T10:00:00",
        updatedAt: "2023-01-01T10:05:00",
        status: "complete",
        log: "Job completed successfully",
      },
    ];

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <JobsTable jobs={jobsFixture} />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    // Check that the table headers are rendered
    expect(screen.getByText("id")).toBeInTheDocument();
    expect(screen.getByText("Job Name")).toBeInTheDocument();
    expect(screen.getByText("User Email")).toBeInTheDocument();
    expect(screen.getByText("Course Id")).toBeInTheDocument();
    expect(screen.getByText("Created")).toBeInTheDocument();
    expect(screen.getByText("Updated")).toBeInTheDocument();
    expect(screen.getByText("Status")).toBeInTheDocument();
    expect(screen.getByTestId("JobsTable-header-cancel")).toHaveTextContent(
      "Cancel",
    );
    expect(screen.getByText("Log")).toBeInTheDocument();

    // Check that the job data is rendered
    expect(screen.getByText("1")).toBeInTheDocument();
    expect(screen.getByText("Test Job")).toBeInTheDocument();
    expect(screen.getByText("user1@example.com")).toBeInTheDocument();
    expect(screen.getByText("101")).toBeInTheDocument();
    expect(screen.getByText("2023-01-01 10:00:00")).toBeInTheDocument();
    expect(screen.getByText("2023-01-01 10:05:00")).toBeInTheDocument();
    expect(screen.getByText("complete")).toBeInTheDocument();
    expect(screen.getByText("Job completed successfully")).toBeInTheDocument();
    expect(screen.getByTestId("JobsTable-header-log")).toBeInTheDocument();
    expect(screen.getByText("Job completed successfully")).toHaveStyle({
      whiteSpace: "pre-wrap",
    });
    expect(screen.getByTestId("JobsTable-cell-row-0-col-log-div")).toHaveStyle(
      "max-width: 450px; max-height: 100px; overflow-y: auto;",
    );

    // Verify formatTime was called with the correct arguments
    expect(formatTime).toHaveBeenCalledTimes(2);
    expect(formatTime).toHaveBeenNthCalledWith(1, "2023-01-01T10:00:00");
    expect(formatTime).toHaveBeenNthCalledWith(2, "2023-01-01T10:05:00");
  });

  test("renders empty string for Course Id when the job is unscoped", () => {
    formatTime
      .mockReturnValueOnce("2023-01-01 10:00:00")
      .mockReturnValueOnce("2023-01-01 10:05:00");

    const jobsFixture = [
      {
        id: 2,
        jobName: "No Course Job",
        createdByEmail: "user2@example.com",
        scopeType: null,
        scopeId: null,
        createdAt: "2023-01-01T10:00:00",
        updatedAt: "2023-01-01T10:05:00",
        status: "complete",
        log: "Job completed successfully",
      },
    ];

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <JobsTable jobs={jobsFixture} />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    // Check that the table headers are rendered
    expect(screen.getByText("id")).toBeInTheDocument();
    expect(screen.getByText("Job Name")).toBeInTheDocument();
    expect(screen.getByText("User Email")).toBeInTheDocument();
    expect(screen.getByText("Course Id")).toBeInTheDocument();
    expect(screen.getByText("Created")).toBeInTheDocument();
    expect(screen.getByText("Updated")).toBeInTheDocument();
    expect(screen.getByText("Status")).toBeInTheDocument();
    expect(screen.getByTestId("JobsTable-header-cancel")).toHaveTextContent(
      "Cancel",
    );
    expect(screen.getByText("Log")).toBeInTheDocument();

    // Check that the job data is rendered
    expect(screen.getByText("2")).toBeInTheDocument();
    expect(screen.getByText("No Course Job")).toBeInTheDocument();
    expect(screen.getByText("user2@example.com")).toBeInTheDocument();
    const courseCell = screen.getByTestId("JobsTable-cell-row-0-col-courseId");

    expect(courseCell).toBeEmptyDOMElement();
    expect(screen.getByText("2023-01-01 10:00:00")).toBeInTheDocument();
    expect(screen.getByText("2023-01-01 10:05:00")).toBeInTheDocument();
    expect(screen.getByText("complete")).toBeInTheDocument();
    expect(screen.getByText("Job completed successfully")).toBeInTheDocument();
    expect(screen.getByTestId("JobsTable-header-log")).toBeInTheDocument();
    expect(screen.getByText("Job completed successfully")).toHaveStyle({
      whiteSpace: "pre-wrap",
    });
    expect(screen.getByTestId("JobsTable-cell-row-0-col-log-div")).toHaveStyle(
      "max-width: 450px; max-height: 100px; overflow-y: auto;",
    );

    // Verify formatTime was called with the correct arguments
    expect(formatTime).toHaveBeenCalledTimes(2);
    expect(formatTime).toHaveBeenNthCalledWith(1, "2023-01-01T10:00:00");
    expect(formatTime).toHaveBeenNthCalledWith(2, "2023-01-01T10:05:00");
  });

  test.each(["queued", "running"])(
    "shows a Cancel button for a %s job",
    (status) => {
      const jobsFixture = [
        {
          id: 5,
          createdAt: "2023-11-01T12:00:00Z",
          updatedAt: "2023-11-01T12:00:00Z",
          status,
          log: "",
        },
      ];

      render(
        <QueryClientProvider client={queryClient}>
          <MemoryRouter>
            <JobsTable jobs={jobsFixture} />
          </MemoryRouter>
        </QueryClientProvider>,
      );

      expect(
        screen.getByTestId("JobsTable-cell-row-0-col-cancel-button"),
      ).toBeInTheDocument();
    },
  );

  test.each(["complete", "error", "cancelling", "cancelled", "interrupted"])(
    "does not show a Cancel button for a %s job",
    (status) => {
      const jobsFixture = [
        {
          id: 5,
          createdAt: "2023-11-01T12:00:00Z",
          updatedAt: "2023-11-01T12:00:00Z",
          status,
          log: "",
        },
      ];

      render(
        <QueryClientProvider client={queryClient}>
          <MemoryRouter>
            <JobsTable jobs={jobsFixture} />
          </MemoryRouter>
        </QueryClientProvider>,
      );

      expect(
        screen.queryByTestId("JobsTable-cell-row-0-col-cancel-button"),
      ).not.toBeInTheDocument();
    },
  );

  test("clicking Cancel requests cancellation and calls onCancelled", async () => {
    axiosMock.onPost("/api/jobs/5/cancel").reply(200, {
      id: 5,
      status: "cancelling",
    });
    const onCancelled = vi.fn();
    const jobsFixture = [
      {
        id: 5,
        createdAt: "2023-11-01T12:00:00Z",
        updatedAt: "2023-11-01T12:00:00Z",
        status: "running",
        log: "",
      },
    ];

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <JobsTable jobs={jobsFixture} onCancelled={onCancelled} />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const cancelButton = screen.getByTestId(
      "JobsTable-cell-row-0-col-cancel-button",
    );
    fireEvent.click(cancelButton);

    await waitFor(() => expect(axiosMock.history.post.length).toEqual(1));
    expect(mockToast).toBeCalledWith("Cancellation requested.");
    expect(onCancelled).toHaveBeenCalledTimes(1);
  });

  test("clicking Cancel works without an onCancelled prop", async () => {
    axiosMock.onPost("/api/jobs/5/cancel").reply(200, {
      id: 5,
      status: "cancelling",
    });
    const jobsFixture = [
      {
        id: 5,
        createdAt: "2023-11-01T12:00:00Z",
        updatedAt: "2023-11-01T12:00:00Z",
        status: "running",
        log: "",
      },
    ];

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <JobsTable jobs={jobsFixture} />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const cancelButton = screen.getByTestId(
      "JobsTable-cell-row-0-col-cancel-button",
    );
    fireEvent.click(cancelButton);

    await waitFor(() => expect(axiosMock.history.post.length).toEqual(1));
    expect(mockToast).toBeCalledWith("Cancellation requested.");
  });
});
