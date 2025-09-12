import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { BrowserRouter as Router } from "react-router";

import CourseStaffForm from "main/components/CourseStaff/CourseStaffForm";
import { courseStaffFixtures } from "fixtures/courseStaffFixtures";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { vi } from "vitest";

const mockedNavigate = vi.fn();
vi.mock("react-router", async (importOriginal) => ({
  ...(await importOriginal()),
  useNavigate: () => mockedNavigate,
}));

describe("CourseStaffForm tests", () => {
  const queryClient = new QueryClient();

  const expectedHeaders = ["First Name", "Last Name", "Email"];
  const testId = "CourseStaffForm";

  test("renders correctly with no initialContents", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <CourseStaffForm />
        </Router>
      </QueryClientProvider>,
    );

    expect(await screen.findByText(/Create/)).toBeInTheDocument();

    expectedHeaders.forEach((headerText) => {
      const header = screen.getByText(headerText);
      expect(header).toBeInTheDocument();
    });
    expect(await screen.findByTestId(`${testId}-email`)).toBeInTheDocument();
    expect(screen.getByText(`Email`)).toBeInTheDocument();
  });

  test("renders correctly when passing in initialContents", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <CourseStaffForm initialContents={courseStaffFixtures.oneStaff} />
        </Router>
      </QueryClientProvider>,
    );

    expect(await screen.findByText(/Create/)).toBeInTheDocument();

    expectedHeaders.forEach((headerText) => {
      const header = screen.getByText(headerText);
      expect(header).toBeInTheDocument();
    });

    expect(
      await screen.findByTestId(`${testId}-firstName`),
    ).toBeInTheDocument();
    expect(screen.getByText(`First Name`)).toBeInTheDocument();
    expect(await screen.findByTestId(`${testId}-lastName`)).toBeInTheDocument();
    expect(screen.getByText(`Last Name`)).toBeInTheDocument();
    expect(await screen.findByTestId(`${testId}-email`)).toBeInTheDocument();
    expect(screen.getByText(`Email`)).toBeInTheDocument();
    expect(await screen.findByTestId(`${testId}-submit`)).toBeInTheDocument();
  });

  test("that navigate(-1) is called when Cancel is clicked", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <CourseStaffForm />
        </Router>
      </QueryClientProvider>,
    );
    expect(await screen.findByTestId(`${testId}-cancel`)).toBeInTheDocument();
    const cancelButton = screen.getByTestId(`${testId}-cancel`);

    fireEvent.click(cancelButton);

    await waitFor(() => expect(mockedNavigate).toHaveBeenCalledWith(-1));
  });

  test("that the correct validations are performed", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <CourseStaffForm />
        </Router>
      </QueryClientProvider>,
    );

    expect(await screen.findByText(/Create/)).toBeInTheDocument();
    const submitButton = screen.getByText(/Create/);
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(screen.getByText(/First Name is required./)).toBeInTheDocument();
    });
    expect(screen.getByText(/Last Name is required./)).toBeInTheDocument();
    expect(screen.getByText(/Email is required/)).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("Email"), {
      target: {
        value: "invalidemail",
      },
    });

    fireEvent.click(submitButton);
    await screen.findByText(/Please enter a valid email/);
  });
});
