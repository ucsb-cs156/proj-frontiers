import { Button, Form } from "react-bootstrap";
import { useForm } from "react-hook-form";

function CanvasApiForm({
  submitAction,
  buttonLabel = "Connect Canvas",
}) {
  // Stryker disable all
  const {
    register,
    formState: { errors },
    handleSubmit,
  } = useForm();
  // Stryker restore all

  const testIdPrefix = "CanvasApiForm";

  return (
    <Form onSubmit={handleSubmit(submitAction)}>
      <Form.Group className="mb-3">
        <Form.Label htmlFor="canvasApiToken">Canvas API Token</Form.Label>
        <Form.Control
          data-testid={testIdPrefix + "-canvasApiToken"}
          id="canvasApiToken"
          type="text"
          isInvalid={Boolean(errors.canvasApiToken)}
          {...register("canvasApiToken", {
            required: "Canvas API Token is required.",
          })}
        />
        <Form.Control.Feedback type="invalid">
          {errors.canvasApiToken?.message}
        </Form.Control.Feedback>
      </Form.Group>
      <Form.Group className="mb-3">
        <Form.Label htmlFor="canvasCourseId">Canvas Course ID</Form.Label>
        <Form.Control
          data-testid={testIdPrefix + "-canvasCourseId"}
          id="canvasCourseId"
          type="text"
          isInvalid={Boolean(errors.canvasCourseId)}
          {...register("canvasCourseId", {
            required: "Canvas Course ID is required.",
          })}
        />
        <Form.Control.Feedback type="invalid">
          {errors.canvasCourseId?.message}
        </Form.Control.Feedback>
      </Form.Group>

      <Button type="submit" data-testid={testIdPrefix + "-submit"}>
        {buttonLabel}
      </Button>
    </Form>
  );
}

export default CanvasApiForm;
