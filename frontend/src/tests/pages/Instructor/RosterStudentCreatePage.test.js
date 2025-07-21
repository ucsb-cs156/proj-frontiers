import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import RosterStudentCreatePage from "main/pages/Instructor/RosterStudentCreatePage";
import { QueryClient, QueryClientProvider } from "react-query";
import { MemoryRouter, Route, Routes } from "react-router-dom";

import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import coursesFixtures from "fixtures/coursesFixtures";

import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";

const mockToast = jest.fn();
jest.mock("react-toastify", () => {
  const originalModule = jest.requireActual("react-toastify");
  return {
    __esModule: true,
    ...originalModule,
    toast: (x) => mockToast(x),
  };
});

const mockNavigate = jest.fn();
jest.mock("react-router-dom", () => {
  const originalModule = jest.requireActual("react-router-dom");
  return {
    __esModule: true,
    ...originalModule,
    Navigate: (x) => {
      mockNavigate(x);
      return null;
    },
  };
});

let axiosMock;

describe("RosterStudentCreatePage tests", () => {
  beforeAll(() => {
    axiosMock = new AxiosMockAdapter(axios);
  });

  beforeEach(() => {
    jest.clearAllMocks();
    axiosMock
      .onGet("/api/currentUser")
      .reply(200, apiCurrentUserFixtures.userOnly);
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.showingNeither);
  });

  afterEach(() => {
    axiosMock.restore();
  });

  const setupInstructorUser = () => {
    axiosMock
      .onGet("/api/currentUser")
      .reply(200, apiCurrentUserFixtures.instructorUser);
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.showingNeither);
  };

  const queryClient = new QueryClient();
  test("renders correctly for Instructor", async () => {
    setupInstructorUser();
    const queryClient = new QueryClient();
    const theCourse = {
      ...coursesFixtures.severalCourses[0],
      id: 1,
      createdByEmail: "phtcon@ucsb.edu",
    };

    axiosMock.onGet("/api/courses/1").reply(200, theCourse);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter
          initialEntries={["/instructor/courses/1/rosterstudents/create"]}
        >
          <Routes>
            <Route
              path="/instructor/courses/:id/rosterstudents/create"
              element={<RosterStudentCreatePage />}
            />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await waitFor(() => {
      expect(screen.getByText("Create New Roster Student")).toBeInTheDocument();
    });

    expect(screen.getByText("Loading...")).toBeInTheDocument();

    const headers = ["Student Id", "First Name", "Last Name", "Email"];
    headers.forEach((header) => {
      expect(screen.getByText(header)).toBeInTheDocument();
    });
  });

  // test("on submit, makes request to backend, and redirects to /restaurants", async () => {
  //   const queryClient = new QueryClient();
  //   const restaurant = {
  //     id: 3,
  //     name: "South Coast Deli",
  //     description: "Sandwiches and Salads",
  //   };

  //   axiosMock.onPost("/api/restaurants/post").reply(202, restaurant);

  //   render(
  //     <QueryClientProvider client={queryClient}>
  //       <MemoryRouter>
  //         <RestaurantCreatePage />
  //       </MemoryRouter>
  //     </QueryClientProvider>,
  //   );

  //   await waitFor(() => {
  //     expect(screen.getByLabelText("Name")).toBeInTheDocument();
  //   });

  //   const nameInput = screen.getByLabelText("Name");
  //   expect(nameInput).toBeInTheDocument();

  //   const descriptionInput = screen.getByLabelText("Description");
  //   expect(descriptionInput).toBeInTheDocument();

  //   const createButton = screen.getByText("Create");
  //   expect(createButton).toBeInTheDocument();

  //   fireEvent.change(nameInput, { target: { value: "South Coast Deli" } });
  //   fireEvent.change(descriptionInput, {
  //     target: { value: "Sandwiches and Salads" },
  //   });
  //   fireEvent.click(createButton);

  //   await waitFor(() => expect(axiosMock.history.post.length).toBe(1));

  //   expect(axiosMock.history.post[0].params).toEqual({
  //     name: "South Coast Deli",
  //     description: "Sandwiches and Salads",
  //   });

  //   // assert - check that the toast was called with the expected message
  //   expect(mockToast).toBeCalledWith(
  //     "New restaurant Created - id: 3 name: South Coast Deli",
  //   );
  //   expect(mockNavigate).toBeCalledWith({ to: "/restaurants" });
  // });
});
