import { Button } from "react-bootstrap";

export default function DownloadsTabComponent({ courseId, testIdPrefix }) {
  const downloadStudentCsv = () => {
    window.open(`/api/csv/rosterstudents?courseId=${courseId}`, "_blank");
  };

  return (
    <div data-testid={`${testIdPrefix}-DownloadsTabComponent`}>
      <Button
        onClick={downloadStudentCsv}
        data-testid={`${testIdPrefix}-download-student-csv-button`}
      >
        Download Student CSV
      </Button>
    </div>
  );
}
