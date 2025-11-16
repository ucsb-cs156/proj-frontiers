import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { BrowserRouter as Router } from "react-router";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { vi } from "vitest";
import TeamMemberForm from "main/components/Teams/TeamMemberForm";
import { rosterStudentFixtures } from "fixtures/rosterStudentFixtures";

const mockedNavigate = vi.fn();
vi.mock("react-router", async (importOriginal) => ({
  ...(await importOriginal()),
  useNavigate: () => mockedNavigate,
}));

describe("TeamMemberForm tests", () => {
  const queryClient = new QueryClient();

  const expectedHeaders = ["Select Student"];
  const testId = "TeamMemberForm";
  test("renders correctly when passing in initialContents", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <TeamMemberForm
            initialContents={{
              rosterStudentId:
                rosterStudentFixtures.studentsWithEachStatus[0].id,
            }}
            rosterStudents={rosterStudentFixtures.studentsWithEachStatus}
          />
        </Router>
      </QueryClientProvider>,
    );

    expect(await screen.findByText(/Select Student/)).toBeInTheDocument();

    expectedHeaders.forEach((headerText) => {
      const header = screen.getByText(headerText);
      expect(header).toBeInTheDocument();
    });

    expect(
      await screen.findByTestId("RosterStudentDropdown"),
    ).toBeInTheDocument();
    expect(await screen.findByTestId(`${testId}-submit`)).toBeInTheDocument();
  });

  test("that navigate(-1) is called when Cancel is clicked", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <TeamMemberForm
            rosterStudents={rosterStudentFixtures.studentsWithEachStatus}
          />
        </Router>
      </QueryClientProvider>,
    );
    expect(await screen.findByTestId(`${testId}-cancel`)).toBeInTheDocument();
    const cancelButton = screen.getByTestId(`${testId}-cancel`);

    fireEvent.click(cancelButton);

    await waitFor(() => expect(mockedNavigate).toHaveBeenCalledWith(-1));
  });

  test("displays error message when no student is selected", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <TeamMemberForm
            rosterStudents={rosterStudentFixtures.studentsWithEachStatus}
          />
        </Router>
      </QueryClientProvider>,
    );

    expect(await screen.findByText(/Select Student/)).toBeInTheDocument();
    const submitButton = screen.getByText(/Add Member/);
    fireEvent.click(submitButton);

    await screen.findByText(/Select a student./);

    await waitFor(() =>
      expect(screen.getByPlaceholderText("Select a student.")).toBeInTheDocument(),
    );
  });
});
