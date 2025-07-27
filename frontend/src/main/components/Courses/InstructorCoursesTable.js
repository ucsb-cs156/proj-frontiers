import OurTable from "main/components/OurTable";
import { hasRole } from "main/utils/currentUser";
import { Button } from "react-bootstrap";
import { FaGithub } from "react-icons/fa";

const columns = [
  {
    header: "id",
    accessorKey: "id", // accessor is the "key" in the data
  },
  {
    header: "Course Name",
    id: "courseName",
    cell: ({ cell }) => {
      return (
        <a
          href={`/instructor/courses/${cell.row.original.id}`}
          data-testid={`CoursesTable-cell-row-${cell.row.index}-col-${cell.column.id}-link`}
        >
          {cell.row.original.courseName}
        </a>
      );
    },
  },
  {
    header: "Term",
    accessorKey: "term",
  },
  {
    header: "School",
    accessorKey: "school",
  },
];

export default function InstructorCoursesTable({
  courses,
  storybook = false,
  currentUser,
  testId = "InstructorCoursesTable",
}) {
  const installCallback = (cell) => {
    const url = `/api/courses/redirect?courseId=${cell.row.original.id}`;
    if (storybook) {
      window.alert(`would have navigated to: ${url}`);
      return;
    }
    window.location.href = url;
  };

  const canInstall = (row) => {
    if (row.original.orgName) {
      return false;
    }
    if (hasRole(currentUser, "ROLE_ADMIN")) {
      return true;
    }
    if (
      hasRole(currentUser, "ROLE_INSTRUCTOR") &&
      row.original.createdByEmail === currentUser.root.user.email
    ) {
      return true;
    }

    return false;
  };

  const columnsWithInstall = [
    ...columns,
    {
      header: "Github Org",
      id: "orgName",
      cell: ({ cell }) => {
        const result = canInstall(cell.row);
        if (result) {
          return (
            <Button
              variant={"primary"}
              onClick={() => installCallback(cell)}
              data-testid={`${testId}-cell-row-${cell.row.index}-col-${cell.column.id}-button`}
            >
              Install Github App
            </Button>
          );
        } else if (!cell.row.original.orgName) {
          return (
            <span
              data-testid={`${testId}-cell-row-${cell.row.index}-col-${cell.column.id}-no-org`}
            ></span>
          );
        } else {
          return (
            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                width: "100%",
              }}
              data-testid={`${testId}-cell-row-${cell.row.index}-col-${cell.column.id}-div`}
            >
              <span>
                <a
                  href={`https://github.com/${cell.row.original.orgName}`}
                  target="_blank"
                  rel="noopener noreferrer"
                  data-testid={`${testId}-cell-row-${cell.row.index}-col-${cell.column.id}-github-link`}
                >
                  {cell.row.original.orgName}
                </a>
              </span>
              <span>
                <a
                  href={`https://github.com/organizations/${cell.row.original.orgName}/settings/installations/${cell.row.original.installationId}`}
                  target="_blank"
                  rel="noopener noreferrer"
                  data-testid={`CoursesTable-cell-row-${cell.row.index}-col-${cell.column.id}-github-settings-link`}
                >
                  <FaGithub
                    size={"1.5em"}
                    data-testid={`CoursesTable-cell-row-${cell.row.index}-col-${cell.column.id}-github-icon`}
                  />
                </a>
              </span>
            </div>
          );
        }
      },
    },
    {
      header: "Created By",
      accessorKey: "createdByEmail",
    },
  ];

  return (
    <OurTable data={courses} columns={columnsWithInstall} testid={testId} />
  );
}
