import { BrowserRouter } from "react-router";
import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import NotFoundPage from "main/pages/Auth/NotFoundPage";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";

const queryClient = new QueryClient();

// BasicLayout fetches /api/currentUser and /api/systemInfo; without a mock the
// real request fails and react-query keeps retrying (and logging) after the
// test has finished, which makes vitest's worker teardown flaky.
const axiosMock = new AxiosMockAdapter(axios);
axiosMock.onGet("/api/currentUser").reply(200, apiCurrentUserFixtures.userOnly);
axiosMock
  .onGet("/api/systemInfo")
  .reply(200, systemInfoFixtures.showingNeither);
test("Not Found Page static checks", async () => {
  render(
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <NotFoundPage />
      </BrowserRouter>
    </QueryClientProvider>,
  );

  await screen.findByText(/Page Not Found/);
  expect(screen.getByText("Let's get you back on track.")).toBeInTheDocument();
  expect(screen.getByText("Click to Return Home")).toBeInTheDocument();
});
