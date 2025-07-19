import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import SanityCheckModal from "main/components/Common/SanityCheckModal";

test("Sanity Check Modal works as designed", async () => {
  const mockSubmit = jest.fn();
  const showModal = jest.fn();
  const toggleShowModal = jest.fn();
  render(
    <div
      className="modal show"
      style={{ display: "block", position: "initial" }}
    >
      <SanityCheckModal
        showModal={showModal}
        setShowModal={(x) => toggleShowModal(x)}
        onYes={mockSubmit}
      >
        <p>renders child!</p>
      </SanityCheckModal>
    </div>,
  );

  expect(screen.getByText("Are You Sure?")).toBeInTheDocument();
  expect(screen.getByText("No, take me back")).toBeInTheDocument();
  expect(screen.getByText("Yes, I'd like to do this")).toHaveClass(
    "ms-auto",
    "btn-danger",
  );
  expect(screen.getByText("renders child!")).toBeInTheDocument();
  fireEvent.click(screen.getByText("Yes, I'd like to do this"));
  await waitFor(() => expect(toggleShowModal).toHaveBeenCalledTimes(1));
  expect(toggleShowModal).toHaveBeenCalledWith(false);
  expect(mockSubmit).toHaveBeenCalledTimes(1);
  fireEvent.click(screen.getByText("No, take me back"));
  await waitFor(() => expect(toggleShowModal).toHaveBeenCalledTimes(2));
  expect(toggleShowModal).toHaveBeenCalledWith(false);
  expect(screen.getByTestId("SanityCheckModal-closeButton")).toHaveClass(
    "btn-close",
  );
  expect(screen.getByTestId("SanityCheckModal-base")).toHaveClass(
    "modal-dialog-centered",
  );
});
