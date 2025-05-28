import OurTable, { ButtonColumn } from "main/components/OurTable";
import { useCurrentUser } from "main/utils/currentUser";
import { hasRole } from "main/utils/currentUser";
import { useNavigate } from "react-router-dom";

const columns = [
  {
    Header: "id",
    accessor: "id", // accessor is the "key" in the data
  },
  {
    Header: "Installation Id",
    accessor: "installationId",
  },
  {
    Header: "Org Name",
    accessor: "orgName",
  },
  {
    Header: "Course Name",
    accessor: "courseName",
  },
  {
    Header: "Term",
    accessor: "term",
  },
  {
    Header: "School",
    accessor: "school",
  },
];

export default function CoursesTable({
  courses,
  showInstallButton = false,
  storybook = false,
}) {
  const installCallback = (cell) => {
    const url = `/api/courses/redirect?courseId=${cell.row.values.id}`;
    if (storybook) {
      window.alert(`would have navigated to: ${url}`);
      return;
    }
    window.location.href = url;
  };

  const buttonColumns = [
    ...columns,
    ButtonColumn(
      "Install Github App",
      "primary",
      installCallback,
      "CoursesTable",
    ),
  ];
  const columnsToDisplay = showInstallButton ? buttonColumns : columns;
  return (
    <OurTable
      data={courses}
      columns={columnsToDisplay}
      testid={"CoursesTable"}
    />
  );
}
