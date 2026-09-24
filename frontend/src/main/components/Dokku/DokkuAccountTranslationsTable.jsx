import React, { useState } from "react";
import Modal from "react-bootstrap/Modal";
import { toast } from "react-toastify";

import OurTable, { ButtonColumn } from "main/components/OurTable";
import ConfirmationModal from "main/components/Common/ConfirmationModal";
import DokkuAccountTranslationsForm from "main/components/Dokku/DokkuAccountTranslationsForm";
import { useBackendMutation } from "main/utils/useBackend";
import {
  dokkuTranslationsQueryKey,
  onDokkuTranslationMutationError,
} from "main/utils/dokkuUtils";

export default function DokkuAccountTranslationsTable({
  translations,
  courseId,
  testIdPrefix = "DokkuAccountTranslationsTable",
}) {
  const [showEditModal, setShowEditModal] = useState(false);
  const [editTranslation, setEditTranslation] = useState(null);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [deleteTranslation, setDeleteTranslation] = useState(null);

  const queryKey = dokkuTranslationsQueryKey(courseId);

  const cellToAxiosParamsEdit = (formData) => ({
    url: "/api/dokku/translations",
    method: "PUT",
    params: {
      courseId: courseId,
      email: formData.email,
      username: formData.username,
    },
  });

  const cellToAxiosParamsDelete = (translation) => ({
    url: "/api/dokku/translations",
    method: "DELETE",
    params: {
      courseId: courseId,
      email: translation.email,
    },
  });

  const onEditSuccess = () => {
    toast("Dokku account translation updated successfully.");
    setShowEditModal(false);
  };

  const onDeleteSuccess = () => {
    toast("Dokku account translation deleted successfully.");
  };

  const editMutation = useBackendMutation(
    cellToAxiosParamsEdit,
    { onSuccess: onEditSuccess, onError: onDokkuTranslationMutationError },
    [queryKey],
  );

  const deleteMutation = useBackendMutation(
    cellToAxiosParamsDelete,
    { onSuccess: onDeleteSuccess },
    [queryKey],
  );

  const editCallback = (cell) => {
    setEditTranslation(cell.row.original);
    setShowEditModal(true);
  };

  const deleteCallback = (cell) => {
    setDeleteTranslation(cell.row.original);
    setShowDeleteModal(true);
  };

  const submitEditForm = (data) => {
    editMutation.mutate(data);
  };

  const confirmDelete = () => {
    deleteMutation.mutate(deleteTranslation);
  };

  const columns = [
    {
      header: "Email",
      accessorKey: "email",
      id: "email",
    },
    {
      header: "Dokku Username",
      accessorKey: "username",
      id: "username",
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
          <Modal.Title>Edit Dokku Account Translation</Modal.Title>
        </Modal.Header>
        <Modal.Body data-testid={`${testIdPrefix}-edit-modal-body`}>
          <DokkuAccountTranslationsForm
            initialContents={editTranslation}
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
          Are you sure you want to delete the dokku account translation for{" "}
          {deleteTranslation?.email} ({deleteTranslation?.username})?
        </span>
      </ConfirmationModal>
      <OurTable data={translations} columns={columns} testid={testIdPrefix} />
    </>
  );
}
