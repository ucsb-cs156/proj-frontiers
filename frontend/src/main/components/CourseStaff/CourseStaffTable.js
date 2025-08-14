import React from "react";
import OurTable, { ButtonColumn } from "main/components/OurTable";
import { Tooltip, OverlayTrigger } from "react-bootstrap";

import { useBackendMutation } from "main/utils/useBackend";
import { hasRole } from "main/utils/currentUser";
import Modal from "react-bootstrap/Modal";
import CourseStaffForm from "main/components/CourseStaff/CourseStaffForm";
import { toast } from "react-toastify";

export default function CourseStaffTable({
  staff,
  currentUser,
  courseId,
  testIdPrefix = "CourseStaffTable",
}) {
  const [showEditModal, setShowEditModal] = React.useState(false);
  const [editStaff, setEditStaff] = React.useState(null);

  // Stryker disable all
  function onDeleteSuccess(message) {
    console.log(message);
    toast(message);
  }
  // Stryker restore all

  function cellToAxiosParamsDelete(cell) {
    return {
      // Stryker disable next-line StringLiteral
      url: "/api/coursestaff/delete",
      method: "DELETE",
      params: {
        id: cell.row.original.id,
        courseId: courseId,
      },
    };
  }

  // Stryker disable all : hard to test for query caching
  const deleteMutation = useBackendMutation(
    cellToAxiosParamsDelete,
    { onSuccess: onDeleteSuccess },
    // Stryker disable next-line all
    [`/api/coursestaff/course?courseId=${courseId}`],
  );
  // Stryker restore all

  const cellToAxiosParamsEdit = (formData) => ({
    url: `/api/coursestaff`,
    method: "PUT",
    // Stryker disable next-line ObjectLiteral
    params: {
      firstName: formData.firstName,
      lastName: formData.lastName,
      id: formData.id,
      courseId: courseId,
    },
  });

  const hideModal = () => {
    setShowEditModal(false);
  };

  const onEditSuccess = () => {
    toast("Staff member updated successfully.");
    hideModal();
  };

  // Stryker disable next-line all
  const deleteCallback = async (cell) => {
    deleteMutation.mutate(cell);
  };

  const editMutation = useBackendMutation(
    cellToAxiosParamsEdit,
    { onSuccess: onEditSuccess },
    // Stryker disable next-line all
    [`/api/coursestaff/course?courseId=${courseId}`],
  );

  const editCallback = (cell) => {
    setEditStaff(cell.row.original);
    setShowEditModal(true);
  };

  const submitEditForm = (data) => {
    editMutation.mutate(data);
  };

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

  if (hasRole(currentUser, "ROLE_INSTRUCTOR")) {
    columns.push(ButtonColumn("Edit", "primary", editCallback, testIdPrefix));
    columns.push(
      ButtonColumn("Delete", "danger", deleteCallback, testIdPrefix),
    );
  }

  return (
    <>
      <Modal show={showEditModal} onHide={hideModal}>
        <Modal.Header closeButton>
          <Modal.Title>Edit Staff Member</Modal.Title>
        </Modal.Header>
        <Modal.Body
          className={"pb-3"}
          data-testid={`${testIdPrefix}-modal-body`}
        >
          <CourseStaffForm
            initialContents={editStaff}
            submitAction={submitEditForm}
            buttonLabel={"Update"}
            cancelDisabled={true}
          />
        </Modal.Body>
      </Modal>

      <OurTable data={staff} columns={columns} testid={testIdPrefix} />
      <div
        style={{ display: "none" }}
        data-testid={`${testIdPrefix}-courseId`}
        data-course-id={`${courseId}`}
      />
    </>
  );
}
