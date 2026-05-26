import { render, screen } from "@testing-library/react";
import StudentCourseShowPage from "main/pages/Student/StudentCourseShowPage";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router";
import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import { expect, vi } from "vitest";

const axiosMock = new AxiosMockAdapter(axios);
const queryClient = new QueryClient();

describe("StudentCourseShowPage tests", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    axiosMock.reset();
    axiosMock.resetHistory();
    queryClient.clear();
  });

  const setupStudentUser = () => {
    axiosMock
      .onGet("/api/currentUser")
      .reply(200, apiCurrentUserFixtures.userOnly);
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.showingNeither);
  };

  test("renders correctly with student user", async () => {
    setupStudentUser();

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/student/courses/123"]}>
          <Routes>
            <Route
              path="/student/courses/:id"
              element={<StudentCourseShowPage />}
            />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    );

    expect(await screen.findByText("Student Course: 123")).toBeInTheDocument();

    expect(screen.getByRole("tab", { name: "Placeholder" })).toBeInTheDocument();

    expect(screen.getByText("More features coming soon.")).toBeInTheDocument();
  });
});