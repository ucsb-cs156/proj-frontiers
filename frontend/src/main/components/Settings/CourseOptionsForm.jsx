import React from "react";
import { useEffect } from "react";
import { Form } from "react-bootstrap";
import { toast } from "react-toastify";
import { useBackendMutation } from "main/utils/useBackend";
import {
  courseOptionsQueryKey,
  titleCaseFromOption,
  useCourseOptions,
} from "main/utils/courseOptionsUtils";

function CourseOptionsForm({
  courseId,
  canEdit,
  onOptionsLoaded,
  onOptionToggled,
}) {
  const { data: optionsMap = {} } = useCourseOptions(courseId);

  const objectToAxiosParams = ({ option, enabled }) => ({
    url: "/api/course/options",
    method: "POST",
    params: { courseId, option, enabled },
  });

  const onSuccessOptionUpdated = (data, variables) => {
    toast(
      `${titleCaseFromOption(variables.option)} set to ${data[variables.option]}`,
    );
  };

  const courseOptionMutation = useBackendMutation(
    objectToAxiosParams,
    { onSuccess: onSuccessOptionUpdated },
    [courseOptionsQueryKey(courseId)],
  );

  useEffect(() => {
    onOptionsLoaded?.(optionsMap);
  }, [optionsMap, onOptionsLoaded]);

  const entries = Object.entries(optionsMap);

  return (
    <div data-testid="CourseOptionsForm">
      <h5 className="mt-4">Course Options</h5>
      {entries.map(([option, enabled]) => (
        <Form.Check
          key={option}
          id={`course-option-${option}`}
          type="switch"
          label={titleCaseFromOption(option)}
          checked={enabled}
          disabled={!canEdit}
          onChange={(event) => {
            const enabled = event.target.checked;
            onOptionToggled?.({ option, enabled });
            courseOptionMutation.mutate({
              option,
              enabled,
            });
          }}
          data-testid={`CourseOptionsForm-toggle-${option}`}
        />
      ))}
    </div>
  );
}

export default CourseOptionsForm;
