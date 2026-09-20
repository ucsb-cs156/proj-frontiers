import React from "react";
import SlackSetupInstructions from "main/components/Settings/SlackSetupInstructions";
import SlackTokenForm from "main/components/Settings/SlackTokenForm";

export default function SlackCourseSettings({
  courseId,
  submitAction,
  errorMessage,
  testIdPrefix = "SlackCourseSettings",
}) {
  return (
    <div className="card mt-4" data-testid={`${testIdPrefix}-slackForm`}>
      <div className="card-body">
        <h5 className="card-title">Slack Integration Settings</h5>
        <SlackSetupInstructions testIdPrefix={`${testIdPrefix}-slack`} />
        <SlackTokenForm
          submitAction={submitAction}
          courseId={courseId}
          errorMessage={errorMessage}
          testIdPrefix={`${testIdPrefix}-slack`}
        />
      </div>
    </div>
  );
}
