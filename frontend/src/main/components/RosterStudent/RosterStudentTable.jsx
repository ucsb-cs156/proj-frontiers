import React from "react";
import OurTable, { ButtonColumn } from "main/components/OurTable";
import { Tooltip, OverlayTrigger } from "react-bootstrap";

import { useBackendMutation } from "main/utils/useBackend";
import { cellToAxiosParamsDelete } from "main/utils/rosterStudentUtils";
import { hasRole } from "main/utils/currentUser";
import Modal from "react-bootstrap/Modal";
import RosterStudentForm from "main/components/RosterStudent/RosterStudentForm";
import { toast } from "react-toastify";
import RosterStudentDeleteModal from "main/components/RosterStudent/RosterStudentDeleteModal";

export default function RosterStudentTable({
  students,
  currentUser,
  courseId,
  testIdPrefix = "RosterStudentTable",
}) {
  const [showEditModal, setShowEditModal] = React.useState(false);
  const [editStudent, setEditStudent] = React.useState(null);
  const [showDeleteModal, setShowDeleteModal] = React.useState(false);
  const [deleteStudent, setDeleteStudent] = React.useState(null);

  const cellToAxiosParamsEdit = (formData) => ({
    url: `/api/rosterstudents/update`,
    method: "PUT",
    params: {
      studentId: formData.studentId,
      firstName: formData.firstName,
      lastName: formData.lastName,
      id: formData.id,
    },
  });

  const hideEditModal = () => {
    setShowEditModal(false);
  };

  const hideDeleteModal = () => {
    setShowDeleteModal(false);
  };

  const onEditSuccess = () => {
    toast("Student updated successfully.");
    hideEditModal();
  };

  const onDeleteSuccess = () => {
    toast("Student deleted successfully.");
    hideDeleteModal();
  };

  const deleteMutation = useBackendMutation(
    cellToAxiosParamsDelete,
    { onSuccess: onDeleteSuccess },
    [`/api/rosterstudents/course/${courseId}`],
  );

  const deleteCallback = async (cell) => {
    setShowDeleteModal(true);
    setDeleteStudent(cell.row.original.id);
  };

  const submitDeleteForm = (data) => {
    deleteMutation.mutate({
      id: deleteStudent,
      ...data,
    });
  };

  const editMutation = useBackendMutation(
    cellToAxiosParamsEdit,
    { onSuccess: onEditSuccess },
    [`/api/rosterstudents/course/${courseId}`],
  );

  const editCallback = (cell) => {
    setEditStudent(cell.row.original);
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
      header: "Student Id",
      accessorKey: "studentId",
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
    {
      id: "teams",
      header: () => (
        <OverlayTrigger
          placement="right"
          overlay={
            <Tooltip id={`tooltip-teams-header`}>
              A list of teams that the student is a member of.
            </Tooltip>
          }
        >
          <span>Teams</span>
        </OverlayTrigger>
      ),
      accessorFn: (row) =>
        Array.isArray(row.teams) ? row.teams.join(", ") : "",
    },
  ];

  const renderTooltip = (orgStatus) => {
    const TooltipComponent = (props) => {
      let set_message;

      switch (orgStatus) {
        case "PENDING":
          set_message =
            "Student cannot join the course until it has been completely set up.";
          break;
        case "JOINCOURSE":
          set_message =
            "Student has been prompted to join, but hasn't yet clicked the 'Join Course' button to generate an invite to the organization.";
          break;
        case "INVITED":
          set_message =
            "Student has generated an invite, but has not yet accepted or declined the invitation.";
          break;
        case "OWNER":
          set_message =
            "Student is an owner of the GitHub organization associated with this course.";
          break;
        case "MEMBER":
          set_message =
            "Student is a member of the GitHub organization associated with this course.";
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
    // Stryker disable next-line all: DisplayName is for debugging purposes and not tested
    TooltipComponent.displayName = "RenderTooltip";
    return TooltipComponent;
  };

  columns.push({
    header: "Status",
    accessorKey: "orgStatus",
    cell: ({ cell }) => {
      const status = cell.row.original.orgStatus;
      if (status === "PENDING") {
        return (
          <OverlayTrigger placement="right" overlay={renderTooltip("PENDING")}>
            <span className="text-danger">Pending</span>
          </OverlayTrigger>
        );
      } else if (status === "JOINCOURSE") {
        return (
          <OverlayTrigger
            placement="right"
            overlay={renderTooltip("JOINCOURSE")}
          >
            <span className="text-primary">Join Course</span>
          </OverlayTrigger>
        );
      } else if (status === "INVITED") {
        return (
          <OverlayTrigger placement="right" overlay={renderTooltip("INVITED")}>
            <span className="text-primary">Invited</span>
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
            <span className="text-success">Member</span>
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
      <Modal show={showEditModal} onHide={hideEditModal}>
        <Modal.Header closeButton>
          <Modal.Title>Edit Student</Modal.Title>
        </Modal.Header>
        <Modal.Body
          className={"pb-3"}
          data-testid={`${testIdPrefix}-modal-body`}
        >
          <RosterStudentForm
            initialContents={editStudent}
            submitAction={submitEditForm}
            buttonLabel={"Update"}
            cancelDisabled={true}
          />
        </Modal.Body>
      </Modal>
      <RosterStudentDeleteModal
        showModal={showDeleteModal}
        toggleShowModal={setShowDeleteModal}
        onSubmitAction={submitDeleteForm}
      />
      <OurTable data={students} columns={columns} testid={testIdPrefix} />
      <div
        style={{ display: "none" }}
        data-testid={`${testIdPrefix}-courseId`}
        data-course-id={`${courseId}`}
      />
    </>
  );
}
