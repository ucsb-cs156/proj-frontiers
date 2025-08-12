import React from "react";
import OurTable from "main/components/OurTable";
import { Tooltip, OverlayTrigger } from "react-bootstrap";

export default function CourseStaffTable({
  staff,
  currentUser: _currentUser,
  courseId,
  testIdPrefix = "CourseStaffTable",
}) {
  const columns = [
    {
      header: "id",
      accessorKey: "id",
      id: "id",
    },
    {
      header: "First Name",
      accessorKey: "firstName",
    },
    {
      header: "Last Name",
      accessorKey: "lastName",
    },
    {
      header: "Email",
      accessorKey: "email",
    },
    {
      header: "GitHub Login",
      accessorKey: "githubLogin",
    },
  ];

  const renderTooltip = (orgStatus) => (props) => {
    let set_message;

    switch (orgStatus) {
      case "PENDING":
        set_message =
          "Staff member cannot join the course until it has been completely set up.";
        break;
      case "JOINCOURSE":
        set_message =
          "Staff member has been prompted to join, but hasn't yet clicked the 'Join Course' button to generate an invite to the organization.";
        break;
      case "INVITED":
        set_message =
          "Staff member has generated an invite, but has not yet accepted or declined the invitation.";
        break;
      case "OWNER":
        set_message =
          "Staff member is an owner of the GitHub organization associated with this course.";
        break;
      case "MEMBER":
        set_message =
          "Staff member is a member of the GitHub organization associated with this course.";
        break;
      default:
        set_message = "Tooltip for illegal status that will never occur";
        break;
    }
    return (
      <Tooltip id={`${orgStatus.toLowerCase()}-tooltip`} {...props}>
        {set_message}
      </Tooltip>
    );
  };

  columns.push({
    header: "Status",
    accessorKey: "orgStatus",
    cell: ({ cell }) => {
      const status = cell.row.original.orgStatus;
      if (status === "PENDING") {
        return (
          <OverlayTrigger placement="right" overlay={renderTooltip("PENDING")}>
            <span style={{ color: "red" }}>Pending</span>
          </OverlayTrigger>
        );
      } else if (status === "JOINCOURSE") {
        return (
          <OverlayTrigger
            placement="right"
            overlay={renderTooltip("JOINCOURSE")}
          >
            <span style={{ color: "blue" }}>Join Course</span>
          </OverlayTrigger>
        );
      } else if (status === "INVITED") {
        return (
          <OverlayTrigger placement="right" overlay={renderTooltip("INVITED")}>
            <span style={{ color: "blue" }}>Invited</span>
          </OverlayTrigger>
        );
      } else if (status === "OWNER") {
        return (
          <OverlayTrigger placement="right" overlay={renderTooltip("OWNER")}>
            <span style={{ color: "purple" }}>Owner</span>
          </OverlayTrigger>
        );
      } else if (status === "MEMBER") {
        return (
          <OverlayTrigger placement="right" overlay={renderTooltip("MEMBER")}>
            <span style={{ color: "green" }}>Member</span>
          </OverlayTrigger>
        );
      }
      return (
        <OverlayTrigger
          placement="right"
          overlay={renderTooltip(cell.row.original.orgStatus)}
        >
          <span>{status}</span>
        </OverlayTrigger>
      );
    },
  });

  // Note: Edit and Delete functionality removed since backend endpoints don't exist yet

  return (
    <>
      <OurTable data={staff} columns={columns} testid={testIdPrefix} />
      <div
        style={{ display: "none" }}
        data-testid={`${testIdPrefix}-courseId`}
        data-course-id={`${courseId}`}
      />
    </>
  );
}
