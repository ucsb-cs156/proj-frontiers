import { render, screen, waitFor } from "@testing-library/react";
// import HomePageLoggedOut from "main/pages/HomePageLoggedOut";
import { QueryClient, QueryClientProvider } from "react-query";
import { MemoryRouter } from "react-router-dom";

// import { apiCurrentUserFixtures, currentUserFixtures } from "fixtures/currentUserFixtures";
// import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
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

//   const setupInstructorUser = () => {
//     axiosMock
//       .onGet("/api/currentUser")
//       .reply(200, apiCurrentUserFixtures.instructorUser);
//     axiosMock
//       .onGet("/api/systemInfo")
//       .reply(200, systemInfoFixtures.showingNeither);
//   };

//   const setupAdminUser = () => {
//     axiosMock
//       .onGet("/api/currentUser")
//       .reply(200, apiCurrentUserFixtures.adminUser);
//     axiosMock
//       .onGet("/api/systemInfo")
//       .reply(200, systemInfoFixtures.showingNeither);
//   };

//     const setupUser = () => {
//     axiosMock
//       .onGet("/api/currentUser")
//       .reply(200, currentUserFixtures.userOnly);
//     axiosMock
//       .onGet("/api/systemInfo")
//       .reply(200, systemInfoFixtures.showingNeither);
//   };

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
        expect(screen.getByTestId(`CoursesTable-cell-row-1-col-id`)).toHaveTextContent(
            "2",
        );
        expect(screen.getByTestId(`CoursesTable-cell-row-2-col-id`)).toHaveTextContent(
        "3",
        );
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
        expect(screen.getByTestId(`StaffCoursesTable-cell-row-1-col-id`)).toHaveTextContent(
            "2",
        );
        expect(screen.getByTestId(`StaffCoursesTable-cell-row-2-col-id`)).toHaveTextContent(
        "3",
        );
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