import { render, screen } from "@testing-library/react";
import HelpCsvPage from "main/pages/Help/HelpCsvPage";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router";

import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";

describe("HelpCsvPage tests", () => {
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
          <HelpCsvPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );
    await screen.findByText(/CSV Upload\/Download Formats/);
    const chicoStateCsvExample = screen.getByTestId("chicoStateCsvExample");
    const oregonStateCsvExample = screen.getByTestId("oregonStateCsvExample");
    const ucsbEgradesCsvExample = screen.getByTestId("ucsbEgradesCsvExample");
    const rosterDownloadCsvExample = screen.getByTestId(
      "rosterDownloadCsvExample",
    );
    const teamsCsvExample = screen.getByTestId("teamsCsvExample");
    expect(chicoStateCsvExample).toBeInTheDocument();
    expect(oregonStateCsvExample).toBeInTheDocument();
    expect(rosterDownloadCsvExample).toBeInTheDocument();
    expect(ucsbEgradesCsvExample).toBeInTheDocument();
    expect(teamsCsvExample).toBeInTheDocument();
    expect(chicoStateCsvExample).toHaveClass("csvExample");
    expect(oregonStateCsvExample).toHaveClass("csvExample");
    expect(rosterDownloadCsvExample).toHaveClass("csvExample");
    expect(ucsbEgradesCsvExample).toHaveClass("csvExample");
    expect(teamsCsvExample).toHaveClass("csvExample");
  });
});
