import React from "react";
import CanvasApiForm from "main/components/Settings/CanvasApiForm";

export default function CanvasCourseSettings({
  courseId,
  submitAction,
  testIdPrefix,
}) {
  return (
    <div className="card mt-4" data-testid={`${testIdPrefix}-canvasForm`}>
      <div className="card-body">
        <h5 className="card-title">Canvas Course Settings</h5>
        <CanvasApiForm submitAction={submitAction} courseId={courseId} />
      </div>
    </div>
  );
}
