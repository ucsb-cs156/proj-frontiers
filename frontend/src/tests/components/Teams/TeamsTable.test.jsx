import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { teamsFixtures } from "fixtures/TeamsFixtures";
import TeamsTable from "main/components/Teams/TeamsTable";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router";
import { currentUserFixtures } from "fixtures/currentUserFixtures";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import { expect, vi } from "vitest";

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
  const expectedHeaders = [
    "rosterStudent.firstName",
    "rosterStudent.lastName",
    "rosterStudent.email",
    "rosterStudent.githubLogin",
  ];
  const expectedFields = ["First Name", "Last Name", "Email", "GitHub Login"];

  const testId = "TeamsTable";

  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    queryClient.clear();
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
  test("delete team calls correct API endpoint with correct params", async () => {
    axiosMock.onDelete("/api/teams").reply(200, {});

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <TeamsTable
            teams={teamsFixtures.teams}
            currentUser={currentUserFixtures.adminUser}
            courseId="12"
            testIdPrefix="TeamsTable"
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
  });
  test("delete member calls correct API endpoint with correct params", async () => {
    axiosMock.onDelete("/api/teams/removeMember").reply(200, {});

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
      "TeamsTable-cell-row-0-col-Remove-button",
    );

    fireEvent.click(deleteButton);

    await waitFor(() => {
      expect(axiosMock.history.delete.length).toBe(1);
    });

    expect(axiosMock.history.delete[0].url).toBe("/api/teams/removeMember");
    expect(axiosMock.history.delete[0].params).toEqual({
      teamMemberId: 3,
      courseId: "12",
    });
  });
});
