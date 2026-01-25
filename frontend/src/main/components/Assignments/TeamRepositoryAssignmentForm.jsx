import { useForm } from "react-hook-form";
import { Button, Form } from "react-bootstrap";

export default function TeamRepositoryAssignmentForm({ submitAction }) {
  const {
    register,
    formState: { errors },
    handleSubmit,
  } = useForm();

  return (
    <Form
      onSubmit={handleSubmit(submitAction)}
      data-testid="TeamRepositoryAssignmentForm"
    >
      <Form.Group className="mb-3">
        <Form.Label htmlFor="repoPrefix">Team Repository Prefix</Form.Label>
        <Form.Control
          id="repoPrefix"
          type="text"
          isInvalid={Boolean(errors.repoPrefix)}
          data-testid="TeamRepositoryAssignmentForm-repoPrefix"
          {...register("repoPrefix", {
            required: "Team Repository Prefix is required.",
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
          type="switch"
          data-testid="TeamRepositoryAssignmentForm-assignmentPrivacy"
          {...register("assignmentPrivacy")}
        />
      </Form.Group>
      <Form.Group className="mb-3">
        <Form.Label htmlFor="permissions">Team Permissions</Form.Label>
        <Form.Control
          as="select"
          defaultValue={"MAINTAIN"}
          data-testid={"TeamRepositoryAssignmentForm-permissions"}
          {...register("permissions")}
        >
          <option value="READ">Read</option>
          <option value="WRITE">Write</option>
          <option value="MAINTAIN">Maintain</option>
          <option value="ADMIN">Admin</option>
        </Form.Control>
      </Form.Group>
      <Form.Group>
        <Button type="submit" data-testid="TeamRepositoryAssignmentForm-submit">
          Create
        </Button>
      </Form.Group>
    </Form>
  );
}
