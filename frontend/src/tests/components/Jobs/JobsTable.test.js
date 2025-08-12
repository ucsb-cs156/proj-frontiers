import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router";
import JobsTable from "main/components/Jobs/JobsTable";
import { formatTime } from "main/utils/dateUtils";

jest.mock("main/utils/dateUtils", () => ({
  formatTime: jest.fn(),
}));

describe("JobsTable tests", () => {
  const queryClient = new QueryClient();

  beforeEach(() => {
    formatTime.mockReset();
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

  test("renders without crashing for null table", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <JobsTable jobs={null} />
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
    expect(screen.getByText("Created")).toBeInTheDocument();
    expect(screen.getByText("Updated")).toBeInTheDocument();
    expect(screen.getByText("Status")).toBeInTheDocument();
    expect(screen.getByText("Log")).toBeInTheDocument();

    // Check that the job data is rendered
    expect(screen.getByText("1")).toBeInTheDocument();
    expect(screen.getByText("2023-01-01 10:00:00")).toBeInTheDocument();
    expect(screen.getByText("2023-01-01 10:05:00")).toBeInTheDocument();
    expect(screen.getByText("complete")).toBeInTheDocument();
    expect(screen.getByText("Job completed successfully")).toBeInTheDocument();

    // Verify formatTime was called with the correct arguments
    expect(formatTime).toHaveBeenCalledTimes(2);
    expect(formatTime).toHaveBeenNthCalledWith(1, "2023-01-01T10:00:00");
    expect(formatTime).toHaveBeenNthCalledWith(2, "2023-01-01T10:05:00");
  });
});
