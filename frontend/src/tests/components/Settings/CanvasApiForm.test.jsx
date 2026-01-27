import { fireEvent, render, screen } from "@testing-library/react";
import { BrowserRouter as Router } from "react-router";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { vi } from "vitest";
import CanvasApiForm from "main/components/Settings/CanvasApiForm";

const mockedNavigate = vi.fn();
vi.mock("react-router", async (importOriginal) => ({
  ...(await importOriginal()),
  useNavigate: () => mockedNavigate,
}));

describe("CanvasApiForm tests", () => {
  const queryClient = new QueryClient();

  const expectedHeaders = ["Canvas Course ID", "Canvas API Token"];
  const testId = "CanvasApiForm";

  test("renders correctly", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <CanvasApiForm />
        </Router>
      </QueryClientProvider>,
    );

    expect(await screen.findByText(/Connect Canvas/)).toBeInTheDocument();

    expectedHeaders.forEach((headerText) => {
      const header = screen.getByText(headerText);
      expect(header).toBeInTheDocument();
    });

    expect(
      await screen.findByTestId(`${testId}-canvasCourseId`),
    ).toBeInTheDocument();
    expect(screen.getByText(`Canvas Course ID`)).toBeInTheDocument();
    expect(
      await screen.findByTestId(`${testId}-canvasApiToken`),
    ).toBeInTheDocument();
    expect(await screen.findByTestId(`${testId}-submit`)).toBeInTheDocument();
  });

  test("Input is required for each field.", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <CanvasApiForm />
        </Router>
      </QueryClientProvider>,
    );

    expect(await screen.findByText(/Connect Canvas/)).toBeInTheDocument();

    fireEvent.click(screen.getByTestId(`${testId}-submit`));

    expect(
      await screen.findByText("Canvas Course ID is required."),
    ).toBeInTheDocument();
    expect(
      await screen.findByText("Canvas API Token is required."),
    ).toBeInTheDocument();
  });
});
