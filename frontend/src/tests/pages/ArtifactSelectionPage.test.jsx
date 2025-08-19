import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { BrowserRouter as Router } from "react-router";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import ArtifactSelectionPage from "main/pages/ArtifactSelectionPage";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import collectionNames from "fixtures/collectionNames";

const axiosMock = new AxiosMockAdapter(axios);
const queryClient = new QueryClient();

const mockedNavigate = jest.fn();

jest.mock("react-router", () => ({
  ...jest.requireActual("react-router"),
  useNavigate: () => mockedNavigate,
}));

describe("ArtifactSelectionPage tests", () => {
  afterEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    queryClient.clear();
  });

  test("Tab assertions", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <ArtifactSelectionPage />
        </Router>
      </QueryClientProvider>,
    );
    expect(screen.getByText("Select Repositories")).toHaveAttribute(
      "data-rr-ui-event-key",
      "select_repos",
    );
    expect(screen.getByText("Select Artifacts")).toHaveAttribute(
      "data-rr-ui-event-key",
      "select_artifacts",
    );
    expect(screen.getByText("Select Repositories")).toHaveAttribute(
      "aria-selected",
      "true",
    );

    const changeTabs = screen.getByText("Select Artifacts");
    fireEvent.click(changeTabs);
  });
  test("Select Repositories tab renders correctly", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <ArtifactSelectionPage />
        </Router>
      </QueryClientProvider>,
    );

    expect(await screen.findByText("Select Repositories")).toBeInTheDocument();
    expect(screen.getByTestId("repository-selection-form")).toBeInTheDocument();
  });
  test("Select Artifacts tab renders correctly", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <ArtifactSelectionPage />
        </Router>
      </QueryClientProvider>,
    );

    const changeTabs = screen.getByText("Select Artifacts");
    fireEvent.click(changeTabs);

    expect(await screen.findByText("Coming Soon")).toBeInTheDocument();
  });
  test("Backend called with correct URL", async () => {
    //backend does not currently exist for this endpoint
    axiosMock
      .onGet("/api/collections/list")
      .reply(200, collectionNames.collectionNamesForOneCourse);

    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <ArtifactSelectionPage />
        </Router>
      </QueryClientProvider>,
    );

    await waitFor(() => {
      expect(
        axiosMock.history.get.some(
          (call) => call.url === "/api/collections/list",
        ),
      ).toBe(true);
    });
  });
});
