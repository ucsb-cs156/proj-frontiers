import React from "react";
import { Form } from "react-bootstrap";
import { toast } from "react-toastify";
import { useBackend, useBackendMutation } from "main/utils/useBackend";

function titleCaseFromOption(option) {
  return option
    .toLowerCase()
    .split("_")
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(" ");
}

function CourseOptionsForm({ courseId, canEdit }) {
  const { data: optionsMap } = useBackend(
    [`/api/course/options/?courseId=${courseId}`],
    {
      method: "GET",
      url: "/api/course/options",
      params: { courseId },
    },
    {},
  );

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
    [`/api/course/options/?courseId=${courseId}`],
  );

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
          onChange={(event) =>
            courseOptionMutation.mutate({
              option,
              enabled: event.target.checked,
            })
          }
          data-testid={`CourseOptionsForm-toggle-${option}`}
        />
      ))}
    </div>
  );
}

// export the component as the default export, and also export the titleCaseFromOption function for testing
export { titleCaseFromOption };
export default CourseOptionsForm;
