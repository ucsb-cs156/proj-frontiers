import React, { useState } from "react";
import { Button, Col, Row } from "react-bootstrap";
import Modal from "react-bootstrap/Modal";

export default function DownloadsTabComponent({ courseId, testIdPrefix }) {
  const [csvModal, setCsvModal] = useState(false);

  const downloadCsv = () => {
    window.open(`/api/csv/rosterstudents?courseId=${courseId}`, "_blank");
  };

  return (
    <div data-testid={`${testIdPrefix}-DownloadsTabComponent`}>
      <Modal
        show={csvModal}
        onHide={() => setCsvModal(false)}
        centered={true}
        data-testid={`${testIdPrefix}-csv-modal`}
      ></Modal>
      <Row sm={3} className="p-2">
        <Col>
          <Button onClick={downloadCsv} className="w-100">
            Download Student CSV
          </Button>
        </Col>
      </Row>
    </div>
  );
}
