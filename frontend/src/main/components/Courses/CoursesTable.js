import OurTable from "main/components/OurTable";
import { Button, OverlayTrigger, Tooltip } from "react-bootstrap";

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

export default function CoursesTable({ courses, testId, storybook = false }) {
   const cellToAxiosParamsJoinCourse = (cell) => {
    return {
      url: 'api/rosterstudents/joinCourse',
      method: "PUT",
      params: {
        rosterStudentId: cell.row.original.rosterStudentId,
      },
    };
  };
  const joinCallback = (cell) => {
    // TODO: Implement the join functionality here
    
    if (storybook) {
      window.alert(
        `Join callback invoked for course with id: ${cell.row.values.id}`,
      );
      return;
    }
  };

  const viewInviteCallback = (cell) => {
    const organizationName = cell.row.original.orgName;
    const gitInvite = `https://github.com/${organizationName}/invitation`;
    window.open(gitInvite, "_blank");
  };

  const renderTooltip = (studentStatus) => (props) => {
    let set_message;

    switch (studentStatus) {
      case "PENDING":
        set_message = "This course has not been completely set up by your instructor yet."; 
        break; 
      case "JOINCOURSE":
        set_message = "Clicking this button will generate an invitation to the GitHub organization associated with this course."; 
        break; 
      case "INVITED":
        set_message = "You have been invited to the GitHub organization associated with this course, but you still need to accept or decline the invitation. Please accept it if you plan to stay enrolled, and decline only if you plan to withdraw from the course."; 
        break; 
      case "OWNER":
        set_message = "You are an owner of the GitHub organization associated with this course."; 
        break; 
      case "MEMBER":
        set_message = "You are a member of the GitHub organization associated with this course."; 
        break; 
    }
    return (
      <Tooltip id={`${studentStatus.toLowerCase()}-tooltip`} {...props}>
        {set_message}
      </Tooltip>
    );
  };


  const columnsWithStatus = [
    ...columns,
    {
      Header: "Status",
      accessor: "studentStatus",
      Cell: ({ cell }) => {
        if (cell.value === "PENDING") {
          return( 
          <OverlayTrigger
            placement="right"
            overlay={renderTooltip("PENDING")}
          >
             <span style={{ color: "orange" }}>Pending</span>
          </OverlayTrigger>
          )
        } else if (cell.value === "JOINCOURSE") {
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
            >
              Join Course
            </Button>
            </span>
            </OverlayTrigger>
          );
        } else if (cell.value === "INVITED") {
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
        } else if (cell.value === "OWNER") {
          return (
            <OverlayTrigger
              placement="right"
              overlay={renderTooltip("OWNER")}
            >
            <span style={{ color: "purple" }}>Owner</span>
          </OverlayTrigger>
          )
        } else if (cell.value === "MEMBER") {
          return(
            <OverlayTrigger
              placement="right"
              overlay={renderTooltip("MEMBER")}
            >
            <span style={{ color: "blue" }}>Member</span>
          </OverlayTrigger>
          )
        }
        return <span>{cell.value}</span>;
      },
    },
  ];
  return (
    <OurTable data={courses} columns={columnsWithStatus} testid={testId} />
  );
}
