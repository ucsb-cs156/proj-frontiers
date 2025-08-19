import { render, fireEvent, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import RosterStudentCSVUploadForm from "main/components/RosterStudent/RosterStudentCSVUploadForm";

describe("RosterStudentCSVUploadForm Tests", () => {
  const mockSubmitAction = jest.fn();
  const file = new File(["there"], "roster.csv", { type: "text/csv" });
  test("Required fires when there's no input", async () => {
    render(<RosterStudentCSVUploadForm />);
    await screen.findByTestId("RosterStudentCSVUploadForm-submit");

    const submitButton = screen.getByTestId(
      "RosterStudentCSVUploadForm-submit",
    );
    fireEvent.click(submitButton);
    await screen.findByText(/Roster is required/);
  });

  test("No errors on good submit", async () => {
    const user = userEvent.setup();
    render(<RosterStudentCSVUploadForm submitAction={mockSubmitAction} />);
    await screen.findByTestId("RosterStudentCSVUploadForm-submit");

    const upload = screen.getByTestId("RosterStudentCSVUploadForm-upload");
    const submitButton = screen.getByTestId(
      "RosterStudentCSVUploadForm-submit",
    );
    await user.upload(upload, file);
    fireEvent.click(submitButton);
    expect(screen.queryByText(/Roster is required/)).not.toBeInTheDocument();

    expect(upload.files).toHaveLength(1);
    expect(upload.files[0]).toStrictEqual(file);
  });
});
