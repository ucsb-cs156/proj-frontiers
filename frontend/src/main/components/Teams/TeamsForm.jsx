import { Button, Form } from "react-bootstrap";
import { useForm } from "react-hook-form";
import { useNavigate } from "react-router";

function TeamsForm({
  initialContents,
  submitAction,
  buttonLabel = "Create",
  cancelDisabled = false,
}) {
  // Stryker disable all
  const {
    register,
    formState: { errors },
    handleSubmit,
  } = useForm({ defaultValues: initialContents || {} });
  // Stryker restore all

  const navigate = useNavigate();

  const testIdPrefix = "TeamsForm";

  return (
    <Form onSubmit={handleSubmit(submitAction)}>
      <Form.Group className="mb-3">
        <Form.Label htmlFor="name">Team Name</Form.Label>
        <Form.Control
          data-testid={testIdPrefix + "-name"}
          id="name"
          type="text"
          isInvalid={Boolean(errors.name)}
          {...register("name", {
            required: "Team Name is required.",
          })}
        />
        <Form.Control.Feedback type="invalid">
          {errors.name?.message}
        </Form.Control.Feedback>
      </Form.Group>

      <Form.Group className="mb-3">
        <Form.Check
          type="switch"
          id="isStaffTeam"
          label="Staff Team"
          data-testid={testIdPrefix + "-isStaffTeam"}
          {...register("isStaffTeam")}
        />
        <Form.Text muted>
          A staff team stands in for a student team: its members are the course
          staff, so that the staff can try out team repositories and autograders
          themselves.
        </Form.Text>
      </Form.Group>

      <Button type="submit" data-testid={testIdPrefix + "-submit"}>
        {buttonLabel}
      </Button>
      {!cancelDisabled && (
        <Button
          variant="Secondary"
          onClick={() => navigate(-1)}
          data-testid={testIdPrefix + "-cancel"}
        >
          Cancel
        </Button>
      )}
    </Form>
  );
}

export default TeamsForm;
