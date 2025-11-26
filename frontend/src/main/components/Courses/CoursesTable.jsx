import OurTable from "main/components/OurTable";
import { Tooltip, OverlayTrigger, Button, Spinner } from "react-bootstrap";

const columns = [
  {
    header: "id",
    accessorKey: "id", // accessor is the "key" in the data
  },
  {
    header: "Course Name",
    accessorKey: "courseName",
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

export default function CoursesTable({
  courses,
  testId,
  joinCallback,
  isLoading,
}) {
  const viewInviteCallback = (cell) => {
    const organizationName = cell.row.original.orgName;
    const gitInvite = `https://github.com/orgs/${organizationName}/invitation`;
    window.open(gitInvite, "_blank");
  };

  const renderTooltip = (studentStatus) => {
    const TooltipComponent = (props) => {
      let set_message;

      switch (studentStatus) {
        case "PENDING":
          set_message =
            "This course has not been completely set up by your instructor yet.";
          break;
        case "JOINCOURSE":
          set_message =
            "Clicking this button will generate an invitation to the GitHub organization associated with this course.";
          break;
        case "INVITED":
          set_message =
            "You have been invited to the GitHub organization associated with this course, but you still need to accept or decline the invitation. Please accept it if you plan to stay enrolled, and decline only if you plan to withdraw from the course.";
          break;
        case "OWNER":
          set_message =
            "You are an owner of the GitHub organization associated with this course.";
          break;
        case "MEMBER":
          set_message =
            "You are a member of the GitHub organization associated with this course.";
          break;
        default:
          set_message = "Tooltip for illegal status that will never occur";
          break;
      }
      return (
        <Tooltip id={`${studentStatus.toLowerCase()}-tooltip`} {...props}>
          {set_message}
        </Tooltip>
      );
    };
    // Stryker disable next-line all: DisplayName is for debugging purposes and not tested
    TooltipComponent.displayName = "RenderTooltip";
    return TooltipComponent;
  };

  const columnsWithStatus = [
    ...columns,
    {
      header: "Status",
      accessorKey: "studentStatus",
      cell: ({ cell }) => {
        const status = cell.row.original.studentStatus;
        if (status === "PENDING") {
          return (
            <OverlayTrigger
              placement="right"
              overlay={renderTooltip("PENDING")}
            >
              <span className="text-warning">Pending</span>
            </OverlayTrigger>
          );
        } else if (status === "JOINCOURSE") {
          const cellIsLoading = isLoading(cell);
          return (
            <OverlayTrigger
              placement="right"
              overlay={renderTooltip("JOINCOURSE")}
            >
              <span>
                <Button
                  variant={"primary"}
                  onClick={() => joinCallback(cell)}
                  data-testid={`${testId}-cell-row-${cell.row.index}-col-${cell.column.id}-button`}
                  disabled={cellIsLoading}
                >
                  {cellIsLoading ? (
                    <>
                      <Spinner
                        as="span"
                        animation="grow"
                        size="sm"
                        role="status"
                      />
                      Joining...
                    </>
                  ) : (
                    <>Join Course</>
                  )}
                </Button>
              </span>
            </OverlayTrigger>
          );
        } else if (status === "INVITED") {
          return (
            <OverlayTrigger
              placement="right"
              overlay={renderTooltip("INVITED")}
            >
              <span>
                <Button
                  variant={"primary"}
                  onClick={() => viewInviteCallback(cell)}
                  data-testid={`${testId}-cell-row-${cell.row.index}-col-${cell.column.id}-button`}
                >
                  View Invite
                </Button>
              </span>
            </OverlayTrigger>
          );
        } else if (status === "OWNER") {
          return (
            <OverlayTrigger placement="right" overlay={renderTooltip("OWNER")}>
              <span className="text-info">Owner</span>
            </OverlayTrigger>
          );
        } else if (status === "MEMBER") {
          return (
            <OverlayTrigger placement="right" overlay={renderTooltip("MEMBER")}>
              <span className="text-primary">Member</span>
            </OverlayTrigger>
          );
        }
        return (
          <OverlayTrigger
            placement="right"
            overlay={renderTooltip(cell.row.original.studentStatus)}
          >
            <span>{status}</span>
          </OverlayTrigger>
        );
      },
    },
  ];
  return (
    <OurTable data={courses} columns={columnsWithStatus} testid={testId} />
  );
}
