import { render, screen, fireEvent } from "@testing-library/react";
import DownloadsTabComponent from "main/components/TabComponent/DownloadsTabComponent";
import { vi } from "vitest";

const testId = "InstructorCourseShowPage";

describe("DownloadsTabComponent Tests", () => {
  test("renders Download Student CSV button", () => {
    render(<DownloadsTabComponent courseId={1} testIdPrefix={testId} />);

    expect(
      screen.getByTestId(`${testId}-DownloadsTabComponent`),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId(`${testId}-download-student-csv-button`),
    ).toHaveTextContent("Download Student CSV");
  });

  test("clicking Download Student CSV opens the correct URL", () => {
    const openSpy = vi.spyOn(window, "open").mockImplementation(() => {});

    render(<DownloadsTabComponent courseId={42} testIdPrefix={testId} />);

    fireEvent.click(
      screen.getByTestId(`${testId}-download-student-csv-button`),
    );

    expect(openSpy).toHaveBeenCalledWith(
      "/api/csv/rosterstudents?courseId=42",
      "_blank",
    );

    openSpy.mockRestore();
  });
});
