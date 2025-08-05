import OurTable from "main/components/OurTable";
import { hasRole } from "main/utils/currentUser";
import { Tooltip, OverlayTrigger, Button } from "react-bootstrap";
import { FaGithub } from "react-icons/fa";
import { Link } from "react-router";

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
        <OverlayTrigger
          placement="right"
          overlay={
            <Tooltip id={`tooltip-coursename-${cell.row.index}`}>
              View course details
            </Tooltip>
          }
        >
          <Link
            to={`/instructor/courses/${cell.row.original.id}`}
            data-testid={`CoursesTable-cell-row-${cell.row.index}-col-${cell.column.id}-link`}
          >
            {cell.row.original.courseName}
          </Link>
        </OverlayTrigger>
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

  const renderTooltip = (cell) => {
    let set_message;
    const result = canInstall(cell.row);
    if (result) {
      set_message = `Click to install the GitHub app for the course: ${cell.row.original.courseName}`;
    } else {
      set_message = `View GitHub organization: ${cell.row.original.orgName}`;
    }
    return (
      <Tooltip id={`tooltip-orgname-${cell.row.index}`}>{set_message}</Tooltip>
    );
  };

  const columnsWithInstall = [
    ...columns,
    {
      header: "GitHub Org",
      id: "orgName",
      cell: ({ cell }) => {
        const result = canInstall(cell.row);
        if (result) {
          return (
            <OverlayTrigger placement="right" overlay={renderTooltip(cell)}>
              <span>
                <Button
                  variant={"primary"}
                  onClick={() => installCallback(cell)}
                  data-testid={`${testId}-cell-row-${cell.row.index}-col-${cell.column.id}-button`}
                >
                  Install GitHub App
                </Button>
              </span>
            </OverlayTrigger>
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
              <OverlayTrigger placement="right" overlay={renderTooltip(cell)}>
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
              </OverlayTrigger>
              <OverlayTrigger
                placement="right"
                overlay={
                  <Tooltip id={`tooltip-githubicon-${cell.row.index}`}>
                    Manage installation settings for the frontiers app,
                    including the option to uninstall it from this GitHub
                    organization.
                  </Tooltip>
                }
              >
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
              </OverlayTrigger>
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
