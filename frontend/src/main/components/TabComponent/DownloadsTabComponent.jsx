import React from "react";
import { Col, Row } from "react-bootstrap";
import DownloadStudentCSVButton from "main/components/TabComponent/DownloadStudentCSVButton";

export default function DownloadsTabComponent({ courseId, testIdPrefix }) {
  return (
    <div data-testid={`${testIdPrefix}-DownloadsTabComponent`}>
      <Row sm={3} className="p-2">
        <Col>
          <DownloadStudentCSVButton courseId={courseId} />
        </Col>
      </Row>
    </div>
  );
}
