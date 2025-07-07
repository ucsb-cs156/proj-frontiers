import OurTable from "main/components/OurTable";
import { Button } from "react-bootstrap";

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

  const columnsWithStatus = [
    ...columns,
    {
      Header: "Status",
      accessor: "status",
      Cell: ({ cell }) => {
        if (cell.value === "Pending") {
          return <span style={{ color: "orange" }}>{cell.value}</span>;
        } else if (cell.value === "Join Course") {
          return (
            <Button
              variant={"primary"}
              onClick={() => joinCallback(cell)}
              data-testid={`CoursesTable-cell-row-${cell.row.index}-col-${cell.column.id}-button`}
            >
              Join Course
            </Button>
          );
        } else if (cell.value === "Invited") {
          return <span style={{ color: "green" }}>{cell.value}</span>;
        } else if (cell.value === "Member") {
          return <span style={{ color: "blue" }}>{cell.value}</span>;
        } else if (cell.value === "Owner") {
          return <span style={{ color: "purple" }}>{cell.value}</span>;
        } else if (cell.value === "Error") {
          return <span style={{ color: "red" }}>{cell.value}</span>;
        }
        return <span>{cell.value}</span>;
      },
    },
  ];
  return (
    <OurTable
      data={courses}
      columns={columnsWithStatus}
      testid={"CoursesTable"}
    />
  );
}
