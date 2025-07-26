import { useForm } from "react-hook-form";
import { Button, Form } from "react-bootstrap";

export default function IndividualAssignmentForm({ submitAction }) {
  const {
    register,
    formState: { errors },
    handleSubmit,
  } = useForm();

  return (
    <Form onSubmit={handleSubmit(submitAction)}>
      <Form.Group>
        <Form.Label htmlFor="repoPrefix">Repository Prefix</Form.Label>
        <Form.Control
          id="repoPrefix"
          type="text"
          isInvalid={Boolean(errors.repoPrefix)}
          data-testid="IndividualAssignmentForm-repoPrefix"
          {...register("repoPrefix", {
            required: "Repository Prefix is required.",
          })}
        />
        <Form.Control.Feedback type="invalid">
          {errors.repoPrefix?.message}
        </Form.Control.Feedback>
      </Form.Group>
      <Form.Group className="mb-3">
        <Form.Label htmlFor="assignmentPrivacy">
          Private Repositories?
        </Form.Label>
        <Form.Check
          id="assignmentPrivacy"
          type="checkbox"
          data-testid="IndividualAssignmentForm-assignmentPrivacy"
          {...register("assignmentPrivacy")}
        />
      </Form.Group>
      <Form.Group>
        <Button type="submit" data-testid="IndividualAssignmentForm-submit">
          Create
        </Button>
      </Form.Group>
    </Form>
  );
}
