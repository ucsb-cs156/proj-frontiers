import { render, fireEvent, screen, waitFor } from "@testing-library/react";
import RoleEmailForm from "main/components/Users/RoleEmailForm";
import usersFixtures from "fixtures/usersFixtures";
import { BrowserRouter as Router } from "react-router-dom";

const mockedNavigate = jest.fn();
jest.mock("react-router-dom", () => ({
  ...jest.requireActual("react-router-dom"),
  useNavigate: () => mockedNavigate,
}));

describe("RoleEmailForm tests", () => {
  test("renders blank create‐mode form", () => {
    render(
      <Router>
        <RoleEmailForm />
      </Router>,
    );

    // should render just one label/input for Email
    const emailInput = screen.getByLabelText(/Email/);
    expect(emailInput).toHaveValue("");
    expect(emailInput).toHaveAttribute("placeholder", "Enter email");

    expect(screen.getByTestId("RoleEmailForm-submit")).toBeInTheDocument();
    expect(screen.getByTestId("RoleEmailForm-cancel")).toBeInTheDocument();
    expect(screen.getByTestId("RoleEmailForm-submit")).toHaveTextContent(
      "Create",
    );
  });

  test("renders update‐mode form with initial email", () => {
    const fixture = usersFixtures.threeUsers[0];
    render(
      <Router>
        <RoleEmailForm initialContents={fixture} />
      </Router>,
    );

    const emailInput = screen.getByLabelText(/Email/);
    expect(emailInput).toHaveValue(fixture.email);
  });

  test("calls submitAction with the email object on submit", async () => {
    const mockSubmit = jest.fn();
    render(
      <Router>
        <RoleEmailForm submitAction={mockSubmit} />
      </Router>,
    );

    const emailInput = screen.getByLabelText(/Email/);
    const submitButton = screen.getByTestId("RoleEmailForm-submit");

    fireEvent.change(emailInput, { target: { value: "test@ucsb.edu" } });
    fireEvent.click(submitButton);

    await waitFor(() =>
      expect(mockSubmit).toHaveBeenCalledWith({ email: "test@ucsb.edu" }),
    );
  });

  test("shows validation error for invalid email", async () => {
    render(
      <Router>
        <RoleEmailForm />
      </Router>,
    );

    const emailInput = screen.getByLabelText(/Email/);
    const submitButton = screen.getByTestId("RoleEmailForm-submit");

    fireEvent.change(emailInput, { target: { value: "not-an-email" } });
    fireEvent.click(submitButton);

    await screen.findByText(/Invalid email address./);
  });

  test("calls navigate(-1) when Cancel is clicked", () => {
    render(
      <Router>
        <RoleEmailForm />
      </Router>,
    );

    const cancelButton = screen.getByTestId("RoleEmailForm-cancel");
    fireEvent.click(cancelButton);

    expect(mockedNavigate).toHaveBeenCalledWith(-1);
  });

  test("shows required‐field message on empty submit", async () => {
    const mockSubmit = jest.fn();
    render(
      <Router>
        <RoleEmailForm submitAction={mockSubmit} />
      </Router>,
    );

    const submitButton = screen.getByTestId("RoleEmailForm-submit");
    fireEvent.click(submitButton);

    await screen.findByText(/Email is required./);
    expect(mockSubmit).not.toHaveBeenCalled();
  });
});
