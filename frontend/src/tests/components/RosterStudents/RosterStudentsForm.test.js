import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { BrowserRouter as Router } from "react-router-dom";

import RosterStudentsForm from "main/components/RosterStudents/RosterStudentsForm";
import rosterStudentsFixtures from "fixtures/rosterStudentsFixtures";

import { QueryClient, QueryClientProvider } from "react-query";

const mockedNavigate = jest.fn();

jest.mock("react-router-dom", () => ({
  ...jest.requireActual("react-router-dom"),
  useNavigate: () => mockedNavigate,
}));

describe("RosterStudentsForm tests", () => {
  const queryClient = new QueryClient();

  const expectedHeaders = ["Student Id", "First Name", "Last Name", "Email"];
  const testId = "RosterStudentsForm";

  test("renders correctly with no initialContents", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <RosterStudentsForm />
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
          <RosterStudentsForm
            initialContents={rosterStudentsFixtures.threeRosterStudents[0]}
          />
        </Router>
      </QueryClientProvider>,
    );

    expect(await screen.findByText(/Create/)).toBeInTheDocument();

    expectedHeaders.forEach((headerText) => {
      const header = screen.getByText(headerText);
      expect(header).toBeInTheDocument();
    });

    expect(await screen.findByTestId(`${testId}-id`)).toBeInTheDocument();
    expect(screen.getByText(`Id`)).toBeInTheDocument();
  });

  test("that navigate(-1) is called when Cancel is clicked", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <RosterStudentsForm />
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
          <RosterStudentsForm />
        </Router>
      </QueryClientProvider>,
    );

    expect(await screen.findByText(/Create/)).toBeInTheDocument();
    const submitButton = screen.getByTestId(`${testId}-submit`);
    fireEvent.click(submitButton);

    await screen.findByTestId(`${testId}-studentId`);
    await screen.findByText(/Student Id is required./);
    expect(screen.getByTestId(`${testId}-firstName`)).toBeInTheDocument();
    expect(screen.getByText(/First Name is required/)).toBeInTheDocument();
    expect(screen.getByTestId(`${testId}-lastName`)).toBeInTheDocument();
    expect(screen.getByText(/Last Name is required/)).toBeInTheDocument();
    expect(screen.getByTestId(`${testId}-email`)).toBeInTheDocument();
    expect(screen.getByText(/Email is required/)).toBeInTheDocument();

    const studentIdInput = screen.getByTestId(`${testId}-studentId`);
    fireEvent.change(studentIdInput, { target: { value: "a".repeat(256) } });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(screen.getByText(/Max length 255 characters/)).toBeInTheDocument();
    });

    const emailInput = screen.getByTestId(`${testId}-email`);
    fireEvent.change(emailInput, { target: { value: "invalid email" } });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(screen.getByText(/Email is not valid/)).toBeInTheDocument();
    });
  });
});
