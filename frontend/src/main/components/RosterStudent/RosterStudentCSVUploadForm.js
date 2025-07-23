import { useForm } from "react-hook-form";
import { Button, Form } from "react-bootstrap";
import React from "react";

export default function RosterStudentCSVUploadForm({ submitAction }) {
  const {
    register,
    formState: { errors },
    handleSubmit,
  } = useForm();

  return (
    <Form onSubmit={handleSubmit(submitAction)}>
      <Form.Group className="mb-2">
        <Form.Label htmlFor="upload">Upload Student Roster</Form.Label>
        <Form.Control
          data-testid="RosterStudentCSVUploadForm-upload"
          id="upload"
          type="file"
          accept=".csv"
          isInvalid={Boolean(errors.upload)}
          {...register("upload", { required: true })}
        />
        <Form.Control.Feedback type="invalid">
          {errors.upload && "Roster is required. "}
        </Form.Control.Feedback>
      </Form.Group>
      <Button
        type="submit"
        data-testid="RosterStudentCSVUploadForm-submit"
        className="mt-3"
      >
        Upload
      </Button>
    </Form>
  );
}
