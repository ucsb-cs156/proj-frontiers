import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import IndividualAssignmentForm from "main/components/Assignments/IndividualAssignmentForm";
import { vi } from "vitest";

const mockSubmit = vi.fn();

beforeEach(() => {
  mockSubmit.mockClear();
});

test("No submit call on empty prefix", async () => {
  render(<IndividualAssignmentForm submitAction={mockSubmit} />);
  await screen.findByText("Create");
  fireEvent.click(screen.getByText("Create"));
  expect(mockSubmit).not.toHaveBeenCalled();
  await screen.findByText("Repository Prefix is required.");
  expect(
    screen.getByTestId("IndividualAssignmentForm-repoPrefix"),
  ).toBeInTheDocument();
  expect(
    screen.getByTestId("IndividualAssignmentForm-assignmentPrivacy"),
  ).toBeInTheDocument();
  expect(
    screen.getByTestId("IndividualAssignmentForm-permissions"),
  ).toHaveValue("MAINTAIN");
  expect(
    screen.getByTestId("IndividualAssignmentForm-creationTarget"),
  ).toHaveValue("STUDENTS_ONLY");
});

test("Submit call on successful data", async () => {
  render(<IndividualAssignmentForm submitAction={mockSubmit} />);

  await screen.findByText("Create");
  fireEvent.change(screen.getByLabelText("Repository Prefix"), {
    target: { value: "test" },
  });
  fireEvent.click(screen.getByTestId("IndividualAssignmentForm-submit"));
  await waitFor(() => expect(mockSubmit).toHaveBeenCalled());
  expect(
    screen.queryByText("Repository Prefix is required."),
  ).not.toBeInTheDocument();
});


test("Submit includes creationTarget with default value", async () => {
  render(<IndividualAssignmentForm submitAction={mockSubmit} />);

  await screen.findByText("Create");
  fireEvent.change(screen.getByLabelText("Repository Prefix"), {
    target: { value: "test-repo" },
  });
  fireEvent.click(screen.getByTestId("IndividualAssignmentForm-submit"));
  
  await waitFor(() => expect(mockSubmit).toHaveBeenCalled());
  const submittedData = mockSubmit.mock.calls[0][0];
  expect(submittedData.creationTarget).toBe("STUDENTS_ONLY");
});

test("Changing creationTarget to STAFF_ONLY works correctly", async () => {
  render(<IndividualAssignmentForm submitAction={mockSubmit} />);

  await screen.findByText("Create");
  
  //Verify default is STUDENTS_ONLY
  const creationTargetSelect = screen.getByTestId("IndividualAssignmentForm-creationTarget");
  expect(creationTargetSelect).toHaveValue("STUDENTS_ONLY");
  
  //Changing to STAFF_ONLY
  fireEvent.change(creationTargetSelect, {
    target: { value: "STAFF_ONLY" },
  });
  expect(creationTargetSelect).toHaveValue("STAFF_ONLY");
  
  //Filling in required info
  fireEvent.change(screen.getByLabelText("Repository Prefix"), {
    target: { value: "test-repo" },
  });
  fireEvent.click(screen.getByTestId("IndividualAssignmentForm-submit"));
  
  await waitFor(() => expect(mockSubmit).toHaveBeenCalled());
  const submittedData = mockSubmit.mock.calls[0][0];
  expect(submittedData.creationTarget).toBe("STAFF_ONLY");
});

test("Form includes creationTarget on submit even without explicit user interaction", async () => {
  render(<IndividualAssignmentForm submitAction={mockSubmit} />);

  await screen.findByText("Create");
  
  // Fill only the required field, don't touch creationTarget
  fireEvent.change(screen.getByLabelText("Repository Prefix"), {
    target: { value: "test-prefix" },
  });
  
  // Submit immediately without changing creationTarget
  fireEvent.click(screen.getByTestId("IndividualAssignmentForm-submit"));
  
  await waitFor(() => expect(mockSubmit).toHaveBeenCalled());
  const submittedData = mockSubmit.mock.calls[0][0];
  // The key assertion: even though we didn't interact with creationTarget,
  // it should be included in the submission with the default value
  expect(submittedData).toHaveProperty("creationTarget");
  expect(submittedData.creationTarget).toBe("STUDENTS_ONLY");
});

test("Changing creationTarget to STUDENTS_AND_STAFF works correctly", async () => {
  render(<IndividualAssignmentForm submitAction={mockSubmit} />);

  await screen.findByText("Create");
  
  // Change to STUDENTS_AND_STAFF
  const creationTargetSelect = screen.getByTestId("IndividualAssignmentForm-creationTarget");
  fireEvent.change(creationTargetSelect, {
    target: { value: "STUDENTS_AND_STAFF" },
  });
  expect(creationTargetSelect).toHaveValue("STUDENTS_AND_STAFF");
  
  // Fill in required field and submit
  fireEvent.change(screen.getByLabelText("Repository Prefix"), {
    target: { value: "test-repo" },
  });
  fireEvent.click(screen.getByTestId("IndividualAssignmentForm-submit"));
  
  await waitFor(() => expect(mockSubmit).toHaveBeenCalled());
  const submittedData = mockSubmit.mock.calls[0][0];
  expect(submittedData.creationTarget).toBe("STUDENTS_AND_STAFF");
});

