import { render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "react-query";
import {  emoryRouter } from "react-router-dom";

import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import coursesFixtures from "fixtures/coursesFixtures";
import HomePageLoggedIn from "main/pages/HomePageLoggedIn";

let axiosMock = new AxiosMockAdapter(axios);

describe("HomePageLoggedIn tests", () => {
  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
  });

  const queryClient = new QueryClient();

  test("tables render correctly", async () => {
    axiosMock
      .onGet("/api/courses/staffCourses")
      .reply(200, coursesFixtures.oneCourseWithEachStatus);
    axiosMock
      .onGet("/api/courses/list")
      .reply(200, coursesFixtures.oneCourseWithEachStatus);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <HomePageLoggedIn />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await waitFor(() => {
      expect(
        screen.getByTestId(`CoursesTable-cell-row-0-col-id`),
      ).toHaveTextContent("1");
    });
    expect(
      screen.getByTestId(`CoursesTable-cell-row-1-col-id`),
    ).toHaveTextContent("2");
    expect(
      screen.getByTestId(`CoursesTable-cell-row-2-col-id`),
    ).toHaveTextContent("3");
    expect(
      screen.getByTestId(`CoursesTable-cell-row-0-col-courseName`),
    ).toHaveTextContent("CMPSC 156");
    expect(
      screen.getByTestId(`CoursesTable-cell-row-0-col-term`),
    ).toHaveTextContent("Spring 2025");
    expect(
      screen.getByTestId(`CoursesTable-cell-row-0-col-school`),
    ).toHaveTextContent("UCSB");
    expect(
      screen.getByTestId(`CoursesTable-cell-row-0-col-studentStatus`),
    ).toHaveTextContent("Pending");

    await waitFor(() => {
      expect(
        screen.getByTestId(`StaffCoursesTable-cell-row-0-col-id`),
      ).toHaveTextContent("1");
    });
    expect(
      screen.getByTestId(`StaffCoursesTable-cell-row-1-col-id`),
    ).toHaveTextContent("2");
    expect(
      screen.getByTestId(`StaffCoursesTable-cell-row-2-col-id`),
    ).toHaveTextContent("3");
    expect(
      screen.getByTestId(`StaffCoursesTable-cell-row-0-col-courseName`),
    ).toHaveTextContent("CMPSC 156");
    expect(
      screen.getByTestId(`StaffCoursesTable-cell-row-0-col-term`),
    ).toHaveTextContent("Spring 2025");
    expect(
      screen.getByTestId(`StaffCoursesTable-cell-row-0-col-school`),
    ).toHaveTextContent("UCSB");
    expect(
      screen.getByTestId(`StaffCoursesTable-cell-row-0-col-studentStatus`),
    ).toHaveTextContent("Pending");
  });
});
