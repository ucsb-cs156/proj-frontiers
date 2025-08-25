import CourseModal from "main/components/Courses/CourseModal";
import React from "react";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
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
});
