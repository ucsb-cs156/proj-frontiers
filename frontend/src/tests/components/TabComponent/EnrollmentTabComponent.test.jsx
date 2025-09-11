import { fireEvent, render, screen, waitFor } from "@testing-library/react";
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
    expect(screen.getByTestId(`${testId}-csv-error-modal`)).toHaveClass(
      "modal-dialog modal-dialog-centered",
    );
    expect(screen.queryByTestId(`${testId}-csv-modal`)).not.toBeInTheDocument();
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
    fireEvent.click(screen.getByTestId("RosterStudentForm-submit"));
    await waitFor(() => expect(axiosMock.history.post.length).toEqual(1));
    expect(axiosMock.history.post[0].params).toEqual({
      courseId: 7,
      studentId: "123456789",
      firstName: "Chris",
      lastName: "Gaucho",
      email: "cgaucho@ucsb.edu",
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
});
