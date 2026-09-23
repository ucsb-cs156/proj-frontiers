import React from "react";
import CanvasCourseSettings from "main/components/Settings/CanvasCourseSettings";
import CourseOptionsForm from "main/components/Settings/CourseOptionsForm";
import SlackCourseSettings from "main/components/Settings/SlackCourseSettings";
import { useBackendMutation } from "main/utils/useBackend";
import { useCourseOptions } from "main/utils/courseOptionsUtils";
import { toast } from "react-toastify";

export default function SettingsTabComponent({
  courseId,
  testIdPrefix,
  canEditCourseOptions,
}) {
  // Shares its query (and cache) with CourseOptionsForm, so toggling an option there updates this
  const { data: optionsMap = {} } = useCourseOptions(courseId);
  const [slackErrorMessage, setSlackErrorMessage] = React.useState();

  const onSuccessCanvasCredentialsAdded = () => {
    toast("Canvas credentials successfully added.");
  };

  const objectToAxiosParamsCanvasToken = (formData) => ({
    url: `/api/courses/updateCourseCanvasToken`,
    method: "PUT",
    params: {
      courseId: courseId,
      canvasCourseId: formData.canvasCourseId,
      canvasApiToken: formData.canvasApiToken,
    },
  });

  const canvasMutation = useBackendMutation(
    objectToAxiosParamsCanvasToken,
    {
      onSuccess: onSuccessCanvasCredentialsAdded,
    },
    [`/api/courses/getCanvasInfo?courseId=${courseId}`],
  );

  const handleSubmit = (formData) => {
    canvasMutation.mutate(formData);
  };

  // The token is sent in the body (not the query string) so that it stays out of access logs.
  const objectToAxiosParamsSlackToken = (formData) => ({
    url: `/api/courses/slack/token`,
    method: "POST",
    params: { courseId: courseId },
    data: new URLSearchParams({ slackBotToken: formData.slackBotToken }),
  });

  const onSuccessSlackTokenSaved = (data) => {
    setSlackErrorMessage(undefined);
    toast(
      `Slack token verified and saved for workspace ${data.slackTeamName} (${data.slackTeamId}).`,
    );
  };

  const onErrorSlackToken = (error) => {
    setSlackErrorMessage(
      error.response?.data?.message ?? `Error saving Slack token: ${error}`,
    );
  };

  const slackMutation = useBackendMutation(
    objectToAxiosParamsSlackToken,
    {
      onSuccess: onSuccessSlackTokenSaved,
      onError: onErrorSlackToken,
    },
    [`/api/courses/slack/info?courseId=${courseId}`],
  );

  const handleSlackSubmit = (formData) => {
    slackMutation.mutate(formData);
  };

  return (
    <div>
      <CourseOptionsForm courseId={courseId} canEdit={canEditCourseOptions} />
      {optionsMap.ENABLE_CANVAS && (
        <CanvasCourseSettings
          courseId={courseId}
          submitAction={handleSubmit}
          testIdPrefix={testIdPrefix}
        />
      )}
      {optionsMap.SLACK_INTEGRATION && (
        <SlackCourseSettings
          courseId={courseId}
          submitAction={handleSlackSubmit}
          errorMessage={slackErrorMessage}
          testIdPrefix={testIdPrefix}
        />
      )}
    </div>
  );
}
