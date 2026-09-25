import React, { useState } from "react";
import Modal from "react-bootstrap/Modal";
import { toast } from "react-toastify";

import OurTable, { ButtonColumn } from "main/components/OurTable";
import ConfirmationModal from "main/components/Common/ConfirmationModal";
import NewIndividualAssignmentForm from "main/components/NewAssignments/NewIndividualAssignmentForm";
import NewTeamAssignmentForm from "main/components/NewAssignments/NewTeamAssignmentForm";
import { useBackendMutation } from "main/utils/useBackend";
import {
  ASSIGNMENT_TYPE_LABELS,
  CREATE_REPOS_FOR_LABELS,
  JOBS_QUERY_KEY,
  NEW_ASSIGNMENTS_URL,
  PERMISSION_LABELS,
  VISIBILITY_LABELS,
  assignmentToFormData,
  assignmentsQueryKey,
  formDataToParams,
  jobStartedMessage,
  onAssignmentMutationError,
} from "main/utils/newAssignmentsUtils";

export default function NewAssignmentsTable({
  assignments,
  courseId,
  testIdPrefix = "NewAssignmentsTable",
}) {
  const [showEditModal, setShowEditModal] = useState(false);
  const [editAssignment, setEditAssignment] = useState(null);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [deleteAssignment, setDeleteAssignment] = useState(null);

  // The type of an assignment cannot be changed, so it is not sent when editing
  const cellToAxiosParamsEdit = (formData) => ({
    url: `${NEW_ASSIGNMENTS_URL}/put`,
    method: "PUT",
    params: {
      courseId: courseId,
      assignmentId: editAssignment.id,
      ...formDataToParams(editAssignment.asnType, formData),
    },
  });

  const cellToAxiosParamsDelete = (assignment) => ({
    url: `${NEW_ASSIGNMENTS_URL}/${assignment.id}`,
    method: "DELETE",
    params: { courseId: courseId },
  });

  const onEditSuccess = (data) => {
    toast(jobStartedMessage("updated", data));
    setShowEditModal(false);
  };

  const onDeleteSuccess = () => {
    toast("Assignment deleted successfully.");
  };

  const editMutation = useBackendMutation(
    cellToAxiosParamsEdit,
    { onSuccess: onEditSuccess, onError: onAssignmentMutationError },
    [assignmentsQueryKey(courseId), JOBS_QUERY_KEY],
  );

  const deleteMutation = useBackendMutation(
    cellToAxiosParamsDelete,
    { onSuccess: onDeleteSuccess, onError: onAssignmentMutationError },
    [assignmentsQueryKey(courseId)],
  );

  const editCallback = (cell) => {
    setEditAssignment(cell.row.original);
    setShowEditModal(true);
  };

  const deleteCallback = (cell) => {
    setDeleteAssignment(cell.row.original);
    setShowDeleteModal(true);
  };

  const submitEditForm = (data) => {
    editMutation.mutate(data);
  };

  const confirmDelete = () => {
    deleteMutation.mutate(deleteAssignment);
  };

  const columns = [
    {
      header: "Repository Prefix",
      accessorKey: "repoPrefix",
      id: "repoPrefix",
    },
    {
      header: "Type",
      accessorFn: (row) => ASSIGNMENT_TYPE_LABELS[row.asnType],
      id: "asnType",
    },
    {
      header: "Visibility",
      accessorFn: (row) => VISIBILITY_LABELS[row.visibility],
      id: "visibility",
    },
    {
      header: "Permission",
      accessorFn: (row) => PERMISSION_LABELS[row.permission],
      id: "permission",
    },
    {
      header: "Repositories For",
      accessorFn: (row) => CREATE_REPOS_FOR_LABELS[row.createReposFor] ?? "",
      id: "createReposFor",
    },
    {
      header: "Team Regex",
      accessorFn: (row) => row.teamRegex ?? "",
      id: "teamRegex",
    },
    ButtonColumn("Edit", "primary", editCallback, testIdPrefix),
    ButtonColumn("Delete", "danger", deleteCallback, testIdPrefix),
  ];

  const editingTeam = editAssignment?.asnType === "TEAM";
  const EditForm = editingTeam
    ? NewTeamAssignmentForm
    : NewIndividualAssignmentForm;

  return (
    <>
      <Modal
        show={showEditModal}
        onHide={() => setShowEditModal(false)}
        centered={true}
        data-testid={`${testIdPrefix}-edit-modal`}
      >
        <Modal.Header closeButton>
          <Modal.Title>
            Edit {editingTeam ? "Team" : "Individual"} Assignment
          </Modal.Title>
        </Modal.Header>
        <Modal.Body data-testid={`${testIdPrefix}-edit-modal-body`}>
          <p data-testid={`${testIdPrefix}-edit-modal-note`}>
            The type of an assignment cannot be changed; to change it, delete
            the assignment and create a new one. Saving starts a job that
            creates the repositories.
          </p>
          <EditForm
            initialContents={
              editAssignment && assignmentToFormData(editAssignment)
            }
            submitAction={submitEditForm}
            buttonLabel="Update"
          />
        </Modal.Body>
      </Modal>
      <ConfirmationModal
        showModal={showDeleteModal}
        setShowModal={setShowDeleteModal}
        onYes={confirmDelete}
      >
        <span data-testid={`${testIdPrefix}-delete-confirmation-message`}>
          Are you sure you want to delete the assignment{" "}
          {deleteAssignment?.repoPrefix}? Repositories that have already been
          created are not deleted.
        </span>
      </ConfirmationModal>
      <OurTable data={assignments} columns={columns} testid={testIdPrefix} />
    </>
  );
}
