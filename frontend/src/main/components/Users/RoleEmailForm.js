import { Button, Form } from "react-bootstrap";
import { useForm } from "react-hook-form";
import { useNavigate } from "react-router-dom";

function RoleEmailForm({
  initialContents,
  submitAction,
  buttonLabel = "Create",
  roleName,
}) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm({
    defaultValues: {
      email: initialContents?.email || "",
    },
  });

  const navigate = useNavigate();

  // Stryker disable Regex
  const emailRegex = /(.?)@(.?).(.*?)/;
  // Stryker restore Regex

  return (
    <Form onSubmit={handleSubmit((data) => submitAction(data))}>
      <Form.Group className="mb-3" controlId="email">
        <Form.Label>{roleName} Email</Form.Label>
        <Form.Control
          type="Create Email"
          placeholder={`Enter email`}
          {...register("email", {
            required: `Email is required.`,
            pattern: { value: emailRegex, message: "Invalid email address." },
          })}
          isInvalid={Boolean(errors.email)}
        />
        <Form.Control.Feedback type="invalid">
          {errors.email?.message}
        </Form.Control.Feedback>
      </Form.Group>

      <Button type="submit" data-testid="RoleEmailForm-submit">
        {buttonLabel}
      </Button>
      <Button
        variant="Secondary"
        className="ms-2"
        onClick={() => navigate(-1)}
        data-testid="RoleEmailForm-cancel"
      >
        Cancel
      </Button>
    </Form>
  );
}

export default RoleEmailForm;
