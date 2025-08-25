import CourseModal from "main/components/Courses/CourseModal";
import React from "react";
import {
  fireEvent,
  render,
  screen,
  waitFor,
  rerender,
} from "@testing-library/react";
import coursesFixtures from "fixtures/coursesFixtures";
import { vi } from "vitest";

const mockSubmit = vi.fn();
const showModal = vi.fn();
const toggleShowModal = vi.fn();

describe("CourseModal Tests", () => {
  test("Validation works correctly", async () => {
    render(
      <div
        className="modal show"
        style={{ display: "block", position: "initial" }}
      >
        <CourseModal
          showModal={showModal}
          toggleShowModal={toggleShowModal}
          onSubmitAction={mockSubmit}
        />
      </div>,
    );

    const modalTitle = screen.getByText("Create Course");
    expect(modalTitle).toBeInTheDocument();

    const submitButton = screen.getByTestId(/CourseModal-submit/);
    fireEvent.click(submitButton);

    expect(mockSubmit).toHaveBeenCalledTimes(0);

    await screen.findByText(/Course Name is required./);
    expect(screen.getByText(/Course Term is required./)).toBeInTheDocument();
    expect(screen.getByText(/School is required./)).toBeInTheDocument();
  });

  test("Can see initialContents", async () => {
    render(
      <div
        className="modal show"
        style={{ display: "block", position: "initial" }}
      >
        <CourseModal
          showModal={showModal}
          toggleShowModal={toggleShowModal}
          onSubmitAction={mockSubmit}
          initialContents={coursesFixtures.severalCourses[0]}
          buttonText={"Edit"}
          modalTitle={"Edit Course"}
        />
      </div>,
    );

    const modalTitle = screen.getByText("Edit Course");
    expect(modalTitle).toBeInTheDocument();

    expect(screen.getByDisplayValue("CMPSC 156")).toBeInTheDocument();
    expect(screen.getByDisplayValue("Spring 2025")).toBeInTheDocument();
    expect(screen.getByDisplayValue("UCSB")).toBeInTheDocument();
    expect(screen.getByText("Edit")).toBeInTheDocument();
  });

  test("Can submit successfully", async () => {
    render(
      <div
        className="modal show"
        style={{ display: "block", position: "initial" }}
      >
        <CourseModal
          showModal={showModal}
          toggleShowModal={toggleShowModal}
          onSubmitAction={mockSubmit}
        />
      </div>,
    );

    const courseName = screen.getByLabelText("Course Name");
    const courseTerm = screen.getByLabelText("Term");
    const school = screen.getByLabelText("School");
    fireEvent.change(courseName, { target: { value: "CMPSC 156" } });
    fireEvent.change(courseTerm, { target: { value: "Spring 2025" } });
    fireEvent.change(school, { target: { value: "UCSB" } });
    expect(screen.getByTestId("CourseModal-courseName")).toBeInTheDocument();
    expect(screen.getByTestId("CourseModal-term")).toBeInTheDocument();
    expect(screen.getByTestId("CourseModal-school")).toBeInTheDocument();
    expect(screen.getByTestId("CourseModal-base")).toHaveClass(
      "modal-dialog-centered",
    );
    const submitButton = screen.getByText("Create");
    fireEvent.click(submitButton);

    await waitFor(() => expect(mockSubmit).toHaveBeenCalledTimes(1));
  });

  test("Can click close", async () => {
    render(
      <div
        className="modal show"
        style={{ display: "block", position: "initial" }}
      >
        <CourseModal
          showModal={showModal}
          toggleShowModal={toggleShowModal}
          onSubmitAction={mockSubmit}
        />
      </div>,
    );

    const closeButton = screen.getByTestId("CourseModal-closeButton");
    fireEvent.click(closeButton);
    await waitFor(() => expect(toggleShowModal).toHaveBeenCalledTimes(1));
    expect(toggleShowModal).toHaveBeenCalledWith(false);
  });

  test("Form starts empty without initialContents", async () => {
    render(
      <div
        className="modal show"
        style={{ display: "block", position: "initial" }}
      >
        <CourseModal
          showModal={showModal}
          toggleShowModal={toggleShowModal}
          onSubmitAction={mockSubmit}
        />
      </div>,
    );

    const courseNameInput = screen.getByTestId("CourseModal-courseName");
    const termInput = screen.getByTestId("CourseModal-term");
    const schoolInput = screen.getByTestId("CourseModal-school");

    expect(courseNameInput.value).toBe("");
    expect(termInput.value).toBe("");
    expect(schoolInput.value).toBe("");
  });

  test("Form gets populated when initialContents change from empty to filled", async () => {
    const MockCourseModal = ({ initialContents }) => (
      <div
        className="modal show"
        style={{ display: "block", position: "initial" }}
      >
        <CourseModal
          showModal={showModal}
          toggleShowModal={toggleShowModal}
          onSubmitAction={mockSubmit}
          initialContents={initialContents}
        />
      </div>
    );

    // Start with no initial contents
    const { rerender } = render(<MockCourseModal initialContents={null} />);

    const courseNameInput = screen.getByTestId("CourseModal-courseName");
    const termInput = screen.getByTestId("CourseModal-term");
    const schoolInput = screen.getByTestId("CourseModal-school");

    // Verify form starts empty
    expect(courseNameInput.value).toBe("");
    expect(termInput.value).toBe("");
    expect(schoolInput.value).toBe("");

    // Change to have initial contents
    rerender(
      <MockCourseModal initialContents={coursesFixtures.severalCourses[0]} />,
    );

    // Verify form gets populated
    await waitFor(() => {
      expect(courseNameInput.value).toBe("CMPSC 156");
      expect(termInput.value).toBe("Spring 2025");
      expect(schoolInput.value).toBe("UCSB");
    });
  });

  test("Form updates when initialContents change from one course to another", async () => {
    const MockCourseModal = ({ initialContents }) => (
      <div
        className="modal show"
        style={{ display: "block", position: "initial" }}
      >
        <CourseModal
          showModal={showModal}
          toggleShowModal={toggleShowModal}
          onSubmitAction={mockSubmit}
          initialContents={initialContents}
        />
      </div>
    );

    // Start with first course
    const { rerender } = render(
      <MockCourseModal initialContents={coursesFixtures.severalCourses[0]} />,
    );

    const courseNameInput = screen.getByTestId("CourseModal-courseName");
    const termInput = screen.getByTestId("CourseModal-term");
    const schoolInput = screen.getByTestId("CourseModal-school");

    // Verify first course data
    expect(courseNameInput.value).toBe("CMPSC 156");
    expect(termInput.value).toBe("Spring 2025");
    expect(schoolInput.value).toBe("UCSB");

    // Change to second course
    rerender(
      <MockCourseModal initialContents={coursesFixtures.severalCourses[1]} />,
    );

    // Verify form updates to second course data
    await waitFor(() => {
      expect(courseNameInput.value).toBe("CPTS 489");
      expect(termInput.value).toBe("Fall 2020");
      expect(schoolInput.value).toBe("WSU");
    });
  });

  test("useEffect handles undefined initialContents correctly", async () => {
    const MockCourseModal = ({ initialContents }) => (
      <div
        className="modal show"
        style={{ display: "block", position: "initial" }}
      >
        <CourseModal
          showModal={showModal}
          toggleShowModal={toggleShowModal}
          onSubmitAction={mockSubmit}
          initialContents={initialContents}
        />
      </div>
    );

    // Start with undefined initial contents
    const { rerender } = render(
      <MockCourseModal initialContents={undefined} />,
    );

    const courseNameInput = screen.getByTestId("CourseModal-courseName");
    const termInput = screen.getByTestId("CourseModal-term");
    const schoolInput = screen.getByTestId("CourseModal-school");

    // Verify form starts empty
    expect(courseNameInput.value).toBe("");
    expect(termInput.value).toBe("");
    expect(schoolInput.value).toBe("");

    // Change to have course data
    rerender(
      <MockCourseModal initialContents={coursesFixtures.severalCourses[0]} />,
    );

    // Verify form gets populated
    await waitFor(() => {
      expect(courseNameInput.value).toBe("CMPSC 156");
      expect(termInput.value).toBe("Spring 2025");
      expect(schoolInput.value).toBe("UCSB");
    });
  });

  test("defaultValues prop works correctly with initialContents", async () => {
    // This test verifies that the defaultValues prop in useForm works
    render(
      <div
        className="modal show"
        style={{ display: "block", position: "initial" }}
      >
        <CourseModal
          showModal={showModal}
          toggleShowModal={toggleShowModal}
          onSubmitAction={mockSubmit}
          initialContents={coursesFixtures.severalCourses[0]}
        />
      </div>,
    );

    // The form should be immediately populated due to defaultValues
    const courseNameInput = screen.getByTestId("CourseModal-courseName");
    const termInput = screen.getByTestId("CourseModal-term");
    const schoolInput = screen.getByTestId("CourseModal-school");

    expect(courseNameInput.value).toBe("CMPSC 156");
    expect(termInput.value).toBe("Spring 2025");
    expect(schoolInput.value).toBe("UCSB");
  });

  test("defaultValues uses empty object when initialContents is null", async () => {
    render(
      <div
        className="modal show"
        style={{ display: "block", position: "initial" }}
      >
        <CourseModal
          showModal={showModal}
          toggleShowModal={toggleShowModal}
          onSubmitAction={mockSubmit}
          initialContents={null}
        />
      </div>,
    );

    const courseNameInput = screen.getByTestId("CourseModal-courseName");
    const termInput = screen.getByTestId("CourseModal-term");
    const schoolInput = screen.getByTestId("CourseModal-school");

    expect(courseNameInput.value).toBe("");
    expect(termInput.value).toBe("");
    expect(schoolInput.value).toBe("");
  });
});
