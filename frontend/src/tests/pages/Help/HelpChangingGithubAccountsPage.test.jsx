import { render, screen } from "@testing-library/react";
import HelpChangingGithubAccountsPage from "main/pages/Help/HelpChangingGithubAccountsPage";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router";

import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";

describe("HelpChangingGithubAccountsPage tests", () => {
  const axiosMock = new AxiosMockAdapter(axios);
  axiosMock
    .onGet("/api/currentUser")
    .reply(200, apiCurrentUserFixtures.userOnly);
  axiosMock
    .onGet("/api/systemInfo")
    .reply(200, systemInfoFixtures.showingNeither);

  const queryClient = new QueryClient();
  test("renders without crashing", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <HelpChangingGithubAccountsPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );
    await screen.findByText(/Changing Github Accounts/);
    expect(
      screen.getByText(/It often happens that during the initial on-boarding/),
    ).toBeInTheDocument();
    expect(
      screen.getByText(/should not immediately join the course/),
    ).toBeInTheDocument();
    expect(
      screen.getByText(/instructor may need to refresh some assignments/),
    ).toBeInTheDocument();
  });
});
