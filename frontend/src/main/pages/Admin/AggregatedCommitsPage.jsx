import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import AggregatedCommitsTable from "main/components/Commits/AggregatedCommitsTable";
import OurPagination from "main/components/Common/OurPagination";
import { useBackend, useBackendMutation } from "main/utils/useBackend";
import { useState } from "react";
import { Button, Form, ListGroup } from "react-bootstrap";
import { toast } from "react-toastify";

const AggregatedCommitsPage = () => {
  const [courseId, setCourseId] = useState("");
  const [owner, setOwner] = useState("");
  const [repo, setRepo] = useState("");
  const [branch, setBranch] = useState("");
  const [branches, setBranches] = useState([]);
  const [sessionId, setSessionId] = useState(null);
  const [currentPage, setCurrentPage] = useState(1);

  const createSession = useBackendMutation(
    (data) => ({
      method: "POST",
      url: `/api/github/graphql/create_commit_session`,
      params: { courseId: data.courseId },
      data: data.branches,
    }),
    {
      onSuccess: (response) => {
        setSessionId(response.sessionId);
        setCurrentPage(1);
        toast("Session created successfully!");
      },
    },
  );

  const {
    data: commits,
    error: _error,
    status: _status,
  } = useBackend(
    // Stryker disable next-line all : don't test internal caching of React Query
    sessionId
      ? [
          `/api/github/graphql/aggregated_commits/${sessionId}/${currentPage - 1}`,
        ]
      : null,
    {
      method: "GET",
      url: `/api/github/graphql/aggregated_commits`,
      params: {
        sessionId,
        page: currentPage - 1,
        size: 20,
        sort: "commitTime,desc",
      },
    },
    { content: [], page: { totalPages: 1 } },
    false,
    { enabled: !!sessionId },
  );

  const handleAddBranch = () => {
    if (!owner || !repo || !branch) {
      toast("Please fill in owner, repo, and branch");
      return;
    }
    setBranches([...branches, { owner, repo, branch }]);
    setOwner("");
    setRepo("");
    setBranch("");
  };

  const handleRemoveBranch = (index) => {
    setBranches(branches.filter((_, i) => i !== index));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!courseId) {
      toast("Please enter a course ID");
      return;
    }
    if (branches.length === 0) {
      toast("Please add at least one branch");
      return;
    }
    createSession.mutate({
      courseId,
      branches,
    });
  };

  return (
    <BasicLayout>
      <h2>Aggregated Commits</h2>
      <Form onSubmit={handleSubmit} data-testid="AggregatedCommitsForm">
        <Form.Group className="mb-3">
          <Form.Label>Course ID</Form.Label>
          <Form.Control
            type="text"
            value={courseId}
            onChange={(e) => setCourseId(e.target.value)}
            placeholder="Enter course ID"
            data-testid="AggregatedCommitsForm-courseId"
          />
        </Form.Group>

        <h5>Branches</h5>
        {branches.length > 0 && (
          <ListGroup
            className="mb-3"
            data-testid="AggregatedCommitsForm-branchList"
          >
            {branches.map((b, i) => (
              <ListGroup.Item
                key={i}
                className="d-flex justify-content-between align-items-center"
                data-testid={`AggregatedCommitsForm-branchItem-${i}`}
              >
                <span>
                  {b.owner}/{b.repo} — {b.branch}
                </span>
                <Button
                  variant="outline-danger"
                  size="sm"
                  onClick={() => handleRemoveBranch(i)}
                  data-testid={`AggregatedCommitsForm-removeBranch-${i}`}
                >
                  Remove
                </Button>
              </ListGroup.Item>
            ))}
          </ListGroup>
        )}

        <Form.Group className="mb-3">
          <Form.Label>Owner</Form.Label>
          <Form.Control
            type="text"
            value={owner}
            onChange={(e) => setOwner(e.target.value)}
            placeholder="e.g. ucsb-cs156"
            data-testid="AggregatedCommitsForm-owner"
          />
        </Form.Group>
        <Form.Group className="mb-3">
          <Form.Label>Repository</Form.Label>
          <Form.Control
            type="text"
            value={repo}
            onChange={(e) => setRepo(e.target.value)}
            placeholder="e.g. proj-frontiers"
            data-testid="AggregatedCommitsForm-repo"
          />
        </Form.Group>
        <Form.Group className="mb-3">
          <Form.Label>Branch</Form.Label>
          <Form.Control
            type="text"
            value={branch}
            onChange={(e) => setBranch(e.target.value)}
            placeholder="e.g. main"
            data-testid="AggregatedCommitsForm-branch"
          />
        </Form.Group>
        <Button
          variant="secondary"
          className="me-2"
          onClick={handleAddBranch}
          data-testid="AggregatedCommitsForm-addBranch"
        >
          Add Branch
        </Button>
        <Button
          type="submit"
          variant="primary"
          data-testid="AggregatedCommitsForm-submit"
        >
          Create Session &amp; View Commits
        </Button>
      </Form>

      {sessionId && (
        <>
          <h3 className="mt-4">Commits for session: {sessionId}</h3>
          <AggregatedCommitsTable commits={commits.content} />
          <div className="d-flex justify-content-evenly">
            <OurPagination
              currentActivePage={currentPage}
              updateActivePage={setCurrentPage}
              totalPages={commits.page.totalPages}
            />
          </div>
        </>
      )}
    </BasicLayout>
  );
};

export default AggregatedCommitsPage;
