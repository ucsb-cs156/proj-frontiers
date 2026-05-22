import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import CourseStaffCSVUploadForm from "main/components/CourseStaff/CourseStaffCSVUploadForm";
import { vi } from "vitest";

describe("CourseStaffCSVUploadForm tests", () => {
  test("renders without crashing", () => {
    render(<CourseStaffCSVUploadForm submitAction={vi.fn()} />);
    expect(screen.getByText("Upload Staff CSV")).toBeInTheDocument();
    expect(
      screen.getByTestId("CourseStaffCSVUploadForm-upload"),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId("CourseStaffCSVUploadForm-submit"),
    ).toBeInTheDocument();
  });

  test("shows validation error when no file is selected", async () => {
    const submitAction = vi.fn();
    render(<CourseStaffCSVUploadForm submitAction={submitAction} />);

    const submitButton = screen.getByTestId("CourseStaffCSVUploadForm-submit");
    await userEvent.click(submitButton);

    await waitFor(() => {
      expect(screen.getByText("Staff CSV is required.")).toBeInTheDocument();
    });
    expect(submitAction).not.toHaveBeenCalled();
  });

  test("calls submitAction when a file is selected and submitted", async () => {
    const submitAction = vi.fn();
    render(<CourseStaffCSVUploadForm submitAction={submitAction} />);

    const file = new File(
      ["firstName,lastName,email\nChris,Gaucho,cgaucho@ucsb.edu"],
      "staff.csv",
      { type: "text/csv" },
    );

    const input = screen.getByTestId("CourseStaffCSVUploadForm-upload");
    await userEvent.upload(input, file);

    const submitButton = screen.getByTestId("CourseStaffCSVUploadForm-submit");
    await userEvent.click(submitButton);

    await waitFor(() => {
      expect(submitAction).toHaveBeenCalledTimes(1);
    });
  });
});
