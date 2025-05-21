import { render, waitFor, fireEvent, screen } from "@testing-library/react";
import RosterStudentsForm from "main/components/RosterStudents/RosterStudentsForm";
import rosterStudentsFixtures from "fixtures/rosterStudentsFixtures";
import { BrowserRouter as Router } from "react-router-dom";

const mockedNavigate = jest.fn();

jest.mock("react-router-dom", () => ({
  ...jest.requireActual("react-router-dom"),
  useNavigate: () => mockedNavigate,
}));

describe("RosterStudentsForm tests", () => {
  test("renders correctly", async () => {
    render(
      <Router>
        <RosterStudentsForm />
      </Router>,
    );
    await screen.findByText(/Student ID/);
    await screen.findByText(/First Name/);
    await screen.findByText(/Last Name/);
    await screen.findByText(/Email/);
  });

  test("renders correctly when passing in a RosterStudent", async () => {
    render(
      <Router>
        <RosterStudentsForm
          initialContents={rosterStudentsFixtures.threeRosterStudents[0]}
        />
      </Router>,
    );
    await screen.findByTestId(/RosterStudentsForm-studentId/);
    expect(screen.getByText(/Student ID/)).toBeInTheDocument();
    expect(screen.getByTestId(/RosterStudentsForm-studentId/)).toHaveValue(
      "9627X84",
    );
  });

  test("Correct Error messsages on bad input", async () => {
    render(
      <Router>
        <RosterStudentsForm />
      </Router>,
    );
    await screen.findByTestId("RosterStudentsForm-firstName");
    const firstNameField = screen.getByTestId("RosterStudentsForm-firstName");
    const lastNameField = screen.getByTestId("RosterStudentsForm-lastName");
    const submitButton = screen.getByTestId("RosterStudentsForm-submit");

    fireEvent.change(firstNameField, { target: { value: "bad-input" } });
    fireEvent.change(lastNameField, { target: { value: "bad-input" } });
    fireEvent.click(submitButton);

    await screen.findByText(/Email is required./);
  });

  test("Correct Error messsages on missing input", async () => {
    render(
      <Router>
        <RosterStudentsForm />
      </Router>,
    );
    await screen.findByTestId("RosterStudentsForm-submit");
    const submitButton = screen.getByTestId("RosterStudentsForm-submit");

    fireEvent.click(submitButton);

    await screen.findByText(/Student ID is required./);
    await screen.findByText(/First Name is required./);
    await screen.findByText(/Last Name is required./);
    await screen.findByText(/Email is required./);
  });

  test("No Error messages on good input", async () => {
    const mockSubmitAction = jest.fn();

    render(
      <Router>
        <RosterStudentsForm submitAction={mockSubmitAction} />
      </Router>,
    );
    await screen.findByTestId("RosterStudentsForm-studentId");

    const studentIdField = screen.getByTestId("RosterStudentsForm-studentId");
    const firstNameField = screen.getByTestId("RosterStudentsForm-firstName");
    const lastNameField = screen.getByTestId("RosterStudentsForm-lastName");
    const emailField = screen.getByTestId("RosterStudentsForm-email");
    const submitButton = screen.getByTestId("RosterStudentsForm-submit");

    fireEvent.change(studentIdField, {
      target: { value: "222222" },
    });
    fireEvent.change(firstNameField, {
      target: { value: "Chris" },
    });
    fireEvent.change(lastNameField, {
      target: { value: "Gaucho" },
    });
    fireEvent.change(emailField, {
      target: { value: "cgaucho@ucsb.edu" },
    });
    fireEvent.click(submitButton);

    await waitFor(() => expect(mockSubmitAction).toHaveBeenCalled());

    expect(
      screen.queryByText(/First Name is required./),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByText(/Student ID is required./),
    ).not.toBeInTheDocument();
    expect(screen.getByText("Create")).toBeInTheDocument();
  });

  test("that navigate(-1) is called when Cancel is clicked", async () => {
    render(
      <Router>
        <RosterStudentsForm />
      </Router>,
    );
    await screen.findByTestId("RosterStudentsForm-cancel");
    const cancelButton = screen.getByTestId("RosterStudentsForm-cancel");

    fireEvent.click(cancelButton);

    await waitFor(() => expect(mockedNavigate).toHaveBeenCalledWith(-1));
  });
});
