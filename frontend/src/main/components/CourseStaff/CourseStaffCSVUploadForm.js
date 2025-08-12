import { useForm } from "react-hook-form";
import { Button, Form } from "react-bootstrap";
import React from "react";

export default function CourseStaffCSVUploadForm({ submitAction }) {
  const {
    register,
    formState: { errors },
    handleSubmit,
  } = useForm();

  return (
    <Form onSubmit={handleSubmit(submitAction)}>
      <Form.Group className="mb-2">
        <Form.Label htmlFor="upload">Upload Staff Roster</Form.Label>
        <Form.Control
          data-testid="CourseStaffCSVUploadForm-upload"
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
        data-testid="CourseStaffCSVUploadForm-submit"
        className="mt-3"
      >
        Upload
      </Button>
    </Form>
  );
}
