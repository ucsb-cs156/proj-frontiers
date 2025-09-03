import { useForm } from "react-hook-form";
import { Button, Form } from "react-bootstrap";
import React from "react";

export default function TeamsCSVUploadForm({ submitAction }) {
  const {
    register,
    formState: { errors },
    handleSubmit,
  } = useForm();

  return (
    <Form onSubmit={handleSubmit(submitAction)}>
      <Form.Group className="mb-2">
        <Form.Label htmlFor="upload">
          Please select a CSV file to upload.
        </Form.Label>
        <Form.Control
          data-testid="TeamsCSVUploadForm-upload"
          id="upload"
          type="file"
          accept=".csv"
          isInvalid={Boolean(errors.upload)}
          {...register("upload", { required: true })}
        />
        <Form.Control.Feedback type="invalid">
          {errors.upload && "Team CSV is required. "}
        </Form.Control.Feedback>
      </Form.Group>
      <Button
        type="submit"
        data-testid="TeamsCSVUploadForm-submit"
        className="mt-3"
      >
        Upload
      </Button>
    </Form>
  );
}
