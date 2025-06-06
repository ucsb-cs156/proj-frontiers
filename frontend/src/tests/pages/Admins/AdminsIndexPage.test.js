import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import AdminsIndexPage from "main/pages/Admins/AdminsIndexPage";
import { QueryClient, QueryClientProvider } from "react-query";
import { MemoryRouter } from "react-router-dom";
import mockConsole from "jest-mock-console";
import { roleEmailFixtures } from "fixtures/roleEmailFixtures";

import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";

const mockToast = jest.fn();

jest.mock("react-toastify", () => {
  const originalModule = jest.requireActual("react-toastify");

  const mockToastFn = (msg) => mockToast(msg);
  mockToastFn.error = (msg) => mockToast(msg);

  return {
    __esModule: true,
    ...originalModule,
    toast: mockToastFn,
  };
});

describe("AdminsIndexPage tests", () => {
  const axiosMock = new AxiosMockAdapter(axios);

  const testId = "RoleEmailTable";

  const setupAdminUser = () => {
    axiosMock.reset();
    axiosMock.resetHistory();
    axiosMock
      .onGet("/api/currentUser")
      .reply(200, apiCurrentUserFixtures.adminUser);
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.showingNeither);
  };

  const queryClient = new QueryClient();

  test("Renders with New Admin Button", async () => {
    setupAdminUser();
    axiosMock.onGet("/api/admin/admins/all").reply(200, []);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AdminsIndexPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await waitFor(() => {
      expect(screen.getByText(/New Admin/)).toBeInTheDocument();
    });
    const button = screen.getByText(/New Admin/);
    expect(button).toHaveAttribute("href", "/admin/admins/create");
    expect(button).toHaveAttribute("style", "float: right;");
  });

  test("renders three items correctly", async () => {
    setupAdminUser();
    axiosMock
      .onGet("/api/admin/admins/all")
      .reply(200, roleEmailFixtures.threeItems);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AdminsIndexPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await waitFor(() => {
      expect(
        screen.getByTestId(`${testId}-cell-row-0-col-email`),
      ).toHaveTextContent("instructor1@example.com");
    });
    expect(
      screen.getByTestId(`${testId}-cell-row-1-col-email`),
    ).toHaveTextContent("admin1@example.com");
    expect(
      screen.getByTestId(`${testId}-cell-row-2-col-email`),
    ).toHaveTextContent("instructor2@example.com");

    // delete button should be visible
    expect(
      screen.getByTestId("RoleEmailTable-cell-row-0-col-Delete-button"),
    ).toBeInTheDocument();
  });

  test("renders empty table when backend unavailable", async () => {
    setupAdminUser();

    axiosMock.onGet("/api/admin/admins/all").timeout();

    const restoreConsole = mockConsole();

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AdminsIndexPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await waitFor(() => {
      expect(axiosMock.history.get.length).toBeGreaterThanOrEqual(1);
    });

    const errorMessage = console.error.mock.calls[0][0];
    expect(errorMessage).toMatch(
      "Error communicating with backend via GET on /api/admin/admins/all",
    );
    restoreConsole();
  });

  test("can delete an admin not in ADMIN_EMAILS user", async () => {
    setupAdminUser();

    const smallAdmin = { email: "adminuno@example.com" };

    axiosMock.onGet("/api/admin/admins/all").reply(200, [smallAdmin]);

    axiosMock
      .onDelete("/api/admin/admins", { params: { email: smallAdmin.email } })
      .reply(200, { email: smallAdmin.email });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AdminsIndexPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await waitFor(() => {
      expect(
        screen.getByTestId(`${testId}-cell-row-0-col-email`),
      ).toHaveTextContent("adminuno@example.com");
    });

    const deleteButton = screen.getByTestId(
      `${testId}-cell-row-0-col-Delete-button`,
    );
    fireEvent.click(deleteButton);

    await waitFor(() => {
      expect(mockToast).toHaveBeenCalledWith(
        "Admin with email adminuno@example.com deleted",
      );
    });
  });

  test("cannot delete an admin whos email is in ADMIN_EMAILS", async () => {
    setupAdminUser();

    const admin = { email: "admin@example.com" };

    axiosMock.onGet("/api/admin/admins/all").reply(200, [admin]);

    axiosMock
      .onDelete("/api/admin/admins", { params: { email: admin.email } })
      .reply(403, "Attempting to delete admin in ADMIN_EMAILS"); // backend should respond with 403 error

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AdminsIndexPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await waitFor(() => {
      expect(
        screen.getByTestId(`${testId}-cell-row-0-col-email`),
      ).toHaveTextContent("admin@example.com");
    });

    const deleteButton = screen.getByTestId(
      `${testId}-cell-row-0-col-Delete-button`,
    );
    fireEvent.click(deleteButton);

    await waitFor(() => {
      expect(mockToast).toHaveBeenCalledWith(
        "Attempting to delete admin in ADMIN_EMAILS",
      );
    });
  });

  test("shows generic error message on unexpected error", async () => {
    setupAdminUser();

    const admin = { email: "not-in-admin-emails@example.com" };

    axiosMock
      .onDelete("/api/admin/admins", { params: { email: admin.email } })
      .reply(500); // internal server error

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AdminsIndexPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const deleteButton = screen.getByTestId(
      `${testId}-cell-row-0-col-Delete-button`,
    );

    fireEvent.click(deleteButton);

    await waitFor(() =>
      expect(mockToast).toHaveBeenCalledWith("Error deleting admin."),
    );
  });

  test("handles error with 403 status but non-string message", async () => {
    setupAdminUser();

    const admin = { email: "test@example.com" };

    axiosMock.onGet("/api/admin/admins/all").reply(200, [admin]);

    // mock a 403 response but with a non-string message
    axiosMock
      .onDelete("/api/admin/admins", { params: { email: admin.email } })
      .reply(403, { error: "This is an object error message" });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AdminsIndexPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await waitFor(() => {
      expect(
        screen.getByTestId(`${testId}-cell-row-0-col-email`),
      ).toHaveTextContent("test@example.com");
    });

    const deleteButton = screen.getByTestId(
      `${testId}-cell-row-0-col-Delete-button`,
    );
    fireEvent.click(deleteButton);

    await waitFor(() => {
      expect(mockToast).toHaveBeenCalledWith("Error deleting admin.");
    });
  });

  test("handles error without response object", async () => {
    setupAdminUser();

    const admin = { email: "test@example.com" };

    axiosMock.onGet("/api/admin/admins/all").reply(200, [admin]);

    // mock an error without a response property
    axiosMock
      .onDelete("/api/admin/admins", { params: { email: admin.email } })
      .reply(() => {
        throw new Error("Network Error"); // this error doesn't have a response property
      });

    const restoreConsole = mockConsole();

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AdminsIndexPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await waitFor(() => {
      expect(
        screen.getByTestId(`${testId}-cell-row-0-col-email`),
      ).toHaveTextContent("test@example.com");
    });

    const deleteButton = screen.getByTestId(
      `${testId}-cell-row-0-col-Delete-button`,
    );
    fireEvent.click(deleteButton);

    await waitFor(() => {
      expect(mockToast.mock.calls).toContainEqual([
        "Axios Error: Error: Network Error",
      ]);
    });

    restoreConsole();
  });

  test("handles error with non-403 status", async () => {
    setupAdminUser();

    const admin = { email: "test@example.com" };

    axiosMock.onGet("/api/admin/admins/all").reply(200, [admin]);

    // mock a non-403 error with a string message
    axiosMock
      .onDelete("/api/admin/admins", { params: { email: admin.email } })
      .reply(400, "Bad Request Error");

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AdminsIndexPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await waitFor(() => {
      expect(
        screen.getByTestId(`${testId}-cell-row-0-col-email`),
      ).toHaveTextContent("test@example.com");
    });

    const deleteButton = screen.getByTestId(
      `${testId}-cell-row-0-col-Delete-button`,
    );
    fireEvent.click(deleteButton);

    await waitFor(() => {
      // generic error message since its not 403
      expect(mockToast).toHaveBeenCalledWith("Error deleting admin.");
    });
  });

  test("invalidateQueries is called with the correct query key when admin is deleted", async () => {
    setupAdminUser();

    const admin = { email: "test@example.com" };

    axiosMock.onGet("/api/admin/admins/all").reply(200, [admin]);
    axiosMock
      .onDelete("/api/admin/admins", { params: { email: admin.email } })
      .reply(200, { email: admin.email });

    // create a mock for queryClient.invalidateQueries
    const invalidateQueriesMock = jest.fn();
    const mockQueryClient = {
      ...queryClient,
      invalidateQueries: invalidateQueriesMock,
    };

    // override the useQueryClient hook to return our mock
    const useQueryClientOrig = require("react-query").useQueryClient;
    jest
      .spyOn(require("react-query"), "useQueryClient")
      .mockImplementation(() => mockQueryClient);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AdminsIndexPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await waitFor(() => {
      expect(
        screen.getByTestId(`${testId}-cell-row-0-col-email`),
      ).toHaveTextContent("test@example.com");
    });

    const deleteButton = screen.getByTestId(
      `${testId}-cell-row-0-col-Delete-button`,
    );
    fireEvent.click(deleteButton);

    await waitFor(() => {
      // verifies that the invalidateQueries was called with the correct query key
      expect(invalidateQueriesMock).toHaveBeenCalledWith({
        queryKey: ["/api/admin/admins/all"],
      });
    });

    // restores the original useQueryClient implementation
    require("react-query").useQueryClient = useQueryClientOrig;
  });
});
