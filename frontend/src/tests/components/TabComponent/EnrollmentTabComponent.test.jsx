import {
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import {
  loadResultFixtures,
  rosterStudentFixtures,
} from "fixtures/rosterStudentFixtures";
import AxiosMockAdapter from "axios-mock-adapter";
import axios from "axios";
import {
  QueryClient,
  QueryClientProvider,
  useQuery,
} from "@tanstack/react-query";
import EnrollmentTabComponent from "main/components/TabComponent/EnrollmentTabComponent";
import userEvent from "@testing-library/user-event";
import { currentUserFixtures } from "fixtures/currentUserFixtures";
import { vi } from "vitest";
import { toast } from "react-toastify";

const axiosMock = new AxiosMockAdapter(axios);
const queryClient = new QueryClient();
const testId = "InstructorCourseShowPage";
vi.mock("react-toastify", async (importOriginal) => {
  const mockToast = vi.fn();
  mockToast.error = vi.fn();
  return {
    ...(await importOriginal()),
    toast: mockToast,
  };
});

const mockedNavigate = vi.fn();
vi.mock("react-router", async (importOriginal) => ({
  ...(await importOriginal()),
  useNavigate: () => mockedNavigate,
}));

const ArbitraryTestQueryComponent = () => {
  const _arbitraryQuery = useQuery({
    queryKey: ["arbitraryQuery"],
    queryFn: () => "banana",
  });
  return <></>;
};

describe("EnrollmentTabComponent Tests", () => {
  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    queryClient.clear();
    vi.resetAllMocks();
  });

  test("Table Renders", async () => {
    axiosMock
      .onGet("/api/rosterstudents/course/1")
      .reply(200, rosterStudentFixtures.threeStudents);

    render(
      <QueryClientProvider client={queryClient}>
        <EnrollmentTabComponent
          courseId={1}
          testIdPrefix={testId}
          currentUser={currentUserFixtures.instructorUser}
        />
      </QueryClientProvider>,
    );

    const rsTestId = "InstructorCourseShowPage-RosterStudentTable";

    await waitFor(() => {
      expect(
        screen.getByTestId(`${rsTestId}-cell-row-0-col-id`),
      ).toHaveTextContent(rosterStudentFixtures.threeStudents[0].id);
    });

    const studentFirstName0 = screen.getByText(
      rosterStudentFixtures.threeStudents[0].firstName,
    );
    expect(studentFirstName0).toBeInTheDocument();

    const studentId0 = screen.getByTestId(
      `${rsTestId}-cell-row-0-col-studentId`,
    );
    expect(studentId0).toHaveTextContent(
      rosterStudentFixtures.threeStudents[0].studentId,
    );
  });
  test("Table Renders with no students", async () => {
    axiosMock.onGet("/api/rosterstudents/course/7").reply(200, []);

    render(
      <QueryClientProvider client={queryClient}>
        <EnrollmentTabComponent
          courseId={7}
          testIdPrefix={testId}
          currentUser={currentUserFixtures.instructorUser}
        />
      </QueryClientProvider>,
    );

    await waitFor(() => {
      expect(
        screen.getByTestId(
          "InstructorCourseShowPage-RosterStudentTable-header-studentId",
        ),
      ).toBeInTheDocument();
    });

    expect(
      screen.queryByTestId(`${testId}-cell-row-0-col-id`),
    ).not.toBeInTheDocument();

    const expectedHeaders = ["Student Id", "First Name", "Last Name", "Email"];
    const expectedFields = ["studentId", "firstName", "lastName", "email"];

    // assert
    expectedHeaders.forEach((headerText, index) => {
      const header = screen.getByTestId(
        `InstructorCourseShowPage-RosterStudentTable-header-${expectedFields[index]}`,
      );
      expect(header).toHaveTextContent(headerText);
    });

    expectedFields.forEach((field) => {
      const fieldElement = screen.queryByTestId(
        `${testId}-cell-row-0-col-${field}`,
      );
      expect(fieldElement).not.toBeInTheDocument();
    });
    expect(screen.queryByTestId(`${testId}-csv-modal`)).not.toBeInTheDocument();
    expect(
      screen.queryByTestId(`${testId}-post-modal`),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByTestId(`${testId}-csv-error-modal`),
    ).not.toBeInTheDocument();
  });

  test("Dropped Students appear correctly", async () => {
    axiosMock
      .onGet("/api/rosterstudents/course/1")
      .reply(200, rosterStudentFixtures.fourStudentsOneDropped);

    render(
      <QueryClientProvider client={queryClient}>
        <EnrollmentTabComponent
          courseId={1}
          testIdPrefix={testId}
          currentUser={currentUserFixtures.instructorUser}
        />
      </QueryClientProvider>,
    );

    await waitFor(() => {
      expect(
        screen.getByTestId(`${testId}-RosterStudentTable-cell-row-0-col-id`),
      ).toHaveTextContent(rosterStudentFixtures.fourStudentsOneDropped[0].id);
    });
    await waitFor(() => {
      expect(
        screen.getByTestId(`DroppedStudentsTable-cell-row-0-col-studentId`),
      ).toHaveTextContent(
        rosterStudentFixtures.fourStudentsOneDropped[3].studentId,
      );
    });
    const table = screen.getByTestId(
      "InstructorCourseShowPage-RosterStudentTable",
    );
    expect(
      within(table).queryByText("aryasue@ucsb.edu"),
    ).not.toBeInTheDocument();
  });

  test("Successfully makes a call to the backend on submit and clears search filter", async () => {
    const queryClientSpecific = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
          staleTime: Infinity,
        },
      },
    });
    const file = new File(["there"], "egrades.csv", { type: "text/csv" });

    axiosMock
      .onGet("/api/rosterstudents/course/7")
      .reply(200, rosterStudentFixtures.threeStudents);

    axiosMock.onPost("/api/rosterstudents/upload/csv").reply(200);

    const user = userEvent.setup();
    render(
      <QueryClientProvider client={queryClientSpecific}>
        <ArbitraryTestQueryComponent />
        <EnrollmentTabComponent
          courseId={7}
          testIdPrefix={testId}
          currentUser={currentUserFixtures.instructorUser}
        />
      </QueryClientProvider>,
    );
    const openModal = await screen.findByTestId(`${testId}-csv-button`);

    const arbitraryUpdateCount = queryClientSpecific.getQueryState([
      "arbitraryQuery",
    ]).dataUpdateCount;

    const updateCountStudent = queryClientSpecific.getQueryState([
      "/api/rosterstudents/course/7",
    ]).dataUpdateCount;

    // Get the search input and set a search term
    const searchInput = screen.getByTestId("InstructorCourseShowPage-search");
    fireEvent.change(searchInput, { target: { value: "test search" } });
    expect(searchInput.value).toBe("test search");

    fireEvent.click(openModal);
    expect(screen.getByTestId(`${testId}-csv-modal`)).toHaveClass(
      "modal-dialog modal-dialog-centered",
    );

    const upload = await screen.findByTestId(
      "RosterStudentCSVUploadForm-upload",
    );
    const submitButton = screen.getByTestId(
      "RosterStudentCSVUploadForm-submit",
    );
    await user.upload(upload, file);
    fireEvent.click(submitButton);
    await waitFor(() => {
      expect(axiosMock.history.post[0].params).toEqual({
        courseId: 7,
      });
    });
    expect(axiosMock.history.post[0].data.get("file")).toEqual(file);
    expect(toast).toBeCalledWith("Roster successfully updated.");
    expect(
      queryClientSpecific.getQueryState(["arbitraryQuery"]).dataUpdateCount,
    ).toBe(arbitraryUpdateCount);
    expect(
      queryClientSpecific.getQueryState(["/api/rosterstudents/course/7"])
        .dataUpdateCount,
    ).toEqual(updateCountStudent + 1);

    // Verify that the search filter is cleared
    await waitFor(() => {
      expect(searchInput.value).toBe("");
    });
    expect(screen.queryByTestId(`${testId}-csv-modal`)).not.toBeInTheDocument();
  });

  test("CsvForm error returns correctly", async () => {
    const file = new File(["there"], "egrades.csv", { type: "text/csv" });

    axiosMock
      .onGet("/api/rosterstudents/course/7")
      .reply(200, rosterStudentFixtures.threeStudents);

    axiosMock
      .onPost("/api/rosterstudents/upload/csv")
      .reply(400, loadResultFixtures.failed);

    const user = userEvent.setup();
    render(
      <QueryClientProvider client={queryClient}>
        <EnrollmentTabComponent
          courseId={7}
          testIdPrefix={testId}
          currentUser={currentUserFixtures.instructorUser}
        />
      </QueryClientProvider>,
    );
    const openModal = await screen.findByTestId(`${testId}-csv-button`);

    // Get the search input and set a search term
    const searchInput = screen.getByTestId("InstructorCourseShowPage-search");
    fireEvent.change(searchInput, { target: { value: "test search" } });
    expect(searchInput.value).toBe("test search");

    fireEvent.click(openModal);
    expect(screen.getByTestId(`${testId}-csv-modal`)).toHaveClass(
      "modal-dialog modal-dialog-centered",
    );

    const upload = await screen.findByTestId(
      "RosterStudentCSVUploadForm-upload",
    );
    const submitButton = screen.getByTestId(
      "RosterStudentCSVUploadForm-submit",
    );
    await user.upload(upload, file);
    fireEvent.click(submitButton);
    await waitFor(() =>
      expect(toast.error).toHaveBeenCalledWith(
        `Error uploading CSV: ${JSON.stringify(loadResultFixtures.failed, null, 2)}`,
      ),
    );
  });

  test("CsvForm rejected shows modal", async () => {
    const file = new File(["there"], "egrades.csv", { type: "text/csv" });

    axiosMock
      .onGet("/api/rosterstudents/course/7")
      .reply(200, rosterStudentFixtures.threeStudents);

    axiosMock
      .onPost("/api/rosterstudents/upload/csv")
      .reply(409, loadResultFixtures.failed);

    const user = userEvent.setup();
    render(
      <QueryClientProvider client={queryClient}>
        <EnrollmentTabComponent
          courseId={7}
          testIdPrefix={testId}
          currentUser={currentUserFixtures.instructorUser}
        />
      </QueryClientProvider>,
    );
    const openModal = await screen.findByTestId(`${testId}-csv-button`);

    // Get the search input and set a search term
    const searchInput = screen.getByTestId("InstructorCourseShowPage-search");
    fireEvent.change(searchInput, { target: { value: "test search" } });
    expect(searchInput.value).toBe("test search");

    fireEvent.click(openModal);
    expect(screen.getByTestId(`${testId}-csv-modal`)).toHaveClass(
      "modal-dialog modal-dialog-centered",
    );

    const upload = await screen.findByTestId(
      "RosterStudentCSVUploadForm-upload",
    );
    const submitButton = screen.getByTestId(
      "RosterStudentCSVUploadForm-submit",
    );
    await user.upload(upload, file);
    fireEvent.click(submitButton);
    await screen.findByTestId("InstructorCourseShowPage-csv-error-modal");
    expect(
      screen.getByText(
        "The following students couldn't be uploaded to the roster as their emails and student IDs match two separate students:",
      ),
    ).toBeInTheDocument();
    expect(
      screen.getByText(loadResultFixtures.failed.rejected[0].studentId),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId(`${testId}-RosterStudentTable-csv-error`),
    ).toBeInTheDocument();
    expect(screen.getByTestId(`${testId}-csv-error-modal`)).toHaveClass(
      "modal-dialog modal-dialog-centered",
    );
    await waitFor(() =>
      expect(
        screen.queryByTestId(`${testId}-csv-modal`),
      ).not.toBeInTheDocument(),
    );
    fireEvent.click(screen.getByRole("button", { name: "Close" }));
    await waitFor(() =>
      expect(
        screen.queryByTestId(`${testId}-csv-error-modal`),
      ).not.toBeInTheDocument(),
    );
  });

  test("RosterStudentForm submit works and clears search filter", async () => {
    const queryClientSpecific = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
          staleTime: Infinity,
        },
      },
    });
    axiosMock
      .onGet("/api/rosterstudents/course/7")
      .reply(200, rosterStudentFixtures.threeStudents);

    axiosMock.onPost("/api/rosterstudents/post").reply(200);
    render(
      <QueryClientProvider client={queryClientSpecific}>
        <ArbitraryTestQueryComponent />
        <EnrollmentTabComponent
          courseId={7}
          testIdPrefix={testId}
          currentUser={currentUserFixtures.instructorUser}
        />
      </QueryClientProvider>,
    );

    //Great time to check initial values
    expect(
      queryClientSpecific.getQueryData(["/api/rosterstudents/course/7"]),
    ).toStrictEqual([]);

    const openModal = await screen.findByTestId(`${testId}-post-button`);
    const arbitraryUpdateCount = queryClientSpecific.getQueryState([
      "arbitraryQuery",
    ]).dataUpdateCount;
    const updateCountStudent = queryClientSpecific.getQueryState([
      "/api/rosterstudents/course/7",
    ]).dataUpdateCount;

    fireEvent.click(openModal);
    await screen.findByLabelText("Student Id");
    expect(screen.getByTestId(`${testId}-post-modal`)).toHaveClass(
      "modal-dialog modal-dialog-centered",
    );

    // Get the search input and set a search term
    const searchInput = screen.getByTestId("InstructorCourseShowPage-search");
    fireEvent.change(searchInput, { target: { value: "test search" } });
    expect(searchInput.value).toBe("test search");

    expect(screen.queryByText("Cancel")).not.toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("Student Id"), {
      target: { value: "123456789" },
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
    fireEvent.change(screen.getByLabelText("Section"), {
      target: { value: "0100" },
    });
    fireEvent.click(screen.getByTestId("RosterStudentForm-submit"));
    await waitFor(() => expect(axiosMock.history.post.length).toEqual(1));
    expect(axiosMock.history.post[0].params).toEqual({
      courseId: 7,
      studentId: "123456789",
      firstName: "Chris",
      lastName: "Gaucho",
      email: "cgaucho@ucsb.edu",
      section: "0100",
    });
    await waitFor(() => expect(toast).toBeCalled());
    expect(toast).toBeCalledWith("Roster successfully updated.");
    expect(
      queryClientSpecific.getQueryState(["arbitraryQuery"]).dataUpdateCount,
    ).toBe(arbitraryUpdateCount);
    expect(
      queryClientSpecific.getQueryState(["/api/rosterstudents/course/7"])
        .dataUpdateCount,
    ).toEqual(updateCountStudent + 1);

    // Verify that the search filter is cleared
    await waitFor(() => {
      expect(searchInput.value).toBe("");
    });
    expect(
      screen.queryByTestId(`${testId}-post-modal`),
    ).not.toBeInTheDocument();
  });

  test("RosterStudentForm works on error", async () => {
    const queryClientSpecific = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
          staleTime: Infinity,
        },
      },
    });
    const postResponse = {
      insertStatus: "REJECTED",
      rosterStudent: rosterStudentFixtures.oneStudent[0],
    };

    axiosMock
      .onGet("/api/rosterstudents/course/7")
      .reply(200, rosterStudentFixtures.threeStudents);

    axiosMock.onPost("/api/rosterstudents/post").reply(409, postResponse);
    render(
      <QueryClientProvider client={queryClientSpecific}>
        <ArbitraryTestQueryComponent />
        <EnrollmentTabComponent
          courseId={7}
          testIdPrefix={testId}
          currentUser={currentUserFixtures.instructorUser}
        />
      </QueryClientProvider>,
    );

    //Great time to check initial values
    expect(
      queryClientSpecific.getQueryData(["/api/rosterstudents/course/7"]),
    ).toStrictEqual([]);

    const openModal = await screen.findByTestId(`${testId}-post-button`);

    fireEvent.click(openModal);
    await screen.findByLabelText("Student Id");
    expect(screen.getByTestId(`${testId}-post-modal`)).toHaveClass(
      "modal-dialog modal-dialog-centered",
    );

    // Get the search input and set a search term
    const searchInput = screen.getByTestId("InstructorCourseShowPage-search");
    fireEvent.change(searchInput, { target: { value: "test search" } });
    expect(searchInput.value).toBe("test search");

    expect(screen.queryByText("Cancel")).not.toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("Student Id"), {
      target: { value: "1234567" },
    });
    fireEvent.change(screen.getByLabelText("First Name"), {
      target: { value: "Bob" },
    });
    fireEvent.change(screen.getByLabelText("Last Name"), {
      target: { value: "Smith" },
    });
    fireEvent.change(screen.getByLabelText("Email"), {
      target: { value: "bobsmith@ucsb.edu" },
    });
    fireEvent.click(screen.getByTestId("RosterStudentForm-submit"));
    screen.debug(null, 1000000);
    await waitFor(() => expect(axiosMock.history.post.length).toEqual(1));
    await waitFor(() =>
      expect(toast.error).toBeCalledWith(
        `Error adding student: ${JSON.stringify(postResponse, null, 2)}`,
      ),
    );
  });

  test("Modals close on close buttons (respectively), download works", async () => {
    const download = vi.fn();
    window.open = (a, b) => download(a, b);
    axiosMock
      .onGet("/api/rosterstudents/course/7")
      .reply(200, rosterStudentFixtures.threeStudents);

    render(
      <QueryClientProvider client={queryClient}>
        <ArbitraryTestQueryComponent />
        <EnrollmentTabComponent
          courseId={7}
          testIdPrefix={testId}
          currentUser={currentUserFixtures.instructorUser}
        />
      </QueryClientProvider>,
    );

    const openModalPost = await screen.findByTestId(`${testId}-post-button`);
    fireEvent.click(openModalPost);
    let closeButton = await screen.findByRole("button", { name: "Close" });
    fireEvent.click(closeButton);
    await waitFor(() =>
      expect(
        screen.queryByTestId(`${testId}-post-modal`),
      ).not.toBeInTheDocument(),
    );
    const openModalCsv = await screen.findByTestId(`${testId}-csv-button`);
    fireEvent.click(openModalCsv);
    closeButton = await screen.findByRole("button", { name: "Close" });
    fireEvent.click(closeButton);
    await waitFor(() =>
      expect(
        screen.queryByTestId(`${testId}-csv-modal`),
      ).not.toBeInTheDocument(),
    );
    fireEvent.click(screen.getByText("Download Student CSV"));
    await waitFor(() => expect(download).toBeCalled());
    expect(download).toBeCalledWith(
      "/api/csv/rosterstudents?courseId=7",
      "_blank",
    );
  });

  test("Info icon displays tooltip and opens help page on click", async () => {
    const openMock = vi.fn();
    window.open = openMock;

    axiosMock
      .onGet("/api/rosterstudents/course/7")
      .reply(200, rosterStudentFixtures.threeStudents);

    const user = userEvent.setup();

    render(
      <QueryClientProvider client={queryClient}>
        <EnrollmentTabComponent
          courseId={7}
          testIdPrefix={testId}
          currentUser={currentUserFixtures.instructorUser}
        />
      </QueryClientProvider>,
    );

    await waitFor(() => {
      expect(screen.getByTestId(`${testId}-csv-button`)).toBeInTheDocument();
    });

    const infoIcon = screen.getByTestId(`${testId}-csv-info-icon`);
    expect(infoIcon).toBeInTheDocument();
    expect(infoIcon).toHaveStyle({ position: "absolute" });
    expect(infoIcon).toHaveStyle({ top: "50%" });
    expect(infoIcon).toHaveStyle({ right: "0.75rem" });
    expect(infoIcon).toHaveStyle({ transform: "translateY(-50%)" });
    expect(infoIcon).toHaveStyle({ color: "#fff" });
    expect(infoIcon).toHaveStyle({ cursor: "pointer" });
    expect(infoIcon).toHaveStyle({ fontSize: "0.9rem" });
    expect(infoIcon).toHaveStyle({ userSelect: "none" });
    expect(infoIcon.tagName.toLowerCase()).toBe("svg");

    await user.hover(infoIcon);

    await waitFor(() => {
      expect(screen.getByText("CSV Upload Format Help")).toBeInTheDocument();
    });

    fireEvent.click(infoIcon);

    await waitFor(() => {
      expect(openMock).toHaveBeenCalledWith("/help/csv", "_blank");
    });
  });

  describe("Search filter works correctly", () => {
    const testId = "InstructorCourseShowPage";
    const rsTestId = "InstructorCourseShowPage-RosterStudentTable";
    const studentList = [
      ...rosterStudentFixtures.studentsWithEachStatus,
      {
        id: 7,
        studentId: "A626737",
        firstName: "Fake",
        lastName: "Name",
        email: "fakename@ucsb.edu",
        githubLogin: "DifferingGitHub",
        orgStatus: "JOINCOURSE",
      },
    ];
    beforeEach(() => {
      axiosMock.onGet("/api/rosterstudents/course/1").reply(200, studentList);
    });

    test("PLaceholder, initial check", async () => {
      render(
        <QueryClientProvider client={queryClient}>
          <EnrollmentTabComponent
            courseId={1}
            testIdPrefix={testId}
            currentUser={currentUserFixtures.instructorUser}
          />
        </QueryClientProvider>,
      );
      await waitFor(() => {
        expect(
          screen.getByTestId(`${rsTestId}-cell-row-0-col-id`),
        ).toBeInTheDocument();
      });

      const searchInput = screen.getByTestId(`${testId}-search`);
      expect(searchInput).toBeInTheDocument();
      expect(searchInput).toHaveAttribute(
        "placeholder",
        "Search by name, email, student ID, or Github Login",
      );
      expect(
        screen.getByTestId(`${rsTestId}-cell-row-0-col-firstName`),
      ).toBeInTheDocument();
      expect(
        screen.getByTestId(`${rsTestId}-cell-row-1-col-firstName`),
      ).toBeInTheDocument();
      expect(
        screen.getByTestId(`${rsTestId}-cell-row-2-col-firstName`),
      ).toBeInTheDocument();
    });

    test("First Name, Email", async () => {
      render(
        <QueryClientProvider client={queryClient}>
          <EnrollmentTabComponent
            courseId={1}
            testIdPrefix={testId}
            currentUser={currentUserFixtures.instructorUser}
          />
        </QueryClientProvider>,
      );

      await waitFor(() => {
        expect(
          screen.getByTestId(`${rsTestId}-cell-row-0-col-id`),
        ).toBeInTheDocument();
      });

      // Verify search input is rendered
      const searchInput = screen.getByTestId(`${testId}-search`);

      const fullNameStudent = rosterStudentFixtures.studentsWithEachStatus[2]; // Emma Watson
      fireEvent.change(searchInput, {
        target: {
          value:
            `${fullNameStudent.firstName} ${fullNameStudent.lastName}`.toUpperCase(),
        },
      });

      expect(
        screen.getByTestId(`${rsTestId}-cell-row-0-col-firstName`),
      ).toHaveTextContent(fullNameStudent.firstName);
      expect(
        screen.getByTestId(`${rsTestId}-cell-row-0-col-lastName`),
      ).toHaveTextContent(fullNameStudent.lastName);
      expect(
        screen.queryByTestId(`${rsTestId}-cell-row-1-col-firstName`),
      ).not.toBeInTheDocument();

      fireEvent.change(searchInput, { target: { value: "" } });

      expect(
        screen.getByTestId(`${rsTestId}-cell-row-0-col-firstName`),
      ).toBeInTheDocument();
      expect(
        screen.getByTestId(`${rsTestId}-cell-row-1-col-firstName`),
      ).toBeInTheDocument();
      expect(
        screen.getByTestId(`${rsTestId}-cell-row-2-col-firstName`),
      ).toBeInTheDocument();

      fireEvent.change(searchInput, {
        target: {
          value:
            rosterStudentFixtures.studentsWithEachStatus[1].email.toUpperCase(),
        },
      });

      expect(
        screen.getByTestId(`${rsTestId}-cell-row-0-col-email`),
      ).toHaveTextContent(
        rosterStudentFixtures.studentsWithEachStatus[1].email,
      );
      expect(
        screen.queryByTestId(`${rsTestId}-cell-row-1-col-email`),
      ).not.toBeInTheDocument();
    });

    test("GitHub Login, Student ID", async () => {
      render(
        <QueryClientProvider client={queryClient}>
          <EnrollmentTabComponent
            courseId={1}
            testIdPrefix={testId}
            currentUser={currentUserFixtures.instructorUser}
          />
        </QueryClientProvider>,
      );
      await waitFor(() => {
        expect(
          screen.getByTestId(`${rsTestId}-cell-row-0-col-id`),
        ).toBeInTheDocument();
      });

      const searchInput = screen.getByTestId(`${testId}-search`);
      const studentWithGithub = studentList[6].githubLogin;
      fireEvent.change(searchInput, {
        target: { value: studentWithGithub.toUpperCase() },
      });

      expect(
        screen.getByTestId(`${rsTestId}-cell-row-0-col-githubLogin`),
      ).toHaveTextContent(studentWithGithub);
      expect(
        screen.queryByTestId(`${rsTestId}-cell-row-1-col-firstName`),
      ).not.toBeInTheDocument();

      fireEvent.change(searchInput, { target: { value: "" } });

      fireEvent.change(searchInput, {
        target: {
          value:
            rosterStudentFixtures.studentsWithEachStatus[1].studentId.toUpperCase(),
        },
      });

      expect(
        screen.getByTestId(`${rsTestId}-cell-row-0-col-studentId`),
      ).toHaveTextContent(
        rosterStudentFixtures.studentsWithEachStatus[1].studentId,
      );
      expect(
        screen.queryByTestId(`${rsTestId}-cell-row-1-col-studentId`),
      ).not.toBeInTheDocument();
    });
  });

  test("purge button is disabled when there are no dropped students", async () => {
    axiosMock
      .onGet("/api/rosterstudents/course/1")
      .reply(200, rosterStudentFixtures.threeStudents);

    render(
      <QueryClientProvider client={queryClient}>
        <EnrollmentTabComponent
          courseId={1}
          testIdPrefix={testId}
          currentUser={currentUserFixtures.instructorUser}
        />
      </QueryClientProvider>,
    );

    await screen.findByTestId(`${testId}-RosterStudentTable-cell-row-0-col-id`);
    const purgeButton = screen.getByTestId(`${testId}-purge-dropped-button`);
    expect(purgeButton).toHaveTextContent("Purge All Dropped Students");
    expect(purgeButton).toHaveClass("btn-danger");
    expect(purgeButton).toBeDisabled();
    expect(
      screen.queryByTestId("PurgeDroppedStudentsModal"),
    ).not.toBeInTheDocument();
  });

  test("purge flow sends DELETE with chosen option, toasts, and refetches", async () => {
    axiosMock
      .onGet("/api/rosterstudents/course/1")
      .reply(200, rosterStudentFixtures.fourStudentsOneDropped);
    axiosMock.onDelete("/api/rosterstudents/purgeDropped").reply(200, {
      deleted: 1,
      removedFromOrg: 1,
      orgRemovalErrors: [],
    });

    render(
      <QueryClientProvider client={queryClient}>
        <EnrollmentTabComponent
          courseId={1}
          testIdPrefix={testId}
          currentUser={currentUserFixtures.instructorUser}
        />
      </QueryClientProvider>,
    );

    const purgeButton = await screen.findByTestId(
      `${testId}-purge-dropped-button`,
    );
    await waitFor(() => expect(purgeButton).not.toBeDisabled());
    fireEvent.click(purgeButton);

    await screen.findByTestId("PurgeDroppedStudentsModal");
    expect(
      screen.getByTestId("PurgeDroppedStudentsModal-message"),
    ).toHaveTextContent("delete all 1 dropped student(s)");
    fireEvent.click(
      screen.getByLabelText(
        "Yes, I'd like to remove them from the GitHub Organization",
      ),
    );
    fireEvent.click(screen.getByTestId("PurgeDroppedStudentsModal-submit"));

    await waitFor(() => expect(axiosMock.history.delete.length).toEqual(1));
    expect(axiosMock.history.delete[0].url).toBe(
      "/api/rosterstudents/purgeDropped",
    );
    expect(axiosMock.history.delete[0].params).toEqual({
      courseId: 1,
      removeFromOrg: "true",
    });
    await waitFor(() =>
      expect(toast).toBeCalledWith("Purged 1 dropped student(s)."),
    );
    expect(toast.error).not.toBeCalled();
    await waitFor(() =>
      expect(
        screen.queryByTestId("PurgeDroppedStudentsModal"),
      ).not.toBeInTheDocument(),
    );
    // roster is refetched after the purge
    await waitFor(() => expect(axiosMock.history.get.length).toEqual(2));
    expect(axiosMock.history.get[1].url).toBe("/api/rosterstudents/course/1");
  });

  test("purge reports GitHub org removal errors", async () => {
    axiosMock
      .onGet("/api/rosterstudents/course/1")
      .reply(200, rosterStudentFixtures.fourStudentsOneDropped);
    axiosMock.onDelete("/api/rosterstudents/purgeDropped").reply(200, {
      deleted: 2,
      removedFromOrg: 0,
      orgRemovalErrors: ["a@ucsb.edu: boom", "b@ucsb.edu: bang"],
    });

    render(
      <QueryClientProvider client={queryClient}>
        <EnrollmentTabComponent
          courseId={1}
          testIdPrefix={testId}
          currentUser={currentUserFixtures.instructorUser}
        />
      </QueryClientProvider>,
    );

    const purgeButton = await screen.findByTestId(
      `${testId}-purge-dropped-button`,
    );
    await waitFor(() => expect(purgeButton).not.toBeDisabled());
    fireEvent.click(purgeButton);
    await screen.findByTestId("PurgeDroppedStudentsModal");
    fireEvent.click(screen.getByTestId("PurgeDroppedStudentsModal-submit"));

    await waitFor(() => expect(axiosMock.history.delete.length).toEqual(1));
    expect(axiosMock.history.delete[0].params).toEqual({
      courseId: 1,
      removeFromOrg: "false",
    });
    await waitFor(() =>
      expect(toast).toBeCalledWith("Purged 2 dropped student(s)."),
    );
    expect(toast.error).toBeCalledWith(
      "Some students could not be removed from the GitHub organization: a@ucsb.edu: boom; b@ucsb.edu: bang",
    );
  });

  test("purge modal can be closed without purging", async () => {
    axiosMock
      .onGet("/api/rosterstudents/course/1")
      .reply(200, rosterStudentFixtures.fourStudentsOneDropped);

    render(
      <QueryClientProvider client={queryClient}>
        <EnrollmentTabComponent
          courseId={1}
          testIdPrefix={testId}
          currentUser={currentUserFixtures.instructorUser}
        />
      </QueryClientProvider>,
    );

    const purgeButton = await screen.findByTestId(
      `${testId}-purge-dropped-button`,
    );
    await waitFor(() => expect(purgeButton).not.toBeDisabled());
    fireEvent.click(purgeButton);
    await screen.findByTestId("PurgeDroppedStudentsModal");
    fireEvent.click(screen.getByRole("button", { name: "Close" }));
    await waitFor(() =>
      expect(
        screen.queryByTestId("PurgeDroppedStudentsModal"),
      ).not.toBeInTheDocument(),
    );
    expect(axiosMock.history.delete.length).toEqual(0);
  });
});

