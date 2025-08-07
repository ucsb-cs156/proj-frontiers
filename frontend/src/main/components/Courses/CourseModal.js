import Modal from "react-bootstrap/Modal";
import { Form } from "react-bootstrap";
import { useForm } from "react-hook-form";

function CourseModal({
  onSubmitAction,
  showModal,
  toggleShowModal,
  initialContents,
  buttonText = "Create",
}) {
  const {
    register,
    formState: { errors },
    handleSubmit,
  } = useForm({ defaultValues: initialContents || {} });

  const closeModal = () => {
    toggleShowModal(false);
  };

  // Determine if we're editing or creating based on whether initialContents has an id
  const isEditing = initialContents && initialContents.id;
  const modalTitle = isEditing ? "Edit Course" : "Create Course";

  return (
    <Modal
      show={showModal}
      onHide={closeModal}
      centered={true}
      data-testid={"CourseModal-base"}
    >
      <Modal.Header>
        <Modal.Title>{modalTitle}</Modal.Title>
        <button
          type="button"
          className="btn-close"
          aria-label="Close"
          data-testid={"CourseModal-closeButton"}
          onClick={closeModal}
        ></button>
      </Modal.Header>
      <Form onSubmit={handleSubmit(onSubmitAction)}>
        <Modal.Body>
          <Form.Group>
            <Form.Label htmlFor="courseName">Course Name</Form.Label>
            <Form.Control
              data-testid={"CourseModal-courseName"}
              id="courseName"
              type="text"
              isInvalid={Boolean(errors.courseName)}
              {...register("courseName", {
                required: "Course Name is required.",
              })}
            />
            <Form.Control.Feedback type="invalid">
              {errors.courseName?.message}
            </Form.Control.Feedback>
            <Form.Label htmlFor="term">Term</Form.Label>
            <Form.Control
              data-testid={"CourseModal-term"}
              id="term"
              type="text"
              isInvalid={Boolean(errors.term)}
              {...register("term", {
                required: "Course Term is required.",
              })}
            />
            <Form.Control.Feedback type="invalid">
              {errors.term?.message}
            </Form.Control.Feedback>
          </Form.Group>
          <Form.Group>
            <Form.Label htmlFor="school">School</Form.Label>
            <Form.Control
              data-testid={"CourseModal-school"}
              id="school"
              type="text"
              isInvalid={Boolean(errors.school)}
              {...register("school", {
                required: "School is required.",
              })}
            />
            <Form.Control.Feedback type="invalid">
              {errors.school?.message}
            </Form.Control.Feedback>
          </Form.Group>
        </Modal.Body>
        <Modal.Footer>
          <button
            type="submit"
            className="btn btn-primary"
            data-testid="CourseModal-submit"
          >
            {buttonText}
          </button>
        </Modal.Footer>
      </Form>
    </Modal>
  );
}

export default CourseModal;
