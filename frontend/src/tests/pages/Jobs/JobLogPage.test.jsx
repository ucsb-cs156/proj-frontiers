import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router";
import { vi } from "vitest";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import JobLogPage from "main/pages/Jobs/JobLogPage";
import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import * as useBackendModule from "main/utils/useBackend";

const mockedNavigate = vi.fn();
vi.mock("react-router", async (importOriginal) => ({
  ...(await importOriginal()),
  useNavigate: () => mockedNavigate,
}));

const ADMIN_URL = "/api/jobs/logs/5";
const COURSE_URL = "/api/jobs/course/logs";

const renderAt = (path, queryClient) =>
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route path="/admin/jobs/logs/:id" element={<JobLogPage />} />
          <Route
            path="/instructor/courses/:courseId/jobs/:jobId/logs"
            element={<JobLogPage />}
          />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );

const getRequestsTo = (axiosMock, url) =>
  axiosMock.history.get.filter((req) => req.url === url);

describe("JobLogPage tests", () => {
  const axiosMock = new AxiosMockAdapter(axios);
  let queryClient;

  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    mockedNavigate.mockReset();
    queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });
    axiosMock
      .onGet("/api/currentUser")
      .reply(200, apiCurrentUserFixtures.adminUser);
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.showingNeither);
  });

  describe("admin route /admin/jobs/logs/:id", () => {
    test("fetches the library endpoint and shows the full log", async () => {
      axiosMock.onGet(ADMIN_URL).reply(200, "line 1\nline 2\nline 3");

      renderAt("/admin/jobs/logs/5", queryClient);

      expect(screen.getByTestId("JobLogPage-heading")).toHaveTextContent(
        "Job Log for Job 5",
      );
      expect(screen.getByTestId("JobLogPage-loading")).toHaveTextContent(
        "Loading...",
      );

      const pre = await screen.findByTestId("JobLogPage-log");
      expect(pre).toHaveTextContent("line 1 line 2 line 3");
      expect(pre.textContent).toBe("line 1\nline 2\nline 3");
      expect(pre).toHaveStyle({ whiteSpace: "pre-wrap" });
      expect(
        screen.queryByTestId("JobLogPage-loading"),
      ).not.toBeInTheDocument();
      expect(screen.queryByTestId("JobLogPage-empty")).not.toBeInTheDocument();
      expect(screen.queryByTestId("JobLogPage-error")).not.toBeInTheDocument();

      const requests = getRequestsTo(axiosMock, ADMIN_URL);
      expect(requests).toHaveLength(1);
      expect(requests[0].params).toBeUndefined();
      expect(getRequestsTo(axiosMock, COURSE_URL)).toHaveLength(0);
      expect(queryClient.getQueryData([ADMIN_URL])).toBe(
        "line 1\nline 2\nline 3",
      );
    });

    test("calls useBackend with the exact admin query key and axios params", () => {
      const useBackendSpy = vi
        .spyOn(useBackendModule, "useBackend")
        .mockReturnValue({ data: "x", isError: false, refetch: vi.fn() });

      renderAt("/admin/jobs/logs/5", queryClient);

      expect(useBackendSpy).toHaveBeenCalledWith([ADMIN_URL], {
        method: "GET",
        url: ADMIN_URL,
      });
      useBackendSpy.mockRestore();
    });

    test("shows 'No log lines yet.' when the log is empty", async () => {
      axiosMock.onGet(ADMIN_URL).reply(200, "");

      renderAt("/admin/jobs/logs/5", queryClient);

      expect(await screen.findByTestId("JobLogPage-empty")).toHaveTextContent(
        "No log lines yet.",
      );
      expect(screen.queryByTestId("JobLogPage-log")).not.toBeInTheDocument();
      expect(
        screen.queryByTestId("JobLogPage-loading"),
      ).not.toBeInTheDocument();
    });

    test("shows an error message when the backend returns an error", async () => {
      axiosMock.onGet(ADMIN_URL).reply(404, {
        type: "EntityNotFoundException",
        message: "Job with id 5 not found",
      });

      renderAt("/admin/jobs/logs/5", queryClient);

      expect(await screen.findByTestId("JobLogPage-error")).toHaveTextContent(
        "Error loading log.",
      );
      expect(screen.queryByTestId("JobLogPage-log")).not.toBeInTheDocument();
      expect(
        screen.queryByTestId("JobLogPage-loading"),
      ).not.toBeInTheDocument();
      expect(screen.queryByTestId("JobLogPage-empty")).not.toBeInTheDocument();
    });

    test("Back navigates to /admin/jobs", async () => {
      axiosMock.onGet(ADMIN_URL).reply(200, "line 1");

      renderAt("/admin/jobs/logs/5", queryClient);

      await screen.findByTestId("JobLogPage-log");
      fireEvent.click(screen.getByTestId("JobLogPage-back"));
      expect(mockedNavigate).toHaveBeenCalledTimes(1);
      expect(mockedNavigate).toHaveBeenCalledWith("/admin/jobs");
    });

    test("Refresh refetches the log", async () => {
      axiosMock.onGet(ADMIN_URL).reply(200, "line 1");

      renderAt("/admin/jobs/logs/5", queryClient);

      await screen.findByTestId("JobLogPage-log");
      expect(getRequestsTo(axiosMock, ADMIN_URL)).toHaveLength(1);

      fireEvent.click(screen.getByTestId("JobLogPage-refresh"));

      await waitFor(() =>
        expect(getRequestsTo(axiosMock, ADMIN_URL)).toHaveLength(2),
      );
    });
  });

  describe("instructor route /instructor/courses/:courseId/jobs/:jobId/logs", () => {
    test("fetches the course-scoped endpoint and shows the full log", async () => {
      axiosMock.onGet(COURSE_URL).reply(200, "course line 1\ncourse line 2");

      renderAt("/instructor/courses/3/jobs/7/logs", queryClient);

      expect(screen.getByTestId("JobLogPage-heading")).toHaveTextContent(
        "Job Log for Job 7",
      );
      expect(screen.getByTestId("JobLogPage-loading")).toHaveTextContent(
        "Loading...",
      );

      const pre = await screen.findByTestId("JobLogPage-log");
      expect(pre.textContent).toBe("course line 1\ncourse line 2");
      expect(pre).toHaveStyle({ whiteSpace: "pre-wrap" });

      const requests = getRequestsTo(axiosMock, COURSE_URL);
      expect(requests).toHaveLength(1);
      expect(requests[0].params).toEqual({ courseId: "3", jobId: "7" });
      expect(getRequestsTo(axiosMock, "/api/jobs/logs/7")).toHaveLength(0);
      expect(getRequestsTo(axiosMock, "/api/jobs/logs/3")).toHaveLength(0);
      expect(queryClient.getQueryData([COURSE_URL, "3", "7"])).toBe(
        "course line 1\ncourse line 2",
      );
    });

    test("calls useBackend with the exact course query key and axios params", () => {
      const useBackendSpy = vi
        .spyOn(useBackendModule, "useBackend")
        .mockReturnValue({ data: "x", isError: false, refetch: vi.fn() });

      renderAt("/instructor/courses/3/jobs/7/logs", queryClient);

      expect(useBackendSpy).toHaveBeenCalledWith([COURSE_URL, "3", "7"], {
        method: "GET",
        url: COURSE_URL,
        params: { courseId: "3", jobId: "7" },
      });
      useBackendSpy.mockRestore();
    });

    test("shows 'No log lines yet.' when the log is empty", async () => {
      axiosMock.onGet(COURSE_URL).reply(200, "");

      renderAt("/instructor/courses/3/jobs/7/logs", queryClient);

      expect(await screen.findByTestId("JobLogPage-empty")).toHaveTextContent(
        "No log lines yet.",
      );
    });

    test("shows an error message when the job is not found for this course", async () => {
      axiosMock.onGet(COURSE_URL).reply(404, {
        type: "EntityNotFoundException",
        message: "Job with id 7 not found",
      });

      renderAt("/instructor/courses/3/jobs/7/logs", queryClient);

      expect(await screen.findByTestId("JobLogPage-error")).toHaveTextContent(
        "Error loading log.",
      );
    });

    test("Back navigates to the course page", async () => {
      axiosMock.onGet(COURSE_URL).reply(200, "course line 1");

      renderAt("/instructor/courses/3/jobs/7/logs", queryClient);

      await screen.findByTestId("JobLogPage-log");
      fireEvent.click(screen.getByTestId("JobLogPage-back"));
      expect(mockedNavigate).toHaveBeenCalledTimes(1);
      expect(mockedNavigate).toHaveBeenCalledWith("/instructor/courses/3");
    });

    test("Refresh refetches the log", async () => {
      axiosMock.onGet(COURSE_URL).reply(200, "course line 1");

      renderAt("/instructor/courses/3/jobs/7/logs", queryClient);

      await screen.findByTestId("JobLogPage-log");
      expect(getRequestsTo(axiosMock, COURSE_URL)).toHaveLength(1);

      fireEvent.click(screen.getByTestId("JobLogPage-refresh"));

      await waitFor(() =>
        expect(getRequestsTo(axiosMock, COURSE_URL)).toHaveLength(2),
      );
      expect(getRequestsTo(axiosMock, COURSE_URL)[1].params).toEqual({
        courseId: "3",
        jobId: "7",
      });
    });
  });
});
