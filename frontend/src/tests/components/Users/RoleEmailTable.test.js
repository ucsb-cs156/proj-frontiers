import React from "react";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import RoleEmailTable from "main/components/Users/RoleEmailTable";
import { QueryClient, QueryClientProvider } from "react-query";
import { MemoryRouter } from "react-router";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import { roleEmailFixtures } from "fixtures/roleEmailFixtures";

const testData = [
  { email: "user1@example.org" },
  { email: "user2@example.org" },
];

const mockDeleteCallback = jest.fn();

const axiosMock = new AxiosMockAdapter(axios);

describe("RoleEmailTable", () => {
  let queryClient;
  let invalidateQueriesSpy;

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
          refetchOnWindowFocus: false,
        },
      },
    });
    invalidateQueriesSpy = jest.spyOn(queryClient, "invalidateQueries");
  });

  // After each test, restore the original axios implementation and clean up.
  afterEach(() => {
    invalidateQueriesSpy.mockRestore(); // Restore original implementation of the spy
    axiosMock.restore(); // Restore axios to its original state (remove mocks)
    queryClient.clear(); // Clear the React Query cache
  });

  test("renders empty table correctly when data is null", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <RoleEmailTable data={null} deleteCallback={mockDeleteCallback} />,
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(screen.getByText("Email")).toBeInTheDocument();
    expect(screen.queryByRole("button")).not.toBeInTheDocument();
    expect(
      screen.queryByTestId("RoleEmailTable-cell-row-0-col-email"),
    ).not.toBeInTheDocument();
  });

  test("renders table with email data", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <RoleEmailTable data={testData} deleteCallback={mockDeleteCallback} />
          ,
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(screen.getByText("Email")).toBeInTheDocument();
    expect(screen.getByText("user1@example.org")).toBeInTheDocument();
    expect(screen.getByText("user2@example.org")).toBeInTheDocument();

    const deleteButtons = screen.getAllByRole("button", { name: /delete/i });
    expect(deleteButtons.length).toBe(testData.length);
  });

  test("renders table with email data plus isInAdminEmails", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <RoleEmailTable
            data={roleEmailFixtures.threeItemsWithIsInAdminEmailField}
            deleteCallback={mockDeleteCallback}
          />
          ,
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(screen.getByText("Email")).toBeInTheDocument();
    const deleteHeader = screen.getByTestId(
      "RoleEmailTable-header-isInAdminEmails",
    );
    expect(deleteHeader).toBeInTheDocument();
    expect(deleteHeader).toHaveTextContent("Delete");
    expect(
      screen.getByTestId("RoleEmailTable-cell-row-0-cannot-delete"),
    ).toBeInTheDocument();
  });

  test("calls deleteCallback when Delete button clicked", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <RoleEmailTable
            data={testData}
            customDeleteCallback={mockDeleteCallback}
          />
          ,
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const buttons = screen.getAllByRole("button");
    expect(buttons.length).toBe(testData.length);

    fireEvent.click(buttons[0]);

    expect(mockDeleteCallback).toHaveBeenCalledTimes(1);
  });

  test("renders Delete button with correct label", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <RoleEmailTable data={testData} deleteCallback={mockDeleteCallback} />
          ,
        </MemoryRouter>
      </QueryClientProvider>,
    );
    const deleteButtons = screen.getAllByRole("button", { name: /delete/i });
    expect(deleteButtons.length).toBe(testData.length);
    deleteButtons.forEach((btn) => {
      expect(btn).toHaveTextContent(/delete/i);
    });
  });

  test("renders table with correct testid", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <RoleEmailTable data={testData} deleteCallback={mockDeleteCallback} />
          ,
        </MemoryRouter>
      </QueryClientProvider>,
    );
    expect(
      screen.getByTestId("RoleEmailTable-header-email"),
    ).toBeInTheDocument();
  });

  test("Delete button has correct Bootstrap class", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <RoleEmailTable data={testData} deleteCallback={mockDeleteCallback} />
          ,
        </MemoryRouter>
      </QueryClientProvider>,
    );
    const deleteButtons = screen.getAllByRole("button", { name: /delete/i });
    deleteButtons.forEach((btn) => {
      expect(btn).toHaveClass("btn-danger");
    });
  });

  test("Delete button has correct data-testid using table ID prefix", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <RoleEmailTable data={testData} deleteCallback={mockDeleteCallback} />
          ,
        </MemoryRouter>
      </QueryClientProvider>,
    );
    const deleteButton = screen.getByTestId(
      "RoleEmailTable-cell-row-0-col-delete-button",
    );
    expect(deleteButton).toBeInTheDocument();
  });

  test("Check defaults for api endpoints", async () => {
    var axiosMock = new AxiosMockAdapter(axios);

    axiosMock.onDelete("/api/admin/delete").reply(200, {});

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <RoleEmailTable data={testData} />,
        </MemoryRouter>
      </QueryClientProvider>,
    );
    const deleteButton = screen.getByTestId(
      "RoleEmailTable-cell-row-0-col-delete-button",
    );
    expect(deleteButton).toBeInTheDocument();
    fireEvent.click(deleteButton);
    await waitFor(() => {
      expect(axiosMock.history.delete.length).toBe(1);
    });
    expect(axiosMock.history.delete[0].url).toBe("/api/admin/delete");
    expect(axiosMock.history.delete[0].params).toEqual({
      email: "user1@example.org",
    });
    expect(invalidateQueriesSpy).toHaveBeenCalledTimes(1);
    expect(invalidateQueriesSpy).toHaveBeenCalledWith(["/api/admin/all"]);
  });

  test("Check that api endpoint overrides work", async () => {
    var axiosMock = new AxiosMockAdapter(axios);

    axiosMock.onDelete("/api/admin/instructor/delete").reply(200, {});

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <RoleEmailTable
            data={testData}
            deleteEndpoint="/api/admin/instructor/delete"
            getEndpoint="/api/admin/instructor/all"
          />
          ,
        </MemoryRouter>
      </QueryClientProvider>,
    );
    const deleteButton = screen.getByTestId(
      "RoleEmailTable-cell-row-0-col-delete-button",
    );
    expect(deleteButton).toBeInTheDocument();
    fireEvent.click(deleteButton);
    await waitFor(() => {
      expect(axiosMock.history.delete.length).toBe(1);
    });
    expect(axiosMock.history.delete[0].url).toBe(
      "/api/admin/instructor/delete",
    );
    expect(axiosMock.history.delete[0].params).toEqual({
      email: "user1@example.org",
    });

    expect(invalidateQueriesSpy).toHaveBeenCalledTimes(1);
    expect(invalidateQueriesSpy).toHaveBeenCalledWith([
      "/api/admin/instructor/all",
    ]);
  });
});
