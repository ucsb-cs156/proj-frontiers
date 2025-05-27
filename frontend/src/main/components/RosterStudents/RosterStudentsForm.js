import { Button, Form, Row, Col } from "react-bootstrap";
import { useForm } from "react-hook-form";
import { useNavigate } from "react-router-dom";

function RosterStudentsForm({
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

  const navigate = useNavigate();

  // Stryker disable all
  const emailRegex = /(.?)@(.?).(.*?)/;
  // Stryker restore all

  return (
    <Form onSubmit={handleSubmit(submitAction)}>
      <Row>
        <Col>
          <Form.Group className="mb-3">
            <Form.Label htmlFor="studentId">Student ID</Form.Label>
            <Form.Control
              data-testid="RosterStudentsForm-studentId"
              id="studentId"
              type="text"
              isInvalid={Boolean(errors.requesterEmail)}
              {...register("studentId", {
                required: "Student ID is required.",
              })}
            />
            <Form.Control.Feedback type="invalid">
              {errors.studentId?.message}
            </Form.Control.Feedback>
          </Form.Group>
        </Col>
      </Row>
      <Row>
        <Col>
          <Form.Group className="mb-3">
            <Form.Label htmlFor="firstName">First Name</Form.Label>
            <Form.Control
              data-testid="RosterStudentsForm-firstName"
              id="firstName"
              type="text"
              isInvalid={Boolean(errors.firstName)}
              {...register("firstName", {
                required: "First Name is required.",
              })}
            />
            <Form.Control.Feedback type="invalid">
              {errors.firstName?.message}
            </Form.Control.Feedback>
          </Form.Group>
        </Col>
      </Row>
      <Row>
        <Col>
          <Form.Group className="mb-3">
            <Form.Label htmlFor="lastName">Last Name</Form.Label>
            <Form.Control
              data-testid="RosterStudentsForm-lastName"
              id="lastName"
              type="text"
              isInvalid={Boolean(errors.lastName)}
              {...register("lastName", {
                required: "Last Name is required.",
              })}
            />
            <Form.Control.Feedback type="invalid">
              {errors.lastName?.message}
            </Form.Control.Feedback>
          </Form.Group>
        </Col>
      </Row>
      <Row>
        <Col>
          <Form.Group className="mb-3">
            <Form.Label htmlFor="email">Email</Form.Label>
            <Form.Control
              data-testid="RosterStudentsForm-email"
              id="email"
              type="email"
              isInvalid={Boolean(errors.email)}
              {...register("email", {
                required: "Email is required.",
                pattern: emailRegex,
              })}
            />
            <Form.Control.Feedback type="invalid">
              {errors.email?.message}
            </Form.Control.Feedback>
          </Form.Group>
        </Col>
      </Row>

      <Row>
        <Col>
          <Button type="submit" data-testid="RosterStudentsForm-submit">
            {buttonLabel}
          </Button>
          <Button
            variant="secondary"
            onClick={() => navigate(-1)}
            data-testid="RosterStudentsForm-cancel"
          >
            Cancel
          </Button>
        </Col>
      </Row>
    </Form>
  );
}

export default RosterStudentsForm;
