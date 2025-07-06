import OurTable, { ButtonColumn } from "main/components/OurTable";

const columns = [
  {
    Header: "id",
    accessor: "id", // accessor is the "key" in the data
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
  {
    Header: "Status",
    accessor: "status",
  },
];

export default function CoursesTable({ courses, storybook = false }) {
  const joinCallback = (cell) => {
    // TODO: Implement the join functionality here
    if (storybook) {
      window.alert(
        `Join callback invoked for course with id: ${cell.row.values.id}`,
      );
      return;
    }
  };

  const columnsWithButtons = [
    ...columns,
    ButtonColumn("Join Course", "primary", joinCallback, "CoursesTable"),
  ];
  return (
    <OurTable
      data={courses}
      columns={columnsWithButtons}
      testid={"CoursesTable"}
    />
  );
}
