import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { BrowserRouter as Router } from "react-router";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { test, vi } from "vitest";
import CanvasApiForm from "main/components/Settings/CanvasApiForm";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";

const mockedNavigate = vi.fn();
vi.mock("react-router", async (importOriginal) => ({
  ...(await importOriginal()),
  useNavigate: () => mockedNavigate,
}));

const axiosMock = new AxiosMockAdapter(axios);
const queryClient = new QueryClient();

describe("CanvasApiForm tests", () => {
  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    queryClient.clear();
  });

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

  test("At least one field is required.", async () => {
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
      await screen.findByText("Please fill in at least one field."),
    ).toBeInTheDocument();
  });

  test("Submitting form with data works", async () => {
    const mockSubmitAction = vi.fn();
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <CanvasApiForm submitAction={mockSubmitAction} />
        </Router>
      </QueryClientProvider>,
    );

    expect(await screen.findByText(/Connect Canvas/)).toBeInTheDocument();

    fireEvent.change(screen.getByTestId(`${testId}-canvasCourseId`), {
      target: { value: "1234567" },
    });

    fireEvent.change(screen.getByTestId(`${testId}-canvasApiToken`), {
      target: { value: "ahfnSa2ed19rhd13ds1lU" },
    });

    fireEvent.click(screen.getByTestId(`${testId}-submit`));

    await waitFor(() => {
      expect(mockSubmitAction).toHaveBeenCalledWith({
        canvasCourseId: "1234567",
        canvasApiToken: "ahfnSa2ed19rhd13ds1lU",
      });
    });
  });
  test("Submitting form with only canvasCourseId filled calls submitAction with correct data", async () => {
    const mockSubmitAction = vi.fn();
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <CanvasApiForm submitAction={mockSubmitAction} />
        </Router>
      </QueryClientProvider>,
    );

    expect(await screen.findByText(/Connect Canvas/)).toBeInTheDocument();

    fireEvent.change(screen.getByTestId(`${testId}-canvasCourseId`), {
      target: { value: "1234567" },
    });

    fireEvent.click(screen.getByTestId(`${testId}-submit`));

    await waitFor(() => {
      expect(mockSubmitAction).toHaveBeenCalledWith({
        canvasCourseId: "1234567",
        canvasApiToken: "",
      });
    });
  });
  test("Placeholder shows correct text when no token is set", async () => {
    const mockSubmitAction = vi.fn();
    axiosMock.onGet(/\/api\/courses\/getCanvasInfo/).reply(200, {
      courseId: "",
      canvasApiToken: "",
      canvasCourseId: "",
    });

    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <CanvasApiForm submitAction={mockSubmitAction} />
        </Router>
      </QueryClientProvider>,
    );

    expect(await screen.findByText(/Connect Canvas/)).toBeInTheDocument();

    const tokenInput = screen.getByTestId(`${testId}-canvasApiToken`);
    expect(tokenInput).toHaveAttribute("placeholder", "Token not set yet.");

    const courseIdInput = screen.getByTestId(`${testId}-canvasCourseId`);
    expect(courseIdInput).toHaveAttribute(
      "placeholder",
      "Course ID not set yet.",
    );
  });

  test("Placeholder shows correct text when a token is set", async () => {
    const mockSubmitAction = vi.fn();
    axiosMock.onGet(/\/api\/courses\/getCanvasInfo/).reply(200, {
      courseId: "1",
      canvasApiToken: "***************d1U",
      canvasCourseId: "1234567",
    });

    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <CanvasApiForm submitAction={mockSubmitAction} courseId={1} />
        </Router>
      </QueryClientProvider>,
    );

    await waitFor(() => {
      expect(screen.getByTestId(`${testId}-canvasApiToken`)).toHaveAttribute(
        "placeholder",
        "Current Token: ***************d1U",
      );
    });

    expect(screen.getByTestId(`${testId}-canvasCourseId`)).toHaveAttribute(
      "placeholder",
      "Current Course ID: 1234567",
    );
  });
  test("Submitting spaces only in fields shows error message", async () => {
    const mockSubmitAction = vi.fn();
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <CanvasApiForm submitAction={mockSubmitAction} />
        </Router>
      </QueryClientProvider>,
    );

    expect(await screen.findByText(/Connect Canvas/)).toBeInTheDocument();

    fireEvent.change(screen.getByTestId(`${testId}-canvasCourseId`), {
      target: { value: "   " },
    });

    fireEvent.change(screen.getByTestId(`${testId}-canvasApiToken`), {
      target: { value: "   " },
    });

    fireEvent.click(screen.getByTestId(`${testId}-submit`));

    expect(
      await screen.findByText("Please fill in at least one field."),
    ).toBeInTheDocument();
  });
  test("Submitting form with only canvasApiToken filled calls submitAction", async () => {
    const mockSubmitAction = vi.fn();
    axiosMock.onGet(/\/api\/courses\/getCanvasInfo/).reply(200, {
      courseId: "",
      canvasApiToken: "",
      canvasCourseId: "",
    });

    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <CanvasApiForm submitAction={mockSubmitAction} courseId={1} />
        </Router>
      </QueryClientProvider>,
    );

    fireEvent.change(screen.getByTestId(`${testId}-canvasApiToken`), {
      target: { value: "someToken123" },
    });

    fireEvent.click(screen.getByTestId(`${testId}-submit`));

    await waitFor(() => {
      expect(mockSubmitAction).toHaveBeenCalledWith({
        canvasApiToken: "someToken123",
        canvasCourseId: "",
      });
    });
  });
  test("Root error clears when valid data is submitted after empty submission", async () => {
    const mockSubmitAction = vi.fn();
    axiosMock.onGet(/\/api\/courses\/getCanvasInfo/).reply(200, {
      courseId: "",
      canvasApiToken: "",
      canvasCourseId: "",
    });

    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <CanvasApiForm submitAction={mockSubmitAction} courseId={1} />
        </Router>
      </QueryClientProvider>,
    );

    // Submit empty form to trigger root error
    fireEvent.click(screen.getByTestId(`${testId}-submit`));

    await waitFor(() => {
      expect(
        screen.getByText("Please fill in at least one field."),
      ).toBeInTheDocument();
    });

    // Now fill in a field and submit again
    fireEvent.change(screen.getByTestId(`${testId}-canvasApiToken`), {
      target: { value: "someToken123" },
    });

    fireEvent.click(screen.getByTestId(`${testId}-submit`));

    await waitFor(() => {
      expect(
        screen.queryByText("Please fill in at least one field."),
      ).not.toBeInTheDocument();
    });

    expect(mockSubmitAction).toHaveBeenCalled();
  });
});
