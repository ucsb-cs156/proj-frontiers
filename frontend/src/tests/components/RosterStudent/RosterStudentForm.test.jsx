import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { BrowserRouter as Router } from "react-router";

import RosterStudentForm from "main/components/RosterStudent/RosterStudentForm";
import { rosterStudentFixtures } from "fixtures/rosterStudentFixtures";
import { sectionsFixtures } from "fixtures/sectionsFixtures";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { vi } from "vitest";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";

const axiosMock = new AxiosMockAdapter(axios);

const mockedNavigate = vi.fn();
vi.mock("react-router", async (importOriginal) => ({
  ...(await importOriginal()),
  useNavigate: () => mockedNavigate,
}));

describe("RosterStudentForm tests", () => {
  const queryClient = new QueryClient();

  beforeEach(() => {
    axiosMock.reset();
  });

  const expectedHeaders = [
    "Student Id",
    "First Name",
    "Last Name",
    "Email",
    "Section",
  ];
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
    expect(await screen.findByTestId(`${testId}-section`)).toBeInTheDocument();
    expect(screen.getByText(`Section`)).toBeInTheDocument();
    expect(await screen.findByTestId(`${testId}-submit`)).toBeInTheDocument();
  });

  test("section is prefilled from initialContents", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <RosterStudentForm
            initialContents={rosterStudentFixtures.oneStudent[0]}
            buttonLabel="Update"
          />
        </Router>
      </QueryClientProvider>,
    );

    expect(await screen.findByTestId(`${testId}-section`)).toHaveValue("0100");
    expect(screen.getByTestId(`${testId}-studentId`)).toHaveValue("1234567");
  });

  test("section is optional and is included in submitted data", async () => {
    const submitAction = vi.fn();
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <RosterStudentForm submitAction={submitAction} />
        </Router>
      </QueryClientProvider>,
    );

    fireEvent.change(screen.getByLabelText("Student Id"), {
      target: { value: "1234567" },
    });
    fireEvent.change(screen.getByLabelText("First Name"), {
      target: { value: "Chris" },
    });
    fireEvent.change(screen.getByLabelText("Last Name"), {
      target: { value: "Gaucho" },
    });
    fireEvent.change(screen.getByLabelText("Email"), {
      target: { value: "cgaucho@ucsb.edu" },
    });
    expect(screen.getByLabelText("Section")).toHaveAttribute(
      "placeholder",
      "Optional",
    );

    // Submit with the section left blank: no validation error, blank section submitted
    fireEvent.click(screen.getByTestId(`${testId}-submit`));
    await waitFor(() => expect(submitAction).toHaveBeenCalledTimes(1));
    expect(screen.queryByText(/Section is required/)).not.toBeInTheDocument();
    expect(submitAction.mock.calls[0][0]).toEqual({
      studentId: "1234567",
      firstName: "Chris",
      lastName: "Gaucho",
      email: "cgaucho@ucsb.edu",
      section: "",
    });

    fireEvent.change(screen.getByLabelText("Section"), {
      target: { value: "0100" },
    });
    fireEvent.click(screen.getByTestId(`${testId}-submit`));
    await waitFor(() => expect(submitAction).toHaveBeenCalledTimes(2));
    expect(submitAction.mock.calls[1][0].section).toBe("0100");
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

    fireEvent.change(screen.getByLabelText("Email"), {
      target: {
        value: "invalidemail",
      },
    });

    fireEvent.click(submitButton);
    await screen.findByText(/Please enter a valid email/);
  });

  test("does not render SectionTranslator when translateSections is false", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <RosterStudentForm courseId={7} translateSections={false} />
        </Router>
      </QueryClientProvider>,
    );

    expect(await screen.findByText(/Create/)).toBeInTheDocument();
    expect(
      screen.queryByTestId(`${testId}-SectionTranslator-select`),
    ).not.toBeInTheDocument();
  });

  test("renders SectionTranslator when translateSections is true, and selecting a section updates the section field", async () => {
    axiosMock
      .onGet("/api/courses/7/sections")
      .reply(200, sectionsFixtures.threeSections);

    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <RosterStudentForm courseId={7} translateSections={true} />
        </Router>
      </QueryClientProvider>,
    );

    const select = await screen.findByTestId(
      `${testId}-SectionTranslator-select`,
    );
    await screen.findByRole("option", { name: "0200 - Tue 10:00am" });

    fireEvent.change(select, { target: { value: "0200" } });

    await waitFor(() =>
      expect(screen.getByTestId(`${testId}-section`)).toHaveValue("0200"),
    );
    expect(
      screen.getByTestId(`${testId}-SectionTranslator-translation`),
    ).toHaveValue("Tue 10:00am");
  });
});
