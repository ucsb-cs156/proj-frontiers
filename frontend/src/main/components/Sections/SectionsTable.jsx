import React, { useState } from "react";
import Modal from "react-bootstrap/Modal";
import { toast } from "react-toastify";

import OurTable, { ButtonColumn } from "main/components/OurTable";
import ConfirmationModal from "main/components/Common/ConfirmationModal";
import SectionsForm from "main/components/Sections/SectionsForm";
import { useBackendMutation } from "main/utils/useBackend";
import { onSectionMutationError } from "main/utils/sectionsUtils";

export default function SectionsTable({
  sections,
  courseId,
  testIdPrefix = "SectionsTable",
}) {
  const [showEditModal, setShowEditModal] = useState(false);
  const [editSection, setEditSection] = useState(null);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [deleteSection, setDeleteSection] = useState(null);

  const sectionsQueryKey = `/api/courses/${courseId}/sections`;

  const cellToAxiosParamsEdit = (formData) => ({
    url: `/api/courses/${courseId}/sections/${formData.id}`,
    method: "PUT",
    params: {
      section: formData.section,
      label: formData.label,
    },
  });

  const cellToAxiosParamsDelete = (section) => ({
    url: `/api/courses/${courseId}/sections/${section.id}`,
    method: "DELETE",
  });

  const onEditSuccess = () => {
    toast("Section updated successfully.");
    setShowEditModal(false);
  };

  const onDeleteSuccess = () => {
    toast("Section deleted successfully.");
  };

  const editMutation = useBackendMutation(
    cellToAxiosParamsEdit,
    { onSuccess: onEditSuccess, onError: onSectionMutationError },
    [sectionsQueryKey],
  );

  const deleteMutation = useBackendMutation(
    cellToAxiosParamsDelete,
    { onSuccess: onDeleteSuccess },
    [sectionsQueryKey],
  );

  const editCallback = (cell) => {
    setEditSection(cell.row.original);
    setShowEditModal(true);
  };

  const deleteCallback = (cell) => {
    setDeleteSection(cell.row.original);
    setShowDeleteModal(true);
  };

  const submitEditForm = (data) => {
    editMutation.mutate(data);
  };

  const confirmDelete = () => {
    deleteMutation.mutate(deleteSection);
  };

  const columns = [
    {
      header: "id",
      accessorKey: "id",
      id: "id",
    },
    {
      header: "Section",
      accessorKey: "section",
      id: "section",
    },
    {
      header: "Label",
      accessorKey: "label",
      id: "label",
    },
    ButtonColumn("Edit", "primary", editCallback, testIdPrefix),
    ButtonColumn("Delete", "danger", deleteCallback, testIdPrefix),
  ];

  return (
    <>
      <Modal
        show={showEditModal}
        onHide={() => setShowEditModal(false)}
        centered={true}
        data-testid={`${testIdPrefix}-edit-modal`}
      >
        <Modal.Header closeButton>
          <Modal.Title>Edit Section</Modal.Title>
        </Modal.Header>
        <Modal.Body data-testid={`${testIdPrefix}-edit-modal-body`}>
          <SectionsForm
            initialContents={editSection}
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
          Are you sure you want to delete section {deleteSection?.section} (
          {deleteSection?.label})?
        </span>
      </ConfirmationModal>
      <OurTable data={sections} columns={columns} testid={testIdPrefix} />
    </>
  );
}
