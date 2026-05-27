import { render, screen, within } from "@testing-library/react";
import HelpCsvPage from "main/pages/Help/HelpCsvPage";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router";
import * as reactRouter from "react-router";
import { vi } from "vitest";

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
  test("scrolls to teams section when hash is present", async () => {
    const scrollIntoViewMock = vi.fn();
    Element.prototype.scrollIntoView = scrollIntoViewMock;

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/help/csv#team-information"]}>
          <HelpCsvPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText("Team Information");
    expect(scrollIntoViewMock).toHaveBeenCalled();
  });

  test("scrolls to staff csv upload section when hash is present", async () => {
    const scrollIntoViewMock = vi.fn();
    Element.prototype.scrollIntoView = scrollIntoViewMock;

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/help/csv#staff-csv-upload"]}>
          <HelpCsvPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText("Staff CSV Upload");
    expect(scrollIntoViewMock).toHaveBeenCalled();
  });

  test("does not scroll when there is no hash", async () => {
    const scrollIntoViewMock = vi.fn();
    const getElementByIdSpy = vi.spyOn(document, "getElementById");
    Element.prototype.scrollIntoView = scrollIntoViewMock;

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/help/csv"]}>
          <HelpCsvPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText("Team Information");
    expect(getElementByIdSpy).not.toHaveBeenCalled();
    expect(scrollIntoViewMock).not.toHaveBeenCalled();
  });

  test("does not scroll when hash points to missing element", async () => {
    const scrollIntoViewMock = vi.fn();
    Element.prototype.scrollIntoView = scrollIntoViewMock;

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/help/csv#missing-section"]}>
          <HelpCsvPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText("Team Information");
    expect(scrollIntoViewMock).not.toHaveBeenCalled();
  });

  test("re-runs scroll effect when hash changes", async () => {
    const scrollIntoViewMock = vi.fn();
    Element.prototype.scrollIntoView = scrollIntoViewMock;

    let currentHash = "";
    const useLocationSpy = vi
      .spyOn(reactRouter, "useLocation")
      .mockImplementation(() => ({
        hash: currentHash,
        pathname: "/help/csv",
        search: "",
        state: null,
        key: "default",
      }));

    const { rerender } = render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <HelpCsvPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText("Team Information");
    expect(scrollIntoViewMock).not.toHaveBeenCalled();

    currentHash = "#team-information";
    rerender(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <HelpCsvPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(scrollIntoViewMock).toHaveBeenCalledTimes(1);
    useLocationSpy.mockRestore();
  });

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
    const staffCsvExample = screen.getByTestId("staffCsvExample");

    expect(chicoStateCsvExample).toBeInTheDocument();
    expect(oregonStateCsvExample).toBeInTheDocument();
    expect(rosterDownloadCsvExample).toBeInTheDocument();
    expect(ucsbEgradesCsvExample).toBeInTheDocument();
    expect(teamsCsvExample).toBeInTheDocument();
    expect(staffCsvExample).toBeInTheDocument();
    expect(chicoStateCsvExample).toHaveClass("csvExample");
    expect(oregonStateCsvExample).toHaveClass("csvExample");
    expect(rosterDownloadCsvExample).toHaveClass("csvExample");
    expect(ucsbEgradesCsvExample).toHaveClass("csvExample");
    expect(teamsCsvExample).toHaveClass("csvExample");
    expect(staffCsvExample).toHaveClass("csvExample");

    // New header exists
    expect(screen.getByText("Team Information")).toBeInTheDocument();
    expect(screen.getByText("Staff CSV Upload")).toBeInTheDocument();
    expect(staffCsvExample).toHaveTextContent("firstName,lastName,email");
    expect(staffCsvExample).toHaveTextContent("Ada,Lovelace,ada@ucsb.edu");

    // Check accordions separation
    const rosterUploadsAccordion = screen.getByTestId("rosterUploadsAccordion");
    const teamsAccordion = screen.getByTestId("teamsAccordion");
    const staffCsvUploadAccordion = screen.getByTestId(
      "staffCsvUploadAccordion",
    );

    // Teams header is in the Team Information accordion
    expect(
      within(teamsAccordion).getByText("Teams (by Email)"),
    ).toBeInTheDocument();
    expect(
      within(staffCsvUploadAccordion).getByText("Staff (by Email)"),
    ).toBeInTheDocument();
    expect(
      within(staffCsvUploadAccordion).getAllByText("firstName").length,
    ).toBeGreaterThan(0);
    expect(
      within(staffCsvUploadAccordion).getAllByText("lastName").length,
    ).toBeGreaterThan(0);
    expect(
      within(staffCsvUploadAccordion).getAllByText("email").length,
    ).toBeGreaterThan(0);

    // Teams header is not in the Roster Uploads accordion
    expect(
      within(rosterUploadsAccordion).queryByText("Teams (by Email)"),
    ).not.toBeInTheDocument();
  });
});
