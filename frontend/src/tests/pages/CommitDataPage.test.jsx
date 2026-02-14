import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router";
import CommitDataPage from "main/pages/CommitDataPage";
import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";

const commitDataFixture = {
  owner: "test-org",
  repo: "test-repo",
  branch: "main",
  count: 2,
  retrievedTime: "2024-01-01T00:00:00Z",
  commits: [
    {
      sha: "abc1234567890",
      message: "first commit",
      authorName: "Author A",
      authorLogin: "authorA",
      committerName: "Committer A",
      committerLogin: "committerA",
      commitTime: "2024-01-01T00:00:00Z",
      url: "https://github.com/test-org/test-repo/commit/abc1234567890",
    },
    {
      sha: null,
      message: "second commit",
      authorName: "Author B",
      authorLogin: "authorB",
      committerName: "Committer B",
      committerLogin: "committerB",
      commitTime: "2024-01-02T00:00:00Z",
      url: "https://github.com/test-org/test-repo/commit/def",
    },
  ],
};

const commitDataFixture2 = {
  owner: "other-org",
  repo: "other-repo",
  branch: "develop",
  count: 1,
  retrievedTime: "2024-02-01T00:00:00Z",
  commits: [
    {
      sha: "xyz9876543210",
      message: "other commit",
      authorName: "Author C",
      authorLogin: "authorC",
      committerName: "Committer C",
      committerLogin: "committerC",
      commitTime: "2024-02-01T00:00:00Z",
      url: "https://github.com/other-org/other-repo/commit/xyz9876543210",
    },
  ],
};

