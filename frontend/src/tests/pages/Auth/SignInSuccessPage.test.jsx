import { render, waitFor, screen } from "@testing-library/react";
import { BrowserRouter } from "react-router";
import SignInSuccessPage from "main/pages/Auth/SignInSuccessPage";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import mockConsole from "tests/testutils/mockConsole";
import { vi } from "vitest";
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

const mockedNavigate = vi.fn();
vi.mock("react-router", async (importOriginal) => ({
  ...(await importOriginal()),
  useNavigate: () => mockedNavigate,
}));
describe("SignInSuccessPage tests", () => {
  beforeEach(() => {
    queryClient.clear();
    sessionStorage.clear();
  });
  test("Page redirects correctly on set value", async () => {
    const restoreConsole = mockConsole();
    sessionStorage.setItem("redirect", "return-url");
    render(
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          <SignInSuccessPage />
        </BrowserRouter>
      </QueryClientProvider>,
    );

    await waitFor(() => expect(mockedNavigate).toBeCalledWith("return-url"));
    expect(screen.getByText("Redirecting...")).toBeInTheDocument();
    restoreConsole();
  });

  test("Page redirects to / on no value", async () => {
    const restoreConsole = mockConsole();
    render(
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          <SignInSuccessPage />
        </BrowserRouter>
      </QueryClientProvider>,
    );
    await waitFor(() => expect(mockedNavigate).toBeCalledWith("/"));
    restoreConsole();
  });
});
