import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { BrowserRouter as Router } from "react-router-dom";

import RosterStudentForm from "main/components/RosterStudent/RosterStudentForm";
import { rosterStudentFixtures } from "fixtures/rosterStudentFixtures";

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

const mockedNavigate = jest.fn();

jest.mock("react-router-dom", () => ({
  ...jest.requireActual("react-router-dom"),
  useNavigate: () => mockedNavigate,
}));

describe("RosterStudentForm tests", () => {
  const queryClient = new QueryClient();

  const expectedHeaders = ["Student Id", "First Name", "Last Name", "Email"];
  const testId = "RosterStudentForm";

  test("renders correctly with no initialContents", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <RosterStudentForm />
        </Router>
      </QueryClientProvider>,
    );

    expect(await screen.findByText(/Create/)).toBeInTheDocument();

    expectedHeaders.forEach((headerText) => {
      const header = screen.getByText(headerText);
      expect(header).toBeInTheDocument();
    });
  });

  test("renders correctly when passing in initialContents", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <RosterStudentForm
            initialContents={rosterStudentFixtures.oneStudent}
          />
        </Router>
      </QueryClientProvider>,
    );

    expect(await screen.findByText(/Create/)).toBeInTheDocument();

    expectedHeaders.forEach((headerText) => {
      const header = screen.getByText(headerText);
      expect(header).toBeInTheDocument();
    });

    expect(
      await screen.findByTestId(`${testId}-studentId`),
    ).toBeInTheDocument();
    expect(screen.getByText(`Student Id`)).toBeInTheDocument();
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
          <RosterStudentForm />
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
          <RosterStudentForm />
        </Router>
      </QueryClientProvider>,
    );

    expect(await screen.findByText(/Create/)).toBeInTheDocument();
    const submitButton = screen.getByText(/Create/);
    fireEvent.click(submitButton);

    await screen.findByText(/Student Id is required/);
    expect(screen.getByText(/First Name is required/)).toBeInTheDocument();
    expect(screen.getByText(/Last Name is required/)).toBeInTheDocument();
    expect(screen.getByText(/Email is required/)).toBeInTheDocument();
  });
});
