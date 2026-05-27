import React from "react";
import { Button, Col, Row } from "react-bootstrap";

export default function DownloadsTabComponent({ courseId, testIdPrefix }) {
  const downloadCsv = () => {
    window.open(`/api/csv/rosterstudents?courseId=${courseId}`, "_blank");
  };

  return (
    <div data-testid={`${testIdPrefix}-DownloadsTabComponent`}>
      <Row sm={3} className="p-2">
        <Col>
          <Button 
            onClick={downloadCsv} 
            className="w-100"
            data-testid={`${testIdPrefix}-download-student-csv-button`}
          >
            Download Student CSV
          </Button>
        </Col>
      </Row>
    </div>
  );
}