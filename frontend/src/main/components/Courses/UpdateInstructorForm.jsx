import { Button, Form } from "react-bootstrap";
import { useForm } from "react-hook-form";

export default function UpdateInstructorForm({
  initialContents,
  handleUpdateInstructor,
}) {
  const {
    register,
    formState: { errors },
    handleSubmit,
  } = useForm({ defaultValues: initialContents || {} });

  return (
    <Form onSubmit={handleSubmit(handleUpdateInstructor)}>
      <Form.Group>
        <Form.Label htmlFor="courseName">Course Name</Form.Label>
        <Form.Control
          id="courseName"
          type="text"
          disabled
          {...register("courseName")}
        />
        <Form.Label htmlFor="instructorEmail">New Instructor Email</Form.Label>
        <Form.Control
          data-testid="update-instructor-email-input"
          id="instructorEmail"
          type="email"
          isInvalid={Boolean(errors.instructorEmail)}
          {...register("instructorEmail", {
            required: "Instructor email is required.",
          })}
        />
        <Form.Control.Feedback type="invalid">
          {errors.instructorEmail?.message}
        </Form.Control.Feedback>
        <Form.Text className="text-muted">
          Email must belong to an existing instructor or admin.
        </Form.Text>
      </Form.Group>
      <Button
        type="submit"
        variant="primary"
        data-testid="update-instructor-submit-button"
      >
        Update Instructor
      </Button>
    </Form>
  );
}
