import { render, fireEvent, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import CourseStaffCSVUploadForm from "main/components/CourseStaff/CourseStaffCSVUploadForm";
import { vi } from "vitest";

describe("CourseStaffCSVUploadForm Tests", () => {
  const mockSubmitAction = vi.fn();
  const file = new File(["firstName,lastName,email\nPhill,Conrad,phtcon@ucsb.edu"], "staff.csv", { type: "text/csv" });
  test("Required fires when there's no input", async () => {
    render(<CourseStaffCSVUploadForm />);
    await screen.findByTestId("CourseStaffCSVUploadForm-submit");

    const submitButton = screen.getByTestId(
        "CourseStaffCSVUploadForm-submit"
    );
    fireEvent.click(submitButton);
    await screen.findByText(/File is required/);
  });

  test("No errors on good submit", async () => {
    const user = userEvent.setup();
    render(<CourseStaffCSVUploadForm submitAction={mockSubmitAction} />);
    await screen.findByTestId("CourseStaffCSVUploadForm-submit");

    const upload = screen.getByTestId("CourseStaffCSVUploadForm-upload");
    const submitButton = screen.getByTestId(
        "CourseStaffCSVUploadForm-submit"
    );
    await user.upload(upload, file);
    fireEvent.click(submitButton);
    expect(screen.queryByText(/File is required/)).not.toBeInTheDocument();
    
    expect(upload.files).toHaveLength(1);
    expect(upload.files[0]).toStrictEqual(file);
  });
});