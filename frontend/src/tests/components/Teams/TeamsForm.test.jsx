import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { BrowserRouter as Router } from "react-router";

import TeamsForm from "main/components/Teams/TeamsForm";
import { teamsFixtures } from "fixtures/TeamsFixtures";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { vi } from "vitest";

const mockedNavigate = vi.fn();
vi.mock("react-router", async (importOriginal) => ({
  ...(await importOriginal()),
  useNavigate: () => mockedNavigate,
}));

describe("TeamsForm tests", () => {
  const queryClient = new QueryClient();

  const expectedHeaders = ["Team Name"];
  const testId = "TeamsForm";

  test("renders correctly with no initialContents", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <TeamsForm />
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
          <TeamsForm initialContents={teamsFixtures.teams[0].name} />
        </Router>
      </QueryClientProvider>,
    );

    expect(await screen.findByText(/Create/)).toBeInTheDocument();

    expectedHeaders.forEach((headerText) => {
      const header = screen.getByText(headerText);
      expect(header).toBeInTheDocument();
    });

    expect(await screen.findByTestId(`${testId}-name`)).toBeInTheDocument();
    expect(await screen.findByTestId(`${testId}-submit`)).toBeInTheDocument();
  });

  test("that navigate(-1) is called when Cancel is clicked", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <TeamsForm />
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
          <TeamsForm />
        </Router>
      </QueryClientProvider>,
    );

    expect(await screen.findByText(/Create/)).toBeInTheDocument();
    const submitButton = screen.getByText(/Create/);
    fireEvent.click(submitButton);

    await screen.findByText(/Team Name is required/);
  });

  test("renders the Staff Team switch, off by default, with its explanation", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <TeamsForm />
        </Router>
      </QueryClientProvider>,
    );

    const staffSwitch = await screen.findByTestId(`${testId}-isStaffTeam`);
    expect(staffSwitch).toHaveAttribute("type", "checkbox");
    expect(staffSwitch).toHaveAttribute("id", "isStaffTeam");
    expect(staffSwitch).not.toBeChecked();
    expect(screen.getByLabelText("Staff Team")).toBe(staffSwitch);
    expect(
      screen.getByText(/A staff team stands in for a student team/),
    ).toBeInTheDocument();

    fireEvent.click(staffSwitch);
    expect(staffSwitch).toBeChecked();
  });

  test("submits isStaffTeam along with the name", async () => {
    const submitAction = vi.fn();
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <TeamsForm submitAction={submitAction} />
        </Router>
      </QueryClientProvider>,
    );

    fireEvent.change(await screen.findByTestId(`${testId}-name`), {
      target: { value: "Staff" },
    });
    fireEvent.click(screen.getByTestId(`${testId}-isStaffTeam`));
    fireEvent.click(screen.getByTestId(`${testId}-submit`));

    await waitFor(() => expect(submitAction).toHaveBeenCalledTimes(1));
    expect(submitAction.mock.calls[0][0]).toEqual({
      name: "Staff",
      isStaffTeam: true,
    });
  });
});
