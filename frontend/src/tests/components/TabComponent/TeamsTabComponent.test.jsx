import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { teamsFixtures, loadTeamResultFixtures } from "fixtures/TeamsFixtures";
import AxiosMockAdapter from "axios-mock-adapter";
import axios from "axios";
import {
  QueryClient,
  QueryClientProvider,
  useQuery,
} from "@tanstack/react-query";
import userEvent from "@testing-library/user-event";
import { currentUserFixtures } from "fixtures/currentUserFixtures";
import { describe, expect, test, vi } from "vitest";
import { toast } from "react-toastify";
import TeamsTabComponent from "main/components/TabComponent/TeamsTabComponent";
import { MemoryRouter } from "react-router";

const axiosMock = new AxiosMockAdapter(axios);
const queryClient = new QueryClient();
const testId = "InstructorCourseShowPage";

vi.mock("react-toastify", async (importOriginal) => {
  const mockToast = vi.fn();
  mockToast.error = vi.fn();
  return {
    ...(await importOriginal()),
    toast: mockToast,
  };
});

const ArbitraryTestQueryComponent = () => {
  const _arbitraryQuery = useQuery({
    queryKey: ["arbitraryQuery"],
    queryFn: () => "banana",
  });
  return <></>;
};

describe("TeamTabComponent tests", () => {
  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    queryClient.clear();
    vi.resetAllMocks();
  });

  test("Table Renders and expected content appears", async () => {
    axiosMock
      .onGet("/api/teams/all?courseId=1")
      .reply(200, teamsFixtures.teams);

    render(
      <QueryClientProvider client={queryClient}>
        <TeamsTabComponent
          courseId={1}
          testIdPrefix={testId}
          currentUser={currentUserFixtures.instructorUser}
        />
      </QueryClientProvider>,
    );

    await waitFor(() => {
      expect(screen.getByText("team1")).toBeInTheDocument();
    });

    expect(
      screen.getByTestId(`${testId}-teams-tab-component`),
    ).toBeInTheDocument();

    expect(screen.getByTestId(`${testId}-search`)).toBeInTheDocument();
    expect(screen.getByTestId(`${testId}-post-button`)).toBeInTheDocument();
    expect(screen.getByTestId(`${testId}-csv-button`)).toBeInTheDocument();

    expect(
      screen.getByTestId(`${testId}-teams-table-accordion`),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId(`${testId}-teams-table-3-delete-button`),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId(`${testId}-teams-table-4-delete-button`),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId(`${testId}-teams-table-3-members-table`),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId(`${testId}-teams-table-4-members-table`),
    ).toBeInTheDocument();

    expect(screen.queryByText("Cancel")).not.toBeInTheDocument();
    expect(screen.queryByTestId(`TeamsForm-cancel`)).not.toBeInTheDocument();
  });
  test("Cancel button is not rendered when not editing", async () => {
    axiosMock
      .onGet("/api/teams/all?courseId=1")
      .reply(200, teamsFixtures.teams);

    render(
      <MemoryRouter>
        <QueryClientProvider client={queryClient}>
          <TeamsTabComponent
            courseId={1}
            testIdPrefix={testId}
            currentUser={currentUserFixtures.instructorUser}
          />
        </QueryClientProvider>
      </MemoryRouter>,
    );

    const openModal = await screen.findByTestId(`${testId}-post-button`);
    fireEvent.click(openModal);
    await screen.findByLabelText("Team Name");

    expect(screen.queryByText("Cancel")).not.toBeInTheDocument();
    expect(screen.queryByTestId(`TeamsForm-cancel`)).not.toBeInTheDocument();
  });
  test("Successfully makes a call to the backend on submit and clears search filter", async () => {
    const queryClientSpecific = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
          staleTime: Infinity,
        },
      },
    });
    const file = new File(["there"], "egrades.csv", { type: "text/csv" });

    axiosMock
      .onGet("/api/teams/all?courseId=1")
      .reply(200, teamsFixtures.teams);

    axiosMock.onPost("/api/teams/upload/csv").reply(200);

    const user = userEvent.setup();
    render(
      <QueryClientProvider client={queryClientSpecific}>
        <ArbitraryTestQueryComponent />
        <TeamsTabComponent
          courseId={1}
          testIdPrefix={testId}
          currentUser={currentUserFixtures.instructorUser}
        />
      </QueryClientProvider>,
    );
    const openModal = await screen.findByTestId(`${testId}-csv-button`);

    const arbitraryUpdateCount = queryClientSpecific.getQueryState([
      "arbitraryQuery",
    ]).dataUpdateCount;

    const updateTeamCount = queryClientSpecific.getQueryState([
      "/api/teams/all?courseId=1",
    ]).dataUpdateCount;

    // Get the search input and set a search term
    const searchInput = screen.getByTestId("InstructorCourseShowPage-search");
    fireEvent.change(searchInput, { target: { value: "test search" } });
    expect(searchInput.value).toBe("test search");

    fireEvent.click(openModal);
    expect(screen.getByTestId(`${testId}-csv-modal`)).toHaveClass(
      "modal-dialog modal-dialog-centered",
    );

    const upload = await screen.findByTestId("TeamsCSVUploadForm-upload");
    const submitButton = screen.getByTestId("TeamsCSVUploadForm-submit");
    await user.upload(upload, file);
    fireEvent.click(submitButton);
    await waitFor(() => {
      expect(axiosMock.history.post[0].params).toEqual({
        courseId: 1,
      });
    });
    expect(axiosMock.history.post[0].data.get("file")).toEqual(file);
    expect(toast).toBeCalledWith("Team successfully added.");
    expect(
      queryClientSpecific.getQueryState(["arbitraryQuery"]).dataUpdateCount,
    ).toBe(arbitraryUpdateCount);
    expect(
      queryClientSpecific.getQueryState(["/api/teams/all?courseId=1"])
        .dataUpdateCount,
    ).toEqual(updateTeamCount + 1);

    // Verify that the search filter is cleared
    await waitFor(() => {
      expect(searchInput.value).toBe("");
    });
    expect(screen.queryByTestId(`${testId}-csv-modal`)).not.toBeInTheDocument();
  });
  test("TeamsForm submit works and clears search filter", async () => {
    const queryClientSpecific = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
          staleTime: Infinity,
        },
      },
    });
    axiosMock
      .onGet("/api/teams/all?courseId=1")
      .reply(200, teamsFixtures.teams);

    axiosMock.onPost("/api/teams/post").reply(200);
    render(
      <MemoryRouter>
        <QueryClientProvider client={queryClientSpecific}>
          <ArbitraryTestQueryComponent />
          <TeamsTabComponent
            courseId={1}
            testIdPrefix={testId}
            currentUser={currentUserFixtures.instructorUser}
          />
        </QueryClientProvider>
      </MemoryRouter>,
    );

    expect(
      screen.getByTestId(`${testId}-teams-tab-component`),
    ).toBeInTheDocument();

    //Great time to check initial values
    expect(
      queryClientSpecific.getQueryData(["/api/teams/all?courseId=1"]),
    ).toStrictEqual([]);

    const openModal = await screen.findByTestId(`${testId}-post-button`);
    const arbitraryUpdateCount = queryClientSpecific.getQueryState([
      "arbitraryQuery",
    ]).dataUpdateCount;
    const updateCountTeam = queryClientSpecific.getQueryState([
      "/api/teams/all?courseId=1",
    ]).dataUpdateCount;

    // Get the search input and set a search term
    const searchInput = screen.getByTestId("InstructorCourseShowPage-search");
    fireEvent.change(searchInput, { target: { value: "test search" } });
    expect(searchInput.value).toBe("test search");

    expect(screen.queryByText("Cancel")).not.toBeInTheDocument();

    fireEvent.click(openModal);
    await screen.findByLabelText("Team Name");

    expect(screen.getByTestId(`${testId}-post-modal`)).toHaveClass(
      "modal-dialog modal-dialog-centered",
    );

    const teamNameInput = screen.getByTestId("TeamsForm-name");

    fireEvent.change(teamNameInput, {
      target: { value: "team5" },
    });

    expect(teamNameInput.value).toBe("team5");
    fireEvent.click(screen.getByTestId("TeamsForm-submit"));
    await waitFor(() => expect(axiosMock.history.post.length).toEqual(1));

    expect(axiosMock.history.post[0].params).toEqual({
      courseId: 1,
      name: "team5",
    });

    await waitFor(() => expect(toast).toBeCalled());
    expect(
      queryClientSpecific.getQueryState(["arbitraryQuery"]).dataUpdateCount,
    ).toBe(arbitraryUpdateCount);
    expect(
      queryClientSpecific.getQueryState(["/api/teams/all?courseId=1"])
        .dataUpdateCount,
    ).toEqual(updateCountTeam + 1);

    // Verify that the search filter is cleared
    await waitFor(() => {
      expect(screen.getByTestId("InstructorCourseShowPage-search").value).toBe(
        "",
      );
    });
    expect(
      screen.queryByTestId(`${testId}-post-modal`),
    ).not.toBeInTheDocument();
  });
  test("Modals close on close buttons, download works", async () => {
    const download = vi.fn();
    window.open = (a, b) => download(a, b);
    axiosMock
      .onGet("/api/teams/all?courseId=1")
      .reply(200, teamsFixtures.teams);

    render(
      <MemoryRouter>
        <QueryClientProvider client={queryClient}>
          <ArbitraryTestQueryComponent />
          <TeamsTabComponent
            courseId={1}
            testIdPrefix={testId}
            currentUser={currentUserFixtures.instructorUser}
          />
        </QueryClientProvider>
      </MemoryRouter>,
    );

    const openModalPost = await screen.findByTestId(`${testId}-post-button`);
    fireEvent.click(openModalPost);
    let closeButton = await screen.findByRole("button", { name: "Close" });
    fireEvent.click(closeButton);
    await waitFor(() =>
      expect(
        screen.queryByTestId(`${testId}-post-modal`),
      ).not.toBeInTheDocument(),
    );
    const openModalCsv = await screen.findByTestId(`${testId}-csv-button`);
    fireEvent.click(openModalCsv);
    closeButton = await screen.findByRole("button", { name: "Close" });
    fireEvent.click(closeButton);
    await waitFor(() =>
      expect(
        screen.queryByTestId(`${testId}-csv-modal`),
      ).not.toBeInTheDocument(),
    );
    fireEvent.click(screen.getByText("Download Team CSV"));
    await waitFor(() => expect(download).toBeCalled());
    expect(download).toBeCalledWith("/api/csv/teams?courseId=1", "_blank");
  });
  test("TeamForm works on error", async () => {
    const queryClientSpecific = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
          staleTime: Infinity,
        },
      },
    });
    const postResponse = {
      insertStatus: "REJECTED",
    };

    axiosMock
      .onGet("/api/teams/all?courseId=1")
      .reply(200, teamsFixtures.teams);

    axiosMock.onPost("/api/teams/post").reply(409, postResponse);
    render(
      <MemoryRouter>
        <QueryClientProvider client={queryClientSpecific}>
          <ArbitraryTestQueryComponent />
          <TeamsTabComponent
            courseId={1}
            testIdPrefix={testId}
            currentUser={currentUserFixtures.instructorUser}
          />
        </QueryClientProvider>
      </MemoryRouter>,
    );

    //Great time to check initial values
    expect(
      queryClientSpecific.getQueryData(["/api/teams/all?courseId=1"]),
    ).toStrictEqual([]);

    const openModal = await screen.findByTestId(`${testId}-post-button`);

    fireEvent.click(openModal);
    await screen.findByLabelText("Team Name");
    expect(screen.getByTestId(`${testId}-post-modal`)).toHaveClass(
      "modal-dialog modal-dialog-centered",
    );

    // Get the search input and set a search term
    const teamNameInput = screen.getByTestId("TeamsForm-name");

    fireEvent.change(teamNameInput, {
      target: { value: "team2" },
    });

    fireEvent.click(screen.getByTestId("TeamsForm-submit"));
    screen.debug(null, 1000000);
    await waitFor(() => expect(axiosMock.history.post.length).toEqual(1));
    await waitFor(() =>
      expect(toast.error).toBeCalledWith(
        `Error adding team: ${JSON.stringify(postResponse, null, 2)}`,
      ),
    );
  });
  test("CsvForm error returns correctly", async () => {
    const file = new File(["there"], "egrades.csv", { type: "text/csv" });

    axiosMock
      .onGet("/api/teams/all?courseId=1")
      .reply(200, teamsFixtures.teams);

    axiosMock
      .onPost("/api/teams/upload/csv")
      .reply(409, loadTeamResultFixtures.failed);

    const user = userEvent.setup();
    render(
      <QueryClientProvider client={queryClient}>
        <TeamsTabComponent
          courseId={1}
          testIdPrefix={testId}
          currentUser={currentUserFixtures.instructorUser}
        />
      </QueryClientProvider>,
    );
    const openModal = await screen.findByTestId(`${testId}-csv-button`);

    // Get the search input and set a search term
    const searchInput = screen.getByTestId("InstructorCourseShowPage-search");
    fireEvent.change(searchInput, { target: { value: "test search" } });
    expect(searchInput.value).toBe("test search");

    fireEvent.click(openModal);
    expect(screen.getByTestId(`${testId}-csv-modal`)).toHaveClass(
      "modal-dialog modal-dialog-centered",
    );

    const upload = await screen.findByTestId("TeamsCSVUploadForm-upload");
    const submitButton = screen.getByTestId("TeamsCSVUploadForm-submit");
    await user.upload(upload, file);
    fireEvent.click(submitButton);
    await waitFor(() =>
      expect(toast.error).toHaveBeenCalledWith(
        `Error uploading CSV: ${JSON.stringify(loadTeamResultFixtures.failed, null, 2)}`,
      ),
    );
  });
  describe("Search filter works correctly", () => {
    const testId = "InstructorCourseShowPage";
    beforeEach(() => {
      axiosMock
        .onGet("/api/teams/all?courseId=1")
        .reply(200, teamsFixtures.teams);
    });

    test("PLaceholder, initial check", async () => {
      render(
        <QueryClientProvider client={queryClient}>
          <TeamsTabComponent
            courseId={1}
            testIdPrefix={testId}
            currentUser={currentUserFixtures.instructorUser}
          />
        </QueryClientProvider>,
      );
      await waitFor(() => {
        expect(screen.getByTestId(`${testId}-search`)).toBeInTheDocument();
      });

      const searchInput = screen.getByTestId(`${testId}-search`);
      expect(searchInput).toBeInTheDocument();
      expect(searchInput).toHaveAttribute(
        "placeholder",
        "Search by Team Name.",
      );
    });

    test("Team Name", async () => {
      render(
        <QueryClientProvider client={queryClient}>
          <TeamsTabComponent
            courseId={1}
            testIdPrefix={testId}
            currentUser={currentUserFixtures.instructorUser}
          />
        </QueryClientProvider>,
      );

      await waitFor(() => {
        expect(screen.getByTestId(`${testId}-search`)).toBeInTheDocument();
      });

      const teamName = teamsFixtures.teams[0].name; // team1
      fireEvent.change(screen.getByTestId(`${testId}-search`), {
        target: {
          value: teamName.toUpperCase(),
        },
      });

      expect(
        screen.getByTestId(`InstructorCourseShowPage-teams-table-3-name`),
      ).toBeInTheDocument();
      expect(
        screen.getByTestId(`InstructorCourseShowPage-teams-table-3-name`),
      ).toHaveTextContent(teamName);

      fireEvent.change(screen.getByTestId(`${testId}-search`), {
        target: {
          value: "",
        },
      });

      expect(
        screen.getByTestId(`InstructorCourseShowPage-teams-table-3-name`),
      ).toBeInTheDocument();
      expect(
        screen.getByTestId(`InstructorCourseShowPage-teams-table-4-name`),
      ).toBeInTheDocument();

      fireEvent.change(screen.getByTestId(`${testId}-search`), {
        target: {
          value: teamsFixtures.teams[1].name.toUpperCase(),
        },
      });
      expect(
        screen.getByTestId(`InstructorCourseShowPage-teams-table-4-name`),
      ).toBeInTheDocument();
      expect(
        screen.getByTestId(`InstructorCourseShowPage-teams-table-4-name`),
      ).toHaveTextContent(teamsFixtures.teams[1].name);
      expect(
        screen.queryByTestId(`InstructorCourseShowPage-teams-table-3-name`),
      ).not.toBeInTheDocument();
    });
  });
  test("Renders correctly when teams data is null/undefined", async () => {
    axiosMock.onGet("/api/teams/all?courseId=1").reply(200, null);

    render(
      <QueryClientProvider client={queryClient}>
        <TeamsTabComponent
          courseId={1}
          testIdPrefix={testId}
          currentUser={currentUserFixtures.instructorUser}
        />
      </QueryClientProvider>,
    );

    await waitFor(() => {
      expect(
        screen.getByTestId(`${testId}-teams-tab-component`),
      ).toBeInTheDocument();
    });

    expect(
      screen.getByTestId(`${testId}-teams-table-accordion`),
    ).toBeInTheDocument();

    expect(screen.queryByText("team1")).not.toBeInTheDocument();

    const searchInput = screen.getByTestId(`${testId}-search`);
    fireEvent.change(searchInput, { target: { value: "test" } });
    expect(searchInput.value).toBe("test");
  });
});
