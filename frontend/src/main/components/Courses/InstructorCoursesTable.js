import OurTable from "main/components/OurTable";
import { hasRole } from "main/utils/currentUser";
import { Button } from "react-bootstrap";

const columns = [
  {
    Header: "id",
    accessor: "id", // accessor is the "key" in the data
  },
  {
    Header: "Course Name",
    accessor: "courseName",
    Cell: ({ cell }) => {
      return (
        <a
          href={`/instructor/courses/${cell.row.values.id}`}
          data-testid={`CoursesTable-cell-row-${cell.row.index}-col-${cell.column.id}-link`}
        >
          {cell.value}
        </a>
      );
    },
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

export default function InstructorCoursesTable({
  courses,
  storybook = false,
  currentUser,
  testId = "InstructorCoursesTable",
}) {
  const installCallback = (cell) => {
    const url = `/api/courses/redirect?courseId=${cell.row.values.id}`;
    if (storybook) {
      window.alert(`would have navigated to: ${url}`);
      return;
    }
    window.location.href = url;
  };

  const canInstall = (row) => {
    if ("orgName" in row.values && row.values.orgName) {
      return { canInstall: false, reason: "already-installed" };
    }
    if (hasRole(currentUser, "ROLE_ADMIN")) {
      return { canInstall: true, reason: "admin" };
    }
    if (
      hasRole(currentUser, "ROLE_INSTRUCTOR") &&
      row.values.createdByEmail === currentUser.root.user.email
    ) {
      return { canInstall: true, reason: "instructor-created-course" };
    }

    return { canInstall: false, reason: "not-authorized" };
  };

  const columnsWithInstall = [
    ...columns,
    {
      Header: "Github Org",
      accessor: "orgName",
      Cell: ({ cell }) => {
        const result = canInstall(cell.row);
        if (result.canInstall) {
          return (
            <Button
              variant={"primary"}
              onClick={() => installCallback(cell)}
              data-testid={`${testId}-cell-row-${cell.row.index}-col-${cell.column.id}-button`}
              data-reason={result.reason}
            >
              Install Github App
            </Button>
          );
        } else {
          return (
            <span
              data-testid={`${testId}-cell-row-${cell.row.index}-col-${cell.column.id}-cannot-install-${result.reason}`}
            >
              {cell.value}
            </span>
          );
        }
      },
    },
    {
      Header: "Created By",
      accessor: "createdByEmail",
    },
  ];

  return (
    <OurTable data={courses} columns={columnsWithInstall} testid={testId} />
  );
}
