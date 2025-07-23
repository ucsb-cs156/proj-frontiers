import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "react-query";
import { MemoryRouter } from "react-router-dom";
import {
  apiCurrentUserFixtures,
  apiCurrentUserFixturesWithGithub,
} from "fixtures/currentUserFixtures";

import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import ProfilePage from "main/pages/ProfilePage";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";

const axiosMock = new AxiosMockAdapter(axios);
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: false,
    },
  },
});

const mockToast = jest.fn();

jest.mock("react-toastify", () => {
  const originalModule = jest.requireActual("react-toastify");
  return {
    __esModule: true,
    ...originalModule,
    toast: (x) => mockToast(x),
  };
});

describe("ProfilePage tests", () => {
  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    queryClient.clear();
  });
  test("renders correctly for regular logged in user", async () => {
    axiosMock
      .onGet("/api/currentUser")
      .reply(200, apiCurrentUserFixtures.userOnly);
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.showingNeither);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <ProfilePage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText("Phillip Conrad");
    expect(screen.getByText("pconrad.cis@gmail.com")).toBeInTheDocument();
  });

  test("renders correctly for admin user", async () => {
    axiosMock
      .onGet("/api/currentUser")
      .reply(200, apiCurrentUserFixtures.adminUser);
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.showingNeither);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <ProfilePage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText("Phill Conrad");
    expect(screen.getByText("phtcon@ucsb.edu")).toBeInTheDocument();
    expect(screen.getByTestId("role-badge-user")).toBeInTheDocument();
    expect(screen.getByTestId("role-badge-admin")).toBeInTheDocument();
    expect(screen.getByTestId("role-badge-member")).toBeInTheDocument();
    expect(
      screen.queryByTestId("ConfirmationModal-base"),
    ).not.toBeInTheDocument();
  });

  test("GitHub disconnect appears", async () => {
    axiosMock
      .onGet("/api/currentUser")
      .reply(200, apiCurrentUserFixturesWithGithub.adminUser);
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.showingNeither);
    axiosMock.onDelete("/api/github/disconnect").reply(200, {
      message:
        "Disconnected from GitHub. You may now log in with a different account.",
    });
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <ProfilePage />
        </MemoryRouter>
      </QueryClientProvider>,
    );
    await screen.findByText("Disconnect GitHub");
    const disconnectButton = screen.getByText("Disconnect GitHub");
    expect(disconnectButton).toHaveClass("btn btn-danger");
    expect(screen.getByTestId("ProfilePage-advancedFeatures")).toHaveClass(
      "mt-3 g-3",
    );
    fireEvent.click(disconnectButton);
    await screen.findByTestId("ConfirmationModal-base");
    expect(
      screen.getByText(
        "Are you sure you want to disconnect your Github account?",
      ),
    ).toBeInTheDocument();
    expect(
      screen.getByText("Please only do so if you know what you're doing."),
    ).toBeInTheDocument();
    const fire = screen.getByText("Yes, I'd like to do this");
    const updateCount =
      queryClient.getQueryState("current user").dataUpdateCount;
    const systemInfoCount =
      queryClient.getQueryState("systemInfo").dataUpdateCount;
    fireEvent.click(fire);
    await waitFor(() =>
      expect(
        screen.queryByTestId("ConfirmationModal-base"),
      ).not.toBeInTheDocument(),
    );
    expect(axiosMock.history.delete.length).toBe(1);
    expect(axiosMock.history.delete[0].url).toBe("/api/github/disconnect");
    expect(mockToast).toBeCalledWith(
      "Disconnected from GitHub. You may now log in with a different account.",
    );
    expect(queryClient.getQueryState("current user").dataUpdateCount).toBe(
      updateCount + 1,
    );
    expect(queryClient.getQueryState("systemInfo").dataUpdateCount).toBe(
      systemInfoCount,
    );
  });
});