describe("CommitDataPage tests", () => {
  let queryClient;
  let axiosMock;

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });
    axiosMock = new AxiosMockAdapter(axios);
    axiosMock
      .onGet("/api/currentUser")
      .reply(200, apiCurrentUserFixtures.adminUser);
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.showingNeither);
  });

  afterEach(() => {
    axiosMock.restore();
    queryClient.clear();
  });

  test("renders without crashing", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <CommitDataPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(await screen.findByText("Commit Data")).toBeInTheDocument();
    expect(screen.getByTestId("CommitDataPage")).toBeInTheDocument();
    expect(screen.getByTestId("CommitDataPage-fetch-button")).toHaveTextContent(
      "Add Commits",
    );
  });

  test("form fields can be filled in", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <CommitDataPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const courseIdInput = screen.getByTestId("CommitDataPage-courseId");
    const ownerInput = screen.getByTestId("CommitDataPage-owner");
    const repoInput = screen.getByTestId("CommitDataPage-repo");
    const branchInput = screen.getByTestId("CommitDataPage-branch");
    const countInput = screen.getByTestId("CommitDataPage-count");

    fireEvent.change(courseIdInput, { target: { value: "5" } });
    fireEvent.change(ownerInput, { target: { value: "test-org" } });
    fireEvent.change(repoInput, { target: { value: "test-repo" } });
    fireEvent.change(branchInput, { target: { value: "main" } });
    fireEvent.change(countInput, { target: { value: "50" } });

    expect(courseIdInput).toHaveValue(5);
    expect(ownerInput).toHaveValue("test-org");
    expect(repoInput).toHaveValue("test-repo");
    expect(branchInput).toHaveValue("main");
    expect(countInput).toHaveValue(50);
  });

  test("courseId defaults to 0 for non-numeric input", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <CommitDataPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const courseIdInput = screen.getByTestId("CommitDataPage-courseId");
    fireEvent.change(courseIdInput, { target: { value: "" } });
    expect(courseIdInput).toHaveValue(0);
  });

  test("count defaults to 100 for non-numeric input", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <CommitDataPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const countInput = screen.getByTestId("CommitDataPage-count");
    fireEvent.change(countInput, { target: { value: "" } });
    expect(countInput).toHaveValue(100);
  });

  test("fetching and displaying commits from one repo", async () => {
    axiosMock
      .onGet("/api/github/graphql/commitData")
      .reply(200, commitDataFixture);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <CommitDataPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    fireEvent.change(screen.getByTestId("CommitDataPage-owner"), {
      target: { value: "test-org" },
    });
    fireEvent.change(screen.getByTestId("CommitDataPage-repo"), {
      target: { value: "test-repo" },
    });
    fireEvent.change(screen.getByTestId("CommitDataPage-branch"), {
      target: { value: "main" },
    });

    fireEvent.click(screen.getByTestId("CommitDataPage-fetch-button"));

    await waitFor(() => {
      expect(screen.getByTestId("CommitDataPage-metadata")).toBeInTheDocument();
    });

    expect(
      screen.getByText("Showing commits from 1 repository"),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId("CommitDataPage-added-repos"),
    ).toBeInTheDocument();
    expect(
      screen.getByText(/test-org\/test-repo \(main\) - 2 commits/),
    ).toBeInTheDocument();

    // Check table has Owner/Repo column
    expect(screen.getByText("Owner/Repo")).toBeInTheDocument();
    expect(screen.getByText("first commit")).toBeInTheDocument();
    expect(screen.getByText("abc1234")).toBeInTheDocument();

    // Check Clear All button is visible
    expect(
      screen.getByTestId("CommitDataPage-clear-button"),
    ).toBeInTheDocument();
  });

  test("accumulates commits from multiple repos", async () => {
    let callCount = 0;
    axiosMock.onGet("/api/github/graphql/commitData").reply(() => {
      callCount++;
      if (callCount === 1) return [200, commitDataFixture];
      return [200, commitDataFixture2];
    });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <CommitDataPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    // First fetch
    fireEvent.change(screen.getByTestId("CommitDataPage-owner"), {
      target: { value: "test-org" },
    });
    fireEvent.change(screen.getByTestId("CommitDataPage-repo"), {
      target: { value: "test-repo" },
    });
    fireEvent.change(screen.getByTestId("CommitDataPage-branch"), {
      target: { value: "main" },
    });
    fireEvent.click(screen.getByTestId("CommitDataPage-fetch-button"));

    await waitFor(() => {
      expect(
        screen.getByText("Showing commits from 1 repository"),
      ).toBeInTheDocument();
    });

    // Second fetch
    fireEvent.change(screen.getByTestId("CommitDataPage-owner"), {
      target: { value: "other-org" },
    });
    fireEvent.change(screen.getByTestId("CommitDataPage-repo"), {
      target: { value: "other-repo" },
    });
    fireEvent.change(screen.getByTestId("CommitDataPage-branch"), {
      target: { value: "develop" },
    });
    fireEvent.click(screen.getByTestId("CommitDataPage-fetch-button"));

    await waitFor(() => {
      expect(
        screen.getByText("Showing commits from 2 repositories"),
      ).toBeInTheDocument();
    });

    expect(
      screen.getByText(/test-org\/test-repo \(main\) - 2 commits/),
    ).toBeInTheDocument();
    expect(
      screen.getByText(/other-org\/other-repo \(develop\) - 1 commit$/),
    ).toBeInTheDocument();
    expect(screen.getByText("first commit")).toBeInTheDocument();
    expect(screen.getByText("other commit")).toBeInTheDocument();
  });

  test("remove button removes a repo from the list", async () => {
    axiosMock
      .onGet("/api/github/graphql/commitData")
      .reply(200, commitDataFixture);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <CommitDataPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    fireEvent.change(screen.getByTestId("CommitDataPage-owner"), {
      target: { value: "test-org" },
    });
    fireEvent.change(screen.getByTestId("CommitDataPage-repo"), {
      target: { value: "test-repo" },
    });
    fireEvent.change(screen.getByTestId("CommitDataPage-branch"), {
      target: { value: "main" },
    });
    fireEvent.click(screen.getByTestId("CommitDataPage-fetch-button"));

    await waitFor(() => {
      expect(
        screen.getByTestId("CommitDataPage-added-repos"),
      ).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId("CommitDataPage-remove-0"));

    await waitFor(() => {
      expect(
        screen.queryByTestId("CommitDataPage-added-repos"),
      ).not.toBeInTheDocument();
    });
    expect(
      screen.queryByTestId("CommitDataPage-metadata"),
    ).not.toBeInTheDocument();
  });

  test("clear all button removes all repos", async () => {
    axiosMock
      .onGet("/api/github/graphql/commitData")
      .reply(200, commitDataFixture);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <CommitDataPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    fireEvent.change(screen.getByTestId("CommitDataPage-owner"), {
      target: { value: "test-org" },
    });
    fireEvent.change(screen.getByTestId("CommitDataPage-repo"), {
      target: { value: "test-repo" },
    });
    fireEvent.change(screen.getByTestId("CommitDataPage-branch"), {
      target: { value: "main" },
    });
    fireEvent.click(screen.getByTestId("CommitDataPage-fetch-button"));

    await waitFor(() => {
      expect(
        screen.getByTestId("CommitDataPage-clear-button"),
      ).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId("CommitDataPage-clear-button"));

    await waitFor(() => {
      expect(
        screen.queryByTestId("CommitDataPage-clear-button"),
      ).not.toBeInTheDocument();
    });
    expect(
      screen.queryByTestId("CommitDataPage-added-repos"),
    ).not.toBeInTheDocument();
  });

  test("clear all button is not shown when no histories", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <CommitDataPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(
      screen.queryByTestId("CommitDataPage-clear-button"),
    ).not.toBeInTheDocument();
  });

  test("shows error message on fetch failure", async () => {
    axiosMock
      .onGet("/api/github/graphql/commitData")
      .reply(500, { message: "Server error" });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <CommitDataPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    fireEvent.change(screen.getByTestId("CommitDataPage-owner"), {
      target: { value: "test-org" },
    });
    fireEvent.change(screen.getByTestId("CommitDataPage-repo"), {
      target: { value: "test-repo" },
    });
    fireEvent.change(screen.getByTestId("CommitDataPage-branch"), {
      target: { value: "main" },
    });
    fireEvent.click(screen.getByTestId("CommitDataPage-fetch-button"));

    await waitFor(() => {
      expect(screen.getByTestId("CommitDataPage-error")).toBeInTheDocument();
    });
  });

  test("handles commit with null sha in table", async () => {
    axiosMock
      .onGet("/api/github/graphql/commitData")
      .reply(200, commitDataFixture);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <CommitDataPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    fireEvent.change(screen.getByTestId("CommitDataPage-owner"), {
      target: { value: "test-org" },
    });
    fireEvent.change(screen.getByTestId("CommitDataPage-repo"), {
      target: { value: "test-repo" },
    });
    fireEvent.change(screen.getByTestId("CommitDataPage-branch"), {
      target: { value: "main" },
    });
    fireEvent.click(screen.getByTestId("CommitDataPage-fetch-button"));

    await waitFor(() => {
      expect(screen.getByText("first commit")).toBeInTheDocument();
    });

    // The second commit has null sha, should render empty string in link
    expect(screen.getByText("second commit")).toBeInTheDocument();
  });

  test("handles commitHistory with no commits array", async () => {
    const noCommitsFixture = {
      owner: "test-org",
      repo: "test-repo",
      branch: "main",
      count: 0,
      retrievedTime: "2024-01-01T00:00:00Z",
    };
    axiosMock
      .onGet("/api/github/graphql/commitData")
      .reply(200, noCommitsFixture);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <CommitDataPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    fireEvent.change(screen.getByTestId("CommitDataPage-owner"), {
      target: { value: "test-org" },
    });
    fireEvent.change(screen.getByTestId("CommitDataPage-repo"), {
      target: { value: "test-repo" },
    });
    fireEvent.change(screen.getByTestId("CommitDataPage-branch"), {
      target: { value: "main" },
    });
    fireEvent.click(screen.getByTestId("CommitDataPage-fetch-button"));

    await waitFor(() => {
      expect(screen.getByTestId("CommitDataPage-metadata")).toBeInTheDocument();
    });

    expect(
      screen.getByText(/test-org\/test-repo \(main\) - 0 commits/),
    ).toBeInTheDocument();
  });
});
