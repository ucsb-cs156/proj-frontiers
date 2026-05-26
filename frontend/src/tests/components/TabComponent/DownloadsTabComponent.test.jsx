import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import DownloadsTabComponent from "main/components/TabComponent/DownloadsTabComponent";
import { expect, test, vi } from "vitest";

test("DownloadsTabComponent renders and downloads student CSV", async () => {
  const download = vi.fn();
  window.open = (a, b) => download(a, b);

  render(<DownloadsTabComponent courseId={7} testIdPrefix="test" />);

  expect(screen.getByTestId("test-DownloadsTabComponent")).toBeInTheDocument();

  fireEvent.click(screen.getByText("Download Student CSV"));

  await waitFor(() => expect(download).toBeCalled());
  expect(download).toBeCalledWith(
    "/api/csv/rosterstudents?courseId=7",
    "_blank",
  );
});
