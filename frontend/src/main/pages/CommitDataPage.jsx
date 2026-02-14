import { useState, useEffect, useRef } from "react";
import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import {
  flexRender,
  getCoreRowModel,
  getFilteredRowModel,
  getSortedRowModel,
  getPaginationRowModel,
  useReactTable,
} from "@tanstack/react-table";
import OurPagination from "main/components/Common/OurPagination";
import SortCaret from "main/components/Common/SortCaret";
import { convertOldStyleColumnsToNewStyle } from "main/components/OurTableUtils";
import { useBackend } from "main/utils/useBackend";
import { Form, Button, Row, Col, ListGroup } from "react-bootstrap";

export default function CommitDataPage() {
  const [courseId, setCourseId] = useState(0);
  const [owner, setOwner] = useState("");
  const [repo, setRepo] = useState("");
  const [branch, setBranch] = useState("");
  const [count, setCount] = useState(100);
  const [params, setParams] = useState(null);
  const [commitHistories, setCommitHistories] = useState([]);
  const [columnFilters, setColumnFilters] = useState([]);
  const keyCounter = useRef(0);

  const {
    data: commitHistory,
    error,
    isLoading,
  } = useBackend(
    ["/api/github/graphql/commitData", params],
    {
      method: "GET",
      url: "/api/github/graphql/commitData",
      params: params,
    },
    null,
    false,
    { enabled: !!params },
  );

  useEffect(() => {
    if (commitHistory) {
      const key = keyCounter.current++;
      const enrichedCommits = (commitHistory.commits || []).map((commit) => ({
        ...commit,
        _owner: commitHistory.owner,
        _repo: commitHistory.repo,
        _branch: commitHistory.branch,
      }));
      setCommitHistories((prev) => [
        ...prev,
        { ...commitHistory, commits: enrichedCommits, _key: key },
      ]);
      setParams(null);
    }
  }, [commitHistory]);

  const handleSubmit = (e) => {
    e.preventDefault();
    setParams({ courseId, owner, repo, branch, count });
  };

  const handleRemove = (key) => {
    setCommitHistories((prev) => prev.filter((h) => h._key !== key));
  };

  const handleClearAll = () => {
    setCommitHistories([]);
  };

  const allCommits = commitHistories.flatMap((h) => h.commits);

  const columns = [
    {
      header: "Owner/Repo",
      accessorFn: (row) => `${row._owner}/${row._repo}`,
      id: "ownerRepo",
    },
    {
      header: "SHA",
      accessorFn: (row) => (row.sha ? row.sha.substring(0, 7) : ""),
      id: "sha",
      cell: ({ row }) => (
        <a href={row.original.url} target="_blank" rel="noopener noreferrer">
          {row.original.sha ? row.original.sha.substring(0, 7) : ""}
        </a>
      ),
    },
    {
      header: "Message",
      accessorKey: "message",
    },
    {
      header: "Author Name",
      accessorKey: "authorName",
    },
    {
      header: "Author Login",
      accessorKey: "authorLogin",
    },
    {
      header: "Committer Name",
      accessorKey: "committerName",
    },
    {
      header: "Committer Login",
      accessorKey: "committerLogin",
    },
    {
      header: "Commit Time",
      accessorKey: "commitTime",
    },
    {
      header: "URL",
      accessorKey: "url",
      cell: ({ cell }) => (
        <a href={cell.getValue()} target="_blank" rel="noopener noreferrer">
          {cell.getValue()}
        </a>
      ),
    },
  ];

  const testid = "CommitDataPage-table";

  const table = useReactTable({
    data: allCommits,
    columns: convertOldStyleColumnsToNewStyle(columns),
    getCoreRowModel: getCoreRowModel(),
    getFilteredRowModel: getFilteredRowModel(),
    getSortedRowModel: getSortedRowModel(),
    getPaginationRowModel: getPaginationRowModel(),
    initialState: { pagination: { pageSize: 50, pageIndex: 0 } },
    state: { columnFilters },
    onColumnFiltersChange: setColumnFilters,
  });

  return (
    <BasicLayout>
      <div data-testid="CommitDataPage">
        <h1>Commit Data</h1>
        <Form onSubmit={handleSubmit}>
          <Row className="mb-3">
            <Col md={4}>
              <Form.Group controlId="CommitDataPage-courseId">
                <Form.Label>Course ID</Form.Label>
                <Form.Control
                  type="number"
                  data-testid="CommitDataPage-courseId"
                  value={courseId}
                  onChange={(e) =>
                    setCourseId(parseInt(e.target.value, 10) || 0)
                  }
                  required
                />
              </Form.Group>
            </Col>
            <Col md={4}>
              <Form.Group controlId="CommitDataPage-owner">
                <Form.Label>Owner</Form.Label>
                <Form.Control
                  type="text"
                  data-testid="CommitDataPage-owner"
                  value={owner}
                  onChange={(e) => setOwner(e.target.value)}
                  required
                />
              </Form.Group>
            </Col>
            <Col md={4}>
              <Form.Group controlId="CommitDataPage-repo">
                <Form.Label>Repo</Form.Label>
                <Form.Control
                  type="text"
                  data-testid="CommitDataPage-repo"
                  value={repo}
                  onChange={(e) => setRepo(e.target.value)}
                  required
                />
              </Form.Group>
            </Col>
          </Row>
          <Row className="mb-3">
            <Col md={4}>
              <Form.Group controlId="CommitDataPage-branch">
                <Form.Label>Branch</Form.Label>
                <Form.Control
                  type="text"
                  data-testid="CommitDataPage-branch"
                  value={branch}
                  onChange={(e) => setBranch(e.target.value)}
                  required
                />
              </Form.Group>
            </Col>
            <Col md={4}>
              <Form.Group controlId="CommitDataPage-count">
                <Form.Label>Count</Form.Label>
                <Form.Control
                  type="number"
                  data-testid="CommitDataPage-count"
                  value={count}
                  onChange={(e) =>
                    setCount(parseInt(e.target.value, 10) || 100)
                  }
                />
              </Form.Group>
            </Col>
            <Col md={4} className="d-flex align-items-end">
              <Button
                type="submit"
                data-testid="CommitDataPage-fetch-button"
                disabled={isLoading}
                className="me-2"
              >
                {isLoading ? "Loading..." : "Add Commits"}
              </Button>
              {commitHistories.length > 0 && (
                <Button
                  variant="danger"
                  data-testid="CommitDataPage-clear-button"
                  onClick={handleClearAll}
                >
                  Clear All
                </Button>
              )}
            </Col>
          </Row>
        </Form>

        {commitHistories.length > 0 && (
          <ListGroup className="mb-3" data-testid="CommitDataPage-added-repos">
            {commitHistories.map((h, index) => (
              <ListGroup.Item
                key={h._key}
                className="d-flex justify-content-between align-items-center"
              >
                {h.owner}/{h.repo} ({h.branch}) - {h.commits.length}{" "}
                {h.commits.length === 1 ? "commit" : "commits"}
                <Button
                  variant="outline-danger"
                  size="sm"
                  data-testid={`CommitDataPage-remove-${index}`}
                  onClick={() => handleRemove(h._key)}
                >
                  Remove
                </Button>
              </ListGroup.Item>
            ))}
          </ListGroup>
        )}

        {error && (
          <div className="text-danger" data-testid="CommitDataPage-error">
            Error fetching commit data: {error?.message || "Unknown error"}
          </div>
        )}

        {commitHistories.length > 0 && (
          <>
            <div className="mb-3" data-testid="CommitDataPage-metadata">
              <p>
                Showing {allCommits.length}{" "}
                {allCommits.length === 1 ? "commit" : "commits"} from{" "}
                {commitHistories.length}{" "}
                {commitHistories.length === 1 ? "repository" : "repositories"}
              </p>
            </div>
            <table
              className="table table-striped table-bordered"
              data-testid={testid}
            >
              <thead>
                {table.getHeaderGroups().map((headerGroup, i) => (
                  <tr
                    data-testid={`${testid}-header-group-${i}`}
                    key={`${testid}-header-group-${i}`}
                  >
                    {headerGroup.headers.map((header) => (
                      <th
                        data-testid={`${testid}-header-${header.column.id}`}
                        key={`${testid}-header-${header.column.id}`}
                        colSpan={header.colSpan}
                      >
                        {header.isPlaceholder ? null : (
                          <div
                            {...(header.column.getCanSort() && {
                              onClick: header.column.getToggleSortingHandler(),
                              style: { cursor: "pointer" },
                            })}
                            data-testid={`${testid}-header-${header.column.id}-sort-header`}
                          >
                            {flexRender(
                              header.column.columnDef.header,
                              header.getContext(),
                            )}
                            <SortCaret header={header} testId={testid} />
                          </div>
                        )}
                        {header.column.getCanFilter() && (
                          <input
                            type="text"
                            placeholder={`Filter...`}
                            value={header.column.getFilterValue() ?? ""}
                            onChange={(e) =>
                              header.column.setFilterValue(e.target.value)
                            }
                            onClick={(e) => e.stopPropagation()}
                            data-testid={`${testid}-header-${header.column.id}-filter`}
                            className="form-control form-control-sm mt-1"
                          />
                        )}
                      </th>
                    ))}
                  </tr>
                ))}
              </thead>
              <tbody>
                {table.getRowModel().rows.map((row) => {
                  const rowTestId = `${testid}-row-${row.index}`;
                  return (
                    <tr data-testid={rowTestId} key={rowTestId}>
                      {row.getVisibleCells().map((cell) => {
                        const cellTestId = `${testid}-cell-row-${cell.row.index}-col-${cell.column.id}`;
                        return (
                          <td data-testid={cellTestId} key={cellTestId}>
                            {flexRender(
                              cell.column.columnDef.cell,
                              cell.getContext(),
                            )}
                          </td>
                        );
                      })}
                    </tr>
                  );
                })}
              </tbody>
            </table>
            {table.getPageCount() > 1 && (
              <OurPagination
                currentActivePage={table.getState().pagination.pageIndex + 1}
                updateActivePage={(page) => table.setPageIndex(page - 1)}
                totalPages={table.getPageCount()}
                testId="CommitDataPage-pagination"
              />
            )}
          </>
        )}
      </div>
    </BasicLayout>
  );
}
