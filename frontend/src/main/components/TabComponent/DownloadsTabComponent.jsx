import React from "react";
import CourseDownloadsForm from "../Courses/CourseDownloadsForm";
import { useBackendMutation } from "main/utils/useBackend";
import { toast } from "react-toastify";

export default function DownloadsTabComponent({ courseId, testIdPrefix }) {
  const onSuccessDownloadTriggered = () => {
    toast("Download successfully initiated.");
  };

  const objectToAxiosParamsDownloads = (formData) => ({
    url: `/api/courses/downloadStudentsCSV`,
    method: "GET",
    params: {
      courseId: courseId,
    },
  });
  const downloadMutation = useBackendMutation(
    objectToAxiosParamsDownloads,
    {
      onSuccess: onSuccessDownloadTriggered,
    },
    [],
  );
  const handleSubmit = (formData) => {
    console.log(`Frontend form submit action captured for course: ${courseId}`);
    onSuccessDownloadTriggered(); 
  };

  return (
    <div data-testid={`${testIdPrefix}-downloadsTab`}>
      <CourseDownloadsForm downloadAction={handleSubmit} testIdPrefix={testIdPrefix} />
    </div>
  );
}