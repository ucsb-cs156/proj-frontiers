import { vi } from "vitest";
import PurgeDroppedStudentsModal from "main/components/RosterStudent/PurgeDroppedStudentsModal";
import { fireEvent, render, waitFor, screen } from "@testing-library/react";

const mockSubmit = vi.fn();
const showModal = vi.fn();
const toggleShowModal = vi.fn();
describe("PurgeDroppedStudentsModal tests", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });
  test("renders correctly, shows count, submits default", async () => {
    render(
      <div
        className="modal show"
        style={{ display: "block", position: "initial" }}
      >
        <PurgeDroppedStudentsModal
          showModal={showModal}
          toggleShowModal={toggleShowModal}
          onSubmitAction={mockSubmit}
          droppedCount={3}
        />
      </div>,
    );

    expect(screen.getByTestId("PurgeDroppedStudentsModal")).toHaveClass(
      "modal-dialog-centered",
    );
    expect(screen.getByText("Purge All Dropped Students")).toBeInTheDocument();
    expect(
      screen.getByTestId("PurgeDroppedStudentsModal-message"),
    ).toHaveTextContent(
      "Are you sure you want to permanently delete all 3 dropped student(s) from this course? This cannot be undone.",
    );

    const submitButton = screen.getByTestId("PurgeDroppedStudentsModal-submit");
    expect(submitButton).toHaveTextContent("Purge Dropped Students");
    expect(submitButton).toHaveClass("btn-danger");
    fireEvent.click(submitButton);
    await waitFor(() => expect(mockSubmit).toHaveBeenCalledTimes(1));
    expect(mockSubmit).toHaveBeenCalledWith(
      expect.objectContaining({
        removeFromOrg: "false",
      }),
      expect.anything(),
    );
  });

  test("submits selected answer", async () => {
    render(
      <div
        className="modal show"
        style={{ display: "block", position: "initial" }}
      >
        <PurgeDroppedStudentsModal
          showModal={showModal}
          toggleShowModal={toggleShowModal}
          onSubmitAction={mockSubmit}
          droppedCount={1}
        />
      </div>,
    );

    fireEvent.click(
      screen.getByLabelText(
        "Yes, I'd like to remove them from the GitHub Organization",
      ),
    );
    fireEvent.click(screen.getByTestId("PurgeDroppedStudentsModal-submit"));
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
        <PurgeDroppedStudentsModal
          showModal={showModal}
          toggleShowModal={toggleShowModal}
          onSubmitAction={mockSubmit}
          droppedCount={1}
        />
      </div>,
    );

    const closeButton = await screen.findByRole("button", { name: "Close" });
    fireEvent.click(closeButton);
    await waitFor(() => expect(toggleShowModal).toHaveBeenCalledTimes(1));
    expect(toggleShowModal).toHaveBeenCalledWith(false);
    expect(mockSubmit).not.toHaveBeenCalled();
  });
});
