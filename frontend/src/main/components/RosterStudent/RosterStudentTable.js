import React from "react";
import OurTable, { ButtonColumn } from "main/components/OurTable";

import { useBackendMutation } from "main/utils/useBackend";
import {
  cellToAxiosParamsDelete,
  onDeleteSuccess,
} from "main/utils/rosterStudentUtils";
import { hasRole } from "main/utils/currentUser";
import Modal from "react-bootstrap/Modal";
import RosterStudentForm from "main/components/RosterStudent/RosterStudentForm";
import { toast } from "react-toastify";

export default function RosterStudentTable({
  students,
  currentUser,
  courseId,
  testIdPrefix = "RosterStudentTable",
}) {
  const [showEditModal, setShowEditModal] = React.useState(false);
  const [editStudent, setEditStudent] = React.useState(null);

  // Stryker disable all : hard to test for query caching
  const deleteMutation = useBackendMutation(
    cellToAxiosParamsDelete,
    { onSuccess: onDeleteSuccess },
    [`/api/rosterstudents/course/${courseId}`],
  );
  // Stryker restore all

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

  const hideModal = () => {
    setShowEditModal(false);
  };

  const onEditSuccess = () => {
    toast("Student updated successfully.");
    hideModal();
  };

  // Stryker disable next-line all
  const deleteCallback = async (cell) => {
    deleteMutation.mutate(cell);
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
  ];

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
      <OurTable data={students} columns={columns} testid={testIdPrefix} />
      <div
        style={{ display: "none" }}
        data-testid={`${testIdPrefix}-courseId`}
        data-course-id={`${courseId}`}
      />
    </>
  );
}
