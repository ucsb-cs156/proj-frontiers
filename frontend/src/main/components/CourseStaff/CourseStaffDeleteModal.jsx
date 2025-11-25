import Modal from "react-bootstrap/Modal";
import { useForm } from "react-hook-form";
import { Form } from "react-bootstrap";

export default function CourseStaffDeleteModal({
  onSubmitAction,
  showModal,
  toggleShowModal,
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
      data-testid="CourseStaffDeleteModal"
    >
      <Modal.Header closeButton>Delete Course Staff</Modal.Header>
      <Form onSubmit={handleSubmit(onSubmitAction)}>
        <Modal.Body>
          <Form.Text>
            Are you sure you want to delete this course staff?
          </Form.Text>
          <Form.Group>
            <Form.Check
              type="radio"
              label="Yes, I'd like to remove them from the GitHub Organization"
              value="true"
              id="remove-yes"
              {...register("removeFromOrg")}
            />
            <Form.Check
              type="radio"
              label="No, I'd like to keep them in the GitHub Organization"
              value="false"
              id="remove-no"
              defaultChecked
              {...register("removeFromOrg")}
            />
          </Form.Group>
        </Modal.Body>
        <Modal.Footer>
          <button type="submit" className="btn btn-primary">
            Delete Staff
          </button>
        </Modal.Footer>
      </Form>
    </Modal>
  );
}
