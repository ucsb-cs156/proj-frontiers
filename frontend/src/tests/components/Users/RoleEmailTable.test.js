import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import { fireEvent, render, waitFor, screen } from "@testing-library/react";
import { roleEmailFixtures } from "fixtures/roleEmailFixtures";
import { BrowserRouter as Router } from "react-router-dom";
import { MemoryRouter } from "react-router-dom";
import RoleEmailTable from "main/components/Users/RoleEmailTable";
import { currentUserFixtures } from "fixtures/currentUserFixtures";
import { QueryClient, QueryClientProvider } from "react-query";
const queryClient = new QueryClient();

describe("RoleEmailTable tests", () => {
  test("renders without crashing for three roleemails", () => {
    const currentUser = currentUserFixtures.userOnly;
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <RoleEmailTable
            roleemails={roleEmailFixtures.threeRoleEmails}
            currentUser={currentUser}
          />
        </Router>
      </QueryClientProvider>,
    );
    const testId = "RoleEmailTable";

    expect(screen.getByText("Email")).toBeInTheDocument();
    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-${"email"}`),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-email`),
    ).toHaveTextContent("joegaucho@ucsb.edu");
  });

  test("Has the expected column headers and content for ordinary user", () => {
    const currentUser = currentUserFixtures.userOnly;
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <RoleEmailTable
            roleemails={roleEmailFixtures.threeRoleEmails}
            currentUser={currentUser}
          />
        </Router>
      </QueryClientProvider>,
    );

    const testId = "RoleEmailTable";

    expect(screen.getByText("Email")).toBeInTheDocument();

    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-${"email"}`),
    ).toBeInTheDocument();

    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-email`),
    ).toHaveTextContent("joegaucho@ucsb.edu");
    expect(screen.queryByText("Delete")).not.toBeInTheDocument();
  });

  test("Has the expected column headers and content for admin user", () => {
    const currentUser = currentUserFixtures.adminUser;
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <RoleEmailTable
            roleemails={roleEmailFixtures.threeRoleEmails}
            currentUser={currentUser}
          />
        </Router>
      </QueryClientProvider>,
    );

    const testId = "RoleEmailTable";
    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-email`),
    ).toBeInTheDocument();

    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-email`),
    ).toHaveTextContent("joegaucho@ucsb.edu");
    const deleteButton = screen.getByTestId(
      `${testId}-cell-row-0-col-Delete-button`,
    );
    expect(deleteButton).toBeInTheDocument();
    expect(deleteButton).toHaveClass("btn-danger");
  });

  test("Delete button calls delete callback", async () => {
    // arrange
    const currentUser = currentUserFixtures.adminUser;
    const testId = "RoleEmailTable";
    const axiosMock = new AxiosMockAdapter(axios);
    axiosMock
      .onDelete("/api/roleemails")
      .reply(200, { message: "User deleted" });

    // act - render the component
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <RoleEmailTable
            roleemails={roleEmailFixtures.threeRoleEmails}
            currentUser={currentUser}
          />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    // assert - check that the expected content is rendered
    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-email`),
    ).toHaveTextContent("joegaucho@ucsb.edu");

    const deleteButton = screen.getByTestId(
      `${testId}-cell-row-0-col-Delete-button`,
    );
    expect(deleteButton).toBeInTheDocument();

    // act - click the delete button
    fireEvent.click(deleteButton);

    // assert - check that the delete endpoint was called

    await waitFor(() => expect(axiosMock.history.delete.length).toBe(1));
    expect(axiosMock.history.delete[0].url).toBe("/api/roleemails");
    expect(axiosMock.history.delete[0].params).toEqual({
      id: "joegaucho@ucsb.edu",
    });
  });
});
