import Modal from "react-bootstrap/Modal";
import { useForm } from "react-hook-form";
import { Form } from "react-bootstrap";

export default function PurgeDroppedStudentsModal({
  onSubmitAction,
  showModal,
  toggleShowModal,
  droppedCount,
}) {
  const hideModal = () => {
    toggleShowModal(false);
  };

  const { register, handleSubmit } = useForm();

  return (
    <Modal
      show={showModal}
      onHide={hideModal}
      centered={true}
      data-testid="PurgeDroppedStudentsModal"
    >
      <Modal.Header closeButton>Purge All Dropped Students</Modal.Header>
      <Form onSubmit={handleSubmit(onSubmitAction)}>
        <Modal.Body>
          <Form.Text data-testid="PurgeDroppedStudentsModal-message">
            Are you sure you want to permanently delete all {droppedCount}{" "}
            dropped student(s) from this course? This cannot be undone.
          </Form.Text>
          <Form.Group>
            <Form.Check
              type="radio"
              label="Yes, I'd like to remove them from the GitHub Organization"
              value="true"
              id="purge-remove-yes"
              {...register("removeFromOrg")}
            />
            <Form.Check
              type="radio"
              label="No, I'd like to keep them in the GitHub Organization"
              value="false"
              id="purge-remove-no"
              defaultChecked
              {...register("removeFromOrg")}
            />
          </Form.Group>
        </Modal.Body>
        <Modal.Footer>
          <button
            type="submit"
            className="btn btn-danger"
            data-testid="PurgeDroppedStudentsModal-submit"
          >
            Purge Dropped Students
          </button>
        </Modal.Footer>
      </Form>
    </Modal>
  );
}
