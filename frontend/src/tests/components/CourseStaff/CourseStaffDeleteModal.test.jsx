import { vi } from "vitest";
import CourseStaffDeleteModal from "main/components/CourseStaff/CourseStaffDeleteModal";
import { fireEvent, render, waitFor, screen } from "@testing-library/react";

const mockSubmit = vi.fn();
const showModal = vi.fn();
const toggleShowModal = vi.fn();
describe("CourseStaffDeleteModal tests", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });
  test("CourseStaffDeleteModal renders correctly, submits default", async () => {
    render(
      <div
        className="modal show"
        style={{ display: "block", position: "initial" }}
      >
        <CourseStaffDeleteModal
          showModal={showModal}
          toggleShowModal={toggleShowModal}
          onSubmitAction={mockSubmit}
        />
      </div>,
    );

    expect(screen.getByTestId("CourseStaffDeleteModal")).toHaveClass(
      "modal-dialog-centered",
    );

    const submitButton = await screen.findByText("Delete Staff Member");
    fireEvent.click(submitButton);
    await waitFor(() => expect(mockSubmit).toHaveBeenCalledTimes(1));
    expect(mockSubmit).toHaveBeenCalledWith(
      expect.objectContaining({
        removeFromOrg: "false",
      }),
      expect.anything(),
    );
  });

  test("CourseStaffDeleteModal submits selected answer", async () => {
    render(
      <div
        className="modal show"
        style={{ display: "block", position: "initial" }}
      >
        <CourseStaffDeleteModal
          showModal={showModal}
          toggleShowModal={toggleShowModal}
          onSubmitAction={mockSubmit}
        />
      </div>,
    );

    const submitButton = await screen.findByText("Delete Staff Member");
    fireEvent.click(
      screen.getByLabelText(
        "Yes, I'd like to remove them from the GitHub Organization",
      ),
    );
    fireEvent.click(submitButton);
    await waitFor(() => expect(mockSubmit).toHaveBeenCalledTimes(1));
    expect(mockSubmit).toHaveBeenCalledWith(
      expect.objectContaining({
        removeFromOrg: "true",
      }),
      expect.anything(),
    );
  });

  test("Can click close", async () => {
    render(
      <div
        className="modal show"
        style={{ display: "block", position: "initial" }}
      >
        <CourseStaffDeleteModal
          showModal={showModal}
          toggleShowModal={toggleShowModal}
          onSubmitAction={mockSubmit}
        />
      </div>,
    );

    const closeButton = await screen.findByRole("button", { name: "Close" });
    fireEvent.click(closeButton);
    await waitFor(() => expect(toggleShowModal).toHaveBeenCalledTimes(1));
    expect(toggleShowModal).toHaveBeenCalledWith(false);
  });
});
