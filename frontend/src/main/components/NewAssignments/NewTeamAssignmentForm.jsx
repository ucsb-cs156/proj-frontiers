import { useForm } from "react-hook-form";
import { Button, Form, OverlayTrigger, Tooltip } from "react-bootstrap";
import { BsInfoCircle } from "react-icons/bs";
import {
  DEFAULT_TEAM_REGEX,
  PERMISSION_LABELS,
} from "main/utils/newAssignmentsUtils";

const testIdPrefix = "NewTeamAssignmentForm";

export default function NewTeamAssignmentForm({
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
      teamRegex: DEFAULT_TEAM_REGEX,
      ...initialContents,
    },
  });
  // Stryker restore all

  return (
    <Form onSubmit={handleSubmit(submitAction)} data-testid={testIdPrefix}>
      <Form.Group className="mb-3">
        <Form.Label htmlFor="repoPrefix">Team Repository Prefix</Form.Label>
        <Form.Control
          id="repoPrefix"
          type="text"
          isInvalid={Boolean(errors.repoPrefix)}
          data-testid={`${testIdPrefix}-repoPrefix`}
          {...register("repoPrefix", {
            validate: (value) =>
              value.trim().length > 0 || "Team Repository Prefix is required.",
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
        <Form.Label htmlFor="permission">Team Permissions</Form.Label>
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
        <Form.Label htmlFor="teamRegex">
          {"Team Regex "}
          <OverlayTrigger
            placement="right"
            overlay={
              <Tooltip id="teamRegex-tooltip">
                Only teams whose names match this regular expression will have
                repositories created. The default, {DEFAULT_TEAM_REGEX}, matches
                every team.
              </Tooltip>
            }
          >
            <BsInfoCircle />
          </OverlayTrigger>
        </Form.Label>
        <Form.Control
          id="teamRegex"
          type="text"
          isInvalid={Boolean(errors.teamRegex)}
          data-testid={`${testIdPrefix}-teamRegex`}
          {...register("teamRegex", {
            validate: (value) =>
              value.trim().length > 0 || "Team Regex is required.",
          })}
        />
        <Form.Control.Feedback type="invalid">
          {errors.teamRegex?.message}
        </Form.Control.Feedback>
      </Form.Group>
      <Button type="submit" data-testid={`${testIdPrefix}-submit`}>
        {buttonLabel}
      </Button>
    </Form>
  );
}
