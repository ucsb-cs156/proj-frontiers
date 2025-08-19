import { fireEvent, render, waitFor, screen } from "@testing-library/react";
import { courseStaffFixtures } from "fixtures/courseStaffFixtures";
import CourseStaffTable from "main/components/CourseStaff/CourseStaffTable";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router";
import { currentUserFixtures } from "fixtures/currentUserFixtures";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import { vi } from "vitest";

const queryClient = new QueryClient();
const mockToast = vi.fn();
vi.mock("react-toastify", async (importOriginal) => {
  return {
    ...(await importOriginal()),
    toast: (x) => mockToast(x),
  };
});

const axiosMock = new AxiosMockAdapter(axios);
describe("CourseStaffTable tests", () => {
  const expectedHeaders = [
    "id",
    "First Name",
    "Last Name",
    "Email",
    "Status",
    "GitHub Login",
  ];
  const expectedFields = [
    "id",
    "firstName",
    "lastName",
    "email",
    "orgStatus",
    "githubLogin",
  ];
  const testId = "CourseStaffTable";

  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    queryClient.clear();
    mockToast.mockClear();
  });

  test("renders empty table correctly", () => {
    // arrange
    const currentUser = currentUserFixtures.adminUser;

    // act
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <CourseStaffTable staff={[]} currentUser={currentUser} courseId="7" />
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
          <CourseStaffTable
            staff={courseStaffFixtures.staffWithEachStatus}
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

    expect(
      screen.getByTestId("CourseStaffTable-cell-row-0-col-Edit-button"),
    ).toBeInTheDocument();

    expectedFields.forEach((field) => {
      const header = screen.getByTestId(`${testId}-cell-row-0-col-${field}`);
      expect(header).toBeInTheDocument();
    });

    expect(screen.getByTestId(`${testId}-cell-row-0-col-id`)).toHaveTextContent(
      "1",
    );

    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-firstName`),
    ).toHaveTextContent("Dr. John");
    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-lastName`),
    ).toHaveTextContent("Professor");
    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-email`),
    ).toHaveTextContent("johnprof@ucsb.edu");

    expect(
      screen.getByTestId(`${testId}-cell-row-3-col-githubLogin`),
    ).toHaveTextContent("sarahlead");

    const pending = screen.getByText("Pending");
    expect(pending).toBeInTheDocument();
    expect(pending).toHaveClass("text-warning");

    const joinCourse = screen.getByText("Join Course");
    expect(joinCourse).toBeInTheDocument();
    expect(joinCourse).toHaveClass("text-primary");

    const members = screen.getAllByText("Member");
    members.forEach((x) => expect(x).toHaveClass("text-primary"));

    const owner = screen.getByText("Owner");
    expect(owner).toBeInTheDocument();
    expect(owner).toHaveClass("text-info");

    const invited = screen.getByText("Invited");
    expect(invited).toBeInTheDocument();
    expect(invited).toHaveClass("text-primary");

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
          <CourseStaffTable
            staff={courseStaffFixtures.threeStaff}
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
      screen.getByTestId(`${testId}-cell-row-0-col-firstName`),
    ).toHaveTextContent("Dr. John");
    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-lastName`),
    ).toHaveTextContent("Professor");
    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-email`),
    ).toHaveTextContent("johnprof@ucsb.edu");

    expect(screen.queryByText("Delete")).not.toBeInTheDocument();
    expect(screen.queryByText("Edit")).not.toBeInTheDocument();
  });

  test("Edit button navigates to the edit modal", async () => {
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
      ["/api/coursestaff/course?courseId=7"],
      courseStaffFixtures.threeStaff,
    );
    queryClientSpecific.setQueryData(["mock queryData"], null);
    axiosMock.onPut(/\/api\/coursestaff?courseId=7.*/).reply(200);
    render(
      <QueryClientProvider client={queryClientSpecific}>
        <MemoryRouter>
          <CourseStaffTable
            staff={courseStaffFixtures.threeStaff}
            currentUser={currentUser}
            courseId={7}
          />
        </MemoryRouter>
      </QueryClientProvider>,
    );
    const editButton = screen.getByTestId(
      "CourseStaffTable-cell-row-0-col-Edit-button",
    );
    fireEvent.click(editButton);
    await screen.findByText("Edit Staff Member");
    expect(screen.getByTestId("CourseStaffTable-modal-body")).toHaveClass(
      "pb-3",
    );
    expect(screen.queryByText("Cancel")).not.toBeInTheDocument();
    expect(screen.getByText("Update")).toBeInTheDocument();
    fireEvent.click(screen.getByText("Update"));
    await waitFor(() => axiosMock.history.put.length === 1);
  });

  test("Delete button calls delete callback", async () => {
    // arrange
    const currentUser = currentUserFixtures.adminUser;

    axiosMock
      .onDelete("/api/coursestaff/delete")
      .reply(200, { message: "Staff member deleted" });

    // act - render the component
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <CourseStaffTable
            staff={courseStaffFixtures.threeStaff}
            currentUser={currentUser}
          />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    // assert - check that the expected content is rendered
    expect(
      await screen.findByTestId(`${testId}-cell-row-0-col-id`),
    ).toHaveTextContent("1");

    const deleteButton = screen.getByTestId(
      `${testId}-cell-row-0-col-Delete-button`,
    );
    expect(deleteButton).toBeInTheDocument();

    // act - click the delete button
    fireEvent.click(deleteButton);

    // assert - check that the delete endpoint was called

    await waitFor(() => expect(axiosMock.history.delete.length).toBe(1));
    expect(axiosMock.history.delete[0].params).toEqual({ id: 1 });
  });

  test("tooltips for PENDING status", async () => {
    const currentUser = currentUserFixtures.adminUser;
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <CourseStaffTable
            staff={courseStaffFixtures.staffWithEachStatus}
            currentUser={currentUser}
          />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    fireEvent.mouseOver(screen.getByText("Pending"));

    await waitFor(() => {
      expect(
        screen.getByText(
          "Staff member cannot join the course until it has been completely set up.",
        ),
      ).toBeInTheDocument();
    });
  });
  test("tooltips for JOINCOURSE status", async () => {
    const currentUser = currentUserFixtures.adminUser;
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <CourseStaffTable
            staff={courseStaffFixtures.staffWithEachStatus}
            currentUser={currentUser}
          />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    fireEvent.mouseOver(screen.getByText("Join Course"));

    await waitFor(() => {
      expect(
        screen.getByText(
          "Staff member has been prompted to join, but hasn't yet clicked the 'Join Course' button to generate an invite to the organization.",
        ),
      ).toBeInTheDocument();
    });
  });
  test("tooltips for INVITED status", async () => {
    const currentUser = currentUserFixtures.adminUser;
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <CourseStaffTable
            staff={courseStaffFixtures.staffWithEachStatus}
            currentUser={currentUser}
          />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    fireEvent.mouseOver(screen.getByText("Invited"));
    await waitFor(() => {
      expect(
        screen.getByText(
          "Staff member has generated an invite, but has not yet accepted or declined the invitation.",
        ),
      ).toBeInTheDocument();
    });
  });
  test("tooltips for OWNER status", async () => {
    const currentUser = currentUserFixtures.adminUser;
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <CourseStaffTable
            staff={courseStaffFixtures.staffWithEachStatus}
            currentUser={currentUser}
          />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    fireEvent.mouseOver(screen.getByText("Owner"));
    await waitFor(() => {
      expect(
        screen.getByText(
          "Staff member is an owner of the GitHub organization associated with this course.",
        ),
      ).toBeInTheDocument();
    });
  });
  test("tooltips for MEMBER status", async () => {
    const currentUser = currentUserFixtures.adminUser;
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <CourseStaffTable
            staff={courseStaffFixtures.staffWithEachStatus}
            currentUser={currentUser}
          />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const members = screen.getAllByText("Member");
    fireEvent.mouseOver(members[0]);

    await waitFor(() => {
      expect(
        screen.getByText(
          "Staff member is a member of the GitHub organization associated with this course.",
        ),
      ).toBeInTheDocument();
    });
  });

  test("tooltips for an illegal status", async () => {
    const currentUser = currentUserFixtures.adminUser;

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <CourseStaffTable
            staff={courseStaffFixtures.staffWithUndefinedStatus}
            currentUser={currentUser}
          />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    fireEvent.mouseOver(screen.getByText("Floating in Space"));

    await waitFor(() => {
      expect(
        screen.getByText("Tooltip for illegal status that will never occur"),
      ).toBeInTheDocument();
    });
  });

  test("onEditSuccess calls toast and hideModal", async () => {
    axiosMock.onPut("/api/coursestaff").reply(200, []);
    // Arrange
    const currentUser = currentUserFixtures.adminUser;
    // Render the component
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <CourseStaffTable
            staff={courseStaffFixtures.threeStaff}
            currentUser={currentUser}
            courseId={7}
          />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    // Access the component instance via screen (simulate edit success)
    // Find and click the Edit button to open modal
    const editButton = screen.getByTestId(
      "CourseStaffTable-cell-row-0-col-Edit-button",
    );
    fireEvent.click(editButton);

    // The modal should be open
    expect(screen.getByText("Edit Staff Member")).toBeInTheDocument();

    // Simulate successful edit by clicking Update
    fireEvent.click(screen.getByText("Update"));

    await waitFor(() => expect(mockToast).toHaveBeenCalled());
    expect(mockToast).toHaveBeenCalledWith(
      "Staff member updated successfully.",
    );

    // Modal should be closed after success
    await waitFor(() => {
      expect(screen.queryByText("Edit Staff Member")).not.toBeInTheDocument();
    });
  });

  test("expect the correct tooltip ID", async () => {
    const currentUser = currentUserFixtures.adminUser;

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <CourseStaffTable
            staff={courseStaffFixtures.staffWithEachStatus}
            currentUser={currentUser}
          />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const members = screen.getAllByText("Member");

    fireEvent.mouseOver(members[0]);

    const tooltip = await screen.findByRole("tooltip");
    expect(tooltip).toHaveAttribute("id", "member-tooltip");
  });
});
