import axios from "axios";
import { fireEvent, render, waitFor, screen } from "@testing-library/react";
import JobTabComponent from "main/components/TabComponent/JobTabComponent";
import AxiosMockAdapter from "axios-mock-adapter";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { vi } from "vitest";
import * as useBackendModule from "main/utils/useBackend";

const axiosMock = new AxiosMockAdapter(axios);

beforeEach(() => {
  axiosMock.resetHistory();
});

const mockJobs = [
  {
    id: 1,
    status: "complete",
    createdAt: "2024-01-01T00:00:00",
    updatedAt: "2024-01-01T00:01:00",
    log: "Job finished successfully.",
  },
  {
    id: 2,
    status: "running",
    createdAt: "2024-01-02T00:00:00",
    updatedAt: "2024-01-02T00:00:30",
    log: "Job is still running.",
  },
];

test("Calls useBackend with correct query key, params, and default value", async () => {
  const mockRefetch = vi.fn();
  const useBackendSpy = vi
    .spyOn(useBackendModule, "useBackend")
    .mockReturnValue({ data: [], refetch: mockRefetch });

  const client = new QueryClient();
  render(
    <QueryClientProvider client={client}>
      <JobTabComponent courseId={7} testIdPrefix="course-7" />
    </QueryClientProvider>,
  );

  await screen.findByTestId("course-7-jobs-tab");

  expect(useBackendSpy).toHaveBeenCalledWith(
    ["/api/jobs/course", 7],
    {
      method: "GET",
      url: "/api/jobs/course",
      params: { courseId: 7 },
    },
    [],
  );

  useBackendSpy.mockRestore();
});

test("Renders job tab with jobs returned from backend", async () => {
  axiosMock.onGet("/api/jobs/course").reply(200, mockJobs);

  const client = new QueryClient();
  render(
    <QueryClientProvider client={client}>
      <JobTabComponent courseId={7} testIdPrefix="course-7" />
    </QueryClientProvider>,
  );

  await screen.findByTestId("course-7-jobs-tab");
  expect(screen.getByText("Job Status")).toBeInTheDocument();
  expect(axiosMock.history.get.length).toEqual(1);
  expect(axiosMock.history.get[0].params).toEqual({ courseId: 7 });
});

test("Renders job tab with correct testIdPrefix in data-testid attributes", async () => {
  axiosMock.onGet("/api/jobs/course").reply(200, []);

  const client = new QueryClient();
  render(
    <QueryClientProvider client={client}>
      <JobTabComponent courseId={42} testIdPrefix="my-prefix" />
    </QueryClientProvider>,
  );

  await screen.findByTestId("my-prefix-jobs-tab");
  expect(screen.getByTestId("my-prefix-refresh-jobs")).toBeInTheDocument();
});

test("Sends correct courseId as param to backend", async () => {
  axiosMock.onGet("/api/jobs/course").reply(200, []);

  const client = new QueryClient();
  render(
    <QueryClientProvider client={client}>
      <JobTabComponent courseId={99} testIdPrefix="test" />
    </QueryClientProvider>,
  );

  await screen.findByTestId("test-jobs-tab");
  expect(axiosMock.history.get.length).toEqual(1);
  expect(axiosMock.history.get[0].params).toEqual({ courseId: 99 });
});

test("Refresh button triggers a refetch of jobs", async () => {
  axiosMock.onGet("/api/jobs/course").reply(200, mockJobs);

  const client = new QueryClient();
  render(
    <QueryClientProvider client={client}>
      <JobTabComponent courseId={7} testIdPrefix="course-7" />
    </QueryClientProvider>,
  );

  await screen.findByTestId("course-7-refresh-jobs");
  expect(axiosMock.history.get.length).toEqual(1);

  fireEvent.click(screen.getByTestId("course-7-refresh-jobs"));
  await waitFor(() => expect(axiosMock.history.get.length).toBeGreaterThan(1));
});

test("Renders empty jobs table when backend returns empty array", async () => {
  axiosMock.onGet("/api/jobs/course").reply(200, []);

  const client = new QueryClient();
  render(
    <QueryClientProvider client={client}>
      <JobTabComponent courseId={7} testIdPrefix="course-7" />
    </QueryClientProvider>,
  );

  await screen.findByTestId("course-7-jobs-tab");
  expect(axiosMock.history.get[0].params).toEqual({ courseId: 7 });
});

test("Passes jobs data to JobsTable", async () => {
  axiosMock.onGet("/api/jobs/course").reply(200, mockJobs);

  const client = new QueryClient();
  render(
    <QueryClientProvider client={client}>
      <JobTabComponent courseId={7} testIdPrefix="course-7" />
    </QueryClientProvider>,
  );

  await screen.findByTestId("course-7-jobs-tab");
  await waitFor(() =>
    expect(screen.getByText("Job finished successfully.")).toBeInTheDocument(),
  );
  expect(screen.getByText("Job is still running.")).toBeInTheDocument();
});