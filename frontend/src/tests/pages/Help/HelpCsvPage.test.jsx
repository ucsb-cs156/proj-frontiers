import { render, screen, within } from "@testing-library/react";
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
  const normalize = (text) => text.replace(/\s+/g, " ").trim();
  const textContentEquals = (text, tagName) => (_, node) => {
    if (!node) {
      return false;
    }
    if (tagName && node.tagName !== tagName) {
      return false;
    }
    return normalize(node.textContent || "") === normalize(text);
  };
  test("renders with separate Team Information section and examples", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <HelpCsvPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );
    await screen.findByText(/CSV Upload\/Download Formats/);

    // Examples still render
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

    // New header exists
    expect(screen.getByText("Team Information")).toBeInTheDocument();

    // Check accordions separation
    const rosterUploadsAccordion = screen.getByTestId("rosterUploadsAccordion");
    const teamsAccordion = screen.getByTestId("teamsAccordion");

    // Teams header is in the Team Information accordion
    expect(
      within(teamsAccordion).getByText("Teams (by Email)"),
    ).toBeInTheDocument();

    // Teams header is not in the Roster Uploads accordion
    expect(
      within(rosterUploadsAccordion).queryByText("Teams (by Email)"),
    ).not.toBeInTheDocument();

    // Dropped students section describes space-sensitive phrases
    expect(
      screen.getByText(
        textContentEquals(
          "Each time you upload a roster CSV, Frontiers temporarily marks every roster student that originally came from a CSV (status ROSTER) as DROPPED. As rows from your new file are processed, matching students are switched back to ROSTER (or inserted if they were new).",
          "P",
        ),
      ),
    ).toBeInTheDocument();
    expect(
      screen.getByText(
        textContentEquals(
          "Students that you added manually keep their MANUAL status and are never moved to DROPPED by an upload.",
          "LI",
        ),
      ),
    ).toBeInTheDocument();
    expect(
      screen.getByText(
        textContentEquals(
          "After the upload finishes, anyone still marked DROPPED is listed in the Dropped tab, and Frontiers queues them for removal from the linked GitHub organization.",
          "LI",
        ),
      ),
    ).toBeInTheDocument();
  });
});
