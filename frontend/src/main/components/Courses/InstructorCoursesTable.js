import OurTable from "main/components/OurTable";
import { hasRole } from "main/utils/currentUser";
import { Button } from "react-bootstrap";
import { FaGithub } from "react-icons/fa";

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
      return false;
    }
    if (hasRole(currentUser, "ROLE_ADMIN")) {
      return true;
    }
    if (
      hasRole(currentUser, "ROLE_INSTRUCTOR") &&
      row.values.createdByEmail === currentUser.root.user.email
    ) {
      return true;
    }

    return false;
  };

  const columnsWithInstall = [
    ...columns,
    {
      Header: "Github Org",
      accessor: "orgName",
      Cell: ({ cell }) => {
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
        } else if (!cell.value) {
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
                  href={`https://github.com/${cell.value}`}
                  target="_blank"
                  rel="noopener noreferrer"
                  data-testid={`${testId}-cell-row-${cell.row.index}-col-${cell.column.id}-github-link`}
                >
                  {cell.value}
                </a>
              </span>
              <span>
                <a
                  href={`https://github.com/organizations/${cell.value}/settings/installations/${cell.row.original.installationId}`}
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
      Header: "Created By",
      accessor: "createdByEmail",
    },
  ];

  return (
    <OurTable data={courses} columns={columnsWithInstall} testid={testId} />
  );
}
