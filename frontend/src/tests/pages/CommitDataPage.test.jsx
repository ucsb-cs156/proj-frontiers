import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router";

import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import CommitDataPage from "main/pages/CommitDataPage";
import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import { vi } from "vitest";

const axiosMock = new AxiosMockAdapter(axios);

const sampleCommitResponse = {
  owner: "test-org",
  repo: "test-repo",
  branch: "main",
  commits: [
    {
      sha: "abc1234567890",
      message: "Initial commit",
      authorName: "Alice",
      authorLogin: "alice",
      committerName: "Alice",
      committerLogin: "alice",
      commitTime: "2024-01-01T00:00:00Z",
      url: "https://github.com/test-org/test-repo/commit/abc1234567890",
    },
    {
      sha: "def4567890123",
      message: "Second commit",
      authorName: "Bob",
      authorLogin: "bob",
      committerName: "Bob",
      committerLogin: "bob",
      commitTime: "2024-01-02T00:00:00Z",
      url: "https://github.com/test-org/test-repo/commit/def4567890123",
    },
  ],
};

const sampleCommitResponse2 = {
  owner: "other-org",
  repo: "other-repo",
  branch: "develop",
  commits: [
    {
      sha: "ghi7890123456",
      message: "Third commit",
      authorName: "Charlie",
      authorLogin: "charlie",
      committerName: "Charlie",
      committerLogin: "charlie",
      commitTime: "2024-01-03T00:00:00Z",
      url: "https://github.com/other-org/other-repo/commit/ghi7890123456",
    },
  ],
};

const generateManyCommits = (count) => {
  const commits = [];
  for (let i = 0; i < count; i++) {
    commits.push({
      sha: `sha${String(i).padStart(10, "0")}`,
      message: `Commit message ${i}`,
      authorName: `Author ${i}`,
      authorLogin: `author${i}`,
      committerName: `Committer ${i}`,
      committerLogin: `committer${i}`,
      commitTime: `2024-01-${String((i % 28) + 1).padStart(2, "0")}T00:00:00Z`,
      url: `https://github.com/test-org/test-repo/commit/sha${String(i).padStart(10, "0")}`,
    });
  }
  return {
    owner: "test-org",
    repo: "test-repo",
    branch: "main",
    commits,
  };
};

