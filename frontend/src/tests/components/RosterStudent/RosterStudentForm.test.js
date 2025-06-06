import { render, waitFor, fireEvent, screen } from "@testing-library/react";
import RosterStudentForm from "main/components/RosterStudent/RosterStudentForm";
import { rosterStudentFixtures } from "fixtures/rosterStudentFixtures";
import { BrowserRouter as Router } from "react-router-dom";

const mockedNavigate = jest.fn();

jest.mock("react-router-dom", () => ({
  ...jest.requireActual("react-router-dom"),
  useNavigate: () => mockedNavigate,
}));

describe("RosterStudentForm tests", () => {
  test("renders correctly", async () => {
    render(
      <Router>
        <RosterStudentForm />
      </Router>,
    );

    await screen.findByText(/Student ID/);
    await screen.findByText(/First Name/);
    await screen.findByText(/Last Name/);
    await screen.findByText(/Email/);
    await screen.findByText(/Create/);
  });

  test("renders correctly when passing in initialContents", async () => {
    render(
      <Router>
        <RosterStudentForm
          initialContents={rosterStudentFixtures.oneRosterStudent[0]}
        />
      </Router>,
    );

    await screen.findByTestId(/RosterStudentForm-id/);
    expect(screen.getByTestId(/RosterStudentForm-id/)).toHaveValue("1");
    expect(screen.getByTestId(/RosterStudentForm-studentId/)).toHaveValue(
      "123456789",
    );
    expect(screen.getByTestId(/RosterStudentForm-firstName/)).toHaveValue(
      "John",
    );
    expect(screen.getByTestId(/RosterStudentForm-lastName/)).toHaveValue("Doe");
    expect(screen.getByTestId(/RosterStudentForm-email/)).toHaveValue(
      "johndoe@example.com",
    );
  });

  test("shows validation errors for invalid inputs", async () => {
    render(
      <Router>
        <RosterStudentForm />
      </Router>,
    );
    await screen.findByTestId("RosterStudentForm-submit");
    const submitButton = screen.getByTestId("RosterStudentForm-submit");

    fireEvent.click(submitButton);

    await screen.findByText(/Student ID is required/);
    expect(screen.getByText(/First name is required/)).toBeInTheDocument();
    expect(screen.getByText(/Last name is required/)).toBeInTheDocument();
    expect(screen.getByText(/Email is required/)).toBeInTheDocument();

    const emailField = screen.getByTestId("RosterStudentForm-email");
    fireEvent.change(emailField, { target: { value: "invalidEmail" } });
    fireEvent.click(submitButton);

    await screen.findByText(/Enter a valid email address/);
  });

  test("validates required fields", async () => {
    render(
      <Router>
        <RosterStudentForm />
      </Router>,
    );

    await screen.findByTestId("RosterStudentForm-submit");
    const submitButton = screen.getByTestId("RosterStudentForm-submit");

    fireEvent.click(submitButton);

    await screen.findByText(/Student ID is required/);
    expect(screen.getByText(/First name is required/)).toBeInTheDocument();
    expect(screen.getByText(/Last name is required/)).toBeInTheDocument();
    expect(screen.getByText(/Email is required/)).toBeInTheDocument();
  });

  test("No Error messages on good input", async () => {
    const mockSubmitAction = jest.fn();

    render(
      <Router>
        <RosterStudentForm submitAction={mockSubmitAction} />
      </Router>,
    );

    await screen.findByTestId("RosterStudentForm-studentId");
    const studentIdField = screen.getByTestId("RosterStudentForm-studentId");
    const firstNameField = screen.getByTestId("RosterStudentForm-firstName");
    const lastNameField = screen.getByTestId("RosterStudentForm-lastName");
    const emailField = screen.getByTestId("RosterStudentForm-email");
    const submitButton = screen.getByTestId("RosterStudentForm-submit");

    fireEvent.change(studentIdField, { target: { value: "12345678" } });
    fireEvent.change(firstNameField, { target: { value: "Jane" } });
    fireEvent.change(lastNameField, { target: { value: "Smith" } });
    fireEvent.change(emailField, {
      target: { value: "janesmith@example.com" },
    });

    fireEvent.click(submitButton);

    await waitFor(() => expect(mockSubmitAction).toHaveBeenCalled());

    expect(
      screen.queryByText(/Student ID is required/),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByText(/First name is required/),
    ).not.toBeInTheDocument();
    expect(screen.queryByText(/Last name is required/)).not.toBeInTheDocument();
    expect(screen.queryByText(/Email is required/)).not.toBeInTheDocument();
    expect(
      screen.queryByText(/Enter a valid email address/),
    ).not.toBeInTheDocument();
  });

  test("cancel button navigates to previous page", async () => {
    render(
      <Router>
        <RosterStudentForm />
      </Router>,
    );

    await screen.findByTestId("RosterStudentForm-cancel");
    const cancelButton = screen.getByTestId("RosterStudentForm-cancel");

    fireEvent.click(cancelButton);

    await waitFor(() => expect(mockedNavigate).toHaveBeenCalledWith(-1));
  });
});
