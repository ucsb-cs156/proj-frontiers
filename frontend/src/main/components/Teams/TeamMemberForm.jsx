import { Button, Form } from "react-bootstrap";
import { useForm, Controller } from "react-hook-form";
import { useNavigate } from "react-router";
import RosterStudentDropdown from "main/components/RosterStudent/RosterStudentDropdown";

function TeamMemberForm({
  initialContents,
  rosterStudents,
  submitAction,
  buttonLabel = "Add Member",
  cancelDisabled = false,
}) {
  // Stryker disable all
  const {
    control,
    formState: { errors },
    handleSubmit,
  } = useForm({ defaultValues: initialContents || {} });
  // Stryker restore all

  const navigate = useNavigate();

  const testIdPrefix = "TeamMemberForm";

  const onSubmit = (data) => {
    submitAction(data);
  };

  return (
    <Form onSubmit={handleSubmit(onSubmit)}>
      <Form.Group className="mb-3">
        <Form.Label htmlFor="rosterStudentId">Select Student</Form.Label>
        <Controller
          name="rosterStudentId"
          control={control}
          rules={{ required: "Select a student." }}
          render={({ field }) => (
            <RosterStudentDropdown
              rosterStudents={rosterStudents}
              {...field}
              isInvalid={Boolean(errors.rosterStudentId)}
            />
          )}
        />
        {errors.rosterStudentId && (
          <Form.Control.Feedback type="invalid">
            {errors.rosterStudentId.message}
          </Form.Control.Feedback>
        )}
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

export default TeamMemberForm;
