import { useForm } from "react-hook-form";
import { Button, Form } from "react-bootstrap";
import {
  CREATE_REPOS_FOR_LABELS,
  PERMISSION_LABELS,
} from "main/utils/newAssignmentsUtils";

const testIdPrefix = "NewIndividualAssignmentForm";

export default function NewIndividualAssignmentForm({
  initialContents,
  submitAction,
  buttonLabel = "Create",
}) {
  // Stryker disable all
  const {
    register,
    formState: { errors },
    handleSubmit,
  } = useForm({
    defaultValues: {
      isPrivate: false,
      permission: "MAINTAIN",
      requireSignedCommit: false,
      createReposFor: "STUDENTS_ONLY",
      ...initialContents,
    },
  });
  // Stryker restore all

  return (
    <Form onSubmit={handleSubmit(submitAction)} data-testid={testIdPrefix}>
      <Form.Group className="mb-3">
        <Form.Label htmlFor="repoPrefix">Repository Prefix</Form.Label>
        <Form.Control
          id="repoPrefix"
          type="text"
          isInvalid={Boolean(errors.repoPrefix)}
          data-testid={`${testIdPrefix}-repoPrefix`}
          {...register("repoPrefix", {
            validate: (value) =>
              value.trim().length > 0 || "Repository Prefix is required.",
          })}
        />
        <Form.Control.Feedback type="invalid">
          {errors.repoPrefix?.message}
        </Form.Control.Feedback>
      </Form.Group>
      <Form.Group className="mb-3">
        <Form.Check
          id="isPrivate"
          type="switch"
          label="Private Repositories?"
          data-testid={`${testIdPrefix}-isPrivate`}
          {...register("isPrivate")}
        />
      </Form.Group>
      <Form.Group className="mb-3">
        <Form.Label htmlFor="permission">Student Permissions</Form.Label>
        <Form.Select
          id="permission"
          data-testid={`${testIdPrefix}-permission`}
          {...register("permission")}
        >
          {Object.entries(PERMISSION_LABELS).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </Form.Select>
      </Form.Group>
      <Form.Group className="mb-3">
        <Form.Check
          id="requireSignedCommit"
          type="switch"
          label="Require Signed Commits?"
          data-testid={`${testIdPrefix}-requireSignedCommit`}
          {...register("requireSignedCommit")}
        />
        <Form.Text
          muted
          data-testid={`${testIdPrefix}-requireSignedCommit-help`}
        >
          Repositories get a ruleset that requires signed commits on every
          branch except gh-pages. When this is off, that ruleset is removed from
          them.
        </Form.Text>
      </Form.Group>
      <Form.Group className="mb-3">
        <Form.Label htmlFor="createReposFor">
          Create Repositories for
        </Form.Label>
        <Form.Select
          id="createReposFor"
          data-testid={`${testIdPrefix}-createReposFor`}
          {...register("createReposFor")}
        >
          {Object.entries(CREATE_REPOS_FOR_LABELS).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </Form.Select>
      </Form.Group>
      <Button type="submit" data-testid={`${testIdPrefix}-submit`}>
        {buttonLabel}
      </Button>
    </Form>
  );
}
