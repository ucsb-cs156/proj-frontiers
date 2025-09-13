import { render, fireEvent, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import TeamsCSVUploadForm from "main/components/Teams/TeamsCSVUploadForm";
import { vi } from "vitest";

describe("TeamsCSVUploadForm Tests", () => {
  const mockSubmitAction = vi.fn();
  const file = new File(["there"], "teams.csv", { type: "text/csv" });
  test("Required fires when there's no input", async () => {
    render(<TeamsCSVUploadForm />);
    await screen.findByTestId("TeamsCSVUploadForm-submit");

    const submitButton = screen.getByTestId("TeamsCSVUploadForm-submit");
    fireEvent.click(submitButton);
    await screen.findByText(/Team CSV is required/);
  });

  test("No errors on good submit", async () => {
    const user = userEvent.setup();
    render(<TeamsCSVUploadForm submitAction={mockSubmitAction} />);
    await screen.findByTestId("TeamsCSVUploadForm-submit");

    const upload = screen.getByTestId("TeamsCSVUploadForm-upload");
    const submitButton = screen.getByTestId("TeamsCSVUploadForm-submit");
    await user.upload(upload, file);
    fireEvent.click(submitButton);
    expect(screen.queryByText(/Team CSV is required/)).not.toBeInTheDocument();

    expect(upload.files).toHaveLength(1);
    expect(upload.files[0]).toStrictEqual(file);
  });
});
