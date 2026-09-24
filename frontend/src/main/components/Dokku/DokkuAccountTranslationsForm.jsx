import { Button, Form } from "react-bootstrap";
import { useForm } from "react-hook-form";

function DokkuAccountTranslationsForm({
  initialContents,
  submitAction,
  buttonLabel = "Create",
}) {
  // Stryker disable all
  const {
    register,
    formState: { errors },
    handleSubmit,
  } = useForm({ defaultValues: initialContents || {} });
  // Stryker restore all

  const testIdPrefix = "DokkuAccountTranslationsForm";

  // The email is the key of a translation, so it cannot be changed once the
  // translation exists; to change it, delete the translation and create a
  // new one.
  const isEditing = Boolean(initialContents);

  return (
    <Form onSubmit={handleSubmit(submitAction)}>
      <Form.Group className="mb-3">
        <Form.Label htmlFor="email">Email</Form.Label>
        <Form.Control
          data-testid={testIdPrefix + "-email"}
          id="email"
          type="text"
          disabled={isEditing}
          isInvalid={Boolean(errors.email)}
          {...register("email", {
            required: "Email is required.",
          })}
        />
        <Form.Control.Feedback type="invalid">
          {errors.email?.message}
        </Form.Control.Feedback>
      </Form.Group>

      <Form.Group className="mb-3">
        <Form.Label htmlFor="username">Dokku Username</Form.Label>
        <Form.Control
          data-testid={testIdPrefix + "-username"}
          id="username"
          type="text"
          isInvalid={Boolean(errors.username)}
          {...register("username", {
            required: "Dokku Username is required.",
          })}
        />
        <Form.Control.Feedback type="invalid">
          {errors.username?.message}
        </Form.Control.Feedback>
      </Form.Group>

      <Button type="submit" data-testid={testIdPrefix + "-submit"}>
        {buttonLabel}
      </Button>
    </Form>
  );
}

export default DokkuAccountTranslationsForm;
