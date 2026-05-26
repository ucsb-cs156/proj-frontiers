import { render, screen, waitFor } from "@testing-library/react";
import StudentCourseShowPage from "main/pages/Student/StudentCourseShowPage";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router";
import coursesFixtures from "fixtures/coursesFixtures";

import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import { vi } from "vitest";

const mockToast = vi.fn();
vi.mock("react-toastify", async (importOriginal) => {
  return {
    ...(await importOriginal()),
    toast: (x) => mockToast(x),
  };
});

const axiosMock = new AxiosMockAdapter(axios);
const queryClient = new QueryClient();

describe("StudentCourseShowPage tests", () => {
  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    queryClient.clear();
    mockToast.mockReset();
    axiosMock
      .onGet("/api/currentUser")
      .reply(200, apiCurrentUserFixtures.userOnly);
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.showingNeither);
  });

  test("renders loading state before course data arrives", async () => {
    const noRetryQueryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    axiosMock.onGet("/api/courses/1").timeout();

    render(
      <QueryClientProvider client={noRetryQueryClient}>
        <MemoryRouter initialEntries={["/student/courses/1"]}>
          <Routes>
            <Route
              path="/student/courses/:id"
              element={<StudentCourseShowPage />}
            />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(noRetryQueryClient.getQueryData(["/api/courses/1"])).toBe(null);

    expect(
      screen.getByTestId("StudentCourseShowPage-loading"),
    ).toHaveTextContent("Course: Loading...");

    await waitFor(() => {
      expect(noRetryQueryClient.getQueryState(["/api/courses/1"])?.status).toBe(
        "error",
      );
    });

    expect(mockToast).not.toHaveBeenCalled();
  });

  test("renders course name and term when data loads", async () => {
    const theCourse = coursesFixtures.oneCourseWithEachStatus[0];
    axiosMock.onGet("/api/courses/1").reply(200, theCourse);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/student/courses/1"]}>
          <Routes>
            <Route
              path="/student/courses/:id"
              element={<StudentCourseShowPage />}
            />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await waitFor(() => {
      expect(
        screen.getByTestId("StudentCourseShowPage-title"),
      ).toHaveTextContent("CMPSC 156");
    });

    expect(screen.getByText("Spring 2025")).toBeInTheDocument();
  });

  test("renders Placeholder tab with correct text", async () => {
    const theCourse = coursesFixtures.oneCourseWithEachStatus[0];
    axiosMock.onGet("/api/courses/1").reply(200, theCourse);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/student/courses/1"]}>
          <Routes>
            <Route
              path="/student/courses/:id"
              element={<StudentCourseShowPage />}
            />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(
      screen.getByRole("tab", { name: "Placeholder" }),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId("StudentCourseShowPage-placeholder-text"),
    ).toHaveTextContent("More features coming soon");
  });
});
