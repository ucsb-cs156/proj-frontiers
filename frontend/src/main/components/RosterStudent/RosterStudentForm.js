import { Button, Form, Row, Col } from "react-bootstrap";
import { useForm } from "react-hook-form";
import { useNavigate } from "react-router-dom";

function RosterStudentForm({
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

  // Stryker disable Regex
  const email_regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  // Stryker restore Regex

  return (
    <Form onSubmit={handleSubmit(submitAction)}>
      <Row>
        {initialContents && initialContents.id && (
          <Col>
            <Form.Group className="mb-3">
              <Form.Label htmlFor="id">ID</Form.Label>
              <Form.Control
                data-testid="RosterStudentForm-id"
                id="id"
                type="text"
                {...register("id")}
                value={initialContents.id}
                disabled
              />
            </Form.Group>
          </Col>
        )}

        <Col>
          <Form.Group className="mb-3">
            <Form.Label htmlFor="studentId">Student ID</Form.Label>
            <Form.Control
              data-testid="RosterStudentForm-studentId"
              id="studentId"
              type="text"
              isInvalid={Boolean(errors.studentId)}
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
              data-testid="RosterStudentForm-firstName"
              id="firstName"
              type="text"
              isInvalid={Boolean(errors.firstName)}
              {...register("firstName", {
                required: "First name is required.",
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
              data-testid="RosterStudentForm-lastName"
              id="lastName"
              type="text"
              isInvalid={Boolean(errors.lastName)}
              {...register("lastName", {
                required: "Last name is required.",
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
              data-testid="RosterStudentForm-email"
              id="email"
              type="email"
              isInvalid={Boolean(errors.email)}
              {...register("email", {
                required: "Email is required.",
                pattern: {
                  value: email_regex,
                  message: "Enter a valid email address.",
                },
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
          <Button type="submit" data-testid="RosterStudentForm-submit">
            {buttonLabel}
          </Button>
          <Button
            variant="secondary"
            onClick={() => navigate(-1)}
            data-testid="RosterStudentForm-cancel"
          >
            Cancel
          </Button>
        </Col>
      </Row>
    </Form>
  );
}

export default RosterStudentForm;
