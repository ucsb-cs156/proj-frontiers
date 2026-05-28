import { useForm } from "react-hook-form";
import { Button, Form } from "react-bootstrap";

export default function DeleteRepoForm({ submitAction }) {
  const {
    register,
    formState: { errors },
    handleSubmit,
  } = useForm();

  return (
    <Form onSubmit={handleSubmit(submitAction)} data-testid="DeleteRepoForm">
      <Form.Group className="mb-3">
        <Form.Label htmlFor="repoPrefix">Repository Prefix</Form.Label>
        <Form.Control
          id="repoPrefix"
          type="text"
          isInvalid={Boolean(errors.repoPrefix)}
          data-testid="DeleteRepoForm-repoPrefix"
          {...register("repoPrefix", {
            required: "Repository Prefix is required.",
          })}
        />
        <Form.Control.Feedback type="invalid">
          {errors.repoPrefix?.message}
        </Form.Control.Feedback>
        <Form.Text className="text-muted">
          This will delete all repos in the organization that have names
          starting with the prefix, and have no commits.
        </Form.Text>
      </Form.Group>
      <Form.Group>
        <Button type="submit" data-testid="DeleteRepoForm-submit">
          Delete Empty Matching Repos
        </Button>
      </Form.Group>
    </Form>
  );
}