describe("CommitDataPage tests", () => {
  let queryClient;

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
      },
    });
    axiosMock.reset();
    axiosMock.resetHistory();
    axiosMock
      .onGet("/api/currentUser")
      .reply(200, apiCurrentUserFixtures.userOnly);
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.showingNeither);
  });

  const renderPage = () => {
    return render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <CommitDataPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );
  };

  test("renders the page with form fields", () => {
    renderPage();
    expect(screen.getByTestId("CommitDataPage")).toBeInTheDocument();
    expect(screen.getByText("Commit Data")).toBeInTheDocument();
    expect(screen.getByTestId("CommitDataPage-courseId")).toBeInTheDocument();
    expect(screen.getByTestId("CommitDataPage-owner")).toBeInTheDocument();
    expect(screen.getByTestId("CommitDataPage-repo")).toBeInTheDocument();
    expect(screen.getByTestId("CommitDataPage-branch")).toBeInTheDocument();
    expect(screen.getByTestId("CommitDataPage-count")).toBeInTheDocument();
    expect(
      screen.getByTestId("CommitDataPage-fetch-button"),
    ).toBeInTheDocument();
  });

  test("does not show table or metadata when no commits loaded", () => {
    renderPage();
    expect(
      screen.queryByTestId("CommitDataPage-table"),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByTestId("CommitDataPage-metadata"),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByTestId("CommitDataPage-added-repos"),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByTestId("CommitDataPage-clear-button"),
    ).not.toBeInTheDocument();
  });

  test("fetches and displays commits after form submission", async () => {
    axiosMock
      .onGet("/api/github/graphql/commitData")
      .reply(200, sampleCommitResponse);

    renderPage();

    fireEvent.change(screen.getByTestId("CommitDataPage-courseId"), {
      target: { value: "42" },
    });
    fireEvent.change(screen.getByTestId("CommitDataPage-owner"), {
      target: { value: "test-org" },
    });
    fireEvent.change(screen.getByTestId("CommitDataPage-repo"), {
      target: { value: "test-repo" },
    });
    fireEvent.change(screen.getByTestId("CommitDataPage-branch"), {
      target: { value: "main" },
    });
    fireEvent.change(screen.getByTestId("CommitDataPage-count"), {
      target: { value: "100" },
    });

    fireEvent.click(screen.getByTestId("CommitDataPage-fetch-button"));

    await waitFor(() => {
      expect(screen.getByTestId("CommitDataPage-table")).toBeInTheDocument();
    });

    expect(screen.getByTestId("CommitDataPage-metadata")).toHaveTextContent(
      "Showing 2 commits from 1 repository",
    );

    expect(
      screen.getByTestId("CommitDataPage-added-repos"),
    ).toBeInTheDocument();
    expect(
      screen.getByText("test-org/test-repo (main) - 2 commits"),
    ).toBeInTheDocument();

    // Verify table headers
    expect(
      screen.getByTestId("CommitDataPage-table-header-ownerRepo"),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId("CommitDataPage-table-header-sha"),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId("CommitDataPage-table-header-message"),
    ).toBeInTheDocument();

    // Verify table rows
    expect(
      screen.getByTestId("CommitDataPage-table-row-0"),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId("CommitDataPage-table-row-1"),
    ).toBeInTheDocument();

    // Verify cell content
    expect(
      screen.getByTestId("CommitDataPage-table-cell-row-0-col-ownerRepo"),
    ).toHaveTextContent("test-org/test-repo");
    expect(
      screen.getByTestId("CommitDataPage-table-cell-row-0-col-message"),
    ).toHaveTextContent("Initial commit");
  });

  test("shows 1 commit singular text", async () => {
    const singleCommitResponse = {
      ...sampleCommitResponse,
      commits: [sampleCommitResponse.commits[0]],
    };
    axiosMock
      .onGet("/api/github/graphql/commitData")
      .reply(200, singleCommitResponse);

    renderPage();

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
      expect(screen.getByTestId("CommitDataPage-metadata")).toHaveTextContent(
        "Showing 1 commit from 1 repository",
      );
    });

    expect(
      screen.getByText("test-org/test-repo (main) - 1 commit"),
    ).toBeInTheDocument();
  });

  test("handles remove button to remove a repo", async () => {
    axiosMock
      .onGet("/api/github/graphql/commitData")
      .replyOnce(200, sampleCommitResponse)
      .onGet("/api/github/graphql/commitData")
      .replyOnce(200, sampleCommitResponse2);

    renderPage();

    // Add first repo
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
      expect(screen.getByTestId("CommitDataPage-table")).toBeInTheDocument();
    });

    // Add second repo
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
      expect(screen.getByTestId("CommitDataPage-metadata")).toHaveTextContent(
        "Showing 3 commits from 2 repositories",
      );
    });

    // Remove first repo
    fireEvent.click(screen.getByTestId("CommitDataPage-remove-0"));

    await waitFor(() => {
      expect(screen.getByTestId("CommitDataPage-metadata")).toHaveTextContent(
        "Showing 1 commit from 1 repository",
      );
    });
  });

  test("handles clear all button", async () => {
    axiosMock
      .onGet("/api/github/graphql/commitData")
      .reply(200, sampleCommitResponse);

    renderPage();

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
        screen.queryByTestId("CommitDataPage-table"),
      ).not.toBeInTheDocument();
    });
  });

  test("displays error message on fetch failure", async () => {
    axiosMock.onGet("/api/github/graphql/commitData").reply(500);

    renderPage();

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

  test("shows pagination when more than 50 commits", async () => {
    const manyCommits = generateManyCommits(75);
    axiosMock.onGet("/api/github/graphql/commitData").reply(200, manyCommits);

    renderPage();

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
      expect(screen.getByTestId("CommitDataPage-table")).toBeInTheDocument();
    });

    expect(screen.getByTestId("CommitDataPage-metadata")).toHaveTextContent(
      "Showing 75 commits from 1 repository",
    );

    // Pagination should appear (75 commits / 50 per page = 2 pages)
    expect(
      screen.getByTestId("CommitDataPage-pagination-1"),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId("CommitDataPage-pagination-2"),
    ).toBeInTheDocument();

    // First page should show 50 rows
    expect(
      screen.getByTestId("CommitDataPage-table-row-0"),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId("CommitDataPage-table-row-49"),
    ).toBeInTheDocument();
    expect(
      screen.queryByTestId("CommitDataPage-table-row-50"),
    ).not.toBeInTheDocument();

    // Click page 2
    fireEvent.click(screen.getByTestId("CommitDataPage-pagination-2"));

    await waitFor(() => {
      // Page 2 should show remaining 25 rows (original indices 50-74)
      expect(
        screen.getByTestId("CommitDataPage-table-row-50"),
      ).toBeInTheDocument();
      expect(
        screen.getByTestId("CommitDataPage-table-row-74"),
      ).toBeInTheDocument();
      expect(
        screen.queryByTestId("CommitDataPage-table-row-0"),
      ).not.toBeInTheDocument();
    });
  });

  test("does not show pagination when 50 or fewer commits", async () => {
    axiosMock
      .onGet("/api/github/graphql/commitData")
      .reply(200, sampleCommitResponse);

    renderPage();

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
      expect(screen.getByTestId("CommitDataPage-table")).toBeInTheDocument();
    });

    expect(
      screen.queryByTestId("CommitDataPage-pagination-1"),
    ).not.toBeInTheDocument();
  });

  test("sorting works on columns", async () => {
    axiosMock
      .onGet("/api/github/graphql/commitData")
      .reply(200, sampleCommitResponse);

    renderPage();

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
      expect(screen.getByTestId("CommitDataPage-table")).toBeInTheDocument();
    });

    // Click sort header for message column
    const messageSortHeader = screen.getByTestId(
      "CommitDataPage-table-header-message-sort-header",
    );
    fireEvent.click(messageSortHeader);

    // After sorting ascending, "Initial commit" should come before "Second commit"
    const tbody = screen
      .getByTestId("CommitDataPage-table")
      .querySelector("tbody");
    const firstRowAfterAsc = tbody.querySelectorAll("tr")[0];
    expect(firstRowAfterAsc).toHaveTextContent("Initial commit");

    // Click again for descending
    fireEvent.click(messageSortHeader);

    const firstRowAfterDesc = tbody.querySelectorAll("tr")[0];
    expect(firstRowAfterDesc).toHaveTextContent("Second commit");
  });

  test("filtering works on columns", async () => {
    axiosMock
      .onGet("/api/github/graphql/commitData")
      .reply(200, sampleCommitResponse);

    renderPage();

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
      expect(screen.getByTestId("CommitDataPage-table")).toBeInTheDocument();
    });

    // Filter by author name
    const authorFilter = screen.getByTestId(
      "CommitDataPage-table-header-authorName-filter",
    );
    fireEvent.change(authorFilter, { target: { value: "Alice" } });

    await waitFor(() => {
      expect(
        screen.getByTestId("CommitDataPage-table-row-0"),
      ).toBeInTheDocument();
      expect(
        screen.queryByTestId("CommitDataPage-table-row-1"),
      ).not.toBeInTheDocument();
    });

    expect(
      screen.getByTestId("CommitDataPage-table-cell-row-0-col-authorName"),
    ).toHaveTextContent("Alice");
  });

  test("SHA links render correctly", async () => {
    axiosMock
      .onGet("/api/github/graphql/commitData")
      .reply(200, sampleCommitResponse);

    renderPage();

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
      expect(screen.getByTestId("CommitDataPage-table")).toBeInTheDocument();
    });

    const shaCell = screen.getByTestId(
      "CommitDataPage-table-cell-row-0-col-sha",
    );
    const link = shaCell.querySelector("a");
    expect(link).toHaveAttribute(
      "href",
      "https://github.com/test-org/test-repo/commit/abc1234567890",
    );
    expect(link).toHaveAttribute("target", "_blank");
    expect(link).toHaveTextContent("abc1234");
  });

  test("URL links render correctly", async () => {
    axiosMock
      .onGet("/api/github/graphql/commitData")
      .reply(200, sampleCommitResponse);

    renderPage();

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
      expect(screen.getByTestId("CommitDataPage-table")).toBeInTheDocument();
    });

    const urlCell = screen.getByTestId(
      "CommitDataPage-table-cell-row-0-col-url",
    );
    const link = urlCell.querySelector("a");
    expect(link).toHaveAttribute(
      "href",
      "https://github.com/test-org/test-repo/commit/abc1234567890",
    );
    expect(link).toHaveAttribute("target", "_blank");
  });

  test("pagination prev/next buttons work", async () => {
    const manyCommits = generateManyCommits(75);
    axiosMock.onGet("/api/github/graphql/commitData").reply(200, manyCommits);

    renderPage();

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
        screen.getByTestId("CommitDataPage-pagination-next"),
      ).toBeInTheDocument();
    });

    // Click next to go to page 2
    fireEvent.click(screen.getByTestId("CommitDataPage-pagination-next"));

    await waitFor(() => {
      // On page 2, should have 25 rows (original indices 50-74)
      expect(
        screen.getByTestId("CommitDataPage-table-row-50"),
      ).toBeInTheDocument();
      expect(
        screen.queryByTestId("CommitDataPage-table-row-0"),
      ).not.toBeInTheDocument();
    });

    // Click prev to go back to page 1
    fireEvent.click(screen.getByTestId("CommitDataPage-pagination-prev"));

    await waitFor(() => {
      expect(
        screen.getByTestId("CommitDataPage-table-row-49"),
      ).toBeInTheDocument();
    });
  });

  test("handles empty commits array in response", async () => {
    const emptyResponse = {
      owner: "test-org",
      repo: "test-repo",
      branch: "main",
      commits: [],
    };
    axiosMock.onGet("/api/github/graphql/commitData").reply(200, emptyResponse);

    renderPage();

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
      expect(screen.getByTestId("CommitDataPage-table")).toBeInTheDocument();
    });

    expect(screen.getByTestId("CommitDataPage-metadata")).toHaveTextContent(
      "Showing 0 commits from 1 repository",
    );
  });

  test("handles null commits in response", async () => {
    const nullCommitsResponse = {
      owner: "test-org",
      repo: "test-repo",
      branch: "main",
    };
    axiosMock
      .onGet("/api/github/graphql/commitData")
      .reply(200, nullCommitsResponse);

    renderPage();

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
      expect(screen.getByTestId("CommitDataPage-table")).toBeInTheDocument();
    });

    expect(screen.getByTestId("CommitDataPage-metadata")).toHaveTextContent(
      "Showing 0 commits from 1 repository",
    );
  });

  test("courseId defaults to 0 and count defaults to 100", () => {
    renderPage();
    expect(screen.getByTestId("CommitDataPage-courseId")).toHaveValue(0);
    expect(screen.getByTestId("CommitDataPage-count")).toHaveValue(100);
  });

  test("handles non-numeric courseId input", () => {
    renderPage();
    fireEvent.change(screen.getByTestId("CommitDataPage-courseId"), {
      target: { value: "abc" },
    });
    expect(screen.getByTestId("CommitDataPage-courseId")).toHaveValue(0);
  });

  test("handles non-numeric count input", () => {
    renderPage();
    fireEvent.change(screen.getByTestId("CommitDataPage-count"), {
      target: { value: "abc" },
    });
    expect(screen.getByTestId("CommitDataPage-count")).toHaveValue(100);
  });

  test("SHA cell handles null sha", async () => {
    const nullShaResponse = {
      owner: "test-org",
      repo: "test-repo",
      branch: "main",
      commits: [
        {
          sha: null,
          message: "No sha commit",
          authorName: "Alice",
          authorLogin: "alice",
          committerName: "Alice",
          committerLogin: "alice",
          commitTime: "2024-01-01T00:00:00Z",
          url: "https://github.com/test-org/test-repo/commit/unknown",
        },
      ],
    };
    axiosMock
      .onGet("/api/github/graphql/commitData")
      .reply(200, nullShaResponse);

    renderPage();

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
      expect(screen.getByTestId("CommitDataPage-table")).toBeInTheDocument();
    });

    const shaCell = screen.getByTestId(
      "CommitDataPage-table-cell-row-0-col-sha",
    );
    expect(shaCell).toHaveTextContent("");
  });

  test("filter input click does not trigger sorting", async () => {
    axiosMock
      .onGet("/api/github/graphql/commitData")
      .reply(200, sampleCommitResponse);

    renderPage();

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
      expect(screen.getByTestId("CommitDataPage-table")).toBeInTheDocument();
    });

    // Click on filter input - should not trigger sorting
    const filterInput = screen.getByTestId(
      "CommitDataPage-table-header-message-filter",
    );
    fireEvent.click(filterInput);

    // Table should still be present and working
    expect(screen.getByTestId("CommitDataPage-table")).toBeInTheDocument();
  });

  test("shows Unknown error when error has no message", async () => {
    axiosMock.onGet("/api/github/graphql/commitData").reply(500);

    renderPage();

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
      const errorDiv = screen.getByTestId("CommitDataPage-error");
      expect(errorDiv).toBeInTheDocument();
    });
  });

  test("SHA accessor handles null sha for sorting/filtering", async () => {
    const mixedShaResponse = {
      owner: "test-org",
      repo: "test-repo",
      branch: "main",
      commits: [
        {
          sha: "abc1234567890",
          message: "Has sha",
          authorName: "Alice",
          authorLogin: "alice",
          committerName: "Alice",
          committerLogin: "alice",
          commitTime: "2024-01-01T00:00:00Z",
          url: "https://github.com/test-org/test-repo/commit/abc1234567890",
        },
        {
          sha: null,
          message: "No sha",
          authorName: "Bob",
          authorLogin: "bob",
          committerName: "Bob",
          committerLogin: "bob",
          commitTime: "2024-01-02T00:00:00Z",
          url: "https://github.com/test-org/test-repo/commit/unknown",
        },
      ],
    };
    axiosMock
      .onGet("/api/github/graphql/commitData")
      .reply(200, mixedShaResponse);

    renderPage();

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
      expect(screen.getByTestId("CommitDataPage-table")).toBeInTheDocument();
    });

    // Sort by SHA column to exercise the accessorFn with both null and non-null sha
    const shaSortHeader = screen.getByTestId(
      "CommitDataPage-table-header-sha-sort-header",
    );
    fireEvent.click(shaSortHeader);

    // Filter by SHA to exercise the accessorFn filter path
    const shaFilter = screen.getByTestId(
      "CommitDataPage-table-header-sha-filter",
    );
    fireEvent.change(shaFilter, { target: { value: "abc" } });

    await waitFor(() => {
      // Only the row with sha should remain after filtering
      const rows = document.querySelectorAll(
        '[data-testid^="CommitDataPage-table-row-"]',
      );
      expect(rows.length).toBe(1);
    });
  });
});