describe("EnrollmentTabComponent Load Students from Canvas button", () => {
  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    queryClient.clear();
    vi.resetAllMocks();
    axiosMock
      .onGet("/api/rosterstudents/course/1")
      .reply(200, rosterStudentFixtures.threeStudents);
  });

  const renderTab = (props) =>
    render(
      <QueryClientProvider client={queryClient}>
        <EnrollmentTabComponent
          courseId={1}
          testIdPrefix={testId}
          currentUser={currentUserFixtures.instructorUser}
          {...props}
        />
      </QueryClientProvider>,
    );

  // The text of the buttons in the row of buttons at the top, from left to right
  const buttonRow = () =>
    screen.getByTestId(`${testId}-csv-button`).closest(".row");
  const buttonTexts = () =>
    within(buttonRow())
      .getAllByRole("button")
      .map((button) => button.textContent);

  const rosterRequests = () =>
    axiosMock.history.get.filter(
      (request) => request.url === "/api/rosterstudents/course/1",
    );

  test("is not shown by default, or when Canvas is not enabled", async () => {
    const { unmount } = renderTab({});
    await screen.findByTestId(`${testId}-csv-button`);

    expect(
      screen.queryByTestId(`${testId}-canvas-sync-button`),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByText("Load Students from Canvas"),
    ).not.toBeInTheDocument();
    expect(buttonTexts()).toEqual([
      "Upload CSV Roster",
      "Add Individual Student",
      "Download Student CSV",
    ]);
    expect(buttonRow()).toHaveClass("row-cols-sm-3");
    expect(buttonRow()).not.toHaveClass("row-cols-sm-4");
    unmount();

    renderTab({ canvasEnabled: false });
    await screen.findByTestId(`${testId}-csv-button`);
    expect(
      screen.queryByTestId(`${testId}-canvas-sync-button`),
    ).not.toBeInTheDocument();
  });

  test("is shown second from the left, between Upload CSV Roster and Add Individual Student, when Canvas is enabled", async () => {
    renderTab({ canvasEnabled: true });

    const button = await screen.findByTestId(`${testId}-canvas-sync-button`);
    expect(button).toHaveTextContent("Load Students from Canvas");
    expect(button).toHaveClass("w-100");
    expect(buttonTexts()).toEqual([
      "Upload CSV Roster",
      "Load Students from Canvas",
      "Add Individual Student",
      "Download Student CSV",
    ]);
    expect(buttonRow()).toHaveClass("row-cols-sm-4");
    expect(buttonRow()).not.toHaveClass("row-cols-sm-3");
    expect(axiosMock.history.post.length).toBe(0);
  });

  test("POSTs to the Canvas sync endpoint for the current course, then refreshes the roster and clears the search", async () => {
    axiosMock
      .onPost("/api/courses/canvas/sync/students")
      .reply(200, loadResultFixtures.successful);

    renderTab({ canvasEnabled: true });

    const search = await screen.findByTestId(`${testId}-search`);
    fireEvent.change(search, { target: { value: "no such student" } });
    expect(search).toHaveValue("no such student");
    await waitFor(() => expect(rosterRequests().length).toBe(1));

    fireEvent.click(screen.getByTestId(`${testId}-canvas-sync-button`));
    fireEvent.click(await screen.findByText("Yes, I'd like to do this"));

    await waitFor(() =>
      expect(toast).toHaveBeenCalledWith("Roster successfully updated."),
    );
    expect(toast).toHaveBeenCalledTimes(1);
    expect(toast.error).not.toHaveBeenCalled();
    expect(axiosMock.history.post.length).toBe(1);
    expect(axiosMock.history.post[0].url).toBe(
      "/api/courses/canvas/sync/students",
    );
    expect(axiosMock.history.post[0].params).toEqual({ courseId: 1 });
    expect(search).toHaveValue("");
    // the roster is fetched again, to show the students that were loaded
    await waitFor(() => expect(rosterRequests().length).toBe(2));
  });

  test("asks for confirmation, explaining the consequences, before calling the backend", async () => {
    axiosMock
      .onPost("/api/courses/canvas/sync/students")
      .reply(200, loadResultFixtures.successful);

    renderTab({ canvasEnabled: true });
    const button = await screen.findByTestId(`${testId}-canvas-sync-button`);

    // no dialog until the button is clicked
    expect(
      screen.queryByTestId(`${testId}-canvas-sync-confirmation-message`),
    ).not.toBeInTheDocument();
    expect(screen.queryByText("Are You Sure?")).not.toBeInTheDocument();

    fireEvent.click(button);

    const message = await screen.findByTestId(
      `${testId}-canvas-sync-confirmation-message`,
    );
    expect(screen.getByText("Are You Sure?")).toBeInTheDocument();
    expect(message.textContent).toBe(
      "This adds the students enrolled in the Canvas course to the roster, and updates the ones that are already on it, including their section." +
        "Students who are not in the Canvas course will be marked as dropped, and removed from the GitHub organization of this course. This applies to students who were loaded from a CSV file or from Canvas; students who were added individually are not affected." +
        "Before going ahead, make sure that the Canvas course ID on the Settings tab is the right one.",
    );
    expect(message.querySelector("strong")).toHaveTextContent(
      "Students who are not in the Canvas course will be marked as dropped, and removed from the GitHub organization of this course.",
    );
    expect(message.lastElementChild).toHaveClass("mb-0");
    // nothing has been sent yet
    expect(axiosMock.history.post.length).toBe(0);

    fireEvent.click(screen.getByText("Yes, I'd like to do this"));

    await waitFor(() => expect(axiosMock.history.post.length).toBe(1));
    await waitFor(() =>
      expect(
        screen.queryByTestId(`${testId}-canvas-sync-confirmation-message`),
      ).not.toBeInTheDocument(),
    );
  });

  test("does not call the backend if the confirmation is declined or dismissed", async () => {
    renderTab({ canvasEnabled: true });
    const button = await screen.findByTestId(`${testId}-canvas-sync-button`);

    fireEvent.click(button);
    await screen.findByTestId(`${testId}-canvas-sync-confirmation-message`);
    fireEvent.click(screen.getByText("No, take me back"));
    await waitFor(() =>
      expect(
        screen.queryByTestId(`${testId}-canvas-sync-confirmation-message`),
      ).not.toBeInTheDocument(),
    );

    fireEvent.click(button);
    await screen.findByTestId(`${testId}-canvas-sync-confirmation-message`);
    fireEvent.click(screen.getByTestId("ConfirmationModal-closeButton"));
    await waitFor(() =>
      expect(
        screen.queryByTestId(`${testId}-canvas-sync-confirmation-message`),
      ).not.toBeInTheDocument(),
    );

    expect(axiosMock.history.post.length).toBe(0);
    expect(toast).not.toHaveBeenCalled();
    expect(toast.error).not.toHaveBeenCalled();
  });

  test("shows the error from the backend if loading from Canvas fails", async () => {
    axiosMock.onPost("/api/courses/canvas/sync/students").reply(400, {
      type: "IllegalArgumentException",
      message: "Course is not linked to a Canvas course",
    });

    renderTab({ canvasEnabled: true });
    const search = await screen.findByTestId(`${testId}-search`);
    fireEvent.change(search, { target: { value: "Chris" } });

    fireEvent.click(screen.getByTestId(`${testId}-canvas-sync-button`));
    fireEvent.click(await screen.findByText("Yes, I'd like to do this"));

    await waitFor(() => expect(toast.error).toHaveBeenCalledTimes(1));
    expect(toast.error).toHaveBeenCalledWith(
      `Error loading students from Canvas: ${JSON.stringify(
        {
          type: "IllegalArgumentException",
          message: "Course is not linked to a Canvas course",
        },
        null,
        2,
      )}`,
    );
    expect(toast).not.toHaveBeenCalled();
    // the search is only cleared when the roster was updated
    expect(search).toHaveValue("Chris");
  });
});
