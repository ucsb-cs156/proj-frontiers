import { render, screen, waitFor } from "@testing-library/react";
import StudentCourseShowPage from "main/pages/Student/StudentCourseShowPage";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router";
import coursesFixtures from "fixtures/coursesFixtures";

import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import { expect } from "vitest";

const axiosMock = new AxiosMockAdapter(axios);
const queryClient = new QueryClient();

describe("StudentCourseShowPage tests", () => {
  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    queryClient.clear();
    axiosMock
      .onGet("/api/currentUser")
      .reply(200, apiCurrentUserFixtures.userOnly);
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.showingNeither);
  });

  test("renders course header and placeholder tab", async () => {
    axiosMock.onGet("/api/courses/1").reply(200, {
      ...coursesFixtures.oneCourseWithEachStatus[0],
      id: 1,
    });

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
      expect(screen.getByTestId("StudentCourseShowPage-title")).toHaveTextContent(
        "CMPSC 156",
      );
    });

    expect(screen.getByText("Spring 2025")).toBeInTheDocument();
    expect(screen.getByText("Placeholder")).toHaveAttribute(
      "data-rr-ui-event-key",
      "placeholder",
    );
    expect(screen.getByText("More features coming soon")).toBeInTheDocument();
  });
});
