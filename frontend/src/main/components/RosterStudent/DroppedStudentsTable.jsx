import React, { useState } from "react";
import { useBackendMutation } from "main/utils/useBackend";
import { toast } from "react-toastify";
import OurTable, { ButtonColumn } from "main/components/OurTable";
import RosterStudentDeleteModal from "main/components/RosterStudent/RosterStudentDeleteModal";
import { cellToAxiosParamsDelete } from "main/utils/rosterStudentUtils";

export default function DroppedStudentsTable({ students, courseId }) {
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [deleteStudent, setDeleteStudent] = useState(null);

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

  const cellToAxiosParamsRestore = (cell) => ({
    url: `/api/rosterstudents/restore`,
    method: "PUT",
    params: {
      id: cell.row.original.id,
    },
  });

  const restoreSuccess = () => {
    toast("Student successfully restored to course.");
  };

  const restoreMutation = useBackendMutation(
    cellToAxiosParamsRestore,
    {
      onSuccess: restoreSuccess,
    },
    [`/api/rosterstudents/course/${courseId}`],
  );

  const restoreCallback = (cell) => {
    restoreMutation.mutate(cell);
  };

  const hideDeleteModal = () => {
    setShowDeleteModal(false);
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

  const deleteCallback = (cell) => {
    setDeleteStudent(cell.row.original.id);
    setShowDeleteModal(true);
  };

  const submitDeleteForm = (data) => {
    deleteMutation.mutate({
      id: deleteStudent,
      ...data,
    });
  };

  columns.push(
    ButtonColumn("Restore", "primary", restoreCallback, "RestoreButton"),
  );
  columns.push(
    ButtonColumn("Delete", "danger", deleteCallback, "DeleteDroppedButton"),
  );

  return (
    <>
      <RosterStudentDeleteModal
        showModal={showDeleteModal}
        toggleShowModal={setShowDeleteModal}
        onSubmitAction={submitDeleteForm}
      />
      <OurTable
        columns={columns}
        data={students}
        testid={"DroppedStudentsTable"}
      />
    </>
  );
}
