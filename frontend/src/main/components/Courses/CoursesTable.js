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

  const viewInviteCallback = (cell) => {
    const organizationName = cell.row.original.orgName; 
    const gitInvite = `https://github.com/${organizationName}/invitation`

    if(storybook) {
      window.alert(
        `Join callback invoked for an invite to organization: ${organizationName}`,
      ); 
      return;
    }

    window.open(gitInvite, "_blank")
  }

  const columnsWithStatus = [
    ...columns,
    {
      Header: "Status",
      accessor: "studentStatus", 
      Cell: ({ cell }) => {
        if (cell.value === "PENDING") {
          return <span style={{ color: "orange" }}>Pending</span>; // Could provide context e.g "Pending. Come back later when the course has been completely set up."
        } else if (cell.value === "JOINCOURSE") {
          return (
            <Button
              variant={"primary"}
              onClick={() => joinCallback(cell)}
              data-testid={`CoursesTable-cell-row-${cell.row.index}-col-${cell.column.id}-button`}
            >
              Join Course
            </Button>
          );
        } else if (cell.value === "INVITED") {
          return (
              <span style={{ color: "green" }}> Invited
              <Button 
                style={{marginLeft: "8px"}}
                variant={"primary"}
                onClick={() => viewInviteCallback(cell)}
                data-testid={`CoursesTable-cell-row-${cell.row.index}-col-${cell.column.id}-button`}
              >
              View Invite
              </Button>
            </span>
          );
        } else if (cell.value === "OWNER") {
          return <span style={{ color: "purple" }}>Owner</span>;
        } else if (cell.value === "MEMBER") {
          return <span style={{ color: "blue" }}>Member</span>;
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
