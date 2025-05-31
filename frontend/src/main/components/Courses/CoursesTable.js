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

export default function CoursesTable({
  courses,
  showInstallButton = false,
  showRosterButton = false,
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

  const rosterCallback = (cell) => {
    const courseId = cell.row.values.id;
    const url = `/admin/courses/${courseId}/roster_students`;
    if (storybook) {
      window.alert(`would have navigated to: ${url}`);
      return;
    }
    window.location.href = url;
  };

  const installColumns = [
    ButtonColumn(
      "Install Github App",
      "primary",
      installCallback,
      "CoursesTable",
    ),
  ];

  const rosterColumns = [
    ButtonColumn("Roster Students", "primary", rosterCallback, "CoursesTable"),
  ];

  let columnsToDisplay = [...columns];

  if (showInstallButton) columnsToDisplay.push(...installColumns);

  if (showRosterButton) columnsToDisplay.push(...rosterColumns);

  return (
    <OurTable
      data={courses}
      columns={columnsToDisplay}
      testid={"CoursesTable"}
    />
  );
}
