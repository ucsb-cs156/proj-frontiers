import Modal from "react-bootstrap/Modal";
import { Button } from "react-bootstrap";

export default function SanityCheckModal({
  children,
  showModal,
  setShowModal,
  onYes,
}) {
  const closeModal = () => {
    setShowModal(false);
  };
  return (
    <Modal
      show={showModal}
      onHide={closeModal}
      centered={true}
      data-testid={"SanityCheckModal-base"}
    >
      <Modal.Header>
        <Modal.Title>Are You Sure?</Modal.Title>
        <Button
          className="btn-close"
          data-testid={"SanityCheckModal-closeButton"}
          onClick={closeModal}
        ></Button>
      </Modal.Header>
      <Modal.Body>{children}</Modal.Body>
      <Modal.Footer>
        <Button type="button" onClick={closeModal}>
          No, take me back
        </Button>
        <Button
          type="button"
          className="ms-auto"
          onClick={() => {
            onYes();
            closeModal();
          }}
          variant={"danger"}
        >
          Yes, I'd like to do this
        </Button>
      </Modal.Footer>
    </Modal>
  );
}