test("Changing permissions to ADMIN works correctly", async () => {
  render(<IndividualAssignmentForm submitAction={mockSubmit} />);

  await screen.findByText("Create");
  
  //Verify default is MAINTAIN
  const permissionsSelect = screen.getByTestId("IndividualAssignmentForm-permissions");
  expect(permissionsSelect).toHaveValue("MAINTAIN");
  
  //Changing to ADMIN
  fireEvent.change(permissionsSelect, {
    target: { value: "ADMIN" },
  });
  expect(permissionsSelect).toHaveValue("ADMIN");
  
  //Filling in required info
  fireEvent.change(screen.getByLabelText("Repository Prefix"), {
    target: { value: "test-repo" },
  });
  fireEvent.click(screen.getByTestId("IndividualAssignmentForm-submit"));
  
  await waitFor(() => expect(mockSubmit).toHaveBeenCalled());
  const submittedData = mockSubmit.mock.calls[0][0];
  expect(submittedData.permissions).toBe("ADMIN");
});

test("Changing permissions to READ works correctly", async () => {
  render(<IndividualAssignmentForm submitAction={mockSubmit} />);

  await screen.findByText("Create");
  
  //Changing to READ
  const permissionsSelect = screen.getByTestId("IndividualAssignmentForm-permissions");
  fireEvent.change(permissionsSelect, {
    target: { value: "READ" },
  });
  expect(permissionsSelect).toHaveValue("READ");
  
  //Filling in required info
  fireEvent.change(screen.getByLabelText("Repository Prefix"), {
    target: { value: "test-repo" },
  });
  fireEvent.click(screen.getByTestId("IndividualAssignmentForm-submit"));
  
  await waitFor(() => expect(mockSubmit).toHaveBeenCalled());
  const submittedData = mockSubmit.mock.calls[0][0];
  expect(submittedData.permissions).toBe("READ");
});

test("Changing permissions to WRITE works correctly", async () => {
  render(<IndividualAssignmentForm submitAction={mockSubmit} />);

  await screen.findByText("Create");
  
  //Changing to WRITE
  const permissionsSelect = screen.getByTestId("IndividualAssignmentForm-permissions");
  fireEvent.change(permissionsSelect, {
    target: { value: "WRITE" },
  });
  expect(permissionsSelect).toHaveValue("WRITE");
  
  //Filling in required info
  fireEvent.change(screen.getByLabelText("Repository Prefix"), {
    target: { value: "test-repo" },
  });
  fireEvent.click(screen.getByTestId("IndividualAssignmentForm-submit"));
  
  await waitFor(() => expect(mockSubmit).toHaveBeenCalled());
  const submittedData = mockSubmit.mock.calls[0][0];
  expect(submittedData.permissions).toBe("WRITE");
});

test("Checking assignmentPrivacy checkbox works correctly", async () => {
  render(<IndividualAssignmentForm submitAction={mockSubmit} />);

  await screen.findByText("Create");
  
  const privacyCheckbox = screen.getByTestId("IndividualAssignmentForm-assignmentPrivacy");
  expect(privacyCheckbox).not.toBeChecked();
  
  fireEvent.click(privacyCheckbox);
  expect(privacyCheckbox).toBeChecked();
  
  fireEvent.change(screen.getByLabelText("Repository Prefix"), {
    target: { value: "test-repo" },
  });
  fireEvent.click(screen.getByTestId("IndividualAssignmentForm-submit"));
  
  await waitFor(() => expect(mockSubmit).toHaveBeenCalled());
  const submittedData = mockSubmit.mock.calls[0][0];
  expect(submittedData.assignmentPrivacy).toBe(true);
});

test("Form has all expected labels", async () => {
  render(<IndividualAssignmentForm submitAction={mockSubmit} />);

  await screen.findByText("Create");
  
  expect(screen.getByText("Repository Prefix")).toBeInTheDocument();
  expect(screen.getByText("Private Repositories?")).toBeInTheDocument();
  expect(screen.getByText("Student Permissions")).toBeInTheDocument();
  expect(screen.getByText("Repository Creation Target")).toBeInTheDocument();
});

test("creationTarget dropdown has the correct default option", async () => {
  render(<IndividualAssignmentForm submitAction={mockSubmit} />);

  await screen.findByText("Create");
  
  const creationTargetSelect = screen.getByTestId("IndividualAssignmentForm-creationTarget");
  
  //The selected option's text should be "Students Only" which corresponds to value "STUDENTS_ONLY"
  const selectedOption = creationTargetSelect.querySelector('option[selected]');
  expect(selectedOption).toHaveValue("STUDENTS_ONLY");
  expect(selectedOption).toHaveTextContent("Students Only");
  expect(creationTargetSelect).toHaveValue("STUDENTS_ONLY");
  expect(creationTargetSelect.value).not.toBe("");
});
