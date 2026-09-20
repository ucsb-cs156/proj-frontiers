import { Button, Form } from "react-bootstrap";
import { useForm } from "react-hook-form";
import { useBackend } from "main/utils/useBackend";
import React from "react";

function SlackTokenForm({
  submitAction,
  buttonLabel = "Verify and Save Token",
  courseId,
  errorMessage,
  testIdPrefix = "SlackTokenForm",
}) {
  const {
    register,
    formState: { errors },
    handleSubmit,
    reset,
  } = useForm();

  const { data: slackInfo } = useBackend(
    [`/api/courses/slack/info?courseId=${courseId}`],
    // Stryker disable next-line StringLiteral : The default value for an empty ("") method is GET. Therefore, there is no way to kill a mutation that transforms "GET" to ""
    { method: "GET", url: `/api/courses/slack/info?courseId=${courseId}` },
    // Stryker disable next-line all : don't test default value of empty object
    {},
  );

  return (
    <Form
      onSubmit={handleSubmit((data) => {
        submitAction({ slackBotToken: data.slackBotToken.trim() });
        reset();
      })}
    >
      <p data-testid={testIdPrefix + "-workspace"}>
        {slackInfo.slackTeamName ? (
          <>
            Connected to Slack workspace:{" "}
            <strong>{slackInfo.slackTeamName}</strong> ({slackInfo.slackTeamId})
          </>
        ) : (
          "Not connected to a Slack workspace yet."
        )}
      </p>
      <Form.Group className="mb-3">
        <Form.Label htmlFor="slackBotToken">Slack Bot Token</Form.Label>
        <Form.Control
          data-testid={testIdPrefix + "-slackBotToken"}
          id="slackBotToken"
          type="password"
          autoComplete="new-password"
          placeholder={
            slackInfo.slackBotToken
              ? `Current Token: ${slackInfo.slackBotToken}`
              : "Token not set yet."
          }
          isInvalid={Boolean(errors.slackBotToken)}
          {...register("slackBotToken", {
            validate: (value) =>
              value.trim().length > 0 || "Slack Bot Token is required.",
          })}
        />
        <Form.Control.Feedback type="invalid">
          {errors.slackBotToken?.message}
        </Form.Control.Feedback>
        <Form.Text className="text-muted">
          Paste the Bot User OAuth Token (starts with <code>xoxb-</code>) from
          your Slack app&apos;s OAuth &amp; Permissions page. The token is
          verified with Slack before it is saved. If you add scopes to the Slack
          app later, reinstall it to the workspace and enter the new token here.
        </Form.Text>
      </Form.Group>
      {errorMessage && (
        <Form.Text
          className="text-danger d-block mb-2"
          data-testid={testIdPrefix + "-error"}
        >
          {errorMessage}
        </Form.Text>
      )}

      <Button type="submit" data-testid={testIdPrefix + "-submit"}>
        {buttonLabel}
      </Button>
    </Form>
  );
}

export default SlackTokenForm;
