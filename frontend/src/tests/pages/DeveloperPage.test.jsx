import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";

import DeveloperPage from "main/pages/DeveloperPage";
import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";

describe("DeveloperPage tests", () => {
  const queryClient = new QueryClient();
  const axiosMock = new AxiosMockAdapter(axios);

  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.showingAll);
    axiosMock
      .onGet("/api/currentUser")
      .reply(200, apiCurrentUserFixtures.userOnly);
  });

  test("Developer Page has expected info", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <DeveloperPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(
      await screen.findByText("Developer Information"),
    ).toBeInTheDocument();

    expect(
      (await screen.findAllByText(systemInfoFixtures.showingAll.sourceRepo))[0],
    ).toBeInTheDocument();
    expect(
      (await screen.findAllByText(systemInfoFixtures.showingAll.githubUrl))[0],
    ).toBeInTheDocument();
    expect(
      (await screen.findAllByText(systemInfoFixtures.showingAll.commitId))[0],
    ).toBeInTheDocument();
    expect(
      (
        await screen.findAllByText(systemInfoFixtures.showingAll.commitMessage)
      )[0],
    ).toBeInTheDocument();

    const swaggerLinks = screen.getAllByText("Swagger");
    expect(swaggerLinks[swaggerLinks.length - 1]).toHaveAttribute(
      "href",
      "/swagger-ui/index.html",
    );

    expect(await screen.findByText("System Info")).toBeInTheDocument();
  });
});
