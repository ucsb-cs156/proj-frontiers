import { Button, Form } from "react-bootstrap";
import { useForm } from "react-hook-form";

function SectionsForm({
  initialContents,
  submitAction,
  buttonLabel = "Create",
  showSlackChannel = false,
}) {
  // Stryker disable all
  const {
    register,
    formState: { errors },
    handleSubmit,
  } = useForm({ defaultValues: initialContents || {} });
  // Stryker restore all

  const testIdPrefix = "SectionsForm";

  return (
    <Form onSubmit={handleSubmit(submitAction)}>
      <Form.Group className="mb-3">
        <Form.Label htmlFor="section">Section</Form.Label>
        <Form.Control
          data-testid={testIdPrefix + "-section"}
          id="section"
          type="text"
          isInvalid={Boolean(errors.section)}
          {...register("section", {
            required: "Section is required.",
          })}
        />
        <Form.Control.Feedback type="invalid">
          {errors.section?.message}
        </Form.Control.Feedback>
      </Form.Group>

      <Form.Group className="mb-3">
        <Form.Label htmlFor="label">Label</Form.Label>
        <Form.Control
          data-testid={testIdPrefix + "-label"}
          id="label"
          type="text"
          isInvalid={Boolean(errors.label)}
          {...register("label", {
            required: "Label is required.",
          })}
        />
        <Form.Control.Feedback type="invalid">
          {errors.label?.message}
        </Form.Control.Feedback>
      </Form.Group>

      {showSlackChannel && (
        <Form.Group className="mb-3">
          <Form.Label htmlFor="slackChannelName">Slack Channel Name</Form.Label>
          <Form.Control
            data-testid={testIdPrefix + "-slackChannelName"}
            id="slackChannelName"
            type="text"
            {...register("slackChannelName")}
          />
        </Form.Group>
      )}

      <Button type="submit" data-testid={testIdPrefix + "-submit"}>
        {buttonLabel}
      </Button>
    </Form>
  );
}

export default SectionsForm;
