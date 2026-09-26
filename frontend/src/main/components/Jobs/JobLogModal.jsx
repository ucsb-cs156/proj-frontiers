import React from "react";
import Modal from "react-bootstrap/Modal";
import JobLogTail from "main/components/Jobs/JobLogTail";

/**
 * A modal that follows the log of a job of a course as it is written. The log
 * is only followed while the modal is open.
 */
export default function JobLogModal({
  show,
  onHide,
  courseId,
  jobId,
  testIdPrefix = "JobLogModal",
}) {
  return (
    <Modal
      show={show}
      onHide={onHide}
      size="lg"
      centered={true}
      data-testid={testIdPrefix}
    >
      <Modal.Header closeButton>
        <Modal.Title>Job {jobId} Log</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <JobLogTail
          courseId={courseId}
          jobId={jobId}
          testIdPrefix={`${testIdPrefix}-tail`}
        />
      </Modal.Body>
    </Modal>
  );
}
