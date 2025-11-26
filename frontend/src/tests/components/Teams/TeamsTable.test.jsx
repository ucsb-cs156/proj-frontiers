import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { teamsFixtures } from "fixtures/TeamsFixtures";
import TeamsTable from "main/components/Teams/TeamsTable";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router";
import { currentUserFixtures } from "fixtures/currentUserFixtures";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import { expect, test, vi } from "vitest";
import { rosterStudentFixtures } from "fixtures/rosterStudentFixtures";

const queryClient = new QueryClient();
const axiosMock = new AxiosMockAdapter(axios);
const mockToast = vi.fn();
vi.mock("react-toastify", async (importOriginal) => {
  return {
    ...(await importOriginal()),
    toast: (x) => mockToast(x),
  };
});
describe("TeamsTable tests", () => {
  let invalidateQueriesSpy;

  const expectedHeaders = [
    "rosterStudent.firstName",
    "rosterStudent.lastName",
    "rosterStudent.email",
    "rosterStudent.githubLogin",
    "rosterStudent.id",
  ];
  const expectedFields = [
    "Roster Student ID",
    "First Name",
    "Last Name",
    "Email",
    "GitHub Login",
  ];

  const testId = "TeamsTable";

  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    queryClient.clear();
    mockToast.mockClear();

    invalidateQueriesSpy = vi.spyOn(queryClient, "invalidateQueries");
  });
  test("table renders corretly when there are teams", () => {
    // arrange
    const currentUser = currentUserFixtures.adminUser;

    // act
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <TeamsTable
            teams={[teamsFixtures.teams[0]]}
            currentUser={currentUser}
            courseId="12"
            testIdPrefix={testId}
          />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(screen.getByText("team1")).toBeInTheDocument();

    // assert
    expectedHeaders.forEach((headerText) => {
      expect(
        screen.getByTestId(`${testId}-3-members-table-header-${headerText}`),
      ).toBeInTheDocument();
    });

    expectedFields.forEach((expectedField) => {
      const header = screen.getByText(expectedField);
      expect(header).toBeInTheDocument();
    });
  });
  test("nothing renders when teams are empty", () => {
    // arrange
    const currentUser = currentUserFixtures.adminUser;

    // act
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <TeamsTable
            teams={[]}
            currentUser={currentUser}
            courseId="12"
            testIdPrefix={testId}
          />
        </MemoryRouter>
      </QueryClientProvider>,
    );
    // assert
    expectedHeaders.forEach((headerText) => {
      expect(
        screen.queryByText(`${testId}-3-members-table-header-${headerText}`),
      ).not.toBeInTheDocument();
    });
  });
  test("If the user is not an instructor/admin, remove buttons are not rendered", async () => {
    // arrange
    const currentUser = currentUserFixtures.studentUser;

    // act
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <TeamsTable
            teams={[teamsFixtures.teams[0]]}
            currentUser={currentUser}
            courseId="12"
            testIdPrefix={testId}
          />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    // Expand the accordion
    const teamAccordion = screen.getByTestId("TeamsTable-3-name");
    fireEvent.click(teamAccordion);

    expect(
      screen.queryByTestId("TeamsTable-3-cell-row-0-col-Remove-button"),
    ).not.toBeInTheDocument();
  });
  test("delete team calls correct API endpoint with correct params", async () => {
    const successMessage = "Team removed successfully";
    axiosMock.onDelete("/api/teams").reply(200, { successMessage });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <TeamsTable
            teams={teamsFixtures.teams}
            currentUser={currentUserFixtures.adminUser}
            courseId="12"
          />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const deleteButton = screen.getByTestId("TeamsTable-3-delete-button");
    fireEvent.click(deleteButton);

    await waitFor(() => {
      expect(axiosMock.history.delete.length).toBe(1);
    });

    // This verifies cellToAxiosParamDeleteTeam worked correctly
    expect(axiosMock.history.delete[0].url).toBe("/api/teams");
    expect(axiosMock.history.delete[0].params).toEqual({
      id: 3,
      courseId: "12",
    });

    expect(invalidateQueriesSpy).toHaveBeenCalledTimes(1);
    expect(invalidateQueriesSpy).toHaveBeenCalledWith({
      queryKey: ["/api/teams/all?courseId=12"],
    });

    expect(mockToast).toHaveBeenCalledWith(successMessage);
  });
  test("delete member calls correct API endpoint with correct params", async () => {
    const successMessage = "Member removed successfully";
    axiosMock
      .onDelete("/api/teams/removeMember")
      .reply(200, { successMessage });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <TeamsTable
            teams={teamsFixtures.teams}
            currentUser={currentUserFixtures.instructorUser}
            courseId="12"
            testIdPrefix="TeamsTable"
          />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    // Expand the accordion
    const teamAccordion = screen.getByTestId("TeamsTable-3-name");
    fireEvent.click(teamAccordion);

    // Wait for the remove button to appear and get the element
    const deleteButton = await screen.findByTestId(
      "TeamsTable-3-cell-row-0-col-Remove-button",
    );

    expect(deleteButton).toHaveClass("btn-danger");

    fireEvent.click(deleteButton);

    await waitFor(() => {
      expect(axiosMock.history.delete.length).toBe(1);
    });

    expect(axiosMock.history.delete[0].url).toBe("/api/teams/removeMember");
    expect(axiosMock.history.delete[0].params).toEqual({
      teamMemberId: 4,
      courseId: "12",
    });

    expect(invalidateQueriesSpy).toHaveBeenCalledTimes(1);
    expect(invalidateQueriesSpy).toHaveBeenCalledWith({
      queryKey: ["/api/teams/all?courseId=12"],
    });

    expect(mockToast).toHaveBeenCalledWith(successMessage);
  });
  test("add member calls correct API endpoint with correct params", async () => {
    const successMessage = "Member added successfully";
    axiosMock.onPost("/api/teams/addMember").reply(200, { successMessage });

    axiosMock
      .onGet("/api/rosterstudents/course/12")
      .reply(200, rosterStudentFixtures.studentsWithEachStatus);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <TeamsTable
            teams={teamsFixtures.teams}
            currentUser={currentUserFixtures.instructorUser}
            courseId="12"
            testIdPrefix="TeamsTable"
          />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    // Wait for the add button to appear and get the element
    const addButton = await screen.findByTestId(
      "TeamsTable-3-add-member-button",
    );

    expect(addButton).toHaveClass("me-3");

    expect(
      screen.queryByTestId("TeamsTable-post-modal"),
    ).not.toBeInTheDocument();

    fireEvent.click(addButton);

    const input = await screen.findByTestId("RosterStudentDropdown");
    fireEvent.change(input, { target: { value: "Jon" } });

    await waitFor(() => {
      expect(
        screen.getByRole("option", { name: "Jon Snow" }),
      ).toBeInTheDocument();
    });
    fireEvent.click(screen.getByRole("option", { name: "Jon Snow" }));

    expect(screen.queryByTestId("TeamsTable-post-modal")).toBeInTheDocument();

    const submitButton = screen.getByTestId("TeamMemberForm-submit");
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(axiosMock.history.post.length).toBe(1);
    });

    expect(axiosMock.history.post[0].url).toBe("/api/teams/addMember");
    expect(axiosMock.history.post[0].params).toEqual({
      rosterStudentId: 4,
      courseId: "12",
      teamId: 3,
    });

    expect(invalidateQueriesSpy).toHaveBeenCalledTimes(1);
    expect(invalidateQueriesSpy).toHaveBeenCalledWith({
      queryKey: ["/api/teams/all?courseId=12"],
    });

    expect(mockToast).toHaveBeenCalledWith(successMessage);

    await waitFor(() => {
      expect(
        screen.queryByTestId("TeamsTable-post-modal"),
      ).not.toBeInTheDocument();
    });
  });
  test("add member returns 409 error modal when member is already in the team", async () => {
    axiosMock.onPost("/api/teams/addMember").reply(409);

    axiosMock
      .onGet("/api/rosterstudents/course/12")
      .reply(200, rosterStudentFixtures.studentsWithEachStatus);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <TeamsTable
            teams={teamsFixtures.teams}
            currentUser={currentUserFixtures.instructorUser}
            courseId="12"
            testIdPrefix="TeamsTable"
          />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    // Wait for the add button to appear and get the element
    const addButton = await screen.findByTestId(
      "TeamsTable-3-add-member-button",
    );

    expect(addButton).toHaveClass("me-3");

    expect(
      screen.queryByTestId("TeamsTable-post-modal"),
    ).not.toBeInTheDocument();

    fireEvent.click(addButton);

    const input = await screen.findByTestId("RosterStudentDropdown");
    fireEvent.change(input, { target: { value: "Alice" } });

    await waitFor(() => {
      expect(
        screen.getByRole("option", { name: "Alice Brown" }),
      ).toBeInTheDocument();
    });
    fireEvent.click(screen.getByRole("option", { name: "Alice Brown" }));

    expect(screen.queryByTestId("TeamsTable-post-modal")).toBeInTheDocument();

    const submitButton = screen.getByTestId("TeamMemberForm-submit");
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(
        screen.queryByTestId("TeamsTable-post-modal"),
      ).not.toBeInTheDocument();
    });

    await waitFor(() => {
      expect(
        screen.getByTestId(`${testId}-error-post-member-modal`),
      ).toBeInTheDocument();
    });

    expect(screen.getByTestId(`${testId}-error-post-member-modal`)).toHaveClass(
      "modal-dialog modal-dialog-centered",
    );

    expect(
      screen.getByTestId(`${testId}-error-post-member-modal`),
    ).toHaveTextContent("This member is already in this team.");

    const closeButton = screen.getByLabelText("Close");
    fireEvent.click(closeButton);

    await waitFor(() => {
      expect(
        screen.queryByTestId(`${testId}-error-post-member-modal`),
      ).not.toBeInTheDocument();
    });
  });
  test("add member returns generic error modal an unexpected error occurs", async () => {
    axiosMock.onPost("/api/teams/addMember").reply(500);

    axiosMock
      .onGet("/api/rosterstudents/course/12")
      .reply(200, rosterStudentFixtures.studentsWithEachStatus);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <TeamsTable
            teams={teamsFixtures.teams}
            currentUser={currentUserFixtures.instructorUser}
            courseId="12"
            testIdPrefix="TeamsTable"
          />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    // Wait for the add button to appear and get the element
    const addButton = await screen.findByTestId(
      "TeamsTable-3-add-member-button",
    );

    expect(addButton).toHaveClass("me-3");

    expect(
      screen.queryByTestId("TeamsTable-post-modal"),
    ).not.toBeInTheDocument();

    fireEvent.click(addButton);

    const input = await screen.findByTestId("RosterStudentDropdown");
    expect(input).toBeInTheDocument();
    fireEvent.change(input, { target: { value: "Alice" } });

    await waitFor(() => {
      expect(
        screen.getByRole("option", { name: "Alice Brown" }),
      ).toBeInTheDocument();
    });
    fireEvent.click(screen.getByRole("option", { name: "Alice Brown" }));

    expect(screen.queryByTestId("TeamsTable-post-modal")).toBeInTheDocument();

    const submitButton = screen.getByTestId("TeamMemberForm-submit");
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(
        screen.queryByTestId("TeamsTable-post-modal"),
      ).not.toBeInTheDocument();
    });

    await waitFor(() => {
      expect(
        screen.getByTestId(`${testId}-error-post-member-modal`),
      ).toBeInTheDocument();
    });

    expect(screen.getByTestId(`${testId}-error-post-member-modal`)).toHaveClass(
      "modal-dialog modal-dialog-centered",
    );

    expect(
      screen.getByTestId(`${testId}-error-post-member-modal`),
    ).toHaveTextContent("500 error occurred while adding member to the team.");

    const closeButton = screen.getByLabelText("Close");
    fireEvent.click(closeButton);

    await waitFor(() => {
      expect(
        screen.queryByTestId(`${testId}-error-post-member-modal`),
      ).not.toBeInTheDocument();
    });
  });
  test("modal closes when onHide is triggered", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <TeamsTable
            teams={teamsFixtures.teams}
            currentUser={currentUserFixtures.instructorUser}
            courseId="12"
            testIdPrefix="TeamsTable"
          />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const addButton = await screen.findByTestId(
      "TeamsTable-3-add-member-button",
    );

    expect(addButton).toHaveClass("me-3");

    expect(
      screen.queryByTestId("TeamsTable-post-modal"),
    ).not.toBeInTheDocument();

    fireEvent.click(addButton);

    // modal should be visible
    expect(screen.getByTestId("TeamsTable-post-modal")).toBeInTheDocument();

    // trigger the bootstrap onHide
    fireEvent.click(screen.getByLabelText("Close"));

    await waitFor(() => {
      expect(
        screen.queryByTestId("TeamsTable-post-modal"),
      ).not.toBeInTheDocument();
    });
  });
  test("TeamsForm submit works and clears search filter", async () => {
    axiosMock.onPost("/api/teams/post").reply(200);
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <TeamsTable
            teams={teamsFixtures.teams}
            currentUser={currentUserFixtures.instructorUser}
            courseId="12"
            testIdPrefix="TeamsTable"
          />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const addButton = await screen.findByTestId(
      "TeamsTable-3-add-member-button",
    );
    expect(addButton).toHaveClass("me-3");

    expect(
      screen.queryByTestId(`${testId}-post-modal`),
    ).not.toBeInTheDocument();

    fireEvent.click(addButton);
    await screen.findByTestId("RosterStudentDropdown");

    expect(screen.getByTestId(`${testId}-post-modal`)).toHaveClass(
      "modal-dialog modal-dialog-centered",
    );
  });

  test("Uses correct query key for roster students", async () => {
    const queryClientSpecific = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
          staleTime: Infinity,
        },
      },
    });
    axiosMock.onGet("/api/rosterstudents/course/12").reply(200, []);

    render(
      <QueryClientProvider client={queryClientSpecific}>
        <MemoryRouter>
          <TeamsTable
            teams={teamsFixtures.teams}
            currentUser={currentUserFixtures.instructorUser}
            courseId="12"
            testIdPrefix="TeamsTable"
          />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    //Great time to check initial values
    expect(
      queryClientSpecific.getQueryData(["/api/rosterstudents/course/12"]),
    ).toStrictEqual([]);
  });
});
