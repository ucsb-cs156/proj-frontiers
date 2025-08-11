import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import AdminJobsPage from "main/pages/Admin/AdminJobsPage";
import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";

describe("AdminJobsPage tests", () => {
  const queryClient = new QueryClient();
  const axiosMock = new AxiosMockAdapter(axios);

  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    axiosMock.onGet("/api/currentUser").reply(200, apiCurrentUserFixtures.adminUser);
    axiosMock.onGet("/api/systemInfo").reply(200, systemInfoFixtures.showingNeither);
  });

  test("renders without crashing", async () => {
    axiosMock.onGet("/api/jobs/all").reply(200, []);
    
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AdminJobsPage />
        </MemoryRouter>
      </QueryClientProvider>
    );

    expect(await screen.findByText("Launch Jobs")).toBeInTheDocument();
    expect(screen.getByText("Job Status")).toBeInTheDocument();
    expect(screen.getByText("Purge Job Log")).toBeInTheDocument();
  });

  test("renders job launchers correctly", async () => {
    axiosMock.onGet("/api/jobs/all").reply(200, []);
    
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AdminJobsPage />
        </MemoryRouter>
      </QueryClientProvider>
    );

    expect(await screen.findByText("Update All Users")).toBeInTheDocument();
    expect(screen.getByText("Audit All Courses")).toBeInTheDocument();
  });

  test("renders job table with data", async () => {
    const jobsFixture = [
      {
        id: 1,
        createdAt: "2023-01-01T10:00:00",
        updatedAt: "2023-01-01T10:05:00",
        status: "complete",
        log: "Job completed successfully",
      },
    ];
    
    axiosMock.onGet("/api/jobs/all").reply(200, jobsFixture);
    
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AdminJobsPage />
        </MemoryRouter>
      </QueryClientProvider>
    );

    expect(await screen.findByText("id")).toBeInTheDocument();
    expect(screen.getByText("Created")).toBeInTheDocument();
    expect(screen.getByText("Updated")).toBeInTheDocument();
    expect(screen.getByText("Status")).toBeInTheDocument();
    expect(screen.getByText("Log")).toBeInTheDocument();
  });

  test("clicking Update All Users button calls the correct API", async () => {
    axiosMock.onGet("/api/jobs/all").reply(200, []);
    axiosMock.onPost("/api/jobs/launch/updateAll").reply(200, {
      id: 1,
      status: "running",
    });
    
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AdminJobsPage />
        </MemoryRouter>
      </QueryClientProvider>
    );

    // Find and click the Update All Users accordion header
    const updateAllUsersHeader = await screen.findByText("Update All Users");
    fireEvent.click(updateAllUsersHeader);

    // Find and click the button inside the accordion
    const updateAllUsersButton = await screen.findByText("Update All Users");
    fireEvent.click(updateAllUsersButton);

    await waitFor(() => expect(axiosMock.history.post.length).toBe(1));
    expect(axiosMock.history.post[0].url).toBe("/api/jobs/launch/updateAll");
  });

  test("clicking Audit All Courses button calls the correct API", async () => {
    axiosMock.onGet("/api/jobs/all").reply(200, []);
    axiosMock.onPost("/api/jobs/launch/auditAllCourses").reply(200, {
      id: 2,
      status: "running",
    });
    
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AdminJobsPage />
        </MemoryRouter>
      </QueryClientProvider>
    );

    // Find and click the Audit All Courses accordion header
    const auditAllCoursesHeader = await screen.findByText("Audit All Courses");
    fireEvent.click(auditAllCoursesHeader);

    // Find and click the button inside the accordion
    const auditAllCoursesButton = await screen.findByText("Audit All Courses");
    fireEvent.click(auditAllCoursesButton);

    await waitFor(() => expect(axiosMock.history.post.length).toBe(1));
    expect(axiosMock.history.post[0].url).toBe("/api/jobs/launch/auditAllCourses");
  });

  test("clicking Purge Job Log button calls the correct API", async () => {
    axiosMock.onGet("/api/jobs/all").reply(200, []);
    axiosMock.onDelete("/api/jobs/all").reply(200, {
      message: "All jobs deleted",
    });
    
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AdminJobsPage />
        </MemoryRouter>
      </QueryClientProvider>
    );

    const purgeJobLogButton = await screen.findByTestId("purgeJobLog");
    fireEvent.click(purgeJobLogButton);

    await waitFor(() => expect(axiosMock.history.delete.length).toBe(1));
    expect(axiosMock.history.delete[0].url).toBe("/api/jobs/all");
  });
});