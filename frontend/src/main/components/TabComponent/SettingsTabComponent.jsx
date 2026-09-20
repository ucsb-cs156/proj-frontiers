import React from "react";
import CanvasCourseSettings from "main/components/Settings/CanvasCourseSettings";
import CourseOptionsForm from "main/components/Settings/CourseOptionsForm";
import { useBackendMutation } from "main/utils/useBackend";
import { toast } from "react-toastify";

export default function SettingsTabComponent({
  courseId,
  testIdPrefix,
  canEditCourseOptions,
}) {
  const [showCanvasSettings, setShowCanvasSettings] = React.useState(false);

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

  return (
    <div>
      <CourseOptionsForm
        courseId={courseId}
        canEdit={canEditCourseOptions}
        onOptionsLoaded={(optionsMap) =>
          setShowCanvasSettings(optionsMap.ENABLE_CANVAS === true)
        }
        onOptionToggled={({ option, enabled }) => {
          if (option === "ENABLE_CANVAS") {
            setShowCanvasSettings(enabled);
          }
        }}
      />
      {showCanvasSettings && (
        <CanvasCourseSettings
          courseId={courseId}
          submitAction={handleSubmit}
          testIdPrefix={testIdPrefix}
        />
      )}
    </div>
  );
}
