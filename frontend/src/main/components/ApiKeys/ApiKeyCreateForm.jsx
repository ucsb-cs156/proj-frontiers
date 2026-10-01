import React from "react";
import { Button, Form } from "react-bootstrap";
import { useForm } from "react-hook-form";

export default function ApiKeyCreateForm({
  submitAction,
  testIdPrefix = "ApiKeyCreateForm",
}) {
  const {
    register,
    formState: { errors },
    handleSubmit,
  } = useForm({ defaultValues: { label: "", choice: "DAYS_90" } });

  return (
    <Form
      onSubmit={handleSubmit((data) =>
        submitAction({ label: data.label.trim(), choice: data.choice }),
      )}
    >
      <Form.Group className="mb-3">
        <Form.Label htmlFor="label">Label (optional)</Form.Label>
        <Form.Control
          id="label"
          type="text"
          placeholder="e.g. jpa02 autograder F26"
          data-testid={`${testIdPrefix}-label`}
          isInvalid={Boolean(errors.label)}
          {...register("label", {
            maxLength: {
              value: 60,
              message: "Label must be at most 60 characters.",
            },
          })}
        />
        <Form.Control.Feedback type="invalid">
          {errors.label?.message}
        </Form.Control.Feedback>
        <Form.Text className="text-muted">
          A short name, so that you can tell this key apart from others later.
        </Form.Text>
      </Form.Group>

      <Form.Group className="mb-3">
        <Form.Label>Expires after</Form.Label>
        <Form.Check
          type="radio"
          id="choice-DAYS_90"
          label="90 days"
          value="DAYS_90"
          data-testid={`${testIdPrefix}-choice-DAYS_90`}
          {...register("choice")}
        />
        <Form.Check
          type="radio"
          id="choice-MONTHS_6"
          label="6 months"
          value="MONTHS_6"
          data-testid={`${testIdPrefix}-choice-MONTHS_6`}
          {...register("choice")}
        />
      </Form.Group>

      <Button type="submit" data-testid={`${testIdPrefix}-submit`}>
        Create API Key
      </Button>
    </Form>
  );
}
