import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { BrowserRouter as Router } from "react-router-dom";

import RoleEmailForm from "main/components/Users/RoleEmailForm";
import { _roleEmailFixtures } from "fixtures/roleEmailFixtures";

import { QueryClient, QueryClientProvider } from "react-query";

const mockedNavigate = jest.fn();

jest.mock("react-router-dom", () => ({
  ...jest.requireActual("react-router-dom"),
  useNavigate: () => mockedNavigate,
}));

describe("RoleEmailForm tests", () => {
  const queryClient = new QueryClient();

  const expectedHeaders = ["Email"];
  const testId = "RoleEmailForm";

  test("renders correctly with no initialContents", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <RoleEmailForm />
        </Router>
      </QueryClientProvider>,
    );

    expect(await screen.findByText(/Add Email/)).toBeInTheDocument();

    expectedHeaders.forEach((headerText) => {
      const header = screen.getByText(headerText);
      expect(header).toBeInTheDocument();
    });
  });

  test("that navigate(-1) is called when Cancel is clicked", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <RoleEmailForm />
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
          <RoleEmailForm />
        </Router>
      </QueryClientProvider>,
    );

    expect(await screen.findByText(/Add Email/)).toBeInTheDocument();
    const submitButton = screen.getByText(/Add Email/);
    fireEvent.click(submitButton);

    await screen.findByText(/Email is required./);
    expect(screen.getByText(/Email is required/)).toBeInTheDocument();

    const testInput = screen.getByTestId(`${testId}-email`);
    fireEvent.change(testInput, { target: { value: "notanemail" } });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(
        screen.getByText(/Please enter a valid email address./),
      ).toBeInTheDocument();
    });
  });

  test("shows error on invalid email with partial valid email", async () => {
    const mockSubmit = jest.fn();

    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <RoleEmailForm submitAction={mockSubmit} />
        </Router>
      </QueryClientProvider>,
    );

    const testInput = screen.getByTestId(`${testId}-email`);
    const submitButton = screen.getByRole("button", { name: /Add Email/i });

    fireEvent.change(testInput, {
      target: { value: "valid@email.com invalid" },
    });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(
        screen.getByText(/Please enter a valid email address./i),
      ).toBeInTheDocument();
    });

    expect(mockSubmit).not.toHaveBeenCalled();
  });

  test("shows error on invalid email", async () => {
    const mockSubmit = jest.fn();

    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <RoleEmailForm submitAction={mockSubmit} />
        </Router>
      </QueryClientProvider>,
    );

    const testInput = screen.getByTestId(`${testId}-email`);
    const submitButton = screen.getByRole("button", { name: /Add Email/i });

    fireEvent.change(testInput, {
      target: { value: "invalid email@ucsb.edu" },
    });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(
        screen.getByText(/Please enter a valid email address./i),
      ).toBeInTheDocument();
    });

    expect(mockSubmit).not.toHaveBeenCalled();
  });

  test("does not show error on valid email and submits", async () => {
    const mockSubmit = jest.fn();

    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <RoleEmailForm submitAction={mockSubmit} />
        </Router>
      </QueryClientProvider>,
    );

    const input = screen.getByLabelText(/email/i);
    const submitButton = screen.getByRole("button", { name: /add email/i });

    fireEvent.change(input, { target: { value: "user@ucsb.com" } });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(mockSubmit).toHaveBeenCalled();
    });
  });
});
