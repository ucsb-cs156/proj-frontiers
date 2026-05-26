import React from "react";
import { Button } from "react-bootstrap";

export default function DownloadStudentCSVButton({ courseId }) {
  const downloadCsv = () => {
    window.open(`/api/csv/rosterstudents?courseId=${courseId}`, "_blank");
  };

  return (
    <Button onClick={downloadCsv} className="w-100">
      Download Student CSV
    </Button>
  );
}
