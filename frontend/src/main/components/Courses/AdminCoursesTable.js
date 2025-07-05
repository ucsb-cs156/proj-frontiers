import OurTable, { ButtonColumn } from "main/components/OurTable";

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

export default function AdminCoursesTable({
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
