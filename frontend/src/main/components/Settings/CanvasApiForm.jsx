import { Button, Form } from "react-bootstrap";
import { useForm } from "react-hook-form";
import { useBackend } from "main/utils/useBackend";
import React from "react";

function CanvasApiForm({
  submitAction,
  buttonLabel = "Connect Canvas",
  courseId,
}) {
  const {
    register,
    formState: { errors },
    handleSubmit,
    reset,
    setError,
  } = useForm();

  const { data: canvasInfo } = useBackend(
    // Stryker disable next-line all : don't test internal caching of React Query
    [`/api/courses/getCanvasInfo?courseId=${courseId}`],
    // Stryker disable next-line StringLiteral : The default value for an empty ("") method is GET. Therefore, there is no way to kill a mutation that transforms "GET" to ""
    { method: "GET", url: `/api/courses/getCanvasInfo?courseId=${courseId}` },
    // Stryker disable next-line all : don't test default value of empty list
    [],
  );

  const testIdPrefix = "CanvasApiForm";

  return (
    <Form
      onSubmit={handleSubmit((data) => {
        const token = data.canvasApiToken.trim();
        const courseIdVal = data.canvasCourseId.trim();

        if (!token && !courseIdVal) {
          setError("root", { message: "Please fill in at least one field." });
          return;
        }
        submitAction({ canvasApiToken: token, canvasCourseId: courseIdVal });
        reset();
      })}
    >
      <Form.Group className="mb-3">
        <Form.Label htmlFor="canvasApiToken">Canvas API Token</Form.Label>
        <Form.Control
          data-testid={testIdPrefix + "-canvasApiToken"}
          id="canvasApiToken"
          type="text"
          placeholder={
            canvasInfo.canvasApiToken
              ? `Current Token: ${canvasInfo.canvasApiToken}`
              : "Token not set yet."
          }
          isInvalid={Boolean(errors.canvasApiToken)}
          {...register("canvasApiToken")}
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
          placeholder={
            canvasInfo.canvasCourseId
              ? `Current Course ID: ${canvasInfo.canvasCourseId}`
              : "Course ID not set yet."
          }
          isInvalid={Boolean(errors.canvasCourseId)}
          {...register("canvasCourseId")}
        />
        <Form.Control.Feedback type="invalid">
          {errors.canvasCourseId?.message}
        </Form.Control.Feedback>
      </Form.Group>
      {errors.root && (
        <Form.Text className="text-danger d-block mb-2">
          {errors.root.message}
        </Form.Text>
      )}

      <Button type="submit" data-testid={testIdPrefix + "-submit"}>
        {buttonLabel}
      </Button>
    </Form>
  );
}

export default CanvasApiForm;
