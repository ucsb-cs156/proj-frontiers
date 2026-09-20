import React from "react";
import CanvasCourseSettings from "main/components/Settings/CanvasCourseSettings";
import CourseOptionsForm from "main/components/Settings/CourseOptionsForm";
import { useCourseOptions } from "main/utils/courseOptionsUtils";
import { useBackendMutation } from "main/utils/useBackend";
import { toast } from "react-toastify";

export default function SettingsTabComponent({
  courseId,
  testIdPrefix,
  canEditCourseOptions,
}) {
  const { data: optionsMap = {} } = useCourseOptions(courseId);
  const showCanvasSettings = optionsMap.ENABLE_CANVAS === true;

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
      <CourseOptionsForm courseId={courseId} canEdit={canEditCourseOptions} />
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
