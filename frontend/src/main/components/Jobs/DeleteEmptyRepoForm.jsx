import React, { useState } from "react";
import { Form, Button, Row, Col } from "react-bootstrap";
import { useBackendMutation } from "main/utils/useBackend"; // Adjust path if needed
import { toast } from "react-toastify";

const DeleteEmptyReposForm = ({ courseId }) => {
  const [prefix, setPrefix] = useState("");

  // Configure the Axios request for the DELETE endpoint
  const objectToAxiosParams = () => ({
    url: "/api/repos",
    method: "DELETE",
    params: {
      courseId: courseId,
      prefix: prefix,
    },
  });

  // Setup the mutation using your team's standard hook
  const deleteMutation = useBackendMutation(
    objectToAxiosParams,
    {
      onSuccess: () => {
        toast(`Delete empty repos job launched for prefix: ${prefix}`);
        setPrefix(""); // Clear the input after launching
      },
      onError: (error) => {
        toast.error(
          `Error starting job: ${error.response?.data?.message || error.message}`,
        );
      },
    },
    // We don't necessarily need to invalidate a cache key here unless you display jobs on the same page
    [],
  );

  const onSubmit = (event) => {
    event.preventDefault();
    if (!prefix) {
      toast.error("Please enter a prefix");
      return;
    }
    deleteMutation.mutate();
  };

  return (
    <Form onSubmit={onSubmit} className="p-3 border rounded bg-light mb-4">
      <h5>Delete Empty Repositories</h5>
      <Form.Group className="mb-3" controlId="prefixInput">
        <Form.Label>Repository Prefix</Form.Label>
        <Row>
          <Col sm={8} md={6}>
            <Form.Control
              type="text"
              placeholder="e.g., lab01"
              value={prefix}
              onChange={(e) => setPrefix(e.target.value)}
              data-testid="DeleteEmptyReposForm-prefix"
            />
          </Col>
          <Col sm={4} md={6}>
            <Button
              variant="danger"
              type="submit"
              data-testid="DeleteEmptyReposForm-submit"
              disabled={deleteMutation.isLoading}
            >
              {deleteMutation.isLoading
                ? "Launching..."
                : "Delete Empty Matching Repos"}
            </Button>
          </Col>
        </Row>
        <Form.Text className="text-muted text-danger mt-2 d-block">
          <strong>Warning:</strong> This will kick off a background job to
          delete all repositories in the organization that have names starting
          with this prefix <strong>and have no commits</strong>. Repositories
          with commits will be kept safe.
        </Form.Text>
      </Form.Group>
    </Form>
  );
};

export default DeleteEmptyReposForm;
