import { render, screen, waitFor } from "@testing-library/react";
import StudentCourseShowPage from "main/pages/Student/StudentCourseShowPage";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router";
import coursesFixtures from "fixtures/coursesFixtures";
import { teamsFixtures } from "fixtures/TeamsFixtures";

import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import { afterEach, expect, vi } from "vitest";
import * as useBackendModule from "main/utils/useBackend";

const axiosMock = new AxiosMockAdapter(axios);
const queryClient = new QueryClient();
const useBackendSpy = vi.spyOn(useBackendModule, "useBackend");

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

  afterEach(() => {
    useBackendSpy.mockClear();
  });

  test("renders loading message", async () => {
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
      screen.getByTestId("StudentCourseShowPage-loading"),
    ).toHaveTextContent("Course: Loading...");
  });

  test("renders course header and my teams tab", async () => {
    axiosMock.onGet("/api/courses/1").reply(200, {
      ...coursesFixtures.oneCourseWithEachStatus[0],
      id: 1,
    });
    axiosMock
      .onGet("/api/teams/all?courseId=1")
      .reply(200, teamsFixtures.teams);

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
    expect(screen.getByText("My Teams")).toHaveAttribute(
      "data-rr-ui-event-key",
      "teams",
    );
    expect(screen.getByRole("tab", { name: "My Teams" })).toHaveAttribute(
      "aria-selected",
      "true",
    );
    expect(screen.queryByText("Placeholder")).not.toBeInTheDocument();
    expect(
      screen.queryByText("More features coming soon"),
    ).not.toBeInTheDocument();
    expect(await screen.findByText("team1")).toBeInTheDocument();
    expect(screen.getByText("team2")).toBeInTheDocument();
    expect(screen.queryByText("Roster Student ID")).not.toBeInTheDocument();
    expect(screen.getAllByText("GitHub ID")[0]).toBeInTheDocument();
    expect(screen.queryByText("GitHub Login")).not.toBeInTheDocument();
    expect(
      screen.queryByTestId("StudentCourseShowPage-post-button"),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByTestId("StudentCourseShowPage-csv-button"),
    ).not.toBeInTheDocument();
    expect(useBackendSpy).toHaveBeenCalledWith(
      ["/api/courses/1"],
      { method: "GET", url: "/api/courses/1" },
      null,
      true,
    );
    expect(useBackendSpy).toHaveBeenCalledWith(
      ["/api/teams/all?courseId=1"],
      { method: "GET", url: "/api/teams/all?courseId=1" },
      [],
      true,
    );
  });
});
