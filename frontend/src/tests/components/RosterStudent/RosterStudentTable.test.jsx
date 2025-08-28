import { fireEvent, render, waitFor, screen } from "@testing-library/react";
import { rosterStudentFixtures } from "fixtures/rosterStudentFixtures";
import RosterStudentTable from "main/components/RosterStudent/RosterStudentTable";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router";
import { currentUserFixtures } from "fixtures/currentUserFixtures";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import { expect, vi } from "vitest";

const queryClient = new QueryClient();
const axiosMock = new AxiosMockAdapter(axios);
const mockToast = vi.fn();
vi.mock("react-toastify", async (importOriginal) => {
  return {
    ...(await importOriginal()),
    toast: (x) => mockToast(x),
  };
});
describe("RosterStudentTable tests", () => {
  const expectedHeaders = [
    "id",
    "Student Id",
    "First Name",
    "Last Name",
    "Email",
    "Status",
    "GitHub Login",
    "Teams",
  ];
  const expectedFields = [
    "id",
    "studentId",
    "firstName",
    "lastName",
    "email",
    "orgStatus",
    "githubLogin",
    "teams",
  ];
  const testId = "RosterStudentTable";

  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    queryClient.clear();
  });
  test("renders empty table correctly", () => {
    // arrange
    const currentUser = currentUserFixtures.adminUser;

    // act
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <RosterStudentTable
            students={[]}
            currentUser={currentUser}
            courseId="7"
          />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    // assert

    const courseIdHiddenElement = screen.getByTestId(`${testId}-courseId`);
    expect(courseIdHiddenElement).toBeInTheDocument();
    expect(courseIdHiddenElement).toHaveAttribute("data-course-id", "7");
    // Expect it to have style display:none
    expect(courseIdHiddenElement).toHaveStyle("display: none");

    expectedHeaders.forEach((headerText) => {
      const header = screen.getByText(headerText);
      expect(header).toBeInTheDocument();
    });

    expectedFields.forEach((field) => {
      const fieldElement = screen.queryByTestId(
        `${testId}-cell-row-0-col-${field}`,
      );
      expect(fieldElement).not.toBeInTheDocument();
    });
  });
  test("Has the expected column headers, content and buttons for admin user", () => {
    // arrange
    const currentUser = currentUserFixtures.adminUser;

    // act
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <RosterStudentTable
            students={rosterStudentFixtures.studentsWithEachStatus}
            currentUser={currentUser}
          />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    // assert
    expectedHeaders.forEach((headerText) => {
      const header = screen.getByText(headerText);
      expect(header).toBeInTheDocument();
    });

    expect(screen.queryByText("Edit Student")).not.toBeInTheDocument();

    expectedFields.forEach((field) => {
      const header = screen.getByTestId(`${testId}-cell-row-0-col-${field}`);
      expect(header).toBeInTheDocument();
    });

    expect(screen.getByTestId(`${testId}-cell-row-0-col-id`)).toHaveTextContent(
      "1",
    );
    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-studentId`),
    ).toHaveTextContent("A123456");

    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-firstName`),
    ).toHaveTextContent("Alice");
    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-lastName`),
    ).toHaveTextContent("Brown");
    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-email`),
    ).toHaveTextContent("alicebrown@ucsb.edu");
    expect(
      screen.getByTestId(`${testId}-cell-row-3-col-githubLogin`),
    ).toHaveTextContent("jonsnow");
    expect(
      screen.getByTestId(`${testId}-cell-row-3-col-teams`),
    ).toHaveTextContent("Team A, Team B");

    const pending = screen.getByText("Pending");
    expect(pending).toBeInTheDocument();
    expect(pending).toHaveClass("text-danger");

    const joinCourse = screen.getByText("Join Course");
    expect(joinCourse).toBeInTheDocument();
    expect(joinCourse).toHaveClass("text-primary");

    const member = screen.getByText("Member");
    expect(member).toBeInTheDocument();
    expect(member).toHaveClass("text-success");

    const owner = screen.getByText("Owner");
    expect(owner).toBeInTheDocument();
    expect(owner).toHaveStyle("color: rgb(128, 0, 128)");

    const invited = screen.getByText("Invited");
    expect(invited).toBeInTheDocument();
    expect(invited).toHaveClass("text-primary");

    const unexpected = screen.getByText("Illegal status that will never occur");
    expect(unexpected).toBeInTheDocument();
    expect(unexpected).not.toHaveClass("text-danger");

    const editButton = screen.getByTestId(
      `${testId}-cell-row-0-col-Edit-button`,
    );
    expect(editButton).toBeInTheDocument();
    expect(editButton).toHaveClass("btn-primary");

    const deleteButton = screen.getByTestId(
      `${testId}-cell-row-0-col-Delete-button`,
    );
    expect(deleteButton).toBeInTheDocument();
    expect(deleteButton).toHaveClass("btn-danger");
  });
  test("Has the expected column headers, content for ordinary user", () => {
    // arrange
    const currentUser = currentUserFixtures.userOnly;

    // act
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <RosterStudentTable
            students={rosterStudentFixtures.threeStudents}
            currentUser={currentUser}
          />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    // assert
    expectedHeaders.forEach((headerText) => {
      const header = screen.getByText(headerText);
      expect(header).toBeInTheDocument();
    });

    expectedFields.forEach((field) => {
      const header = screen.getByTestId(`${testId}-cell-row-0-col-${field}`);
      expect(header).toBeInTheDocument();
    });

    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-studentId`),
    ).toHaveTextContent("2");
    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-firstName`),
    ).toHaveTextContent("Alice");
    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-lastName`),
    ).toHaveTextContent("Brown");
    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-email`),
    ).toHaveTextContent("alicebrown@ucsb.edu");

    expect(screen.queryByText("Delete")).not.toBeInTheDocument();
    expect(screen.queryByText("Edit")).not.toBeInTheDocument();
  });

  test("Edit button navigates to the edit page", async () => {
    const currentUser = currentUserFixtures.adminUser;
    const queryClientSpecific = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
          staleTime: Infinity,
        },
      },
    });
    queryClientSpecific.setQueryData(
      ["/api/rosterstudents/course/7"],
      rosterStudentFixtures.threeStudents,
    );
    queryClientSpecific.setQueryData(["mock queryData"], null);
    axiosMock.onPut("/api/rosterstudents/update").reply(200);
    render(
      <QueryClientProvider client={queryClientSpecific}>
        <MemoryRouter>
          <RosterStudentTable
            students={rosterStudentFixtures.threeStudents}
            currentUser={currentUser}
            courseId={7}
          />
        </MemoryRouter>
      </QueryClientProvider>,
    );
    const editButton = screen.getByTestId(
      "RosterStudentTable-cell-row-0-col-Edit-button",
    );
    fireEvent.click(editButton);
    await screen.findByText("Edit Student");
    expect(screen.getByTestId("RosterStudentTable-modal-body")).toHaveClass(
      "pb-3",
    );
    expect(screen.queryByText("Cancel")).not.toBeInTheDocument();
    expect(screen.getByText("Update")).toBeInTheDocument();
    fireEvent.click(screen.getByText("Update"));
    await waitFor(() => axiosMock.history.put.length === 1);
    expect(axiosMock.history.put[0].params).toEqual({
      firstName: "Alice",
      id: 3,
      lastName: "Brown",
      studentId: "A123456",
    });
    await waitFor(() =>
      expect(screen.queryByText("Edit Student")).not.toBeInTheDocument(),
    );
    expect(mockToast).toBeCalledWith("Student updated successfully.");
    expect(
      queryClientSpecific.getQueryState(["/api/rosterstudents/course/7"])
        .isInvalidated,
    ).toBe(true);
    expect(
      queryClientSpecific.getQueryState(["mock queryData"]).isInvalidated,
    ).toBe(false);
  });

  test("Delete button calls delete callback", async () => {
    // arrange
    const currentUser = currentUserFixtures.adminUser;

    axiosMock
      .onDelete("/api/rosterstudents/delete")
      .reply(200, { message: "Student deleted" });

    // act - render the component
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <RosterStudentTable
            students={rosterStudentFixtures.threeStudents}
            currentUser={currentUser}
          />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    // assert - check that the expected content is rendered
    expect(
      await screen.findByTestId(`${testId}-cell-row-0-col-studentId`),
    ).toHaveTextContent("2");

    const deleteButton = screen.getByTestId(
      `${testId}-cell-row-0-col-Delete-button`,
    );
    expect(deleteButton).toBeInTheDocument();

    // act - click the delete button
    fireEvent.click(deleteButton);

    // assert - check that the delete endpoint was called

    await waitFor(() => expect(axiosMock.history.delete.length).toBe(1));
    expect(axiosMock.history.delete[0].params).toEqual({ id: 3 });
  });
});
test("tooltips for Team column name", async () => {
  const currentUser = currentUserFixtures.adminUser;
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <RosterStudentTable
          students={rosterStudentFixtures.studentsWithEachStatus}
          currentUser={currentUser}
        />
      </MemoryRouter>
    </QueryClientProvider>,
  );

  fireEvent.mouseOver(screen.getByText("Teams"));

  await waitFor(() => {
    expect(
      screen.getByText("A list of teams that the student is a member of."),
    ).toBeInTheDocument();
  });
  expect(screen.getByRole("tooltip")).toHaveAttribute(
    "id",
    "tooltip-teams-header",
  );
});
test("tooltips for PENDING status", async () => {
  const currentUser = currentUserFixtures.adminUser;
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <RosterStudentTable
          students={rosterStudentFixtures.studentsWithEachStatus}
          currentUser={currentUser}
        />
      </MemoryRouter>
    </QueryClientProvider>,
  );

  fireEvent.mouseOver(screen.getByText("Pending"));

  await waitFor(() => {
    expect(
      screen.getByText(
        "Student cannot join the course until it has been completely set up.",
      ),
    ).toBeInTheDocument();
  });
});
test("tooltips for JOINCOURSE status", async () => {
  const currentUser = currentUserFixtures.adminUser;
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <RosterStudentTable
          students={rosterStudentFixtures.studentsWithEachStatus}
          currentUser={currentUser}
        />
      </MemoryRouter>
    </QueryClientProvider>,
  );

  fireEvent.mouseOver(screen.getByText("Join Course"));

  await waitFor(() => {
    expect(
      screen.getByText(
        "Student has been prompted to join, but hasn't yet clicked the 'Join Course' button to generate an invite to the organization.",
      ),
    ).toBeInTheDocument();
  });
});
test("tooltips for INVITED status", async () => {
  const currentUser = currentUserFixtures.adminUser;
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <RosterStudentTable
          students={rosterStudentFixtures.studentsWithEachStatus}
          currentUser={currentUser}
        />
      </MemoryRouter>
    </QueryClientProvider>,
  );

  fireEvent.mouseOver(screen.getByText("Invited"));
  await waitFor(() => {
    expect(
      screen.getByText(
        "Student has generated an invite, but has not yet accepted or declined the invitation.",
      ),
    ).toBeInTheDocument();
  });
});
test("tooltips for OWNER status", async () => {
  const currentUser = currentUserFixtures.adminUser;
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <RosterStudentTable
          students={rosterStudentFixtures.studentsWithEachStatus}
          currentUser={currentUser}
        />
      </MemoryRouter>
    </QueryClientProvider>,
  );

  fireEvent.mouseOver(screen.getByText("Owner"));
  await waitFor(() => {
    expect(
      screen.getByText(
        "Student is an owner of the GitHub organization associated with this course.",
      ),
    ).toBeInTheDocument();
  });
});
test("tooltips for MEMBER status", async () => {
  const currentUser = currentUserFixtures.adminUser;
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <RosterStudentTable
          students={rosterStudentFixtures.studentsWithEachStatus}
          currentUser={currentUser}
        />
      </MemoryRouter>
    </QueryClientProvider>,
  );

  fireEvent.mouseOver(screen.getByText("Member"));

  await waitFor(() => {
    expect(
      screen.getByText(
        "Student is a member of the GitHub organization associated with this course.",
      ),
    ).toBeInTheDocument();
  });
});
test("tooltips for an illegal status", async () => {
  const currentUser = currentUserFixtures.adminUser;

  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <RosterStudentTable
          students={rosterStudentFixtures.studentsWithEachStatus}
          currentUser={currentUser}
        />
      </MemoryRouter>
    </QueryClientProvider>,
  );

  fireEvent.mouseOver(screen.getByText("Illegal status that will never occur"));

  await waitFor(() => {
    expect(
      screen.getByText("Tooltip for illegal status that will never occur"),
    ).toBeInTheDocument();
  });
});
test("expect the correct tooltip ID", async () => {
  const currentUser = currentUserFixtures.adminUser;

  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <RosterStudentTable
          students={rosterStudentFixtures.studentsWithEachStatus}
          currentUser={currentUser}
        />
      </MemoryRouter>
    </QueryClientProvider>,
  );

  fireEvent.mouseOver(screen.getByText("Member"));

  const tooltip = await screen.findByRole("tooltip");
  expect(tooltip).toHaveAttribute("id", "member-tooltip");
});
test("if teams is null, shows empty string", async () => {
  const currentUser = currentUserFixtures.adminUser;
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <RosterStudentTable
          students={rosterStudentFixtures.studentsWithEachStatus}
          currentUser={currentUser}
        />
      </MemoryRouter>
    </QueryClientProvider>,
  );

  expect(
    screen.getByTestId("RosterStudentTable-cell-row-0-col-teams"),
  ).toHaveTextContent("");
});
